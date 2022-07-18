package com.example.gps_gyro;

public class RTMPPackage {
    private byte[] buffer;
    private long tms;

    public RTMPPackage(byte[] buffer, long tms) {
        this.buffer = buffer;
        this.tms = tms;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public long getTms() {
        return tms;
    }

    public void setTms(long tms) {
        this.tms = tms;
    }
}

