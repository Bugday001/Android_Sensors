package com.example.gps_gyro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;


public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback, NV21EncoderH264.EncoderListener{
    private SurfaceHolder holder;
    private Camera camera;
    private FileOutputStream outputStream;

    CameraLive cameraLive;
    String url = "rtmp://192.168.0.101:1935/live/home";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        if (Build.VERSION.SDK_INT>22){
            if (ContextCompat.checkSelfPermission(VideoActivity.this,
                    android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                //先判断有没有权限 ，没有就在这里进行权限的申请
                ActivityCompat.requestPermissions(VideoActivity.this,
                        new String[]{android.Manifest.permission.CAMERA},1234);

            }else {
                //说明已经获取到摄像头权限了 想干嘛干嘛
                initView();
                //
                cameraLive = new CameraLive();
                cameraLive.startLive(url);
            }
        }else {
                //这个说明系统版本在6.0之下，不需要动态获取权限。
            Log.d("Tag", "Build.VERSION.SDK_INT<=22");
        }
        //createFile();
    }

    //菜单显示
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_activity, menu);
        return true;
    }
    //菜单点击任务绑定
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //case判断建议常量，google建议用if代替
        int item_id = item.getItemId();
        if(item_id == R.id.set_IpPort){
            Intent intent = new Intent(VideoActivity.this, SetBasicData.class);
            startActivity(intent);
        }
        else if(item_id == R.id.remove_item){
            Toast.makeText(this, "Nothing happen", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void initView() {
        SurfaceView surfaceView = findViewById(R.id.sfv);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //打开相机
        openCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //关闭相机
        releaseCamera(camera);
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        //获取相机参数
        Camera.Parameters parameters = camera.getParameters();
        //获取相机支持的预览的大小
        Camera.Size previewSize = getCameraPreviewSize(parameters);
        int width = previewSize.width;
        int height = previewSize.height;
        //设置预览格式（也就是每一帧的视频格式）YUV420下的NV21
        parameters.setPreviewFormat(ImageFormat.NV21);
        //设置预览图像分辨率
        parameters.setPreviewSize(width/2, height/2);
        //相机旋转90度
        camera.setDisplayOrientation(90);
        //配置camera参数
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final NV21EncoderH264 nv21EncoderH264 = new NV21EncoderH264(width/2, height/2);
        nv21EncoderH264.setEncoderListener(this);
        //设置监听获取视频流的每一帧
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                nv21EncoderH264.encoderH264(data);
            }
        });
        //调用startPreview()用以更新preview的surface
        camera.startPreview();
    }

    /**
     * 获取设备支持的最大分辨率
     */
    private Camera.Size getCameraPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size needSize = null;
        for (Camera.Size size : list) {
            if (needSize == null) {
                needSize = size;
                continue;
            }
            if (size.width >= needSize.width) {
                if (size.height > needSize.height) {
                    needSize = size;
                }
            }
        }
        return needSize;
    }

    /**
     * 关闭相机
     */
    public void releaseCamera(Camera camera) {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    /**
     * 创建文件保存视频流h264
     */
    private void createFile() {
        File file = new File(getExternalCacheDir(), "test.h264");
        try {
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void h264(byte[] data) {
        Log.d("TAG", data.length + "");
        //TCP_H264.SendMagTCP(VideoActivity.this, data);
        //outputStream.write(data);
    }
}


