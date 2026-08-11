## MediaRecorder介绍

7.0之后的版本MediaRecorder也支持暂停和恢复录制了，部分场景下使用MediaRecorder更加方便。

当然AudioRecord也有自己的优势，录制过程可以处理PCM,可以做混音，降噪等等。

现在Android版本分布在7.0以上占96%，8.0以上占93%，所以我们可以放心的使用MediaRecorder。~前提是你**说服**产品，哈哈

## 方法介绍

1. `setAudioSource(int audioSource)`: 设置音频源，例如麦克风。

2. `setOutputFormat(int output_format)`: 设置输出文件的格式，例如MPEG_4。

3. `setAudioEncoder(int audio_encoder)`: 设置音频文件的编码。

4. `setOutputFile(String path)`: 设置输出文件的路径。

5. `prepare()`: 准备录制器开始捕获和编码数据。这个方法必须在设置了音频和视频源、编码器、文件格式等之后，但在`start()`之前调用。

6. `start()`: 开始录制。在`prepare()`之后调用。

7. `pause()`: 暂停录制。在Android 7.0（API级别24）及更高版本中可用。调用此方法后，可以通过调用`resume()`方法来恢复录制。

8. `resume()`: 恢复录制。在Android 7.0（API级别24）及更高版本中可用。在调用`pause()`方法后，可以调用此方法来恢复录制。

9. `stop()`: 停止录制。在`start()`之后调用。一旦录制停止，你将必须重新配置它，就像它刚刚被构造一样。

10. `reset()`: 重置MediaRecorder到其空闲状态。调用此方法后，你将必须再次配置它，就像它刚刚被构造一样。

11. `release()`: 释放与此MediaRecorder对象关联的资源。当你完成使用MediaRecorder时，调用此方法是个好习惯。

12. `getMaxAmplitude()`: 返回自上次调用此方法以来采样的最大绝对振幅。只有在设置了音频源之后才调用此方法。

13. `setMaxDuration(int max_duration_ms)`: 设置最大录制时长，单位为毫秒。如果设置了这个值，当录制时间达到这个值时，MediaRecorder会自动停止，并发送一个`MEDIA_RECORDER_INFO_MAX_DURATION_REACHED`的消息。在你的代码中，`setMaxDuration(1000 * 60 * 60 * 24)`设置了最大录制时长为24小时。

14. `setVideoSource(int videoSource)`: 设置视频源，例如摄像头。

15. `setVideoEncoder(int video_encoder)`: 设置视频文件的编码。

16. `setVideoSize(int width, int height)`: 设置视频的宽度和高度。

17. `setVideoFrameRate(int rate)`: 设置视频的帧率。

18. `setOrientationHint(int degrees)`: 设置输出视频的旋转角度。

19. `setLocation(float latitude, float longitude)`: 设置录制的地理位置。

20. `setPreviewDisplay(Surface sv)`: 设置用于显示预览的Surface。

21. `setNextOutputFile(File file)`: 在Android 8.0（API级别26）及更高版本中可用。设置下一个输出文件，当当前输出文件达到最大大小或最大持续时间时，录制将继续到下一个文件。

22. `getMetrics()`: 返回一个包含有关底层媒体框架的度量的PersistableBundle。这些度量可以用于了解录制会话的性能和资源使用情况。

23. `setMaxFileSize(long max_filesize_bytes)`：用于设置输出文件的最大大小，单位为字节。如果设置了这个值，当录制的文件大小达到这个值时，`MediaRecorder`会自动停止，并发送一个`MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED`的消息。 


## 总结
1. 7.0之后的版本，`pause()`和`resume()`方法可以用来暂停和恢复录制。
2. 8.0之后的版本，`setNextOutputFile(File file)`方法可以在当前输出文件达到最大大小或最大持续时间时，录制将继续到下一个文件。应用场景：每60秒发送一个音频。

当然我的这个lib,也都支持上面的功能
