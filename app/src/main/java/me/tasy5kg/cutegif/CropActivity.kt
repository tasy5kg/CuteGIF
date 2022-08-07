package me.tasy5kg.cutegif

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.canhub.cropper.CropImageView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import me.tasy5kg.cutegif.MyConstants.EXTRA_CROP_PARAMS
import me.tasy5kg.cutegif.MyConstants.FIRST_FRAME_PATH
import me.tasy5kg.cutegif.databinding.ActivityCropBinding

class CropActivity : AppCompatActivity() {
  private lateinit var binding: ActivityCropBinding
  private lateinit var chipGroupCropRatio: ChipGroup
  private lateinit var cropImageView: CropImageView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCropBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    setFinishOnTouchOutside(false)
    chipGroupCropRatio = binding.chipGroupCropRatio
    loadCropRatio()
    cropImageView = binding.cropImageView
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

  private fun loadCropRatio() {
    val cropRatioTextList = listOf(
      getString(R.string.crop_free),
      getString(R.string.crop_square),
      getString(R.string.crop_4_3),
      getString(R.string.crop_16_9)
    )
    cropRatioTextList.forEach {
      chipGroupCropRatio.addView(Chip(ContextThemeWrapper(this, com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
        text = it
        chipBackgroundColor = MyToolbox.createColorStateList(
          arrayOf(
            android.R.attr.state_checked to R.color.green_light,
            android.R.attr.state_checkable to R.color.light
          )
        )
        isCheckable = true
        isCheckedIconVisible = false
        isChecked = (it == getString(R.string.crop_free))
        setOnClickListener { view ->
          val clickedChip = view as Chip
          clickedChip.isChecked = true
          with(cropImageView) {
            when (clickedChip.text) {
              getString(R.string.crop_free) -> clearAspectRatio()
              getString(R.string.crop_square) -> setAspectRatio(1, 1)
              getString(R.string.crop_4_3) -> when (aspectRatio) {
                Pair(4, 3) -> setAspectRatio(3, 4)
                else -> setAspectRatio(4, 3)
              }
              getString(R.string.crop_16_9) -> when (aspectRatio) {
                Pair(16, 9) -> setAspectRatio(9, 16)
                else -> setAspectRatio(16, 9)
              }
            }
          }
        }
      }
      )
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
