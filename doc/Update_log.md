### 2020年2月13日
- 优化`MP3Recorder`,添加方法`updateDataEncode`，使之中间替换输出文件
- 上述需求背景：希望录音可以变成60秒，一段一段的文件发送出去，所有每60秒我就替换输出文件，同时把上一个60秒音频文件上传



### 2019年10月31日
- 0.0.4
- 优化录音：单双声道 多模式录制


### 2019年10月31日  
- 版本 ：0.0.3
- 优化lame转双声道PCM ,以前的方法存在噪音
```
   if (is2CHANNEL) {
     //双声道
      readSize = buffer.size / 2
      encodedSize = LameUtils.encodeInterleaved(buffer,readSize,mMp3Buffer)
      } else {
      readSize = buffer.size
      encodedSize = LameUtils.encode(buffer, buffer, readSize, mMp3Buffer)
   }
```

### 2019年10月19日
- 尝试修改录制后声音的大小计算


#### 2019年10月15日
- fix 不设置背景音乐录制崩溃问题


#### 2019年10月11日
- 优化`PlayBackMusic`
    - 加入播放状态和进度回调
- 去掉Androidx的注解，方便不是Androidx的项目使用

#### 2019年10月10日
- 优化`MixRecorder`, 修改使用lame 进行边录制变转换，优化默认录制来源
    - 因为lame边录制边转可以支持, 录制中 进行播放试听
    - 因为背景音乐是合成进去的所有，使用VOICE_COMMUNICATION ,使用系统自带的AEC
 

#### 2019年10月9号
- 添加speex 进行去噪音，但是好像效果不佳，
- 最后去掉了


#### 2019年9月29日
- 优化`MixRecorder`,支持背景音乐切换
- 支持`播放中`或者`暂停`切换背景音乐

#### 2019年9月28日
- 优化`MixRecorder` 背景音乐支持循环播放

#### 2019年8月22日
- `Mp3Recorder`可以设置是否是继续录制功能（已完成）
```
setOutputFile(filePath，isContinue) 
```

#### 2019年8月20日
- `Mp3Recorder`通过扬声器器录制，所有暂时只支持录制麦克风 