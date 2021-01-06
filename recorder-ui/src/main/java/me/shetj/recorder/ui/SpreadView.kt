package me.shetj.recorder.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import kotlin.math.ceil
import kotlin.math.min

class SpreadView : AppCompatImageView {

    /**
     * 是否匀速模式
     */
    private var isUniformSpeed: Boolean = false

    var start: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            visibility = when (value) {
                true -> View.VISIBLE
                else -> View.GONE
            }
            invalidate()
        }
    /**
     * 背景的宽高
     */
    private var bgHeight: Float = 0f
    private var bgWidth: Float = 0f

    /**
     * 扩散圆半径增加的粒度
     */
    private var grain: Float = 1f
    /**
     * 最大半径和最小半径差，等分 255 。
     */
    private var oneUnitInAlpha: Int = 1

    var trigger: Boolean = false
    @ColorInt
    private var spreadColor: Int = -1

    @ColorInt
    private var centerColor: Int = -1
    private var radius = 0f

    private val centerPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }
    private val spreadPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            alpha = 255
        }
    }

    private var centerX = 0f
    private var centerY = 0f

    private var distance = 5f
    private var maxRadius = 0f
    private var minRadius = 0f

    private var bg: Drawable? = null

    private val bgRect: Rect by lazy {
        Rect()
    }


    private var delayMilliseconds = 0
    private val spreadRadiusList: ArrayList<Float> by lazy {
        ArrayList<Float>(12)
    }
    private val alphaList: ArrayList<Int> by lazy {
        ArrayList<Int>(spreadRadiusList.size)
    }

    /**
     * 最大的波纹数
     * 每个波纹如果占据一个透明度，那最多也就 255 个
     */
    private var circleCount: Int = 255


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        context.obtainStyledAttributes(attrs, R.styleable.SpreadView).run {
            isUniformSpeed = getBoolean(R.styleable.SpreadView_spread_uniform_mode, false)
            radius = getDimension(R.styleable.SpreadView_spread_radius, radius)
            maxRadius = getDimension(R.styleable.SpreadView_spread_max_radius, maxRadius)
            minRadius = getDimension(R.styleable.SpreadView_spread_min_radius, minRadius)
            circleCount =
                    getInt(R.styleable.SpreadView_spread_circle_count, circleCount).trapMaxCircle()

            centerColor = getColor(
                    R.styleable.SpreadView_spread_center_color,
                    ContextCompat.getColor(context, R.color.pop_main_color)
            )
            spreadColor = getColor(
                    R.styleable.SpreadView_spread_spread_color,
                    ContextCompat.getColor(context, R.color.pop_main_color)
            )
            distance = getDimension(R.styleable.SpreadView_spread_distance, distance)
            bg = getDrawable(R.styleable.SpreadView_spread_src)
            bgWidth = getDimension(R.styleable.SpreadView_spread_bg_width, bgWidth)
            bgHeight = getDimension(R.styleable.SpreadView_spread_bg_width, bgHeight)
            // 扩散粒度
            grain = getFloat(R.styleable.SpreadView_spread_gain, grain)
            recycle()
        }
        centerPaint.color = centerColor
        alphaList.add(255)
        spreadRadiusList.add(minRadius)
        spreadPaint.color = spreadColor
        // 对圆环做等分 circleCount 分，因为最大波纹数为 circleCount，以此确定每个波纹的 alpha 差值
        oneUnitInAlpha = (255 / (maxRadius - minRadius) * grain).trapPaintAlpha()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w.toFloat() / 2
        centerY = h.toFloat() / 2
        if (maxRadius >= min(w, h).toFloat() / 2) {
            maxRadius = min(w, h).toFloat() / 2
        }
        bgRect.set(
                (centerX - bgWidth / 2).toInt(),
                (centerY - bgHeight / 2).toInt(),
                (centerX + bgWidth / 2).toInt(),
                (centerY + bgHeight / 2).toInt()
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        spreadRadiusList.iterator().run {
            while (this.hasNext()) {
                val spreadRadius = this.next()
                val index = spreadRadiusList.indexOf(spreadRadius)
                spreadRadius.trapRadius(maxRadius, minRadius)
                spreadRadiusList[index] = spreadRadius
                var alpha = alphaList[index]
                spreadPaint.alpha = alpha
                canvas?.drawCircle(centerX, centerY, spreadRadius, spreadPaint)

                if (spreadRadius > maxRadius) {
                    remove()
                    alphaList.removeAt(index)
                    continue
                }

                // 每次扩散圆半径递增，透明度递减（透明度范围是 [0,255]）
                alpha = if (alpha - oneUnitInAlpha > 0) {
                    (alpha - oneUnitInAlpha)
                } else {
                    0
                }
                alphaList[index] = alpha
                spreadRadiusList[index] = (spreadRadius + grain)
            }
        }

        if (isUniformSpeed){
            // 匀速水波纹
            drawUniform()
        } else {
            // 动态添加波纹
            drawDynamic()
        }

        // 中间的圆
        canvas?.apply {
            drawCircle(centerX, centerY, radius, centerPaint)
            bg?.bounds = bgRect
            bg?.draw(this)
        }
        // 延迟 draw，造成扩散效果
        delayMilliseconds.takeIf {
            start || !spreadRadiusList.hadSpreadOut<Float>(maxRadius)
        }?.let {
            postInvalidateOnAnimation()
        }
    }


    private fun drawUniform() {
        // 当最外层扩散圆半径达到最大半径时添加新扩散圆
        if (spreadRadiusList.size == 0 || (spreadRadiusList.size in 1 until circleCount && spreadRadiusList.last() - minRadius > distance)) {
            spreadRadiusList.add(minRadius)
            alphaList.add(255)
            trigger = false
        }

        // 超过 circleCount 个扩散圆，删除最先绘制的圆，即外层的圆
        if (spreadRadiusList.size >= circleCount && spreadRadiusList.first() > maxRadius) {
            alphaList.removeFirst()
            spreadRadiusList.removeFirst()
        }
    }

    private fun drawDynamic() {
        // 当最外层扩散圆半径达到最大半径时添加新扩散圆
        if (trigger && (spreadRadiusList.size < circleCount)) {
            spreadRadiusList.add(minRadius)
            alphaList.add(255)
            trigger = false
        }

        // 超过 circleCount 个扩散圆，删除最先绘制的圆，即外层的圆
        if (spreadRadiusList.size >= circleCount && spreadRadiusList.first() > maxRadius) {
            alphaList.removeFirst()
            spreadRadiusList.removeFirst()
        }
    }

    fun setBg(bgRes: Int) {
        bg = ContextCompat.getDrawable(context, bgRes)
    }

}

private fun <E> java.util.ArrayList<E>.hadSpreadOut(maxRadius: Float): Boolean {
    return this.isEmpty() || this.all { currentRadius ->
        currentRadius is Float && currentRadius >= maxRadius
    }
}

private fun Float.trapRadius(maxRadius: Float, minRadius: Float): Float {
    return when {
        this > maxRadius -> {
            maxRadius
        }
        this < minRadius -> {
            minRadius
        }
        else -> {
            this
        }
    }
}

private fun Float.trapPaintAlpha(): Int {
    return ceil(this).toInt()
}

private fun Int.trapMaxCircle(): Int {
    return if (this > 255) {
        255
    } else {
        this
    }
}

private fun <E> java.util.ArrayList<E>.removeFirst() {
    removeAt(0)
}

private fun <E> java.util.ArrayList<E>.removeLast() {
    this.removeAt(this.size - 1)
}
