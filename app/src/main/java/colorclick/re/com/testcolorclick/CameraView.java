package colorclick.re.com.testcolorclick;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

// ----------------------------------------------------------------------

public class CameraView extends Activity {
    private Preview mPreview;
    private ColorUtils colorDictionary;
    private boolean flashlighton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        colorDictionary = new ColorUtils();
        flashlighton = false;

        // Create our Preview view and set it as the content of our activity.
        mPreview = new Preview(this);
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int x = (int) (mPreview.getPreviewHeight() * event.getX() / mPreview.getWidth());
                int y = (int) (mPreview.getPreviewWidth() * event.getY() / mPreview.getHeight());
                int pixel = mPreview.getPixels()[mPreview.getPreviewWidth() * (mPreview.getPreviewHeight() - x) + y];

                //then do what you want with the pixel data, e.g
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                //Toast toast = Toast.makeText(getBaseContext(), colorDictionary.getColorNameFromRgb(r, g, b) + ", " + r + ", " + g + ", " + b, Toast.LENGTH_SHORT);
                //toast.show();
                TextView textView = (TextView) findViewById(R.id.color_textview);
                textView.setText(colorDictionary.getColorNameFromRgb(r, g, b) + ", " + r + ", " + g + ", " + b);

                ImageView imageView = (ImageView) findViewById(R.id.colorrectimage);
                imageView.setBackgroundColor(Color.rgb(r, g, b));

                return false;
            }
        });

        setContentView(R.layout.activity_camera);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.camera_preview);
        frameLayout.addView(mPreview);

        RelativeLayout relativeLayoutSensorsData = (RelativeLayout) findViewById(R.id.color_text);
        relativeLayoutSensorsData.bringToFront();

        Button button = (Button) findViewById(R.id.flashlight);
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flashlighton) {
                    flashLightOff();
                    flashlighton = false;
                } else {
                    flashLightOn();
                    flashlighton = true;
                }
            }
        });
    }

    public void flashLightOn() {

        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                Camera.Parameters p = mPreview.mCamera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mPreview.mCamera.setParameters(p);
                //mPreview.mCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOn()",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void flashLightOff() {
        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                Camera.Parameters p = mPreview.mCamera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mPreview.mCamera.setParameters(p);
                //mPreview.mCamera.stopPreview();
                //mPreview.mCamera.release();
                //mPreview.mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOff",
                    Toast.LENGTH_SHORT).show();
        }
    }

}

// ----------------------------------------------------------------------

class Preview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    SurfaceHolder mHolder;
    Camera mCamera;
    float mDist;

    //This variable is responsible for getting and setting the camera settings
    private Camera.Parameters parameters;
    //this variable stores the camera preview size
    private Camera.Size previewSize;
    //this array stores the pixels as hexadecimal pairs
    private int[] pixels;

    Preview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID
        Camera.Parameters params = mCamera.getParameters();
        int action = event.getAction();


        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                mCamera.cancelAutoFocus();
                handleZoom(event, params);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params);
            }
        }
        return true;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
            {
                zoom = zoom + maxZoom / 10;

                //clip to maxzoom after incrementing
                zoom = zoom > maxZoom ? maxZoom : zoom;
            }
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0) {
                zoom = zoom - maxZoom / 10;

                //clip to zero after decrementing
                zoom = zoom < 0 ? 0 : zoom;
            }
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);

        try {
            mCamera.setPreviewDisplay(surfaceHolder);

            //sets the camera callback to be the one defined in this class
            mCamera.setPreviewCallback(this);

            ///initialize the variables
            parameters = mCamera.getParameters();
            previewSize = parameters.getPreviewSize();
            pixels = new int[previewSize.width * previewSize.height];

        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        //before changing the application orientation, you need to stop the preview, rotate and then start it again
        if(mHolder.getSurface() == null)//check if the surface is ready to receive camera data
            return;

        try{
            mCamera.stopPreview();
        } catch (Exception e){
            //this will happen when you are trying the camera if it's not running
        }

        //now, recreate the camera preview
        try{
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            parameters.setPreviewSize(w, h);
            //set the camera's settings
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        //our app has only one screen, so we'll destroy the camera in the surface
        //if you are unsing with more screens, please move this code your activity
        mCamera.stopPreview();
        mCamera.release();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //transforms NV21 pixel data into RGB pixels
        decodeYUV420SP(pixels, data, previewSize.width, previewSize.height);
        //Outuput the value of the top left pixel in the preview to LogCat
        //System.out.println("The top right pixel has the following RGB (hexadecimal) values:"
        //        +Integer.toHexString(pixels[0]));
    }

    //Method from Ketai project! Not mine! See below...
    void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)                  r = 0;               else if (r > 262143)
                    r = 262143;
                if (g < 0)                  g = 0;               else if (g > 262143)
                    g = 262143;
                if (b < 0)                  b = 0;               else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    //getters and setters
    public int[] getPixels() { return pixels; }
    public int getPreviewWidth() { return previewSize.width; }
    public int getPreviewHeight() { return previewSize.height; }
}