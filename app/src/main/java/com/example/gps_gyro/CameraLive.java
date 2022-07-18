package com.example.gps_gyro;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class CameraLive extends Thread {
    private String url;
    private MediaProjection mediaProjection;
    private static LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();
    private static boolean isLiving;
    static {
        System.loadLibrary("native-lib");
    }

    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        start();
    }

    public void startLive(String url) {
        this.url = url;
        //this.mediaProjection = mediaProjection;
        start();
    }

    @Override
    public void run() {
        if (!connect(url)) {
            Log.i("liuyi", "run: ----------->推送失败");
            return;
        }

//        VideoCodec videoCodec = new VideoCodec(this);
//        videoCodec.startLive(mediaProjection);

        isLiving = true;
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i("liuyi","java sendData");
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer().length , rtmpPackage.getTms());
            }
        }
    }

    public static void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }

    private native boolean connect(String url);

    private native boolean sendData(byte[] data, int len, long tms);

}

