package top.xjunz.automator.stats

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    private val vm by lazy {
        ViewModelProvider(requireActivity()).get(DetailViewModel::class.java)
    }
    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    class DetailViewModel : ViewModel() {
        lateinit var record: Record
        lateinit var wrapper: RecordWrapper
        lateinit var appIcon: Drawable
        lateinit var appName: CharSequence
        fun setRecordWrapper(wrapper: RecordWrapper) {
            this.wrapper = wrapper
            this.record = wrapper.source
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog)
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
            tvAppName.text = vm.appName
            ivAppIcon.setImageDrawable(vm.appIcon)
            tvPkgName.text = vm.record.pkgName
            if (vm.record.firstTimestamp > 0) {
                tvStartTime.text = getString(R.string.format_first_time, formatTime(vm.record.firstTimestamp))
            } else {
                tvStartTime.setVisible(false)
            }
            if (vm.record.latestTimestamp > 0) {
                tvEndTime.text = getString(R.string.format_latest_time, formatTime(vm.record.latestTimestamp))
                if (vm.record.firstTimestamp > 0) {
                    tvDuration.text = vm.wrapper.getFormattedDuration()
                    tvAveCount.text = getString(R.string.format_count_per_day, vm.wrapper.getFrequencyPerDay())
                } else {
                    tvAveCount.setVisible(false)
                    tvDuration.setVisible(false)
                }
            } else {
                tvEndTime.setVisible(false)
                tvAveCount.setVisible(false)
            }
            tvCount.text = getString(R.string.format_total_count, vm.record.count)
        }
    }


    private fun initDummyFrame() {
        val rect = vm.record.portraitBounds ?: vm.record.landscapeBounds ?: return
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
                        text = vm.record.text
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
        vm.record.text?.run {
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