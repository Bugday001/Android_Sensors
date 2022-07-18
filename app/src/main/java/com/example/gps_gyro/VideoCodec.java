package com.example.gps_gyro;


import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoCodec extends  Thread {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec mediaCodec;
    private CameraLive cameraLive;
    private boolean isLiving;
    private long timeStamp;
    private long startTime;
    public VideoCodec(CameraLive cameraLive) {
        this.cameraLive = cameraLive;
    }

    public void startLive(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,  MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "screen-codec",
                    720, 1280, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }

    @Override
    public void run() {
        isLiving = true;
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (isLiving) {
            if (System.currentTimeMillis() - timeStamp >= 2000) {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                //dsp 芯片触发I帧
                mediaCodec.setParameters(params);
                timeStamp = System.currentTimeMillis();
            }
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >= 0) {
                if (startTime == 0) {
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
                RTMPPackage rtmpPackage = new RTMPPackage(outData, (bufferInfo.presentationTimeUs / 1000) - startTime);
                cameraLive.addPackage(rtmpPackage);
                mediaCodec.releaseOutputBuffer(index, false);
            }
        }
        isLiving = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        virtualDisplay.release();
        virtualDisplay = null;
        mediaProjection.stop();
        mediaProjection = null;
        startTime = 0;
    }


}

