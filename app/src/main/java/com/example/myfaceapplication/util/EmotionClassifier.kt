package com.example.myfaceapplication.util

import com.google.mlkit.vision.face.Face

enum class Emotion {
    HAPPY, SAD, SURPRISED, NEUTRAL
}

fun classifyEmotion(face: Face): Emotion {
    val smile = face.smilingProbability ?: 0f
    val leftEye = face.leftEyeOpenProbability ?: 0f
    val rightEye = face.rightEyeOpenProbability ?: 0f

    return when {
        smile > 0.7 && leftEye > 0.5 && rightEye > 0.5 -> Emotion.HAPPY
        smile < 0.3 && leftEye < 0.3 && rightEye < 0.3 -> Emotion.SAD
        leftEye > 0.6 && rightEye > 0.6 && smile < 0.1 -> Emotion.SURPRISED
        else -> Emotion.NEUTRAL
    }

}
fun getEmotionProbabilities(face: Face): Map<Emotion, Float> {
    val smile = face.smilingProbability ?: 0f
    val leftEye = face.leftEyeOpenProbability ?: 0f
    val rightEye = face.rightEyeOpenProbability ?: 0f

    val rawScores = mapOf(
        Emotion.HAPPY to (smile + leftEye + rightEye) / 3f,
        Emotion.SAD to ((1 - smile) + (1 - leftEye) + (1 - rightEye)) / 3f,
        Emotion.SURPRISED to ((leftEye + rightEye) / 2f) * (1 - smile),
        Emotion.NEUTRAL to 1f - (smile + leftEye + rightEye) / 3f
    ).mapValues { (_, v) -> v.coerceIn(0f, 1f) }

    val total = rawScores.values.sum().takeIf { it > 0 } ?: 1f

    return rawScores.mapValues { (_, v) -> v / total }
}


