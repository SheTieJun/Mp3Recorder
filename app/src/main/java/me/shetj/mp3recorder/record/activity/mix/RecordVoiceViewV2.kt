package me.shetj.mp3recorder.record.activity.mix

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.animation.addListener
import androidx.core.graphics.withSave
import kotlin.math.abs
import kotlin.math.min
import me.shetj.base.ktx.dp2px
import me.shetj.mp3recorder.record.utils.covertToTime

/**
 * Record voice view v2
 * 新增：1、 刻度
 *      2、 放大缩小
 *
 * @constructor
 *
 * @param context
 * @param attrs
 * @param defStyleAttr
 */
class RecordVoiceViewV2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var viewCallBack: CallBack? = null
    private var duration: Long = 0
    private var currentPosition: Long = 0
    private var anima: ValueAnimator? = null
    private var playAnima: ValueAnimator? = null

    private var sizeSecond = 25

    //每一秒25个
    private val secondWidth: Int
        get() {
            return (sizeSecond * rectWidth * mScaleFactor + sizeSecond * rectSpace * mScaleFactor).toInt()
        }

    //滚动距离
    private var offsetX: Float = 0f
    private var rectStart = 0f //重复计算矩形的left,矩形宽度= rectEnd-rectStart
    private var rectEnd = 0f//重复计算矩形的right,矩形宽度= rectEnd-rectStart

    //边界 屏幕宽度+音频长度
    private var emptyLength = 1080 //空白区域的长度
    private val halfEmptyLength = emptyLength / 2
    private val contentLength: Int
        get() {
            return frameArray.getSize() * secondWidth / sizeSecond
        }
    private val rectVoiceLine = RectF()//右边波纹矩形的数据，10个矩形复用一个rectF
    private var rectWidth: Int = 3f.dp2px //矩形的宽度
    private val rectSpace: Int = 3f.dp2px
    private val mGravityScroller = Scroller(
        getContext(),
        DecelerateInterpolator()
    )

    private val min = 0
    private val max: Int
        get() {
            return 1080
        }

    private var frameArray = FrameArray()

    private var offsetLeftRegion = 0f //左边的偏移
    private var offsetRightRegion = 0f //右边的偏移
    private val leftRegion = Region() //判断是否是点击特定的左边区域
    private val rightRegion = Region() //判断是否是点击特定的右边区域
    private val leftRectF = RectF(0f, 0f, 0f, 0f)
    private val rightRectF = RectF(0f, 0f, 0f, 0f)


    //刻度部分
    private val rectTimeLine = RectF()//右边波纹矩形的数据，10个矩形复用一个rectF
    private var rectTimeStart = 0f //重复计算矩形的left
    //region 画笔部分
    /**
     * Rect right paint 右边的画笔
     */
    private val rectRightPaint = Paint().apply {
        color = Color.parseColor("#c8cad0")

    }

    /**
     * Rect left paint，左边的画笔
     */
    private val rectLeftPaint = Paint().apply {
        color = Color.parseColor("#93b3ea")
    }

    /**
     * Center line paint 中间的画笔
     */
    private val centerLinePaint = Paint().apply {
        color = Color.parseColor("#93b3ea")
        strokeWidth = 3f.dp2px.toFloat()
    }

