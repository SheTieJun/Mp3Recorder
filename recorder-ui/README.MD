## RecorderPopup 录音组件：底部PopupWindow

### 初始化
```
       private val recorderPopup: RecorderPopup by lazy {
        RecorderPopup(this,(60 * 60 * 1000).toLong()) {
            it.showToast()
        }
    }
```

### 使用
```
     // activity 没有初始化成功前 不可以调用，否则会崩溃
    recorderPopup.showPop()
```