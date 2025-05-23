package com.example.myfaceapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.myfaceapplication.ui.theme.MyFaceApplicationTheme
import com.example.myfaceapplication.util.Emotion
import com.example.myfaceapplication.util.getEmotionProbabilities
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFaceApplicationTheme {
                FaceAppScreen()
            }
        }
    }
}

@Composable
fun FaceAppScreen() {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var emotionScores by remember { mutableStateOf<Map<Emotion, Float>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val detector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            processImageFromUri(context, detector, it) { bitmap, scores ->
                selectedImage = bitmap
                emotionScores = scores
                isLoading = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            isLoading = true
            processImageFromUri(context, detector, cameraImageUri!!) { bitmap, scores ->
                selectedImage = bitmap
                emotionScores = scores
                isLoading = false
            }
        }
    }

    fun createImageUri(): Uri {
        val imageFile = File.createTempFile("face_capture_", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing image, please wait...")
                }

                selectedImage != null && emotionScores != null -> {
                    Image(
                        bitmap = selectedImage!!.asImageBitmap(),
                        contentDescription = "Selected Face",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Detected Emotions:", style = MaterialTheme.typography.titleMedium)

                    emotionScores!!.forEach { (emotion, score) ->
                        Text("${emotion.name}: ${(score * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        selectedImage = null
                        emotionScores = null
                    }) {
                        Text("Back")
                    }
                }

                else -> {
                    Button(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("Select from Gallery")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val uri = createImageUri()
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    }) {
                        Text("Take Photo")
                    }
                }
            }
        }
    }
}

fun processImageFromUri(
    context: Context,
    detector: com.google.mlkit.vision.face.FaceDetector,
    uri: Uri,
    onResult: (Bitmap, Map<Emotion, Float>) -> Unit
) {
    val bitmap = if (Build.VERSION.SDK_INT < 28) {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } else {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    }

    val image = InputImage.fromBitmap(bitmap, 0)
    detector.process(image)
        .addOnSuccessListener { faces ->
            val scores = if (faces.isNotEmpty()) getEmotionProbabilities(faces[0]) else emptyMap()
            onResult(bitmap, scores)
        }
        .addOnFailureListener {
            it.printStackTrace()
            onResult(bitmap, emptyMap()) // Still call onResult to stop loading
        }
}
