package com.example.myfaceapplication.ui.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.myfaceapplication.R
import com.example.myfaceapplication.util.classifyEmotion
import com.example.myfaceapplication.util.Emotion
import com.google.mlkit.vision.face.Face

class FaceGraphic(
    overlay: GraphicOverlay,
    private val face: Face
) : GraphicOverlay.Graphic(overlay) {

    private val emojiMap: Map<Emotion, Bitmap> = mapOf(
        Emotion.HAPPY to BitmapFactory.decodeResource(overlay.context.resources, R.drawable.emoji),
        Emotion.SAD to BitmapFactory.decodeResource(overlay.context.resources, R.drawable.sademoji),
        Emotion.SURPRISED to BitmapFactory.decodeResource(overlay.context.resources, R.drawable.surprised_emoji),
        Emotion.NEUTRAL to BitmapFactory.decodeResource(overlay.context.resources, R.drawable.neutral_emoji)
    )

    override fun draw(canvas: Canvas) {
        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.top.toFloat())

        val emotion = classifyEmotion(face)
        val emoji = emojiMap[emotion]

        emoji?.let {
            val scaled = Bitmap.createScaledBitmap(it, 100, 100, true)
            canvas.drawBitmap(scaled, x - 50, y - 50, null)
        }
    }
}
