package com.example.myfaceapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myfaceapplication.ui.theme.MyFaceApplicationTheme
import com.example.myfaceapplication.util.Emotion
import com.example.myfaceapplication.util.getEmotionProbabilities
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

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
    val cameraPermission = android.Manifest.permission.CAMERA

    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var emotionScores by remember { mutableStateOf<Map<Emotion, Float>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var cameraImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraImageUri = cameraImageUriString?.let { Uri.parse(it) } // ✅ Fix here

    val detector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("FaceApp", "🖼 Gallery image URI: $it")
            isLoading = true
            coroutineScope.launch(Dispatchers.IO) {
                val (bitmap, scores) = processImageFromUriSuspend(context, detector, it)
                launch(Dispatchers.Main) {
                    selectedImage = bitmap
                    emotionScores = scores
                    isLoading = false
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("FaceApp", "\uD83C\uDFC6 TakePicture result callback - Success: $success")
        Log.d("FaceApp", "\uD83D\uDCCC cameraImageUri inside result callback: $cameraImageUri")

        if (success && cameraImageUri != null) {
            isLoading = true
            coroutineScope.launch(Dispatchers.IO) {
                val (bitmap, scores) = processImageFromUriSuspend(context, detector, cameraImageUri)
                launch(Dispatchers.Main) {
                    selectedImage = bitmap
                    emotionScores = scores
                    isLoading = false
                }
            }
        } else {
            Log.e("FaceApp", "❌ Failed to take picture or URI was null")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            if (uri != null) {
                cameraImageUriString = uri.toString()

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val resInfos = context.packageManager.queryIntentActivities(intent, 0)
                for (resInfo in resInfos) {
                    context.grantUriPermission(
                        resInfo.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Failed to create image URI", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
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
                        if (ContextCompat.checkSelfPermission(
                                context,
                                cameraPermission
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            val uri = createImageUri(context)
                            if (uri != null) {
                                cameraImageUriString = uri.toString()

                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                val resInfos = context.packageManager.queryIntentActivities(intent, 0)
                                for (resInfo in resInfos) {
                                    context.grantUriPermission(
                                        resInfo.activityInfo.packageName,
                                        uri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                                cameraLauncher.launch(uri)
                            } else {
                                Toast.makeText(context, "Failed to create image URI", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            permissionLauncher.launch(cameraPermission)
                        }
                    }) {
                        Text("Take Photo")
                    }
                }
            }
        }
    }
}

fun createImageUri(context: Context): Uri? {
    return try {
        val imageFile = File.createTempFile("face_capture_", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    } catch (e: Exception) {
        Log.e("FaceApp", "❌ Failed to create temp file for camera image: ${e.message}")
        null
    }
}

suspend fun processImageFromUriSuspend(
    context: Context,
    detector: com.google.mlkit.vision.face.FaceDetector,
    uri: Uri
): Pair<Bitmap, Map<Emotion, Float>> {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).await()

        val scores = if (faces.isNotEmpty()) getEmotionProbabilities(faces[0]) else emptyMap()
        Pair(bitmap, scores)
    } catch (e: Exception) {
        Log.e("FaceApp", "❌ Error processing image: ${e.message}")
        val fallback = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        Pair(fallback, emptyMap())
    }
}
