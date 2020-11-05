/*TODO
   Works on API 16 Android 4.1
   ---------------------------
   Sort problems with android 10 SDK 29
        Won't allow service to start dialler if screen off
   Add video recording time?  and time lapse option? to preferences
   Text message status, start app? turn alarm on and off
   Add movement/vibration detection?
   Add Motion detection?
   Use Twitter or similar for alerts? If internet available
   Upload data to Google Drive?
 */

package com.mcssoftware.andralert;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity{

    //Auto start at phone boot and check power connection
    public static class BootUpReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case Intent.ACTION_POWER_DISCONNECTED:
                    if (enableSMS){
                        TestAlertsActivity.sendSmsByManager("Alarm - power disconnected");
                    }
                    break;
                case Intent.ACTION_POWER_CONNECTED:
                    if (enableSMS) {
                        TestAlertsActivity.sendSmsByManager("Alarm - power connected");
                    }
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    //Start MainActivity
                    Intent i = new Intent(context, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //Tell MainActivity it is being called from ACTION_BOOT_COMPLETED
                    //i.putExtra("boot","true");
                    i.putExtra("StartOnBoot",true);
                    context.startActivity(i);
                    break;
            }
        }
    }

    /** Settings from preferences activity **/
    public static int delay;
    public static int noiseThreshold;
    public static String phoneNumber;
    public static boolean enableSMS;
    public static boolean enableCall;
    public static boolean enablePhotos;
    public static boolean enableVideo;
    public static boolean enableBoot;
    public static boolean enableAutoActivate;

    Button btnActivate;
    public boolean blnAlarmOn = false;
    CountDownTimer timer;
    public int counter = 0;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        readApplicationPreferences();

        //Check if MainActivity called from ACTION_BOOT_COMPLETED
        boolean boot;
        try {
            //Check if intent has extras
            if (getIntent().hasExtra("StartOnBoot")) {
                boot = getIntent().getExtras().getBoolean("StartOnBoot");
            }else{
                boot = false;
            }
        } catch (NullPointerException e ) {
            boot = false;
            e.printStackTrace();
        }
        //If called from ACTION_BOOT_COMPLETED finish if enableBoot setting is false
        if (boot) {
            //Finish if enableBoot is false
            if (!enableBoot) {
                finish();
            }
        }

        //Stops the FileUriExposedException Error on Android v7 and above
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.ANSWER_PHONE_CALLS,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.WAKE_LOCK,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.FOREGROUND_SERVICE
        };

        // Call for runtime permissions on API>22
        if (Build.VERSION.SDK_INT > 22) {
            //hasPermissions is a helper to check all permissions are granted
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

/*        //Set to landscape to stop refresh on resume and get landscape photos and videos
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/

        //Set to portrait to stop refresh on resume
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Activate/Stop button
        btnActivate = findViewById(R.id.btn_activate);
        btnActivate.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if ((blnAlarmOn) || (counter != 0)) {
                    stopAlarm();
                } else {
                    activateAlarm();
                }
            }
        });

        //Activate alarm at start up if option set.
        if (enableAutoActivate){
            activateAlarm();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Toast.makeText(this, "Settings Pressed", Toast.LENGTH_LONG).show();
            Intent prefs = new Intent(this, Preferences.class);
            startActivity(prefs);

        } else if (id == R.id.action_test) {
            startActivity(new Intent(MainActivity.this, TestAlertsActivity.class));
            return true;

        } else if (id == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //hasPermissions is a helper to check all permissions are granted
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void readApplicationPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        noiseThreshold = Integer.parseInt(prefs.getString("threshold", "6"));
        delay = Integer.parseInt(prefs.getString("activation_delay", "30000"));
        enableCall = prefs.getBoolean("enable_call", true);
        enableSMS = prefs.getBoolean("enable_sms", false);
        enablePhotos = prefs.getBoolean("enable_photos", false);
        enableVideo = prefs.getBoolean("enable_video", false);
        enableBoot = prefs.getBoolean("enable_boot", false);
        enableAutoActivate = prefs.getBoolean("enable_boot", false);
        phoneNumber = prefs.getString("alert_phone_number", "00447951991772");
    }

    @Override
    public void onResume() {
        super.onResume();
        readApplicationPreferences();
    }

    public void stopAlarm(){

        //Press STOP and set to ACTIVATE
        btnActivate.setText(R.string.activate);
        blnAlarmOn = false;
        if (timer != null){
            timer.cancel();
            counter = 0;
        }

        //Stop AudioAlertService if running
        if(isMyServiceRunning(AudioAlertService.class)){
            Intent svc = new Intent(getBaseContext(), AudioAlertService.class);
            stopService(svc);
        }

        //Stop CameraService if running
        if(isMyServiceRunning(CameraService.class)){
            Intent svc = new Intent(getBaseContext(), CameraService.class);
            stopService(svc);
        }

        //Stop VideoService if running
        if(isMyServiceRunning(VideoService.class)){
            Intent svc = new Intent(getBaseContext(), VideoService.class);
            stopService(svc);
        }
    }

    public void activateAlarm(){

        //Press ACTIVATE and set to STOP
        timer = new CountDownTimer(delay, 1000){
            public void onTick(long millisUntilFinished){
                btnActivate.setText(String.valueOf(delay /1000 - counter));
                counter++;
            }
            public  void onFinish(){
                counter = 0;
                btnActivate.setText(R.string.stop);
                blnAlarmOn = true;
                //Start AudioAlertService
                Intent svc = new Intent(getBaseContext(), AudioAlertService.class);
                startService(svc);
            }
        };
        timer.start();
    }

    //Check if a service is running
    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}