package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED
import me.tasy5kg.cutegif.MyConstants.UNKNOWN_INT
import me.tasy5kg.cutegif.MyToolbox.getKeyByValue
import me.tasy5kg.cutegif.databinding.ViewCustomMenuItemBinding

class CustomMenuItemView(context: Context, attrs: AttributeSet) : LinearLayoutCompat(context, attrs) {
  private val binding: ViewCustomMenuItemBinding
  private val mtvTitle: MaterialTextView
  private val mtvSubTitle: MaterialTextView
  private val mtvSelectedKey: MaterialTextView
  private var customMenuItemViewType: Int = UNKNOWN_INT
  private lateinit var allOptionsMap: LinkedHashMap<String, Int>
  private lateinit var sliderInPopupView: Slider
  private lateinit var chipGroup: ChipGroup

  init {
    binding = ViewCustomMenuItemBinding.inflate(LayoutInflater.from(context), this, true)
    mtvTitle = binding.mtvTitle
    mtvSubTitle = binding.mtvSubTitle
    mtvSelectedKey = binding.mtvSelectedKey
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
    if (!mtvSubTitle.text.isNullOrBlank()) {
      mtvSubTitle.visibility = VISIBLE
    } else {
      mtvSubTitle.visibility = GONE
    }
    if (mtvSelectedKey.text.isNullOrBlank()) {
      mtvSelectedKey.visibility = GONE
    } else {
      mtvSelectedKey.visibility = VISIBLE
    }
  }

  fun selectedValue() = allOptionsMap[mtvSelectedKey.text]!!

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
        val popupView = LayoutInflater.from(context).inflate(R.layout.view_popup_config_slider, null)
        val popupWindow by lazy {
          PopupWindow(popupView, binding.root.width, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MyConstants.POPUP_WINDOW_ELEVATION, resources.displayMetrics)
          }
        }
        // popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        sliderInPopupView = popupView.findViewById<Slider>(R.id.slider).apply {
          addOnChangeListener { it, value, _ ->
            val speedText = allOptionsMap.keys.elementAt((value).toInt())
            mtvSelectedKey.text = speedText
            popupView.findViewById<MaterialTextView>(R.id.mtv_guide).apply {
              if (selectedValue() == MyConstants.GIF_SPEED_GLANCE_MODE) {
                text = context.getString(R.string.glance_mode_guide_text)
                visibility = VISIBLE
              } else {
                visibility = GONE
              }
            }
            it.setLabelFormatter {
              return@setLabelFormatter speedText
            }
          }
          valueFrom = 0f
          valueTo = MyConstants.GIF_SPEED_MAP.size.toFloat() - 1
          value = MyConstants.GIF_SPEED_MAP.keys.indexOf(mtvSelectedKey.text).toFloat()
          setLabelFormatter { return@setLabelFormatter mtvSelectedKey.text.toString() }
        }
        binding.root.setOnClickListener {
          popupWindow.showAsDropDown(it)
        }
      }
      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS -> {
        val menuItemList = allOptionsMap.keys
        val popupView = LayoutInflater.from(context).inflate(R.layout.view_popup_config_chips, null)
        val popupWindow by lazy {
          PopupWindow(popupView, binding.root.width, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MyConstants.POPUP_WINDOW_ELEVATION, resources.displayMetrics)
          }
        }
        //  popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        chipGroup = popupView.findViewById(R.id.chip_group)
        menuItemList.forEach { menuItemName ->
          chipGroup.addView(Chip(ContextThemeWrapper(context, com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
            text = menuItemName
            chipBackgroundColor = MyToolbox.createColorStateList(
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
        val popupView = LayoutInflater.from(context).inflate(R.layout.view_popup_config_slider, null)
        val popupWindow by lazy {
          PopupWindow(popupView, binding.root.width, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MyConstants.POPUP_WINDOW_ELEVATION, resources.displayMetrics)
          }
        }
        // popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
        popupView.findViewById<MaterialTextView>(R.id.mtv_guide).apply {
          visibility = VISIBLE
          text = context.getString(R.string.pause_for_a_while_at_the_end_of_the_gif)
        }
        sliderInPopupView = popupView.findViewById<Slider>(R.id.slider).apply {
          addOnChangeListener { it, value, _ ->
            val selectedText = allOptionsMap.keys.elementAt((value).toInt())
            mtvSelectedKey.text = selectedText
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
      else -> {
        throw IllegalArgumentException("customMenuItemViewType = $customMenuItemViewType")
      }
    }
  }

  /*
  fun setUpWithDropDownView(
  ) {

  }
   */

  fun setUpAsIntSetting(allOptionsMap: LinkedHashMap<String, Int>, settingsKey: String, defValue: Int) {
    this.allOptionsMap = allOptionsMap
    mtvSelectedKey.text = allOptionsMap.getKeyByValue(MySettings.getInt(settingsKey, defValue))
    binding.root.setOnClickListener {
      val updatedSelectedValue = (selectedValue() + 1) % allOptionsMap.size
      MySettings.setInt(settingsKey, updatedSelectedValue)
      mtvSelectedKey.text = allOptionsMap.getKeyByValue(updatedSelectedValue)
    }
  }

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
      CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS -> {
        val outViews: ArrayList<View> = ArrayList()
        chipGroup.findViewsWithText(outViews, allOptionsMap.getKeyByValue(value), FIND_VIEWS_WITH_TEXT)
        (outViews.first() as Chip).isChecked = true
      }
      else -> {
        throw IllegalArgumentException("customMenuItemViewType = $customMenuItemViewType")
      }
      // do nothing
    }
  }
}