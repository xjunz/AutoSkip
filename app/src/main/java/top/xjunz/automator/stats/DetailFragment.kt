package top.xjunz.automator.stats

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import top.xjunz.automator.R
import top.xjunz.automator.app.AutomatorApp
import top.xjunz.automator.databinding.FragmentDetailBinding
import top.xjunz.automator.model.Record
import top.xjunz.automator.stats.model.RecordWrapper
import top.xjunz.automator.util.formatTime
import top.xjunz.automator.util.setVisible

/**
 * @author xjunz 2021/8/13
 */
class DetailFragment : DialogFragment() {
    private lateinit var binding: FragmentDetailBinding
    private lateinit var record: Record
    private lateinit var wrapper: RecordWrapper
    private lateinit var appIcon: Drawable
    private lateinit var appName: CharSequence
    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog)
    }

    fun setRecordWrapper(wrapper: RecordWrapper): DetailFragment {
        this.wrapper = wrapper
        this.record = wrapper.source
        return this
    }

    fun setIcon(icon: Drawable): DetailFragment {
        this.appIcon = icon
        return this
    }

    fun setAppName(name: CharSequence): DetailFragment {
        this.appName = name
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDetailBinding.inflate(inflater, container, true)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        handler.postDelayed({ initDummyFrame() }, 200)
    }

    private fun initViews() {
        binding.apply {
            tvAppName.text = appName
            ivAppIcon.setImageDrawable(appIcon)
            tvPkgName.text = record.pkgName
            if (record.firstTimestamp > 0) {
                tvStartTime.text = getString(R.string.format_first_time, formatTime(record.firstTimestamp))
            } else {
                tvStartTime.setVisible(false)
            }
            if (record.latestTimestamp > 0) {
                tvEndTime.text = getString(R.string.format_latest_time, formatTime(record.latestTimestamp))
                if (record.firstTimestamp > 0) {
                    tvDuration.text = wrapper.getFormattedDuration()
                    tvAveCount.text = getString(R.string.format_count_per_day, wrapper.getFrequencyPerDay())
                } else {
                    tvAveCount.setVisible(false)
                    tvDuration.setVisible(false)
                }
            } else {
                tvEndTime.setVisible(false)
                tvAveCount.setVisible(false)
            }
            tvCount.text = getString(R.string.format_total_count, record.count)
        }
    }


    private fun initDummyFrame() {
        val rect = record.portraitBounds ?: record.landscapeBounds ?: return
        binding.run {
            val dummyCloseupShrinkRatio = 3 / 4f
            dummyFrameCloseup.apply {
                visibility = View.VISIBLE
                layoutParams = layoutParams.apply {
                    height = (AutomatorApp.getScreenHeight() / 4f).toInt()
                }
                post {
                    dummyTargetCloseup.apply {
                        visibility = View.VISIBLE
                        width = (rect.width() * dummyCloseupShrinkRatio).toInt()
                        height = (rect.height() * dummyCloseupShrinkRatio).toInt()
                        text = record.text
                        startCountDown()
                        if (rect.centerY() < AutomatorApp.getScreenHeight() / 2) {
                            dummyFrameCloseup.setBackgroundResource(R.drawable.bg_frame_closeup_top)
                            x = (dummyFrameCloseup.width - rect.width() * dummyCloseupShrinkRatio -
                                    (AutomatorApp.getScreenWidth() - rect.right) * dummyCloseupShrinkRatio)
                            y = rect.top.toFloat() * dummyCloseupShrinkRatio
                        } else {
                            dummyFrameCloseup.setBackgroundResource(R.drawable.bg_frame_closeup_bottom)
                            x = dummyFrameCloseup.width - rect.width() * dummyCloseupShrinkRatio -
                                    (AutomatorApp.getScreenWidth() - rect.right) * dummyCloseupShrinkRatio
                            y = dummyFrameCloseup.height - rect.height() * dummyCloseupShrinkRatio -
                                    (AutomatorApp.getScreenHeight() - rect.bottom) * dummyCloseupShrinkRatio
                        }
                    }
                }
            }
        }
    }

    private var countDownRunnable: Runnable? = null
    private fun startCountDown() {
        record.text?.run {
            var countdown = -1
            var index = -1
            forEachIndexed { i, char ->
                if (char in '0'..'9') {
                    index = i
                    countdown = char.digitToInt()
                    return@forEachIndexed
                }
            }
            if (countdown < 0) {
                return
            }
            val initialCountDown = countdown
            countDownRunnable = object : Runnable {
                override fun run() {
                    countdown -= 1
                    if (countdown <= 0) {
                        countdown = initialCountDown
                    }
                    binding.dummyTargetCloseup.text = replaceRange(index, index + 1, countdown.toString())
                    handler.postDelayed(this, 1000)
                }
            }
            handler.postDelayed(countDownRunnable!!, 1000)
        }
    }

    override fun dismiss() {
        super.dismiss()
        countDownRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

}