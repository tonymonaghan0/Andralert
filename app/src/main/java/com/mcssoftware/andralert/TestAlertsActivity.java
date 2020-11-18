package com.mcssoftware.andralert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;

import static com.mcssoftware.andralert.MainActivity.enableCall;
import static com.mcssoftware.andralert.MainActivity.enablePhotos;
import static com.mcssoftware.andralert.MainActivity.enableSMS;
import static com.mcssoftware.andralert.MainActivity.enableVideo;
import static com.mcssoftware.andralert.MainActivity.phoneNumber;



public class TestAlertsActivity extends AppCompatActivity  {

    //Used to pass context to static methods in TestAlertsActivity
    Context mContext = this;
    //Handler for delay till dialler stops in endCall
    private final Handler handler = new Handler(Looper.getMainLooper());
    public String alertType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            alertType = bundle.getString("alertType");
            startAlerts(mContext, alertType);
        }

        //Add back arrow to action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button button1 = findViewById(R.id.button1);
        //Needed to pass context to callNumber
        final Context mContext = this;
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callNumber(mContext);
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String smsBody = "Alarm test";
                sendSmsByManager(smsBody);
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAlerts(mContext, "Test alert");
            }
        });

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //Handles back arrow pressed
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    //Call number immediately, call a non-static method from a static method
    public void callNumber(Context mContext) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    //Will send sms immediately to number
    public static void sendSmsByManager(String smsBody) {
        try {
            // Get the default instance of the SmsManager
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null,
                    smsBody, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endCall(int delay) {
        //delay in approximate seconds??? is converted to milliseconds
        delay = delay*1000;
        handler.postDelayed(new Runnable() {
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
            //Finish TestAlertsActivity
            finish();
            }
        }, delay);
    }

    public void startAlerts(Context context, String alertType){
        //Send SMS
        if (enableSMS){
            sendSmsByManager(alertType);
        }

        //Call phone number in preferences
        if (enableCall) {

            //Call phone number in preferences
            callNumber(context);

            //End call after delay. NB don't make it too small 20 will ring for about 10s
            int callEndDelay = 20 ;
            endCall(callEndDelay);
        }

//TODO Check services starting and stopping OK
        //Start CameraService overrides VideoService
        if (enablePhotos){
            Intent svc = new Intent(getBaseContext(), CameraService.class);
            startService(svc);
            //Finish TestAlertsActivity
            finish();
        }else{
            //Start VideoService
            if (enableVideo){
                //Intent svc = new Intent(getBaseContext(), VideoService.class);
                Intent svc = new Intent(this, VideoService.class);
                startService(svc);
                //Finish TestAlertsActivity
                finish();
            }
        }
    }

}


//Call number immediately, call a non-static method from a static method
/*    public static void callNumber(Context mContext) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }*/

//public class TestAlertsActivity extends AppCompatActivity implements OnClickListener {

//button1.setText("Call Now");
//button1.setOnClickListener(this);

//button2.setText("Text Now");
//button2.setOnClickListener(this);

/*
    //Method to populate the data arrays for the button names
    public void populateArrays() {
        buttonLabels[0] = "Dialler";
        buttonLabels[1] = "Call Now";
        buttonLabels[2] = "Show Text App";
        buttonLabels[3] = "Text Now";
    }*/
/*

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.button1:
                //will dial number immediately
                callNumber(this);
                break;

            case R.id.button2:
                //will send sms immediately to number
                String smsBody = "Alarm test";
                sendSmsByManager(smsBody);
                break;
        }
    }*/


/*    //will open text application with number and message
    public void textNumber() {
        String numberToText = "smsto:" + phoneNumber;
        String sms = "Alarm test";
        // Create the intent.
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        // Set the data for the intent as the phone number.
        smsIntent.setData(Uri.parse(numberToText));
        // Add the message (sms) with the key ("sms_body").
        smsIntent.putExtra("sms_body", sms);
        // If package resolves (target app installed), send intent.
        if (smsIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(smsIntent);
        } else {
            Log.d(TAG, "Can't resolve app for ACTION_SENDTO Intent");
        }
    }*/

/*    // Launch the phone dialer with number
    public static void launchDialer(Context mContext) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }*/

//private String[] phoneNum;

//public String TAG = "ERROR";

/*
<Button
        android:id="@+id/button3"
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:textSize="18sp" />

<Button
        android:id="@+id/button4"
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:textSize="18sp" />*/

/*
    Button button3 = findViewById(R.id.button3);
        button3.setText(buttonLabels[2]);
                button3.setOnClickListener(this);

                Button button4 = findViewById(R.id.button4);
                button4.setText(buttonLabels[3]);
                button4.setOnClickListener(this);*/

/*            case R.id.button1:
                //will open dialler with number
                launchDialer(this);
                break;*/
/*            case R.id.button3:
                //will open text application with number
                textNumber();
                break;*/


//int entries = 4;
//private String[] buttonLabels;

//buttonLabels = new String[entries];

/*        // Populate the data arrays
        populateArrays();*/

// Set up buttons and attach click listeners

