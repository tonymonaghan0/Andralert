package com.mcssoftware.andralert;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import static com.mcssoftware.andralert.MainActivity.phoneNumber;

public class TestAlertsActivity extends AppCompatActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);

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
    public static void callNumber(Context mContext) {
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

}

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

