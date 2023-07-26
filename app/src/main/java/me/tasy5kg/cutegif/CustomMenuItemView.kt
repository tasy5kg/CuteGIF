package me.tasy5kg.cutegif

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS_COLOR_QUALITY
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED
import me.tasy5kg.cutegif.MyConstants.UNKNOWN_INT
import me.tasy5kg.cutegif.Toolbox.getKeyByValue
import me.tasy5kg.cutegif.Toolbox.visibleIf
import me.tasy5kg.cutegif.databinding.ViewCustomMenuItemBinding

@Deprecated("replace")
class CustomMenuItemView(context: Context, attrs: AttributeSet) :
  LinearLayoutCompat(context, attrs) {
  private val binding by lazy {
    ViewCustomMenuItemBinding.inflate(
      LayoutInflater.from(context),
      this,
      true
    )
  }
  private val mtvTitle by lazy { binding.mtvTitle }
  private val mtvSubTitle by lazy { binding.mtvSubTitle }
  private val mtvSelectedKey by lazy { binding.mtvSelectedKey }
  private var negativeSelectedValue = false
  private var customMenuItemViewType: Int = UNKNOWN_INT
  private lateinit var allOptionsMap: LinkedHashMap<String, Int>
  private lateinit var sliderInPopupView: Slider
  private lateinit var chipGroup: ChipGroup

  init {
    context.theme.obtainStyledAttributes(
      attrs,
      R.styleable.CustomMenuItemView,
      0, 0
    ).apply {
      mtvTitle.text = getString(R.styleable.CustomMenuItemView_title)
      mtvSubTitle.text = getString(R.styleable.CustomMenuItemView_subTitle)
      mtvSelectedKey.text = getString(R.styleable.CustomMenuItemView_selectedKey)
      recycle()
    }
    mtvSubTitle.visibleIf { !mtvSubTitle.text.isNullOrBlank() }
    if (mtvSelectedKey.text.isNullOrBlank()) {
      mtvSelectedKey.visibility = GONE
    } else {
      mtvSelectedKey.visibility = VISIBLE
    }
  }

  fun selectedValue() =
    if (negativeSelectedValue) {
      -allOptionsMap[mtvSelectedKey.text]!!
    } else {
      allOptionsMap[mtvSelectedKey.text]!!
    }

/*
  @SuppressLint("InflateParams")
  fun setUpWithDropDownConfig(
    allOptionsMap: LinkedHashMap<String, Int>,
    // @StringRes guideText: Int,
    customMenuItemViewType: Int,
  ) {
    this.allOptionsMap = allOptionsMap
    this.customMenuItemViewType = customMenuItemViewType
    when (customMenuItemViewType) {
      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED -> {
        val popupView = LayoutInflater.from(context)
          .inflate(R.layout.view_popup_config_slider_speed, null)
        val popupWindow by lazy {
          PopupWindow(
            popupView,
            binding.root.width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
          ).apply {
            elevation = TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP,
              POPUP_WINDOW_ELEVATION,
              resources.displayMetrics
            )
            isOutsideTouchable = true
          }
        }
        // popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        sliderInPopupView = popupView.findViewById<Slider>(R.id.slider).apply {
          addOnChangeListener { it, value, _ ->
            val speedText = allOptionsMap.keys.elementAt((value).toInt())
            mtvSelectedKey.text = speedText
            it.setLabelFormatter {
              return@setLabelFormatter speedText
            }
          }
          valueFrom = 0f
          valueTo = MyConstants.GIF_SPEED_MAP.size.toFloat() - 1
          value = MyConstants.GIF_SPEED_MAP.keys.indexOf(mtvSelectedKey.text).toFloat()
          setLabelFormatter { return@setLabelFormatter mtvSelectedKey.text.toString() }
        }
        popupView.findViewById<MaterialCheckBox>(R.id.mcb_reverse_video).apply {
          setOnCheckedChangeListener { buttonView, isChecked ->
            negativeSelectedValue = isChecked
          }
        }
        binding.root.setOnClickListener {
          popupWindow.showAsDropDown(it)
        }
      }

      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS -> {
        val menuItemList = allOptionsMap.keys
        val popupView =
          LayoutInflater.from(context).inflate(R.layout.view_popup_config_chips, null)
        val popupWindow by lazy {
          PopupWindow(
            popupView,
            binding.root.width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
          ).apply {
            elevation = TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP,
              POPUP_WINDOW_ELEVATION,
              resources.displayMetrics
            )
            isOutsideTouchable = true
          }
        }
        //  popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        chipGroup = popupView.findViewById(R.id.chip_group)
        menuItemList.forEach { menuItemName ->
          chipGroup.addView(
            Chip(
              ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter
              )
            ).apply {
              text = menuItemName
              chipBackgroundColor = Toolbox.createColorStateListFromColorResource(
                arrayOf(
                  android.R.attr.state_checked to R.color.green_light,
                  android.R.attr.state_checkable to R.color.light
                )
              )
              isCheckable = true
              isCheckedIconVisible = false
              if (text == mtvSelectedKey.text) {
                isChecked = true
              }
              setOnClickListener { clickedChip ->
                (clickedChip as Chip).isChecked = true
                mtvSelectedKey.text = clickedChip.text
                popupWindow.dismiss()
              }
            }
          )
        }
        binding.root.setOnClickListener {
          popupWindow.showAsDropDown(it)
        }
      }

      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY -> {
        val popupView = LayoutInflater.from(context)
          .inflate(R.layout.view_popup_config_slider_final_delay, null)
        val popupWindow by lazy {
          PopupWindow(
            popupView,
            binding.root.width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
          ).apply {
            elevation = TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP,
              POPUP_WINDOW_ELEVATION,
              resources.displayMetrics
            )
            isOutsideTouchable = true
          }
        }
        // popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        */
/*
        popupView.findViewById<MaterialTextView>(R.id.mtv_guide).apply {
          visibility = VISIBLE
          text = context.getString(R.string.add_a_pause_at_the_end_of_the_gif)
        }
        *//*

        sliderInPopupView = popupView.findViewById<Slider>(R.id.slider).apply {
          addOnChangeListener { it, value, _ ->
            val selectedText = allOptionsMap.keys.elementAt((value).toInt())
            mtvSelectedKey.text = selectedText
            MySettings.gifFinalDelay = selectedValue()
            it.setLabelFormatter {
              return@setLabelFormatter selectedText
            }
          }
          valueTo = allOptionsMap.size.toFloat() - 1
          value = allOptionsMap.keys.indexOf(mtvSelectedKey.text).toFloat()
          setLabelFormatter { return@setLabelFormatter mtvSelectedKey.text.toString() }
        }
        binding.root.setOnClickListener {
          popupWindow.showAsDropDown(it)
        }
      }

      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS_COLOR_QUALITY -> {
        val menuItemList = allOptionsMap.keys
        val popupView = LayoutInflater.from(context)
          .inflate(R.layout.view_popup_config_chips_color_quality, null)
        val popupWindow by lazy {
          PopupWindow(
            popupView,
            binding.root.width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
          ).apply {
            elevation = TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP,
              POPUP_WINDOW_ELEVATION,
              resources.displayMetrics
            )
            isOutsideTouchable = true
          }
        }
        //  popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        chipGroup = popupView.findViewById(R.id.chip_group)
        menuItemList.forEach { menuItemName ->
          chipGroup.addView(
            Chip(
              ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter
              )
            ).apply {
              text = menuItemName
              chipBackgroundColor = Toolbox.createColorStateListFromColorResource(
                arrayOf(
                  android.R.attr.state_checked to R.color.green_light,
                  android.R.attr.state_checkable to R.color.light
                )
              )
              isCheckable = true
              isCheckedIconVisible = false
              if (text == mtvSelectedKey.text) {
                isChecked = true
              }
              popupView.findViewById<MaterialCheckBox>(R.id.mcb_better_color_accuracy)
                .apply {
                  //TODO  isChecked = MySettings.previousGifConfigAnalyzeVideoSlowly
                  setOnCheckedChangeListener { buttonView, isChecked ->
                    //TODO        MySettings.previousGifConfigAnalyzeVideoSlowly = isChecked
                  }
                }
              setOnClickListener { clickedChip ->
                (clickedChip as Chip).isChecked = true
                mtvSelectedKey.text = clickedChip.text
                popupWindow.dismiss()
              }
            }
          )
        }
        binding.root.setOnClickListener {
          popupWindow.showAsDropDown(it)
        }
      }

      else -> {
        throw IllegalArgumentException("customMenuItemViewType = $customMenuItemViewType")
      }
    }
  }
*/

  /*
  fun setUpWithDropDownView(
  ) {

  }
   */

  /*
  fun setUpAsIntSetting(allOptionsMap: LinkedHashMap<String, Int>, settingsKey: String, defValue: Int) {
    this.allOptionsMap = allOptionsMap
    mtvSelectedKey.text = allOptionsMap.getKeyByValue(MySettings.getInt(settingsKey, defValue))
    binding.root.setOnClickListener {
      val updatedSelectedValue = (selectedValue() + 1) % allOptionsMap.size
      MySettings.setInt(settingsKey, updatedSelectedValue)
      mtvSelectedKey.text = allOptionsMap.getKeyByValue(updatedSelectedValue)
    }
  }
   */

  fun setUpWithLambda(lambda: () -> Unit) {
    binding.root.setOnClickListener {
      lambda()
    }
  }

  fun setSelectedValue(value: Int) {
    mtvSelectedKey.text = allOptionsMap.getKeyByValue(value)
    when (customMenuItemViewType) {
      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY -> {
        sliderInPopupView.value = allOptionsMap.values.indexOf(value).toFloat()
      }

      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS_COLOR_QUALITY -> {
        val outViews: ArrayList<View> = ArrayList()
        chipGroup.findViewsWithText(
          outViews,
          allOptionsMap.getKeyByValue(value),
          FIND_VIEWS_WITH_TEXT
        )
        (outViews.first() as Chip).isChecked = true
      }

      else -> {
        throw NotImplementedError("customMenuItemViewType = $customMenuItemViewType")
      }
      // do nothing
    }
  }
}