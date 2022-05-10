/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.player

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2018/10/24 0024<br></br>
 * **@company：**<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**<br></br>
 */
interface PlayerListener {

    /**
     * 开始播放
     * @param url 播放路径
     * @param duration 最大时间
     */
    fun onStart(duration: Int)

    /**
     * 暂停
     */
    fun onPause()

    /**
     * 继续播放
     */
    fun onResume()

    /**
     * 停止播放
     */
    fun onStop()

    /**
     * 播放结束
     */
    fun onCompletion()

    /**
     * 错误
     * @param throwable 异常信息
     */
    fun onError(throwable: Exception?)

    /**
     * 进度条
     * @param current 当前播放位置
     * @param duration 一共
     */
    fun onProgress(current: Int, duration: Int)
}
