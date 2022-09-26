package uz.gita.cameraexcample

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import uz.gita.cameraexcample.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val viewBinding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
//    private lateinit var outputDirectory: File

    //Since 2021, an update to CameraX has rendered CameraX.LensFacing unusable. Use CameraSelector instead.
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA //telli oldi orqa camerasini boshqarish

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // action barni koddan o'chirish
        supportActionBar?.hide()

        cameraExecutor = Executors.newSingleThreadExecutor()


        if (allPermissionsGranted()) {
            startCamera(cameraSelector)
        } else {
            ActivityCompat.requestPermissions(
                this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSION
            )
        }

        viewBinding.takePhoto.setOnClickListener {
            takePhoto()
        }
//flip camera
        viewBinding.ivFlipCamera.setOnClickListener {
            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Only bind use cases if we can query a camera with this orientation
                startCamera(cameraSelector)
            } catch (exc: Exception) {
                // Do nothing
            }
        }

    }

    private fun flipCamera() {
        cameraSelector =
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
        startCamera(cameraSelector)
    }

    //(camera preview)camera ishga tushadi. rasmga olishmas faqat ishga tushishi
    private fun startCamera(cameraSelector: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        viewBinding.preView.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder()

                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "start camera Fail: $e", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))

    }

    //permission -> ruhsat olish camera ishlatishga
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera(cameraSelector)
            }
        } else {
            Toast.makeText(this, "permission not granted by user", Toast.LENGTH_SHORT).show()
            finish()
        }


    }
//faylga saqlash yozib korish kere
//    private fun getOutputDirectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(R.string.app_name)).apply {
//                mkdirs()
//            }
//        }
//        return if (mediaDir != null && mediaDir.exists()) {mk90-
//        /            mediaDir
//        } else filesDir
//
//    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val contentValues = ContentValues()
        val currentTime = System.currentTimeMillis()
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "$currentTime + photo.jpg")
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
            contentValues.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/" + "CAMERA"
            )
        } else {
            createFolderIfNotExist()
            val path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).toString() + "/" + "CAMERA" + "/" + "photo.jpg"
            contentValues.put(MediaStore.Images.Media.DATA, path)
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        this@MainActivity,
                        "${outputFileResults.savedUri}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        "error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        )
    }


    private fun allPermissionsGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun createFolderIfNotExist() {
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).toString() + "/" + "CAMERA"
        )
        if (!file.exists()) {
            if (!file.mkdir()) {
                Log.d(ContentValues.TAG, "Folder Create -> Failure")
            } else {
                Log.d(ContentValues.TAG, "Folder Create -> Success")
            }
        }
    }


}