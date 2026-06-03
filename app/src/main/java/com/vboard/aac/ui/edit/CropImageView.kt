package com.vboard.aac.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()

    // Viewport (1:1 crop area)
    private val viewportRect = RectF()

    // Paint objects
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // Semi-transparent black (70% opacity)
    }
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Touch handling variables
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            invalidate()
            return true
        }
    })

    fun setBitmap(newBitmap: Bitmap) {
        this.bitmap = newBitmap
        matrix.reset()
        if (width > 0 && height > 0) {
            setupInitialImagePosition()
        }
        requestLayout()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupViewport(w, h)
        setupInitialImagePosition()
    }

    private fun setupViewport(w: Int, h: Int) {
        val size = min(w, h) * 0.8f
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        viewportRect.set(left, top, left + size, top + size)
    }

    private fun setupInitialImagePosition() {
        val bmp = bitmap ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val viewportSize = viewportRect.width()
        val bmpWidth = bmp.width.toFloat()
        val bmpHeight = bmp.height.toFloat()

        // Fit image so that it covers the 1:1 viewport completely with a 15% buffer
        val scale = max(viewportSize / bmpWidth, viewportSize / bmpHeight) * 1.15f
        val dx = viewportRect.left + (viewportSize - bmpWidth * scale) / 2f
        val dy = viewportRect.top + (viewportSize - bmpHeight * scale) / 2f

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (scaleGestureDetector.isInProgress) {
            isDragging = false
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        // Draw image
        canvas.drawBitmap(bmp, matrix, bitmapPaint)

        // Draw overlay with 1:1 transparent hole
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(viewportRect, clearPaint)
        canvas.restoreToCount(saveCount)

        // Draw viewport border
        canvas.drawRect(viewportRect, borderPaint)
    }

    /**
     * Crops the portion of the image currently within the viewport and returns it as a new square bitmap.
     * The resulting bitmap is scaled to [destSize] x [destSize] (default 512).
     */
    fun getCroppedBitmap(destSize: Int = 512): Bitmap? {
        val bmp = bitmap ?: return null

        val croppedBmp = Bitmap.createBitmap(destSize, destSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBmp)

        val viewToDest = Matrix()
        viewToDest.setRectToRect(viewportRect, RectF(0f, 0f, destSize.toFloat(), destSize.toFloat()), Matrix.ScaleToFit.FILL)

        val drawMatrix = Matrix(matrix)
        drawMatrix.postConcat(viewToDest)

        canvas.drawBitmap(bmp, drawMatrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return croppedBmp
    }
}
