package br.com.projetos.cameraremote

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.Surface
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.projetos.cameraremote.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var commandServer: LocalCommandServer? = null
    private var isCapturing = false
    private var pendingCapture: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private val cameraSelector: CameraSelector
        get() = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

    private val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        .build()

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showStatus("Permissão de câmera necessária")
            setControlsEnabled(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.switchCameraButton.setOnClickListener {
            if (isCapturing) return@setOnClickListener
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }
        binding.previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) focusAt(event.x, event.y)
            true
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            showStatus("Solicitando acesso à câmera…")
            cameraPermission.launch(Manifest.permission.CAMERA)
        }

        commandServer = LocalCommandServer(8765, ::onCommand)
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            binding.topBar.setPadding(
                binding.topBar.paddingLeft,
                dp(18) + systemBars.top,
                binding.topBar.paddingRight,
                binding.topBar.paddingBottom,
            )

            val panelParams = binding.controlsPanel.layoutParams as ViewGroup.MarginLayoutParams
            panelParams.bottomMargin = dp(16) + systemBars.bottom
            binding.controlsPanel.layoutParams = panelParams
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onStart() {
        super.onStart()
        commandServer?.start()
        if (!isCapturing && pendingCapture == null && imageCapture != null) {
            setControlsEnabled(true)
        }
    }

    override fun onStop() {
        cancelPendingCapture()
        commandServer?.stop()
        super.onStop()
    }

    private fun onCommand(command: String, delaySeconds: Int) {
        runOnUiThread {
            showStatus(if (delaySeconds > 0) {
                "Comando recebido • foto em ${delaySeconds}s"
            } else {
                "Comando recebido: $command"
            })
            schedulePhoto(delaySeconds)
        }
    }

    private fun schedulePhoto(delaySeconds: Int) {
        if (imageCapture == null) {
            showStatus("Câmera ainda não está pronta")
            return
        }
        if (pendingCapture != null) {
            showStatus("Já existe uma foto temporizada aguardando")
            return
        }
        if (delaySeconds <= 0) {
            takePhoto()
            return
        }

        setControlsEnabled(false)
        val capture = Runnable {
            pendingCapture = null
            takePhoto()
        }
        pendingCapture = capture
        mainHandler.postDelayed(capture, delaySeconds * 1_000L)
        showStatus("Foto em ${delaySeconds}s…")
    }

    private fun cancelPendingCapture() {
        pendingCapture?.let(mainHandler::removeCallbacks)
        pendingCapture = null
    }

    private fun startCamera() {
        camera = null
        imageCapture = null
        setControlsEnabled(false)
        updateCameraButton()
        showStatus("Preparando ${cameraLabel()}…")
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val selector = cameraSelector
                if (!provider.hasCamera(selector)) {
                    throw IllegalStateException("Esta câmera não está disponível no aparelho")
                }

                val rotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0
                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(rotation)
                    .build().also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(rotation)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setJpegQuality(95)
                    .build()
                imageCapture = capture
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, capture)
                setControlsEnabled(true)
                showStatus("${cameraLabel()} pronta • toque no preview para focar")
            } catch (exception: Exception) {
                imageCapture = null
                camera = null
                binding.captureButton.isEnabled = false
                binding.switchCameraButton.isEnabled = true
                showStatus("${cameraLabel()} indisponível")
                Toast.makeText(this, exception.message ?: "Erro ao iniciar a câmera", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun focusAt(x: Float, y: Float) {
        if (isCapturing) return
        val currentCamera = camera ?: return
        val point = binding.previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        val focusFuture = currentCamera.cameraControl.startFocusAndMetering(action)
        focusFuture.addListener({
            try {
                showStatus(if (focusFuture.get().isFocusSuccessful) "Foco ajustado" else "Foco não encontrado")
            } catch (_: Exception) {
                showStatus("Foco automático indisponível")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: run {
            showStatus("Câmera ainda não está pronta")
            return
        }
        if (isCapturing) {
            showStatus("Captura em andamento…")
            return
        }

        isCapturing = true
        setControlsEnabled(false)
        showStatus("Capturando foto…")
        val name = "Amazfit_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AmazfitRemote")
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()
        capture.takePicture(output, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                isCapturing = false
                setControlsEnabled(true)
                showStatus("Foto salva • aguardando comando")
                Toast.makeText(this@MainActivity, "Foto salva na galeria", Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                isCapturing = false
                setControlsEnabled(true)
                showStatus("Erro ao salvar foto")
                Toast.makeText(this@MainActivity, exception.message ?: "Erro", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showStatus(message: String) {
        binding.statusText.text = message
    }

    private fun cameraLabel(): String = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
        "Câmera frontal"
    } else {
        "Câmera traseira"
    }

    private fun updateCameraButton() {
        binding.switchCameraButton.text = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            "USAR CÂMERA TRASEIRA"
        } else {
            "USAR CÂMERA FRONTAL"
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        binding.captureButton.isEnabled = enabled
        binding.switchCameraButton.isEnabled = enabled
    }

    override fun onDestroy() {
        cancelPendingCapture()
        commandServer?.stop()
        super.onDestroy()
    }
}
