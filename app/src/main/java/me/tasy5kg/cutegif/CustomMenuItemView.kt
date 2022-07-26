package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import me.tasy5kg.cutegif.databinding.ViewCustomMenuItemBinding

class CustomMenuItemView(context: Context, attrs: AttributeSet) : LinearLayoutCompat(context, attrs) {
    private val binding: ViewCustomMenuItemBinding
    private val mtvTitle: MaterialTextView
    private val mtvSubTitle: MaterialTextView
    private val mtvSelectedKey: MaterialTextView
    private lateinit var allOptionsMap: LinkedHashMap<String, Int>

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

    fun getSelectedValue() = allOptionsMap[mtvSelectedKey.text]!!

    @SuppressLint("InflateParams")
    fun setUpWithDropDownConfig(
        allOptionsMap: LinkedHashMap<String, Int>,
        @StringRes guideText: Int,
        showAsSliderInsteadOfChip: Boolean = false,
    ) {
        this.allOptionsMap = allOptionsMap
        if (showAsSliderInsteadOfChip) {
            val popupView = LayoutInflater.from(context).inflate(R.layout.view_popup_config_speed, null)
            val popupWindow by lazy {
                PopupWindow(popupView, binding.root.width, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                    elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.POPUP_WINDOW_ELEVATION, resources.displayMetrics)
                }
            }
            popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
            popupView.findViewById<Slider>(R.id.slider).apply {
                addOnChangeListener { it, value, _ ->
                    val speedText = Constants.GIF_SPEED_MAP.keys.elementAt((value).toInt())
                    mtvSelectedKey.text = speedText
                    it.setLabelFormatter {
                        return@setLabelFormatter speedText
                    }
                }
                valueTo = Constants.GIF_SPEED_MAP.size.toFloat() - 1
                value = Constants.GIF_SPEED_MAP.keys.indexOf(mtvSelectedKey.text).toFloat()
            }
            binding.root.setOnClickListener {
                popupWindow.showAsDropDown(it)
            }
        } else {
            val menuItemList = allOptionsMap.keys
            val popupView = LayoutInflater.from(context).inflate(R.layout.view_popup_config, null)
            val popupWindow by lazy {
                PopupWindow(popupView, binding.root.width, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                    elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.POPUP_WINDOW_ELEVATION, resources.displayMetrics)
                }
            }
            popupView.findViewById<MaterialTextView>(R.id.mtv_guide).text = context.getString(guideText)
            val chipGroup = popupView.findViewById<ChipGroup>(R.id.chip_group)
            menuItemList.forEach { menuItemName ->
                chipGroup.addView(Chip(ContextThemeWrapper(context, com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
                    text = menuItemName
                    chipBackgroundColor = Toolbox.createColorStateList(
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
    }

    /*
    fun setUpWithDropDownView(
    ) {

    }
     */

    fun setUpWithLambda(lambda: () -> Unit) {
        binding.root.setOnClickListener {
            lambda()
        }
    }
}