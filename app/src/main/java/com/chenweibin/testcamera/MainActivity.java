package com.chenweibin.testcamera;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements PreviewCallback, Camera.AutoFocusCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Camera camera;
    private Button start, stop;
    private AutoFitSurfaceView sfv;

    private int previewFormat;

    private UdpClient socketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.start);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera = Camera.open();
                Parameters params = camera.getParameters();
                params.setRecordingHint(true);
                params.setPreviewSize(640, 480);
                printSupportFocusMode(params);
                printSupportPictureSize(params);
                printSupportPreviewSize(params);
                printSupportPreviewFormat(params);

                camera.setDisplayOrientation(90);
                camera.setParameters(params);
                camera.setPreviewCallback(MainActivity.this);
                socketClient = new UdpClient();
                try {
                    camera.setPreviewDisplay(sfv.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.startPreview();
                camera.autoFocus(MainActivity.this);

                params = camera.getParameters(); //重新get一次
                Size size = params.getPreviewSize();
                Log.i(TAG, "最终设置:PreviewSize--With = " + size.width
                        + "Height = " + size.height);

                sfv.setAspectRatio(size.height, size.width);
                Log.i(TAG, "最终设置:PictureSize--With = " + params.getPictureSize().width
                        + "Height = " + params.getPictureSize().height);
                previewFormat = params.getPreviewFormat();

                Log.i(TAG, "Preview size = " + params.get("preview-size") + " Format = " + params.get("preview-format"));
                Log.i(TAG, "Picture size = " + params.get("picture-size") + " Format = " + params.get("picture-format"));
            }
        });

        sfv = (AutoFitSurfaceView) findViewById(R.id.surfaceView1);

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCamera();
            }
        });
    }

    private void stopCamera() {
        if (camera == null)
            return;
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
        socketClient.close();
    }

    @Override
    protected void onDestroy() {
        stopCamera();
        super.onDestroy();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.i(TAG, "onPreviewFrame size = " + data.length);
        Size previewSize = camera.getParameters().getPreviewSize();
        YuvImage yuvImage = new YuvImage(data, previewFormat, previewSize.width, previewSize.height, null);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 20, outStream);
        byte[] bytes = outStream.toByteArray();
        socketClient.sentData(bytes, bytes.length);
        Log.i(TAG, "Jpeg with = " + yuvImage.getWidth() + " height = " + yuvImage.getHeight() + " size = " + bytes.length);
    }

    /**
     * 打印支持的previewSizes
     */
    public void printSupportPreviewSize(Parameters params) {
        List<Size> previewSizes = params.getSupportedPreviewSizes();
        for (Size size : previewSizes) {
            Log.i(TAG, "previewSizes:width = " + size.width + " height = " + size.height);
        }
    }

    /**
     * 打印支持的pictureSizes
     */
    public void printSupportPictureSize(Parameters params) {
        List<Size> pictureSizes = params.getSupportedPictureSizes();
        for (Size size : pictureSizes) {
            Log.i(TAG, "pictureSizes:width = " + size.width
                    + " height = " + size.height);
        }
    }

    /**
     * 打印支持的聚焦模式
     */
    public void printSupportFocusMode(Parameters params) {
        List<String> focusModes = params.getSupportedFocusModes();
        for (String mode : focusModes) {
            Log.i(TAG, "focusModes--" + mode);
        }
    }

    public void printSupportPreviewFormat(Parameters parameters) {
        List<Integer> formats = parameters.getSupportedPreviewFormats();
        for (Integer format : formats) {
            Log.i(TAG, "PreviewFormat : " + format);
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }
}
