package me.tasy5kg.cutegif

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageView
import me.tasy5kg.cutegif.MyConstants.EXTRA_CROP_PARAMS
import me.tasy5kg.cutegif.MyConstants.FIRST_FRAME_PATH
import me.tasy5kg.cutegif.databinding.ActivityCropBinding

class CropActivity : AppCompatActivity() {
  private lateinit var binding: ActivityCropBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCropBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    setFinishOnTouchOutside(false)
    val cropImageView = binding.cropImageView
    val myCropParams = intent!!.extras!!.get(EXTRA_CROP_PARAMS) as MyCropParams
    cropImageView.apply {
      setImageBitmap(BitmapFactory.decodeFile(FIRST_FRAME_PATH))
      isAutoZoomEnabled = false
      guidelines = CropImageView.Guidelines.ON
      isShowProgressBar = false
      rotatedDegrees = myCropParams.rotatedDegrees
      //  cropRect = cropParams.rect()
    }
    binding.mbRotate.setOnClickListener {
      cropImageView.rotateImage(90)
    }
    binding.mbDone.setOnClickListener {
      setResult(RESULT_OK, Intent().putExtra(EXTRA_CROP_PARAMS, MyCropParams(cropImageView.cropRect!!, cropImageView.rotatedDegrees)))
      finish()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_close, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_close -> finish()
    }
    return true
  }
}
