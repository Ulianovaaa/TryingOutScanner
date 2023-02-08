package com.example.tryingoutscanner

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {


    //UI Views
    private lateinit var cameraBtn: MaterialButton
    private lateinit var galleryBtn: MaterialButton
    private lateinit var imageIv: ImageView
    private lateinit var scanBtn: MaterialButton
    private lateinit var resultTView: TextView

    companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
        private const val TAG = "MAIN_TAG"
    }

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>


    private var imageUri:Uri? = null

    private var barcodeScannerOptions: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraBtn = findViewById(R.id.cameraBtn)
        galleryBtn = findViewById(R.id.galleryBtn)
        imageIv = findViewById(R.id.imageIv)
        scanBtn = findViewById(R.id.scanBtn)
        resultTView = findViewById(R.id.resultTView)

        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        barcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)

        cameraBtn.setOnClickListener{
            if (checkCameraPermission()){
                pickImageCamera()
            } else requestCameraPermission()

        }

        galleryBtn.setOnClickListener{
            if (checkStoragePermission()){
                pickImageGallery()
            } else requestStoragePermission()
        }

        scanBtn.setOnClickListener{
            if (imageUri==null){
                Toast.makeText(this, "Pick an image", Toast.LENGTH_LONG)
            }else{
                detectResultFromImage()
            }

        }

    }

    private fun detectResultFromImage(){
        Log.d(TAG, "detectResultFromImage: ")
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            val barcodeResult = barcodeScanner!!.process(inputImage)
                .addOnSuccessListener { barcodes ->
                  extractCodeInfo(barcodes)  

                }
                .addOnFailureListener{ e->
                    Log.e(TAG, "detectResultFromImage: ", e)
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG)
                }
        }catch(e: java.lang.Exception){
            Log.e(TAG, "detectResultFromImage: ", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG)
        }
    }

    private fun extractCodeInfo(barcodes: List<Barcode>) {
        for (barcode in barcodes){
            val bound = barcode.boundingBox
            val corners = barcode.cornerPoints

            val rawValue = barcode.rawValue
            Log.d(TAG, "extractCodeInfo: rawVal: $rawValue")

            val valueType = barcode.valueType

            when(valueType){
                Barcode.TYPE_TEXT ->{
                    Log.d(TAG, "This is text!")
                    resultTView.text = "$rawValue"
                }else->{
                resultTView.text = "$rawValue"
                }

            }

        }


    }

    private fun pickImageGallery(){

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private  val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if (result.resultCode == Activity.RESULT_OK){
            val data = result.data
            imageUri = data?.data
            Log.d(TAG, ": gallery imageURI: $imageUri")
            imageIv.setImageURI(imageUri)
        }else{
            Toast.makeText(this, "Cancelled 0_0", Toast.LENGTH_SHORT)
        }
    }

    private fun pickImageCamera(){
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample image desc")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)

    }

    private  val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if (result.resultCode == Activity.RESULT_OK){
            val data = result.data
           // imageUri = data?.data
            Log.d(TAG, ": cam imageURI: $imageUri")
            imageIv.setImageURI(imageUri)
        }else{
            Toast.makeText(this, "Cancelled 0_0", Toast.LENGTH_SHORT)
        }
    }


    private fun checkStoragePermission(): Boolean{

        val result = (ContextCompat
            .checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        return result;
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    private  fun checkCameraPermission(): Boolean{
        val resultCamera = (ContextCompat
            .checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        val resultStorage = (ContextCompat
            .checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
         return resultCamera && resultStorage
    }

    private fun requestCameraPermission(){
            ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            CAMERA_REQUEST_CODE ->{
                if (grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if(cameraAccepted&&storageAccepted){
                        pickImageCamera()
                    } else { Toast.makeText(this, "Camera & Storage permissions required", Toast.LENGTH_LONG) }
                }
            }
            //break;
            STORAGE_REQUEST_CODE ->{
                if (grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if(storageAccepted){
                        pickImageGallery()
                    } else { Toast.makeText(this, "Storage permission required", Toast.LENGTH_LONG) }
                }
            }

        }
    }

}