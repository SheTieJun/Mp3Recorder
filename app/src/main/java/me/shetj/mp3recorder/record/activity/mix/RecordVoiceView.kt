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
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.min
import me.shetj.base.ktx.dp2px


class RecordVoiceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var anima: ValueAnimator? = null

    //滚动距离
    private var offsetX: Float = 0f
    private var rectStart = 0f //重复计算矩形的left,矩形宽度= rectEnd-rectStart
    private var rectEnd = 0f//重复计算矩形的right,矩形宽度= rectEnd-rectStart

    //边界 屏幕宽度+音频长度
    private var emptyLength = 1080 //空白区域的长度
    private val halfEmptyLength = emptyLength / 2
    private val contentLength: Int
        get() {
            return frameArray.getSize() * rectWidth + frameArray.getSize() * rectSpace
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

    private var frameArray = FrameArray().apply {
//        repeat(500) {
//            add((1..9).random())
//        }
    }


    //region 画笔部分
    /**
     * Rect right paint 右边的画笔
     */
    private val rectRightPaint = Paint().apply {
        color = Color.parseColor("#c8cad0")
        isAntiAlias = true
    }

    /**
     * Rect left paint，左边的画笔
     */
    private val rectLeftPaint = Paint().apply {
        color = Color.parseColor("#CDF36C60")
        isAntiAlias = true
    }

    /**
     * Center line paint 中间的画笔
     */
    private val centerLinePaint = Paint().apply {
        color = Color.parseColor("#F36C60")
        strokeWidth = 3f.dp2px.toFloat()
        isAntiAlias = true
    }


    /**
     * Cut marker point：剪切指示器的画笔
     */
    private val cutMarkerPoint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = (true)
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
    }

    //测量
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        setMeasuredDimension(width, height)
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
    }
    private var canScroll = true

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

    fun addFrame(frame: Int) {
        if (frame < 0) return
        frameArray.add(frame)
        offsetX = (-contentLength).toFloat()
        checkOffsetX()
        postInvalidate()
    }


    fun setCanScroll(canScroll: Boolean) {
        this.canScroll = canScroll
        if (!canScroll) {
            scrollToEnd()
        }
    }

    fun scrollToEnd() {
        anima?.cancel()
        anima = ObjectAnimator.ofFloat(offsetX, -contentLength.toFloat()).also { an ->
            an.duration = 50
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = changeSize
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        val halfWidth = (width / 2).toFloat()
        val halfHeight = (height / 2).toFloat()
        canvas?.apply {
            frameArray.get().forEachIndexed { index, i ->
                rectStart = getStartX() + index * rectWidth + rectSpace * index
                rectEnd = getStartX() + (index + 1) * rectWidth + rectSpace * index
                if (rectEnd <= width + rectWidth || rectStart >= -rectWidth) {
                    val halfRectHeight = min(i / 15f, 1f) / 2 * halfHeight //矩形的半高
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
            drawLine(halfWidth, 0f, halfWidth, height.toFloat(), centerLinePaint)
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
}