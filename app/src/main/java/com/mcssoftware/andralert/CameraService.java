package com.mcssoftware.andralert;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class CameraService extends Service
{
    public CameraService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //Used to increment number on image file and set number of pictures to take
    static int x=0;
    //a variable to control the camera
    public static Camera mCamera;   // Has to be static, as if orientation changed
                                    // onDestroy() destroys it?

    @Override
    public void onCreate()
    {
        super.onCreate();
        Toast.makeText(this, "Camera service started", Toast.LENGTH_LONG).show();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "AUDIO")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Andralert")
                .setContentText("Capture Image")
                .setContentIntent(pendingIntent).build();

        startForeground(3, notification);

        captureImage();

    }

    public void captureImage(){

        mCamera = Camera.open();
        //SurfaceView sv = new SurfaceView(getApplicationContext());
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
            mCamera.startPreview();
            mCamera.takePicture(null, null, mCall);
        } catch (IOException e) {
            Log.d("CAMERA captureImage", e.getMessage());
        }
    }



    Camera.PictureCallback mCall = new Camera.PictureCallback()
    {

        public void onPictureTaken(byte[] data, Camera camera)
        {
            FileOutputStream outStream;
            try{
                x++;
                outStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Image" + x + ".jpg");
                outStream.write(data);
                outStream.close();
                releaseCamera();
                //Take 5 pictures only
                if (x >= 5) {
                    x=0;
                    stopSelf();
                }
                //Call captureImage() X times
                captureImage();
            } catch (IOException e){
                Log.d("CAMERA onPictureTaken", e.getMessage());
            }

        }
    };

    public void onDestroy() {
        releaseCamera();
        Toast.makeText(this, "Camera service destroyed", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
}
