package com.mcssoftware.andralert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class SensorAlertService extends Service implements SensorEventListener {
    public SensorAlertService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    float last_x, last_y, last_z;
    double lastUpdate, lastSpeed, curTime;
    boolean moving;
    SensorManager SM;
    Sensor mySensor;

    //Used to pass context to static methods in TestAlertsActivity
    Context mContext = this;

    //Handler for rearmDelay after alarm has triggered
    private final Handler handler1 = new Handler(Looper.getMainLooper());


    //Used to test for Shake
/*    private boolean init;
    private float x1, x2, x3;
    private static final float ERROR = (float) 7.0;*/
/*
    private static final float SHAKE_THRESHOLD = 15.00f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 500;
    private long mLastShakeTime;*/


    @Override
    public void onCreate() {

        lastUpdate = 0;
        last_x = 0;
        last_y = 0;
        last_z = 0;
        lastSpeed = 0;
        moving = false;


        super.onCreate();
        Toast.makeText(this, "Sensor alert service started", Toast.LENGTH_LONG).show();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "AUDIO")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Andralert")
                .setContentText("Srnsor Alert")
                .setContentIntent(pendingIntent).build();

        if (Build.VERSION.SDK_INT >= 27)
            startMyOwnForeground();
        else
            startForeground(5, notification);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.mcssoftware.andralert";
        String channelName = "Background Sensor Service";
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

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!moving){
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

    /*            //Test for shake
                curTime = System.currentTimeMillis();
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double acceleration = Math.sqrt(Math.pow(x, 2) +
                            Math.pow(y, 2) +
                            Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;
                    //Log.d("mySensor", "Acceleration is " + acceleration + "m/s^2");

                    if (acceleration > SHAKE_THRESHOLD) {
                        mLastShakeTime = curTime;
                        Toast.makeText(getApplicationContext(), "Shake Detected",
                                Toast.LENGTH_LONG).show();
                    }
                }*/

                //Test for movement
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                curTime = System.currentTimeMillis();
                if ((curTime - lastUpdate) > 100) {
                    double diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;
                    //Movement algorithm
                    double speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                    if (speed > 40 && lastSpeed > 40) {
                        moving = true;
                        Toast.makeText(getApplicationContext(), "Movement Detected",
                                Toast.LENGTH_LONG).show();
                        triggerAlerts();

                        //re-arming delay
                        int rearmDelay = 120000;
                        handler1.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Start audio monitor after delay
                                Toast.makeText(mContext, "Sensor monitor re-enabled", Toast.LENGTH_LONG).show();
                                moving = false;
                            }
                        }, rearmDelay);

                    } else {
                        moving = false;
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                    lastSpeed = speed;
                }
            }
        }
    }

    public void triggerAlerts(){
        Toast.makeText(getApplicationContext(), "Sensor Movement Detected",
                Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, TestAlertsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("alertType","Sensor Movement Alert");
        startActivity(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
        return Service.START_STICKY;
    }

    public void onDestroy() {
        //Turn off sensor listener
        SM.unregisterListener(this);
        Toast.makeText(this, "Sensor alert service destroyed", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }
}




//Turn off sensor listener
//SM.unregisterListener(this);

//Turn on sensor listener, after a delay?
//SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

/*                     Intent intent = new Intent(this, TestAlertsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("alertType","Sensor Movement Alert");
                    startActivity(intent);

                   TestAlertsActivity tA = new TestAlertsActivity();
                    tA.startAlerts(mContext, "Sensor Movement Alert");*/

//Toast.makeText(this, "Start Sensor Detection", Toast.LENGTH_LONG).show();
//SensorManager SM = (SensorManager) getSystemService(SENSOR_SERVICE);
//Sensor mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

/*   public void onSensorChanged(SensorEvent sensorevent) {

        //TextView motion_dat = findViewById(R.id.motion_text);
        //TextView accelerometer_dat = findViewById(R.id.accel_reading);
        //Sensor mySensor = sensorevent.sensor;
        double x = sensorevent.values[0];
        double y = sensorevent.values[1];
        double z = sensorevent.values[2];
        double curTime = System.currentTimeMillis();
        //accelerometer_dat.setText(getString(R.string.gyro_string, x, y, z));
        if ((curTime - lastUpdate) > 100) {
            double diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;
            double speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;  //algorithim

            if (speed > 40 && lastSpeed > 40) {
                motion_dat.setText(R.string.moving);
                moving = true;
            } else {
                motion_dat.setText(R.string.not_moving);
                moving = false;
            }
            last_x = x;
            last_y = y;
            last_z = z;
            lastSpeed = speed;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }*/	
	
	