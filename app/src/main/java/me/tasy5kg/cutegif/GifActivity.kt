package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformationSession
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import me.tasy5kg.cutegif.Constants.DOUBLE_BACK_TO_EXIT_DELAY
import me.tasy5kg.cutegif.Constants.GIF_COLOR_QUALITY_MAP
import me.tasy5kg.cutegif.Constants.GIF_FRAME_RATE_MAP
import me.tasy5kg.cutegif.Constants.GIF_RESOLUTION_MAP
import me.tasy5kg.cutegif.Constants.GIF_SPEED_MAP
import me.tasy5kg.cutegif.Constants.PALETTE_PATH
import me.tasy5kg.cutegif.Constants.THUMBNAIL_PATH
import me.tasy5kg.cutegif.databinding.ActivityGifBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@SuppressLint("InflateParams")
class GifActivity : AppCompatActivity() {
    private var gifViewRatio = 0.0
    private var converting = false
    private var doubleBackToExitPressedOnce = false
    private lateinit var binding: ActivityGifBinding
    private lateinit var inputVideoUri: Uri
    private lateinit var mbConvert: MaterialButton
    private lateinit var linearProgressIndicator: LinearProgressIndicator
    private lateinit var videoInformationSession: MediaInformationSession
    private lateinit var cmivResolution: CustomMenuItemView
    private lateinit var cmivColorQuality: CustomMenuItemView
    private lateinit var cmivFrameRate: CustomMenuItemView
    private lateinit var cmivSpeed: CustomMenuItemView
    private lateinit var gifUri: Uri
    private lateinit var materialToolbar: MaterialToolbar
    private lateinit var mbClose: MaterialButton
    private lateinit var viewMaskLayer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGifBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFinishOnTouchOutside(false)
        materialToolbar = binding.materialToolbar
        setSupportActionBar(materialToolbar)
        binding.llcMoreOptionsButton.setOnClickListener {
            it.visibility = View.GONE
            binding.llcMoreOptionsGroup.visibility = View.VISIBLE
        }
        linearProgressIndicator = binding.linearProgressIndicator
        inputVideoUri = intent.getParcelableExtra(EXTRA_NAME_VIDEO_URI) ?: intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.data ?: Uri.EMPTY
        if (inputVideoUri == Uri.EMPTY) {
            loadVideoFailed()
            return
        }
        videoInformationSession = FFprobeKit.getMediaInformation(inputVideoSaf())
        viewMaskLayer = binding.viewMaskLayer
        mbConvert = binding.mbConvert.apply { setOnClickListener { onConvertClick() } }
        mbClose = binding.mbCancel.apply {
            setOnClickListener {
                conversionUnsuccessfully(canceledByUser = true, finishActivity = true)
            }
        }
        if (Toolbox.isFirstStart()) {
            AboutActivity.start(this@GifActivity)
        }
        loadCustomMenuItemView()
        loadThumbnail()

    }

    private fun loadCustomMenuItemView() {
        cmivResolution = binding.cmivResolution
        cmivColorQuality = binding.cmivColorQuality
        cmivFrameRate = binding.cmivFrameRate
        cmivSpeed = binding.cmivSpeed
        cmivResolution.setUpWithDropDownConfig(GIF_RESOLUTION_MAP, R.string.resolution_guide)
        cmivColorQuality.setUpWithDropDownConfig(GIF_COLOR_QUALITY_MAP, R.string.color_quality_guide)
        cmivFrameRate.setUpWithDropDownConfig(GIF_FRAME_RATE_MAP, R.string.frame_rate_guide)
        cmivSpeed.setUpWithDropDownConfig(GIF_SPEED_MAP, R.string.speed_guide, true)

    }

    private fun loadThumbnail() {
        try {
            FFmpegKit.executeAsync("-hwaccel mediacodec -hide_banner -benchmark -an -i ${inputVideoSaf()} -vframes 1 -lavfi scale=${resolutionPara(false)}:flags=lanczos -y $THUMBNAIL_PATH") {
                if (it.returnCode.isValueSuccess) {
                    runOnUiThread {
                        Glide.with(this@GifActivity)
                            .load(THUMBNAIL_PATH)
                            .fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .transition(withCrossFade())
                            .into(binding.aciv)
                    }
                } else {
                    loadVideoFailed()
                }
            }
        } catch (e: Exception) {
            loadVideoFailed()
        }
    }

    private fun loadVideoFailed() {
        runOnUiThread {
            Toast.makeText(this@GifActivity, getString(R.string.load_video_failed), Toast.LENGTH_LONG).show()
            finishAndRemoveTask()
        }
    }

    private fun updateConvertingState() {
        runOnUiThread {
            converting = !converting
            if (converting) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                viewMaskLayer.visibility = View.VISIBLE
                linearProgressIndicator.apply {
                    visibility = View.VISIBLE
                    progress = 0
                    isIndeterminate = true
                }
                materialToolbar.subtitle = getString(R.string.converting)
                mbClose.visibility = View.GONE
                mbConvert.apply {
                    text = getString(R.string.cancel)
                    icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_close_24)
                }
            } else {
                conversionUnsuccessfully(canceledByUser = true, finishActivity = false)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                viewMaskLayer.visibility = View.GONE
                linearProgressIndicator.apply {
                    progress = 0
                    isIndeterminate = true
                    visibility = View.GONE
                }
                mbConvert.text = getString(R.string.convert_to_gif)
                materialToolbar.subtitle = getString(R.string.conversion_canceled)
                mbClose.visibility = View.VISIBLE
                mbConvert.apply {
                    text = getString(R.string.convert)
                    icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_video_library_24)
                }
            }

        }

    }

    private fun conversionSuccessfully() {
        converting = false
        runOnUiThread {
            linearProgressIndicator.visibility = View.GONE
            binding.frameLayoutGoneWhenFinished.visibility = View.GONE
            viewMaskLayer.visibility = View.GONE
            materialToolbar.subtitle = getString(R.string.gif_saved_s_mb, Toolbox.keepNDecimalPlaces(Toolbox.getFileSizeFromUri(gifUri) / 1048576.0, 2))
            Glide.with(this@GifActivity)
                .load(gifUri)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.aciv.apply {
                    this.layoutParams.height =
                        min((this@GifActivity.resources.displayMetrics.heightPixels * 0.5).toInt(), (this@apply.width * gifViewRatio).toInt())
                    requestLayout()
                })
            mbConvert.apply {
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                text = getString(R.string.done)
                icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_done_24)
                setOnClickListener {
                    finishAndRemoveTask()
                }
            }
            mbClose.apply {
                visibility = View.VISIBLE
                text = getString(R.string.share)
                setOnClickListener {
                    startActivity(Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, gifUri)
                        type = "image/gif"
                    }, "Share GIF to"))
                }

            }

            binding.space.visibility = View.VISIBLE
            binding.mbDelete.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    finishAndRemoveTask()
                    try {
                        contentResolver.delete(gifUri, null, null)
                    } catch (e: Exception) {
                    }
                    Toast.makeText(this@GifActivity, getString(R.string.gif_deleted), Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    private fun onConvertClick() {
        updateConvertingState()
        if (converting) {
            gifUri = Toolbox.createNewGifFileAndReturnUri(Toolbox.getFileNameFromUri(inputVideoUri, true))
            val command1 =
                "-hwaccel mediacodec -hide_banner -benchmark -an -skip_frame nokey -i ${inputVideoSaf()} -vf scale=${resolutionPara(true)}:flags=fast_bilinear,palettegen=max_colors=${
                    cmivColorQuality.getSelectedValue()
                }:stats_mode=diff -y $PALETTE_PATH"
            Toolbox.logging("command1", command1)
            FFmpegKit.executeAsync(command1) {
                when {
                    it.returnCode.isValueSuccess -> {
                        val outputFpsTarget = cmivFrameRate.getSelectedValue()
                        val outputSpeed = cmivSpeed.getSelectedValue() / 100.0
                        var frameStep = round(inputVideoFps() * outputSpeed / outputFpsTarget).toInt()
                        if (frameStep < 1) {
                            frameStep = 1
                        }
                        val outputFpsReal = inputVideoFps() * outputSpeed / frameStep
                        val command2 =
                            "-hwaccel mediacodec -hide_banner -benchmark -an -i ${inputVideoSaf()} -i $PALETTE_PATH -r $outputFpsReal -lavfi \"framestep=$frameStep," +
                                    "setpts=PTS/$outputSpeed,scale=${resolutionPara(false)}:flags=lanczos [x]; [x][1:v] paletteuse=dither=bayer\" -y ${outputGifSaf()}"
                        runOnUiThread {
                            linearProgressIndicator.isIndeterminate = false
                        }

                        val outputFramesTotalEstimated = inputVideoDuration() * inputVideoFps() / frameStep
                        Toolbox.logging("estimated", (inputVideoDuration() * inputVideoFps()).toString())
                        var progress: Int
                        Toolbox.logging("command2", command2)
                        FFmpegKit.executeAsync(command2, { ffmpegSession ->
                            runOnUiThread {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                            when {
                                ffmpegSession.returnCode.isValueSuccess -> {
                                    conversionSuccessfully()
                                }
                                ffmpegSession.returnCode.isValueError ->
                                    conversionUnsuccessfully(canceledByUser = false, finishActivity = true)
                            }
                        }, { log ->
                            Toolbox.logging("logcallback", log.message.toString())
                        }, {
                            runOnUiThread {
                                progress = (it.videoFrameNumber * 100 / outputFramesTotalEstimated).toInt()
                                if (progress >= 99) {
                                    progress = 99
                                }
                                linearProgressIndicator.setProgress(progress, true)
                                materialToolbar.subtitle = getString(R.string.converting_s_s_mb, progress, Toolbox.keepNDecimalPlaces(it.size / 1048576.0, 2))
                            }
                        })
                    }

                    it.returnCode.isValueError ->
                        conversionUnsuccessfully(canceledByUser = false, finishActivity = true)
                }

            }
        }
    }

    private fun inputVideoDuration() = videoInformationSession.mediaInformation.duration.toDouble()

    private fun inputVideoSaf() = FFmpegKitConfig.getSafParameterForRead(this, inputVideoUri)

    private fun outputGifSaf() = FFmpegKitConfig.getSafParameterForWrite(this, gifUri)

    private fun conversionUnsuccessfully(canceledByUser: Boolean, finishActivity: Boolean) {
        runOnUiThread {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (!canceledByUser) {
                Toast.makeText(this@GifActivity, getString(R.string.conversion_failed), Toast.LENGTH_LONG).show()
            }
            FFmpegKitConfig.clearSessions()
            FFmpegKit.cancel()
            if (::gifUri.isInitialized) {
                try {
                    contentResolver.delete(gifUri, null, null)
                } catch (e: Exception) {
                }
            }
            if (finishActivity) {
                finishAndRemoveTask()
            }
        }
    }

    private fun inputVideoFps(): Double {
        val fpsFraction = videoInformationSession.mediaInformation.streams.first { it.type == "video" }.averageFrameRate
        val numerator = fpsFraction.split("/").toTypedArray()[0].toInt()
        val denominator = fpsFraction.split("/").toTypedArray()[1].toInt()
        return numerator.toDouble() / denominator
    }

    private fun resolutionPara(min: Boolean = false): String {
        val videoStreamInfo = videoInformationSession.mediaInformation.streams.first { it.type == "video" }
        val rotation =
            try {
                ((videoStreamInfo.allProperties.get("side_data_list") as JSONArray).get(0) as JSONObject).getInt("rotation")
            } catch (e: Exception) {
                0
            }
        val pixel = if (min) {
            GIF_RESOLUTION_MAP.values.min()
        } else {
            cmivResolution.getSelectedValue()
        }
        val long = max(videoStreamInfo.width, videoStreamInfo.height)
        val short = min(videoStreamInfo.width, videoStreamInfo.height)
        return if ((videoStreamInfo.width > videoStreamInfo.height) == (rotation % 180 == 0)) {
            gifViewRatio = short.toDouble() / long
            "-2:$pixel"
        } else {
            gifViewRatio = long.toDouble() / short
            "$pixel:-2"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_gif, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_close -> {
                conversionUnsuccessfully(canceledByUser = true, finishActivity = true)
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (!(converting && !doubleBackToExitPressedOnce)) {
            conversionUnsuccessfully(canceledByUser = true, finishActivity = true)
            super.onBackPressed()
            return
        }
        doubleBackToExitPressedOnce = true
        Toast.makeText(this@GifActivity, getString(R.string.press_back_again_to_quit), Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, DOUBLE_BACK_TO_EXIT_DELAY)
    }

    companion object {
        const val EXTRA_NAME_VIDEO_URI = "EXTRA_NAME_VIDEO_URI"
        fun start(context: Context, videoUri: Uri) {
            context.startActivity(Intent(context, GifActivity::class.java).putExtra(EXTRA_NAME_VIDEO_URI, videoUri))
        }
    }
}