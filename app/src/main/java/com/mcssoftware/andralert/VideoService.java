package com.mcssoftware.andralert;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class VideoService extends Service
{
    private static final String TAG = "VideoService";

    public VideoService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //a variable to control the camera
    public static Camera mCamera;   // Has to be static, as if orientation changed
                                    // onDestroy() destroys it?
    public MediaRecorder mediaRecorder;
    CountDownTimer timer;
    public int counter = 0;

    @Override
    public void onCreate()
    {
        mCamera = getCameraInstance();
        if (mCamera == null){
            Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
            stopSelf();
        }

        mediaRecorder = new MediaRecorder();

        super.onCreate();
        Toast.makeText(this, "Video service started", Toast.LENGTH_LONG).show();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "AUDIO")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Andralert")
                .setContentText("Capture Video")
                .setContentIntent(pendingIntent).build();

        startForeground(4, notification);

        captureVideo();

        //Set recording time, 10000 is 10s
        timer = new CountDownTimer(10000, 1000){
            public void onTick(long millisUntilFinished){
                counter++;
            }
            public  void onFinish(){
                counter = 0;
                mediaRecorder.stop();  // stop the recording
                stopSelf();
            }
        };
        timer.start();
    }

    public void captureVideo(){
        int texName = 1;    //Not sure what thia value should be?
        SurfaceTexture st = new SurfaceTexture(texName);
        try {
            mCamera.setPreviewTexture(st);

            //the camera parameters
            Parameters parameters = mCamera.getParameters();
            //Turn on flash
            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
            // set the focus mode
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //set camera parameters
            mCamera.setParameters(parameters);

            // Step 1: Unlock and set camera to MediaRecorder
            mCamera.unlock();

            mediaRecorder.setCamera(mCamera);

            // Step 2: Set sources
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

            // Step 4: Set output file
            mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/Video" + ".mp4");

            mediaRecorder.prepare();
            mediaRecorder.start();

        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }
    }

    public void onDestroy() {
        releaseMediaRecorder();
        releaseCamera();
        Toast.makeText(this, "Video service destroyed", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance of first
                                //back facing camera. Use  Camera.open(int) for other cameras
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
}

