package com.mcssoftware.andralert;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;

import static com.mcssoftware.andralert.MainActivity.enableCall;
import static com.mcssoftware.andralert.MainActivity.enablePhotos;
import static com.mcssoftware.andralert.MainActivity.enableVideo;
import static com.mcssoftware.andralert.MainActivity.noiseThreshold;
import static com.mcssoftware.andralert.MainActivity.enableSMS;

public class AudioAlertService extends Service {
    public AudioAlertService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //Used to pass context to static methods in TestAlertsActivity
    Context mContext = this;

    private MediaRecorder mRecorder = null;
    private static final int POLL_INTERVAL = 300;
    private int HitCount =0;
    //Handler for audio sample rate
    private final Handler handler = new Handler(Looper.getMainLooper());
    //Handler for delay till dialler stops in endCall
    private final Handler handler2 = new Handler(Looper.getMainLooper());
    //Handler for rearmDelay after alarm has triggered
    private final Handler handler3 = new Handler(Looper.getMainLooper());

    private final Runnable PollTask = new Runnable() {
        public void run() {
            double amp = getAmplitude();
            if ((amp > noiseThreshold)) {
                HitCount++;
                if (HitCount > 10){

                    //Send SMS
                    if (enableSMS){
                        TestAlertsActivity.sendSmsByManager("Alarm - Audio alert triggered");
                    }

                    //Call phone number in preferences
                    if (enableCall) {

                        //Call phone number in preferences
                        TestAlertsActivity.callNumber(mContext);

                        //End call after delay NB don't make it too small 15 will ring for about 4s
                        int callEndDelay = 15;
                        endCall(callEndDelay);
                    }

                    //Start CameraService overrides VideoService
                    if (enablePhotos){
                        Intent svc = new Intent(getBaseContext(), CameraService.class);
                        startService(svc);
                    }else{
                        //Start VideoService
                        if (enableVideo){
                            Intent svc = new Intent(getBaseContext(), VideoService.class);
                            startService(svc);
                        }
                    }


                    //Stop audio monitor
                    stopAudioMonitor();

                    //re-arming delay
                    int rearmDelay = 120000;
                    handler3.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Start audio monitor after delay
                            Toast.makeText(mContext, "Audiomonitor re-armed", Toast.LENGTH_LONG).show();
                            startAudioMonitor();
                        }
                    }, rearmDelay);
                    return;
                }
            }
            handler.postDelayed(PollTask, POLL_INTERVAL);
        }
    };



    @Override
    public void onCreate() {

        super.onCreate();
        Toast.makeText(this, "Audio alert service started", Toast.LENGTH_LONG).show();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "AUDIO")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Andralert")
                .setContentText("Audio Alert")
                .setContentIntent(pendingIntent).build();

        if (Build.VERSION.SDK_INT >= 27)
            startMyOwnForeground();
        else
            startForeground(2, notification);
            startAudioMonitor();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.mcssoftware.andralert";
        String channelName = "Background Audio Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        //chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);

        startAudioMonitor();

    }

    void startAudioMonitor() {

        HitCount = 0;

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
                mRecorder.start();
            } catch (IllegalStateException | IOException e) {
                e.printStackTrace();
            }
        }
        handler.postDelayed(PollTask, POLL_INTERVAL);
    }

    void stopAudioMonitor() {

        handler.removeCallbacks(PollTask);
        if (mRecorder != null)
        {
            try {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        //stopSelf();
    }

    double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude()/2700.0);
        else
            return 0;

    }

    public void onDestroy() {
        stopAudioMonitor();
        Toast.makeText(this, "Audio alert service destroyed", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    private void endCall(int delay) {
        //delay in approximate seconds??? is converted to milliseconds
        delay = delay*1000;
        handler2.postDelayed(new Runnable() {
            @SuppressLint({"DiscouragedPrivateApi", "SoonBlockedPrivateApi"})
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 28) {
                    final TelecomManager telecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
                    if (telecomManager != null && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.endCall();
                    }
                }else {
                    TelephonyManager tm;
                    tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    Method m1 = null;
                    try {
                        //if (Build.VERSION.SDK_INT < 30) {
                            m1 = tm.getClass().getDeclaredMethod("getITelephony");
                        //}
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    assert m1 != null;
                    m1.setAccessible(true);
                    Object iTelephony = null;
                    try {
                        iTelephony = m1.invoke(tm);
                    //} catch (IllegalAccessException | InvocationTargetException e) {
                    } catch(Exception e) { //All exceptions are caught here as all are inheriting java.lang.Exception
                        e.printStackTrace();
                    }

                    Method m3 = null;
                    try {
                        assert iTelephony != null;
                        m3 = iTelephony.getClass().getDeclaredMethod("endCall");
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    try {
                        assert m3 != null;
                        m3.invoke(iTelephony);
                    //} catch (IllegalAccessException | InvocationTargetException e) {
                    } catch(Exception e) { //All exceptions are caught here as all are inheriting java.lang.Exception
                        e.printStackTrace();
                    }
                }
            }
        }, delay);
    }


}



//Send SMS
/*                if (enableSMS){
                    TestAlertsActivity.sendSmsByManager("Alarm - Audio alert triggered");
                }*/

/*
    //Check if internet available
    public boolean isInternetAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

 */

/*    //Start CameraService
    Intent svc = new Intent(getBaseContext(), CameraService.class);
    startService(svc);*/

/*
@SuppressLint("PrivateApi")
public static boolean endCall(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            telecomManager.endCall();
            return true;
        }
        return false;
    }
    //use unofficial API for older Android versions, as written here: https://stackoverflow.com/a/8380418/878126
    try {
        final Class<?> telephonyClass = Class.forName("com.android.internal.telephony.ITelephony");
        final Class<?> telephonyStubClass = telephonyClass.getClasses()[0];
        final Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
        final Class<?> serviceManagerNativeClass = Class.forName("android.os.ServiceManagerNative");
        final Method getService = serviceManagerClass.getMethod("getService", String.class);
        final Method tempInterfaceMethod = serviceManagerNativeClass.getMethod("asInterface", IBinder.class);
        final Binder tmpBinder = new Binder();
        tmpBinder.attachInterface(null, "fake");
        final Object serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
        final IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
        final Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);
        final Object telephonyObject = serviceMethod.invoke(null, retbinder);
        final Method telephonyEndCall = telephonyClass.getMethod("endCall");
        telephonyEndCall.invoke(telephonyObject);
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        LogManager.e(e);
    }
    return false;
}
 */

/*    private void wakeupScreen() {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "AudioAlertService: FULL WAKE LOCK");
                    fullWakeLock.acquire(); // turn on
                    try {
                        Thread.sleep(10000); // turn on duration
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    fullWakeLock.release();
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        }.execute();
    }
        */