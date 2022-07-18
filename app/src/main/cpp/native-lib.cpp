#include <jni.h>
#include <string>
#include <android/log.h>
#define LOGE(...) __android_log_print(ANDROID_LOG_INFO,"liuyi",__VA_ARGS__)

extern "C"{
#include  "librtmp/rtmp.h"
}

typedef  struct {
    RTMP *rtmp;
    int16_t sps_len;
    int8_t *sps;
    int16_t pps_len;
    int8_t *pps;
}Live;
Live *live = NULL;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_gps_1gyro_CameraLive_connect(JNIEnv *env, jobject thiz, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    int ret;
    do {
        live = (Live*)malloc(sizeof(Live));
        memset(live, 0, sizeof(Live));

        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);

        live->rtmp->Link.timeout = 10;

        LOGE("connect %s", url);
        if (!(ret = RTMP_SetupURL(live->rtmp, (char*)url))) break;

        RTMP_EnableWrite(live->rtmp);

        LOGE("RTMP_Connect");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) break;

        LOGE("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) break;
        LOGE("connect success");
    }  while (0);

    if (!ret && live) {
        free(live);
        live = nullptr;
    }

    env->ReleaseStringUTFChars(url_, url);
    return ret;
}

// 传递第一帧 00 00 00 01 67 64 00 28ACB402201E3CBCA41408081B4284D4  00000001 68 EE 06 F2 C0
void prepareVideo(int8_t *data, int len, Live *live) {
    for (int i = 0; i < len; i++) {
        if (i + 4 < len) {
            if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                if (data[i + 4]  == 0x68) {
                    //sps解析
                    live->sps_len = i - 4;
                    live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                    memcpy(live->sps, data + 4, live->sps_len);

                    //pps解析
                    live->pps_len = len - (4 + live->sps_len) - 4;
                    live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                    memcpy(live->pps, data + 4 + live->sps_len + 4, live->pps_len);
                    break;
                }
            }
        }
    }
}

//sps  pps 的 packaet
RTMPPacket *createVideoPackage(Live *live) {
    int body_size = 16 + live->sps_len + live->pps_len; //为什么是16？？？？参考rtmp视频包结构
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    packet->m_body[i++] = 0x01;

    packet->m_body[i++] = live->sps[1]; //profile 如baseline、main、 high
    packet->m_body[i++] = live->sps[2]; //profile_compatibility 兼容性
    packet->m_body[i++] = live->sps[3]; //profile level

    packet->m_body[i++] = 0xFF;
    packet->m_body[i++] = 0xE1;

    //sps length
    packet->m_body[i++] = (live->sps_len >> 8) & 0xFF;//高八位
    packet->m_body[i++] = live->sps_len & 0xff;//低八位

    //拷贝sps的内容
    memcpy(&packet->m_body[i], live->sps, live->sps_len);

    i +=live->sps_len;

    packet->m_body[i++] = 0x01;

    //pps length
    packet->m_body[i++] = (live->pps_len >> 8) & 0xff; //高八位
    packet->m_body[i++] = live->pps_len & 0xff;//低八位

    // 拷贝pps内容
    memcpy(&packet->m_body[i], live->pps, live->pps_len);

    //视频类型
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

RTMPPacket *createVideoPackage(int8_t *buf, int len, const long tms, Live *live) {
    buf += 4;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    int body_size = len + 9;

    //初始化RTMP内部的body数组
    RTMPPacket_Alloc(packet, body_size);

    if (buf[0] == 0x65) {//
        packet->m_body[0] = 0x17;
        LOGE("发送关键帧 data");
    } else{
        packet->m_body[0] = 0x27;
        LOGE("发送非关键帧 data");
    }

    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    //长度
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;

    //数据
    memcpy(&packet->m_body[9], buf, len);//为什么是9？？？？参考rtmp视频包结构

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

int sendPacket(RTMPPacket *packet) {
    int r = RTMP_SendPacket(live->rtmp, packet, 1);
    if(r){
        LOGE("发送rtmp包成功");
    }
    RTMPPacket_Free(packet);
    free(packet);
    return r;
}

// 传递第一帧 00 00 00 01 67 64 00 28ACB402201E3CBCA41408081B4284D4  0000000168 EE 06 F2 C0
int sendVideo(int8_t *buf, int len, long tms) {
    int ret = 0;
    if (buf[4] == 0x67) {
        // 缓存sps 和pps 到全局遍历 不需要推流
        if (live && (!live->pps || !live->sps)) {
            prepareVideo(buf, len, live);
        }
        return ret;
    }

    if (buf[4] == 0x65) {//关键帧
        RTMPPacket *packet = createVideoPackage(live);
        sendPacket(packet);
    }

    RTMPPacket *packet2 = createVideoPackage(buf, len, tms, live);
    ret = sendPacket(packet2);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_gps_1gyro_CameraLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_,
                                               jint len, jlong tms) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    ret = sendVideo(data, len, tms);
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}