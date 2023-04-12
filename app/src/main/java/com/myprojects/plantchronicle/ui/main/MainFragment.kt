package com.myprojects.plantchronicle.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.core.motion.utils.Utils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.myprojects.plantchronicle.R
import com.myprojects.plantchronicle.databinding.FragmentMainBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias CornersListener = () -> Unit
class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private lateinit var safeContext: Context

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    companion object {
        fun newInstance() = MainFragment()
        val TAG = "CameraXFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        internal const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        var isOffline = false // prevent app crash when goes offline
    }

    private lateinit var viewModel: MainViewModel

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("UseRequireInsteadOfGet")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.plants.observe(this, Observer { plants ->
            binding.plantName.setAdapter(
                ArrayAdapter(
                    context!!,
                    androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                    plants
                )
            )
        })
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity!!, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else activity?.filesDir!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(safeContext), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                val msg = "Photo capture succeeded: $savedUri"
                Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder().build()

            imageCapture = ImageCapture.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder().build().apply {
                setAnalyzer(Executors.newSingleThreadExecutor(), CornerAnalyzer {
                })
            }
            // Select back camera
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(safeContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = safeContext.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            safeContext.resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(safeContext, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
//                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private class CornerAnalyzer(private val listener: CornersListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            if (!isOffline) {
                listener()
            }
            imageProxy.close() // important! if it is not closed it will only run once
        }

    }

    override fun onPause() {
        super.onPause()
        isOffline = true
    }

    override fun onResume() {
        super.onResume()
        isOffline = false
    }
}
// binding.btnTakePhoto.setOnClickListener {
// prepTakePhoto()
// }
// binding.btnLogon.setOnClickListener {
// prepOpenImageGallery()
// }
// }
//
// private fun prepOpenImageGallery() {
// Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
// type = "image/*"
// startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
// }
// }
//
// @SuppressLint("UseRequireInsteadOfGet")
// private fun prepTakePhoto() {
// if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
// takePhoto()
// } else {
// val permissionRequest = arrayOf(Manifest.permission.CAMERA);
// requestPermissions(permissionRequest, CAMERA_PERMISSION_REQUEST_CODE)
//
// }
// }
//
// override fun onRequestPermissionsResult(
// requestCode: Int,
// permissions: Array<out String>,
// grantResults: IntArray
// ) {
// super.onRequestPermissionsResult(requestCode, permissions, grantResults)
// when(requestCode) {
// CAMERA_PERMISSION_REQUEST_CODE -> {
// if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
// // permission granted, let's do stuff.
// takePhoto()
// } else {
// Toast.makeText(context, "Unable to take photo without permission", Toast.LENGTH_LONG).show()
// }
// }
// }
// }
//
// @SuppressLint("UseRequireInsteadOfGet")
// private fun takePhoto() {
// Intent(MediaStore.ACTION_IMAGE_CAPTURE).also{
// takePictureIntent -> takePictureIntent.resolveActivity(context!!.packageManager)
// if (takePictureIntent == null) {
// Toast.makeText(context, "Unable to save photo", Toast.LENGTH_LONG).show()
// } else {
// // if we are here, we have a valid intent.
// val photoFile: File = createImageFile()
// photoFile?.also {
// val photoURI = FileProvider.getUriForFile(activity!!.applicationContext, "com.myprojects.plantchronicle.fileprovider", it)
// takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)
// startActivityForResult(takePictureIntent, SAVE_IMAGE_REQUEST_CODE)
// }
// }
// }
// }
//
// @SuppressLint("UseRequireInsteadOfGet")
// private fun createImageFile(): File {
// // genererate a unique filename with date.
// val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
// // get access to the directory where we can write pictures.
// val storageDir:File? = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
// return File.createTempFile("PlantDiary${timestamp}", ".jpg", storageDir).apply {
// currentPhotoPath = absolutePath
// }
// }
//
// @SuppressLint("UseRequireInsteadOfGet", "NewApi")
// override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
// super.onActivityResult(requestCode, resultCode, data)
// if (resultCode == RESULT_OK) {
// if (requestCode == CAMERA_REQUEST_CODE)  {
// // now we can get the thumbnail
// val imageBitmap = data!!.extras!!.get("data") as Bitmap
// binding.plantImage.setImageBitmap(imageBitmap)
// } else if (requestCode == SAVE_IMAGE_REQUEST_CODE) {
// Toast.makeText(context, "Image Saved", Toast.LENGTH_LONG).show()
// } else if (requestCode == IMAGE_GALLERY_REQUEST_CODE) {
// if (data != null && data.data != null) {
// val image = data.data
// val source = ImageDecoder.createSource(activity!!.contentResolver, image!!)
// val bitmap = ImageDecoder.decodeBitmap(source)
// binding.plantImage.setImageBitmap(bitmap)
//
// }
// }
// }
// }