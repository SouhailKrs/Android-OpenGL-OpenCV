package com.example.opencv

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.example.opencv.databinding.ActivityMainBinding
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*

class MainActivity : CameraActivity() {
    private lateinit var binding: ActivityMainBinding
    private val SELECT_CODE = 100
    private val CAMERA_CODE = 101
    private lateinit var mat: Mat
    private var cameraView: CameraBridgeViewBase? = null
    private lateinit var curr_gray: Mat
    private lateinit var prev_gray:Mat
    private lateinit var rgb:Mat
    private lateinit var diff:Mat
    private lateinit var cnts:MutableList<MatOfPoint>
    //boolean
    var is_init:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getCameraPermission()



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val camera = binding.camera
        val select = binding.select
        cameraView = binding.cameraView

        (cameraView as JavaCameraView).setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
            override fun onCameraViewStarted(width: Int, height: Int) {
                curr_gray = Mat()
                prev_gray = Mat()
                rgb = Mat()
                diff = Mat()
                cnts = ArrayList()
            }

            override fun onCameraViewStopped() {
                mat.release()
            }

            override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
                if(!is_init){
                    is_init = true
                    prev_gray = inputFrame.gray()
                    return prev_gray
                }
                rgb = inputFrame.rgba()
                curr_gray = inputFrame.gray()
                Core.absdiff(curr_gray, prev_gray, diff)
                Imgproc.threshold(diff, diff, 40.0, 255.0, Imgproc.THRESH_BINARY)
                Imgproc.findContours(diff, cnts, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
                Imgproc.drawContours(rgb, cnts, -1, Scalar(0.0, 255.0, 0.0), 2)
                for (MatOfPoint in cnts) {
                   var Rect = Imgproc.boundingRect(MatOfPoint)
                    Imgproc.rectangle(rgb, Rect.tl(), Rect.br(), Scalar(0.0, 255.0, 0.0), 2)

                }
                cnts?.clear()
                prev_gray = curr_gray.clone()
              return rgb
            }
        })
        if(OpenCVLoader.initDebug()){
            cameraView!!.enableView()
        }

        select.setOnClickListener() {
            intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                .apply {
                    type = "image/*"
                startActivityForResult(this, SELECT_CODE)
                }
        }
        camera.setOnClickListener(){
        if(PackageManager.PERMISSION_GRANTED == checkSelfPermission(android.Manifest.permission.CAMERA)) {
            intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_CODE)
           }
            getCameraPermission()

        }


    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return Collections.singletonList(cameraView) as MutableList<out CameraBridgeViewBase>
    }

    private fun getCameraPermission() {
        val permission = android.Manifest.permission.CAMERA
        val grant = checkCallingOrSelfPermission(permission)
        if (grant !=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 103)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 103) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED ) {
                getCameraPermission()
            }else {
                Log.d("Permission", "Camera permission granted")
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_CODE && data!=null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
                binding.imageView.setImageBitmap(bitmap)
                mat = Mat()
                Utils.bitmapToMat(bitmap, mat)

                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
                Utils.matToBitmap(mat, bitmap)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (requestCode == CAMERA_CODE && data!=null) {
            try {
                val bitmap = data.extras?.get("data") as android.graphics.Bitmap
                binding.imageView.setImageBitmap(bitmap)
                mat = Mat()
                Utils.bitmapToMat(bitmap, mat)

                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2Lab)
                Utils.matToBitmap(mat, bitmap)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}