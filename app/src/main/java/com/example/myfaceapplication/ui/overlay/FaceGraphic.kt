package com.example.myfaceapplication.ui.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.example.myfaceapplication.R
import com.google.mlkit.vision.face.Face

class FaceGraphic(
    overlay: GraphicOverlay,
    private val face: Face
) : GraphicOverlay.Graphic(overlay) {

    private val smileThreshold = 0.5f

    // Load both emoji bitmaps
    private val smileEmoji: Bitmap = BitmapFactory.decodeResource(overlay.context.resources, R.drawable.emoji)
    private val sadEmoji: Bitmap = BitmapFactory.decodeResource(overlay.context.resources, R.drawable.sademoji)
    private val surprisedEmoji :Bitmap= BitmapFactory.decodeResource(overlay.context.resources, R.drawable.surprised_emoji)
    private val neutralEmoji : Bitmap = BitmapFactory.decodeResource(overlay.context.resources,R.drawable.neutral_emoji)

    override fun draw(canvas: Canvas) {
        // Get position of face
        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.top.toFloat())
        val smile = face.smilingProbability ?: 0f
        val leftEye = face.leftEyeOpenProbability ?: 0f
        val rightEye = face.rightEyeOpenProbability ?: 0f
        val headTilt = Math.abs(face.headEulerAngleZ)
        val headTurn = Math.abs(face.headEulerAngleY)

        // Choose emoji based on smile probability
        val selectedEmoji: Bitmap = when {
            smile > 0.7 && leftEye > 0.5 && rightEye > 0.5 -> smileEmoji
            smile < 0.3 && leftEye < 0.3 && rightEye < 0.3 -> sadEmoji
            //smile > 0.4 && headTilt > 20 -> coolEmoji
           // smile > 0.4 && (leftEye < 0.2 || rightEye < 0.2) -> winkingEmoji // optional
            //smile < 0.2 && headTurn > 25 -> angryEmoji
            leftEye > 0.6 && rightEye > 0.6 && smile < 0.1 -> surprisedEmoji
            else -> neutralEmoji
        }
//        val selectedEmoji = if (face.smilingProbability != null && face.smilingProbability!! > smileThreshold) {
//            smileEmoji
//        } else {
//            sadEmoji
//        }

        // Draw emoji on canvas
        selectedEmoji.let {
            val scaledBitmap = Bitmap.createScaledBitmap(it, 100, 100, true)
             canvas.drawBitmap(scaledBitmap, x - 50, y - 50, null)
        }
    }
}