//    private val bitmap = ContextCompat.getDrawable(context, R.mipmap.icon_test_touch)?.toBitmap()

    /**
     * Cut marker point：剪切指示器的画笔
     */
    private val cutMarkerPoint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val cutPoint = Paint()

    private var isEdit = true
    private var canScroll = true

    /**
     * Cut marker point：剪切指示器的画笔
     */
    private val mTimecodePaint = Paint().apply {
        textSize = 12f
        isAntiAlias = true
        strokeWidth = 3f
        color = Color.parseColor("#c8cad0")
    }

    private fun getStartX(): Float {
        return offsetX + halfEmptyLength
    }

    //endregion


    fun dip2px(dpVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal, context.resources.displayMetrics
        ).toInt()
    }


    private var mScaleFactor = 0.8f

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = 0.2f.coerceAtLeast(mScaleFactor.coerceAtMost(1f))
            checkOffsetX()
            invalidate()
            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    private val ongestureListener = object : SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (!canScroll) return false
            offsetX -= distanceX
            checkOffsetX()
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (!canScroll) return false
            mGravityScroller.fling(
                0,
                0,
                velocityX.toInt(),
                velocityY.toInt(),
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Int.MAX_VALUE
            )
            startAnimaFling()
            return true
        }


        override fun onDown(e: MotionEvent?): Boolean {
            if (!canScroll) return false
            anima?.cancel()
            return true
        }
    }

    private fun checkOffsetX() {
        if (offsetX > 0) {
            offsetX = 0f
        }
        if (offsetX < 0 && abs(offsetX) > (contentLength)) {
            offsetX = (-contentLength).toFloat()
        }
        //通过偏移量得到时间
        if (contentLength == 0) return
        this.currentPosition = (duration * abs(offsetX) / contentLength).toLong()
        viewCallBack?.onCurrentPosition(currentPosition)
    }

    //测量
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        leftRectF.left = (right - left).toFloat() / 2 - 100
        leftRectF.right = (right - left).toFloat() / 2 - 100
        leftRectF.top = top.toFloat()
        leftRectF.bottom = bottom.toFloat()

        rightRectF.left = (right - left).toFloat() / 2
        rightRectF.right = (right - left).toFloat() / 2
        rightRectF.top = top.toFloat()
        rightRectF.bottom = bottom.toFloat()

        leftRegion.set(
            leftRectF.left.toInt(), (leftRectF.bottom - 30).toInt(), leftRectF.right.toInt(),
            leftRectF.bottom.toInt()
        )
        rightRegion.set(
            rightRectF.left.toInt(), (rightRectF.bottom - 30).toInt(), rightRectF.right.toInt(),
            rightRectF.bottom.toInt()
        )
    }


    private fun measureHeight(measureSpec: Int, defaultSize: Int = suggestedMinimumHeight): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = defaultSize + paddingTop + paddingBottom
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize)
            }
        }
        result = result.coerceAtLeast(suggestedMinimumHeight)
        return result
    }

    private fun measureWidth(measureSpec: Int, defaultSize: Int = suggestedMinimumWidth): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = defaultSize + paddingLeft + paddingRight
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize)
            }
        }
        result = result.coerceAtLeast(suggestedMinimumWidth)
        return result
    }


    private val gestureDetector = GestureDetector(getContext(), ongestureListener)

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    fun setCanScroll(canScroll: Boolean, toEnd: Boolean = false) {
        this.canScroll = canScroll
        if (toEnd) {
            scrollToEnd()
        }
    }

    private val paint = Paint().apply {
        style = Style.STROKE
        color = Color.RED
        strokeWidth = 20f
        isAntiAlias = true
        val gradient = LinearGradient(
            30f,
            0f,
            50f,
            0f,
            intArrayOf(0x00000000, 0xFF0000FF.toInt(), 0x00000000),
            null,
            Shader.TileMode.MIRROR
        )
        shader = gradient
        pathEffect = CornerPathEffect(10f)
    }

    fun addFrame(frame: Float, duration: Long) {
        frameArray.add(frame)
        offsetX += -(rectWidth + rectSpace)
        this.duration = duration
        checkOffsetX()
        postInvalidate()
    }

    fun scrollToEnd() {
        anima?.cancel()
        anima = ObjectAnimator.ofFloat(offsetX, -contentLength.toFloat()).also { an ->
            an.duration = 50
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }

    fun clearFrame() {
        frameArray.reset()
        offsetX = 0f
        postInvalidate()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isPlaying()) return true
        return if (event.pointerCount >= 2) {
            mScaleDetector.onTouchEvent(event)
            true
        } else {
            gestureDetector.onTouchEvent(event)
        }
    }

    private val fontMetrics = textPaint.fontMetrics
    private val top = fontMetrics.top
    private val bottom = fontMetrics.bottom
    private val baseLineY = 15f - top / 2 - bottom / 2 //基线中间点的y轴计算公式

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        val halfWidth = (width / 2).toFloat()
        val halfHeight = (height / 2).toFloat()
        canvas?.apply {

            canvas.withSave {


                val time = contentLength / secondWidth

                val i = width / secondWidth

                repeat(i + 3 + time) {

                    rectTimeStart = getStartX() + it * secondWidth
                    rectTimeLine.left = rectTimeStart
                    rectTimeLine.top = 30f
                    rectTimeLine.right = rectTimeStart + 2f
                    rectTimeLine.bottom = 40f

                    if (rectTimeStart >= 0 && rectTimeStart <= width) {
                        drawRoundRect(rectTimeLine, 1f, 1f, mTimecodePaint)
                        if (mScaleFactor > 0.5) {
                            val covertToTime = it.covertToTime()
                            drawText(covertToTime, rectTimeStart, baseLineY, textPaint)
                        } else {
                            if (it % 2 == 0) {
                                val covertToTime = it.covertToTime()
                                drawText(covertToTime, rectTimeStart, baseLineY, textPaint)
                            }
                        }
                    }
                }
            }
            // 画声音播放矩形
            frameArray.get().forEachIndexed { index, i ->

                rectStart =
                    getStartX() + index  * rectWidth * mScaleFactor + rectSpace * mScaleFactor * index
                rectEnd =
                    getStartX() + (index  + 1) * rectWidth * mScaleFactor + rectSpace * mScaleFactor * index
                if (rectEnd <= width + 20 || rectStart >= -10) {
                    val halfRectHeight = min(i / 10f, 1f) / 2 * halfHeight //矩形的半高
                    rectVoiceLine.left = rectStart
                    rectVoiceLine.top = halfHeight - halfRectHeight
                    rectVoiceLine.right = rectEnd
                    rectVoiceLine.bottom = halfHeight + halfRectHeight
                    if (rectEnd < halfWidth) {
                        drawRoundRect(rectVoiceLine, 6f, 6f, rectLeftPaint)
                    } else {
                        drawRoundRect(rectVoiceLine, 6f, 6f, rectRightPaint)
                    }
                }
            }

            // 画中线
            drawLine(halfWidth, 0f, halfWidth, height.toFloat(), centerLinePaint)
            // 上线 + 刻度
            drawLine(
                min(offsetX, 0f),
                40f,
                min(getStartX() + contentLength + halfEmptyLength, width.toFloat()),
                40f,
                mTimecodePaint
            )
            // 下线
            drawLine(
                min(offsetX, 0f),
                height.toFloat(),
                min(getStartX() + contentLength + halfEmptyLength, width.toFloat()),
                height.toFloat(),
                mTimecodePaint
            )


            if (isEdit) {
                canvas.withSave {

                    // 画开始左线

                    // 画结束右线

                    // 画矩形
                }
            }

        }
    }

    private fun startAnimaFling() {
        anima?.cancel()
        val finalX = mGravityScroller.finalX
        val duration = mGravityScroller.duration
        val offsetX1 = offsetX
        anima = ObjectAnimator.ofFloat(0.toFloat(), finalX.toFloat()).also { an ->
            an.duration = duration.toLong()
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = offsetX1 + changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }


    fun startPlay() {
        playAnima?.cancel()
        playAnima = ObjectAnimator.ofFloat(offsetX, -contentLength.toFloat()).also { an ->
            an.duration = 50
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.addListener(onStart = {

            }, onCancel = {

            })

            an.start()
        }
    }

    fun pausePlay() {
        playAnima?.cancel()
    }

    fun isPlaying(): Boolean {
        return playAnima?.isRunning ?: false
    }


    fun setCallBack(callBack: CallBack) {
        this.viewCallBack = callBack
    }

    interface CallBack {


        fun onCurrentPosition(position: Long)


    }
}