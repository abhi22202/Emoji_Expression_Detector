// GraphicOverlay.kt
package com.example.myfaceapplication.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val lock = Any()
    private val graphics = mutableListOf<Graphic>()

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)

        fun scaleX(horizontal: Float): Float = horizontal * overlay.width / overlay.width.toFloat()
        fun scaleY(vertical: Float): Float = vertical * overlay.height / overlay.height.toFloat()
        fun translateX(x: Float): Float = scaleX(x)
        fun translateY(y: Float): Float = scaleY(y)
    }

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}
