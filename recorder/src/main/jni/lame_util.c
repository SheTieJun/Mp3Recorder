//
// Created by 37510 on 2019/9/6.
//
#include "libmp3lame_3.100/lame.h"
#include "jni.h"
#include "stdio.h"

static lame_global_flags *lame =NULL;

JNIEXPORT jstring JNICALL Java_me_shetj_recorder_util_LameUtils_version(
        JNIEnv *env,
        jclass jcls){
    return (*env)->NewStringUTF(env,"3.100");
};


JNIEXPORT void JNICALL Java_me_shetj_recorder_util_LameUtils_init(
        JNIEnv *env,
        jclass cls,
        jint inSamplerate,
        jint inChannel,
        jint outSamplerate,
        jint outBitrate,
        jint quality){
    if(lame != NULL){
        lame_close(lame);
        lame = NULL;
    }

    lame =lame_init();
    //初始化，设置参数
    lame_set_in_samplerate(lame,inSamplerate);//输入采样率
    lame_set_out_samplerate(lame,outSamplerate);//输出采样率
    lame_set_num_channels(lame,inChannel);//声道
    lame_set_brate(lame,outBitrate);//比特率
    lame_set_quality(lame,quality);//质量
    lame_init_params(lame);

}


JNIEXPORT jint JNICALL Java_me_shetj_recorder_util_LameUtils_encode(
        JNIEnv  *env,
        jclass cls,
        jshortArray buffer_left,
        jshortArray buffer_right,
        jint samples,
        jbyteArray mp3buf){

    //把Java传过来参数转成C中的参数进行修改
    jshort * j_buff_left =(*env)->GetShortArrayElements(env,buffer_left,NULL);
    jshort * j_buff_right = (*env) ->GetShortArrayElements(env,buffer_right,NULL);

    const jsize mp3buf_size = (*env) ->GetArrayLength(env,mp3buf);

    jbyte* j_mp3buff = (*env) ->GetByteArrayElements(env,mp3buf,NULL);

    int result = lame_encode_buffer(lame,j_buff_left,j_buff_right,samples,j_mp3buff,mp3buf_size);

    //释放参数
    (*env)->ReleaseShortArrayElements(env,buffer_left,j_buff_left,0);
    (*env)->ReleaseShortArrayElements(env,buffer_right,j_buff_right,0);
    (*env)->ReleaseByteArrayElements(env,mp3buf,j_mp3buff,0);
    return result;

}

JNIEXPORT jint JNICALL Java_me_shetj_recorder_util_LameUtils_flush(
        JNIEnv *env,
        jclass cls,
        jbyteArray mp3buf){
    const jsize mp3buf_size = (*env) ->GetArrayLength(env,mp3buf);

    jbyte* j_mp3buff = (*env) ->GetByteArrayElements(env,mp3buf,NULL);

    int result = lame_encode_flush(lame,j_mp3buff,mp3buf_size);
    //释放
    (*env)->ReleaseByteArrayElements(env,mp3buf,j_mp3buff,0);

    return result;
}

JNIEXPORT void JNICALL Java_me_shetj_recorder_util_LameUtils_close(
        JNIEnv *env,
        jclass cls){
    lame_close(lame);
    lame =NULL;
}