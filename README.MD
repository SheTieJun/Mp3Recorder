# 录音工具-Mp3Recorder 

 [![](https://jitpack.io/v/SheTieJun/Mp3Recorder.svg)](https://jitpack.io/#SheTieJun/Mp3Recorder)

- 边录边转码MP3,支持暂停，实时返回已录制时长和当前声音大小。
- 可添加背景音乐,可以设置背景音乐声音的大小
- 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
- 可设置最大录制时间
- 录音中途可以替换输出文件，比如每60秒替换一个输出文件，然后发送
- 可以使用耳机配置方式：如果没有连接耳机会只用外放的背景音乐，如果连接上了耳机，会使用写入合成背景音乐的方式
- 其他...

> tip: 背景音乐支持网络链接，但是网络差的时候可能导致ANR,所有请尽量不要使用网络链接作为背景音乐应用
> 最低版本提升到：`21`
> 如果需要`19`的请使用`1.5.0`以前的版本


### [Demo下载](http://d.zqapps.com/ne21)
<img src="https://github.com/SheTieJun/Mp3Recorder/blob/master/doc/img/recorder.gif" width="35%" height="35%" />

#### 背景音乐相关
  - 录制中可以随时中断、播放、替换背景音乐
  - 如果背景音乐的参数我的库中不一样，需要自行设置参数，如果不一样会让背景音乐拉长或者变快

#### 缺点

1. 录制声道数设置，因为合成，所有你**需要设置和背景音乐相同的参数**
2. 如果设置单声道，播放的背景是双声道，（MIX）会让音乐拉长；反之双声音合成，背景音乐是单声音，节奏会变快




#### [接入配置文档](https://github.com/SheTieJun/Mp3Recorder/wiki/%E6%8E%A5%E5%85%A5%E6%96%87%E6%A1%A3)

#### [API使用文档](https://github.com/SheTieJun/Mp3Recorder/wiki/%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)

### [**Old version**](https://github.com/SheTieJun/Mp3Recorder/tree/master_copy)

### [Update_log](https://github.com/SheTieJun/Mp3Recorder/wiki/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97)

### [License](https://github.com/SheTieJun/Mp3Recorder/blob/master/LICENSE)

