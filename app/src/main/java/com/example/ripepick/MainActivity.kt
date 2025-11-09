package com.example.ripepick

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    // Modern UI Components
    private lateinit var fruitImageView: ImageView
    private lateinit var cameraButton: MaterialButton
    private lateinit var galleryButton: MaterialButton
    private lateinit var resultText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var placeholderText: TextView
    private lateinit var placeholderAnimation: LottieAnimationView
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var loadingOverlay: RelativeLayout
    private lateinit var resultCard: MaterialCardView
    private lateinit var confidenceBar: LinearProgressIndicator
    private lateinit var confidenceText: TextView
    private lateinit var confidenceLayout: LinearLayout
    private lateinit var fabScan: FloatingActionButton
    private lateinit var toolbar: Toolbar

    // ML Components
    private lateinit var imageClassifier: ImageClassifier
    private lateinit var labels: List<String>
    private var isModelLoaded = false

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
            showModernSnackbar("Camera permission granted! 📸")
        } else {
            resultText.text = "Camera permission denied ❌"
            showModernSnackbar("Camera permission is required to take pictures")

            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showPermissionPermanentlyDeniedDialog()
            }
        }
    }

    // Camera launcher
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Convert camera bitmap to supported format
            val supportedBitmap = convertToSupportedFormat(bitmap)
            fruitImageView.setImageBitmap(supportedBitmap)
            hidePlaceholder()
            resultText.text = "📸 Analyzing with your AI model..."
            showModernSnackbar("Image captured successfully! 🎯")
            analyzeFruitWithYourML(supportedBitmap)
        } else {
            resultText.text = "Failed to capture image ❌"
            showModernSnackbar("Could not capture image. Please try again.")
        }
    }

    // Gallery launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                fruitImageView.setImageURI(uri)
                hidePlaceholder()
                resultText.text = "🖼️ Analyzing with your AI model..."

                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                analyzeFruitWithYourML(bitmap!!)
                showModernSnackbar("Image loaded from gallery! 🖼️")

            } catch (e: Exception) {
                resultText.text = "Error loading image ❌"
                showModernSnackbar("Error loading image from gallery: ${e.message}")
            }
        } else {
            resultText.text = "No image selected"
            showModernSnackbar("No image was selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge display
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeModernViews()
        setupModernButtonListeners()
        initializeYourMLModel()

        showModernWelcome()
    }

    private fun initializeModernViews() {
        fruitImageView = findViewById(R.id.fruitImageView)
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        resultText = findViewById(R.id.resultText)
        placeholderAnimation = findViewById(R.id.placeholderAnimation)
        loadingAnimation = findViewById(R.id.loadingAnimation)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        resultCard = findViewById(R.id.resultCard)
        confidenceBar = findViewById(R.id.confidenceBar)
        confidenceText = findViewById(R.id.confidenceText)
        confidenceLayout = findViewById(R.id.confidenceLayout)
        fabScan = findViewById(R.id.fabScan)
    }

    private fun setupModernButtonListeners() {
        cameraButton.setOnClickListener {
            animateButtonClick(cameraButton)
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openCamera()
                }
                else -> {
                    resultText.text = "🔒 Requesting camera permission..."
                    showModernSnackbar("Please allow camera permission to take photos")
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        galleryButton.setOnClickListener {
            animateButtonClick(galleryButton)
            resultText.text = "Opening gallery..."
            pickImage.launch("image/*")
        }

        fabScan.setOnClickListener {
            animateFabClick(fabScan)
            // Quick scan action - opens camera directly
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openCamera()
                }
                else -> {
                    showModernSnackbar("Requesting camera access...")
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    private fun initializeYourMLModel() {
        try {
            // Load YOUR Teachable Machine model
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)  // Get top 3 results
                .setScoreThreshold(0.5f)  // Minimum confidence
                .build()

            imageClassifier = ImageClassifier.createFromFileAndOptions(
                this,
                "teachable_model.tflite",  // YOUR model file
                options
            )

            loadYourLabels()
            isModelLoaded = true
            resultText.text = "✅ Your Personal AI Model Loaded!"
            showModernSnackbar("Your custom fruit detector is ready! 🤖")
        } catch (e: Exception) {
            isModelLoaded = false
            resultText.text = "❌ Model Load Failed - Using Smart Analysis"
            showModernSnackbar("ML model error: ${e.message}. Using fallback analysis.")
        }
    }

    private fun loadYourLabels() {
        try {
            val inputStream = assets.open("labels.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Parse labels with numbers: "0 Fresh Apple" -> "Fresh Apple"
            labels = reader.readLines().map { line ->
                // Remove the number prefix and keep the label text
                line.substringAfter(" ").trim()
            }

            reader.close()
            inputStream.close()

            // Show what fruits your model knows
            val fruitList = labels.joinToString(", ")
            showModernSnackbar("Model trained on: $fruitList 🍎🍌🍊")

        } catch (e: Exception) {
            // Fallback labels
            labels = listOf(
                "Fresh Apple", "Rotten Apple",
                "Fresh Banana", "Rotten Banana",
                "Fresh Orange", "Rotten Orange"
            )
            showModernSnackbar("Using default labels")
        }
    }

    private fun openCamera() {
        try {
            resultText.text = "Opening camera..."
            takePicture.launch(null)
        } catch (e: Exception) {
            resultText.text = "Cannot open camera ❌"
            showModernSnackbar("Camera error: ${e.message ?: "Unknown error"}")
        }
    }

    private fun analyzeFruitWithYourML(bitmap: Bitmap) {
        showLoadingState()

        Thread {
            try {
                val result = performYourMLInference(bitmap)

                runOnUiThread {
                    hideLoadingState()
                    displayModernResult(result)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hideLoadingState()
                    resultText.text = "❌ Analysis Failed"
                    showModernSnackbar("Analysis error: ${e.message}")
                }
            }
        }.start()
    }

    private fun performYourMLInference(bitmap: Bitmap): MLResult {
        // Convert and preprocess image for YOUR model
        var image = TensorImage.fromBitmap(bitmap)

        // Teachable Machine usually expects 224x224 images
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        image = imageProcessor.process(image)

        // Run inference on YOUR model
        val results = imageClassifier.classify(image)

        if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
            val topResult = results[0].categories[0]
            val confidence = (topResult.score * 100).toInt()

            // Get the actual label text from our parsed labels list
            val labelIndex = topResult.index
            val labelText = if (labelIndex < labels.size) {
                labels[labelIndex]
            } else {
                topResult.label
            }

            // Parse YOUR specific label format
            val (fruit, condition) = parseYourLabel(labelText)

            return MLResult(
                fruit = fruit,
                condition = condition,
                confidence = "$confidence%",
                isFresh = condition == "Fresh"
            )
        }

        throw Exception("No results from your model")
    }

    private fun parseYourLabel(label: String): Pair<String, String> {
        // Parse labels like "Fresh Apple", "Rotten Banana", etc.
        return when {
            label.contains("Fresh") -> {
                val fruit = label.removePrefix("Fresh").trim()
                Pair(fruit, "Fresh")
            }
            label.contains("Rotten") -> {
                val fruit = label.removePrefix("Rotten").trim()
                Pair(fruit, "Rotten")
            }
            else -> {
                // Fallback: try to extract from any label format
                val parts = label.split(" ")
                if (parts.size >= 2) {
                    Pair(parts.last(), parts.first())
                } else {
                    Pair(label, "Unknown")
                }
            }
        }
    }

    // Modern UI Animations
    private fun animateButtonClick(button: MaterialButton) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateFabClick(fab: FloatingActionButton) {
        fab.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .rotationBy(360f)
            .setDuration(200)
            .withEndAction {
                fab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun showModernWelcome() {
        // Animate the placeholder in
        placeholderAnimation.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        // Show a modern snackbar
        showModernSnackbar("🎯 Your AI fruit detector is ready!")
    }

    private fun showModernSnackbar(message: String) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.primary))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun showLoadingState() {
        loadingOverlay.visibility = View.VISIBLE
        loadingOverlay.alpha = 0f
        loadingOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        placeholderAnimation.visibility = View.GONE
    }

    private fun hideLoadingState() {
        loadingOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                loadingOverlay.visibility = View.GONE
            }
            .start()
    }

    private fun hidePlaceholder() {
        placeholderAnimation.visibility = View.GONE
        findViewById<TextView>(R.id.placeholderText).visibility = View.GONE
    }

    private fun animateConfidenceBar(confidence: Int) {
        confidenceLayout.visibility = View.VISIBLE
        confidenceLayout.alpha = 0f
        confidenceLayout.animate()
            .alpha(1f)
            .setDuration(500)
            .start()

        val animator = ValueAnimator.ofInt(0, confidence)
        animator.duration = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val progress = valueAnimator.animatedValue as Int
            confidenceBar.progress = progress
            confidenceText.text = "$progress%"
        }
        animator.start()
    }

    private fun displayModernResult(result: MLResult) {
        // Animate result card entrance
        resultCard.scaleX = 0.8f
        resultCard.scaleY = 0.8f
        resultCard.alpha = 0f
        resultCard.visibility = View.VISIBLE

        resultCard.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .start()

        // Set result with modern styling
        val confidenceValue = result.confidence.removeSuffix("%").toIntOrNull() ?: 0
        animateConfidenceBar(confidenceValue)

        // Display the actual result
        displayYourMLResult(result)
    }

    private fun displayYourMLResult(result: MLResult) {
        val emoji = when (result.fruit.toLowerCase()) {
            "apple" -> "🍎"
            "banana" -> "🍌"
            "orange" -> "🍊"
            else -> "🍎"
        }

        val status = if (result.isFresh) {
            "✅ FRESH & DELICIOUS"
        } else {
            "❌ ROTTEN - AVOID"
        }

        val color = if (result.isFresh) "#2e7d32" else "#d32f2f"

        val resultMessage = """
            $status
            
            $emoji ${result.fruit}
            🎯 ${result.condition}
            📊 ${result.confidence} Confidence
            
            ${getYourAdvice(result.fruit, result.condition, result.confidence)}
        """.trimIndent()

        resultText.text = resultMessage
        resultText.setTextColor(Color.parseColor(color))

        // Update confidence bar color based on result
        confidenceBar.setIndicatorColor(Color.parseColor(if (result.isFresh) "#4CAF50" else "#f44336"))

        val confidenceValue = result.confidence.removeSuffix("%").toIntOrNull() ?: 0
        if (confidenceValue > 80) {
            showModernSnackbar("High confidence detection! 🎯")
        } else if (confidenceValue > 60) {
            showModernSnackbar("Good detection! 👍")
        } else {
            showModernSnackbar("Low confidence - try a clearer photo 📸")
        }
    }

    private fun getYourAdvice(fruit: String, condition: String, confidence: String): String {
        val fruitLower = fruit.toLowerCase()
        return if (condition == "Fresh") {
            when {
                fruitLower.contains("apple") -> "Crisp and fresh! Perfect for eating or salads."
                fruitLower.contains("banana") -> "Fresh banana! Great for snacks or smoothies."
                fruitLower.contains("orange") -> "Juicy and fresh! Rich in Vitamin C."
                else -> "Fresh and ready to eat!"
            }
        } else {
            when {
                fruitLower.contains("apple") -> "Rotten apple. Do not consume - may contain mold."
                fruitLower.contains("banana") -> "Overripe banana. Best for baking banana bread."
                fruitLower.contains("orange") -> "Rotten orange. Discard immediately."
                else -> "Not safe for consumption. Please discard."
            } + if ((confidence.removeSuffix("%").toIntOrNull() ?: 0) < 70) "\n\n⚠️ Low confidence - double check visually!" else ""
        }
    }

    // Utility function to convert camera bitmap format
    private fun convertToSupportedFormat(bitmap: Bitmap): Bitmap {
        return if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
        } else {
            bitmap
        }
    }

    private fun showPermissionPermanentlyDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔒 Camera Permission Required")
            .setMessage("Camera permission is permanently denied. To use camera features, please enable it in app settings:\n\n1. Open App Settings\n2. Tap 'Permissions'\n3. Enable 'Camera'")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                resultText.text = "Camera permission required"
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        } catch (e: Exception) {
            showModernSnackbar("Cannot open settings: ${e.message}")
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::imageClassifier.isInitialized) {
            imageClassifier.close()
        }
    }

    // Data class for ML results
    data class MLResult(
        val fruit: String,
        val condition: String,
        val confidence: String,
        val isFresh: Boolean
    )
}