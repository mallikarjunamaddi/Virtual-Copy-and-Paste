/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.imagesegmentation

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import org.tensorflow.lite.examples.imagesegmentation.camera.CameraFragment
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

// This is an arbitrary number we are using to keep tab of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), CameraFragment.OnCaptureFinished {

  private lateinit var cameraFragment: CameraFragment
  private lateinit var viewModel: MLExecutionViewModel
  private lateinit var viewFinder: FrameLayout
  private lateinit var resultImageView: ImageView
  private lateinit var chipsGroup: ChipGroup
  private lateinit var rerunButton: Button
  private lateinit var captureButton: ImageButton
  private lateinit var pasteButton: ImageButton

  private var lastSavedFile = ""
  private var useGPU = false
  private lateinit var imageSegmentationModel: ImageSegmentationModelExecutor
  private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private val mainScope = MainScope()

  private var lensFacing = CameraCharacteristics.LENS_FACING_FRONT

  private var isViewImage = false
  private lateinit var viewImage: Bitmap
  private lateinit var resultImage: Bitmap

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.tfe_is_activity_main)

    val toolbar: Toolbar = findViewById(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    viewFinder = findViewById(R.id.view_finder)
    resultImageView = findViewById(R.id.result_imageview)
    chipsGroup = findViewById(R.id.chips_group)
    captureButton = findViewById(R.id.capture_button)
    pasteButton = findViewById(R.id.paste_button)
    val useGpuSwitch: Switch = findViewById(R.id.switch_use_gpu)

    // Request camera permissions
    if (allPermissionsGranted()) {
      addCameraFragment()
    } else {
      ActivityCompat.requestPermissions(
        this,
        REQUIRED_PERMISSIONS,
        REQUEST_CODE_PERMISSIONS
      )
    }

    viewModel = ViewModelProviders.of(this)
      .get(MLExecutionViewModel::class.java)
    viewModel.resultingBitmap.observe(
      this,
      Observer { resultImage ->
        if (resultImage != null) {
          updateUIWithResults(resultImage)
          this.resultImage = resultImage.bitmapResult
          val task = PasteTask(this)
          task.execute(this.resultImage)
        }
      }
    )

    imageSegmentationModel = ImageSegmentationModelExecutor(this, useGPU)

    useGpuSwitch.setOnCheckedChangeListener { _, isChecked ->
      useGPU = isChecked
      mainScope.async(inferenceThread) {
        imageSegmentationModel.close()
        imageSegmentationModel = ImageSegmentationModelExecutor(this@MainActivity, useGPU)
      }
    }

    rerunButton = findViewById(R.id.rerun_button)
    rerunButton.setOnClickListener {
      if (lastSavedFile.isNotEmpty()) {
        enableControls(false)
        viewModel.onApplyModel(lastSavedFile, imageSegmentationModel, inferenceThread)
      }
    }

    animateCameraButton()

    setChipsToLogView(HashSet())
    setupControls()
    enableControls(true)
  }

  private fun animateCameraButton() {
    val animation = AnimationUtils.loadAnimation(this, R.anim.scale_anim)
    animation.interpolator = BounceInterpolator()
    captureButton.animation = animation
    captureButton.animation.start()
  }

  private fun setChipsToLogView(itemsFound: Set<Int>) {
    chipsGroup.removeAllViews()

    val paddingDp = TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, 10F,
      resources.displayMetrics
    ).toInt()

    for (i in 0 until ImageSegmentationModelExecutor.NUM_CLASSES) {
      if (itemsFound.contains(i)) {
        val chip = Chip(this)
        chip.text = ImageSegmentationModelExecutor.labelsArrays[i]
        chip.chipBackgroundColor =
          getColorStateListForChip(ImageSegmentationModelExecutor.segmentColors[i])
        chip.isClickable = false
        chip.setPadding(0, paddingDp, 0, paddingDp)
        chipsGroup.addView(chip)
      }
      val labelsFoundTextView: TextView = findViewById(R.id.tfe_is_labels_found)
      if (chipsGroup.childCount == 0) {
        labelsFoundTextView.text = getString(R.string.tfe_is_no_labels_found)
      } else {
        labelsFoundTextView.text = getString(R.string.tfe_is_labels_found)
      }
    }
    chipsGroup.parent.requestLayout()
  }

  private fun getColorStateListForChip(color: Int): ColorStateList {
    val states = arrayOf(
      intArrayOf(android.R.attr.state_enabled), // enabled
      intArrayOf(android.R.attr.state_pressed) // pressed
    )

    val colors = intArrayOf(color, color)
    return ColorStateList(states, colors)
  }

  private fun setImageView(imageView: ImageView, image: Bitmap) {
    Glide.with(baseContext)
      .load(image)
      .override(512, 512)
      .fitCenter()
      .into(imageView)
  }

  private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
    setImageView(resultImageView, modelExecutionResult.bitmapResult)
    val logText: TextView = findViewById(R.id.log_view)
    logText.text = modelExecutionResult.executionLog

    setChipsToLogView(modelExecutionResult.itemsFound)
    enableControls(true)
  }

  private fun enableControls(enable: Boolean) {
    rerunButton.isEnabled = enable && lastSavedFile.isNotEmpty()
    captureButton.isEnabled = enable
  }

  private fun setupControls() {
    captureButton.setOnClickListener {
      it.clearAnimation()
      isViewImage = false
      cameraFragment.takePicture()
    }

    pasteButton.setOnClickListener {
        isViewImage = true;
        cameraFragment.takePicture()
  }

    findViewById<ImageButton>(R.id.toggle_button).setOnClickListener {
      lensFacing = if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
        CameraCharacteristics.LENS_FACING_FRONT
      } else {
        CameraCharacteristics.LENS_FACING_BACK
      }
      cameraFragment.setFacingCamera(lensFacing)
      addCameraFragment()
    }
  }

  /**
   * Process result from permission request dialog box, has the request
   * been granted? If yes, start Camera. Otherwise display a toast
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
      if (allPermissionsGranted()) {
        addCameraFragment()
        viewFinder.post { setupControls() }
      } else {
        Toast.makeText(
          this,
          "Permissions not granted by the user.",
          Toast.LENGTH_SHORT
        ).show()
        finish()
      }
    }
  }

  private fun addCameraFragment() {
    cameraFragment = CameraFragment.newInstance()
    cameraFragment.setFacingCamera(lensFacing)
    supportFragmentManager.popBackStack()
    supportFragmentManager.beginTransaction()
      .replace(R.id.view_finder, cameraFragment)
      .commit()
  }

  /**
   * Check if all permission specified in the manifest have been granted
   */
  private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(
      baseContext, it
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun onCaptureFinished(file: File) {
    val msg = "Photo capture succeeded: ${file.absolutePath}"
    Log.d(TAG, msg)

    lastSavedFile = file.absolutePath
    if(!isViewImage) {
      enableControls(false)
      viewModel.onApplyModel(file.absolutePath, imageSegmentationModel, inferenceThread)
    } else {
      viewImage = ImageUtils.decodeBitmap(File(lastSavedFile))
      val task = PasteTask(this)
      task.execute(viewImage)
    }
  }

  // To make a post call and upload an image to server on background thread.
  companion object {
    class PasteTask internal constructor(context: MainActivity) : AsyncTask<Bitmap, String, String>() {
      val _context = context

      override fun doInBackground(vararg params: Bitmap?): String? {
        val attachmentName = "data"
        val attachmentFileName = "data.bmp"
        val crlf = "\r\n"
        val twoHyphens = "--"
        val boundary =  "*****"
        val data = ByteArrayOutputStream()

        var SERVER_URL = ""
        if(this._context.isViewImage) {
          params[0]?.compress(Bitmap.CompressFormat.JPEG, 50, data)
          SERVER_URL = this._context.getString(R.string.SERVER_PASTE_URL)
        } else {
          params[0]?.compress(Bitmap.CompressFormat.PNG, 50, data)
          SERVER_URL = this._context.getString(R.string.SERVER_CAPTURE_OBJECT_URL)
        }

        try {
          val url = URL(SERVER_URL)
          val connection = url.openConnection() as HttpURLConnection
          connection.setUseCaches(false)
          connection.setDoOutput(true)
          connection.setDoInput(true)
          connection.setRequestMethod("POST")
          connection.setRequestProperty("Connection", "Keep-Alive");
          connection.setRequestProperty("Cache-Control", "no-cache");
          connection.setRequestProperty(
                  "Content-Type", "multipart/form-data;boundary=" + boundary);
          connection.connect()

          val request = DataOutputStream(
                  connection.getOutputStream()) 

          request.writeBytes(twoHyphens + boundary + crlf)
          request.writeBytes("Content-Disposition: form-data; name=\"" +
                  attachmentName.toString() + "\";filename=\"" +
                  attachmentFileName.toString() + "\"" + crlf)
          request.writeBytes(crlf)


          request.write(data.toByteArray())
          request.writeBytes(crlf);
          request.writeBytes(twoHyphens + boundary +
                  twoHyphens + crlf);

          request.flush();
          request.close();

          val responseStream: InputStream = BufferedInputStream(connection.getInputStream())

          val responseStreamReader = BufferedReader(InputStreamReader(responseStream))

          var line: String? = ""
          val stringBuilder = StringBuilder()

          while (responseStreamReader.readLine().also({ line = it }) != null) {
            stringBuilder.append(line).append("\n")
          }
          responseStreamReader.close()

          responseStream.close();

          connection.disconnect();
        } catch (e: IOException) {
          Log.e("ImageUploader", "Error uploading image", e)
        }
        return "Success!!";
      }
    }
  }
}
