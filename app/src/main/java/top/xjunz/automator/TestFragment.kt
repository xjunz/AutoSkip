package top.xjunz.automator

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import top.xjunz.automator.databinding.FragmentTestBinding

/**
 * @author xjunz 2021/6/23
 */
class TestFragment : Fragment() {
    lateinit var binding: FragmentTestBinding
    private val timeSecondsLeft = ObservableInt()
    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }
    private var isFingerTouched = false
    private val countDownTask = object : Runnable {
        override fun run() {
            val left = timeSecondsLeft.get() - 1
            if (left == 0) {
                if (isAdded) {
                    requireFragmentManager().popBackStack()
                }
            } else {
                timeSecondsLeft.set(left)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentTestBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timeSecondsLeft.set(5)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            host = this@TestFragment
            countDown = timeSecondsLeft
            skip.setOnTouchListener { _, event ->
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> isFingerTouched = true
                    MotionEvent.ACTION_CANCEL -> handler.postDelayed({ isFingerTouched = false }, 17)
                    MotionEvent.ACTION_UP -> handler.postDelayed({ isFingerTouched = false }, 17)
                }
                false
            }
        }
        handler.postDelayed(countDownTask, 1000)
    }

    fun skip() {
        if (!isFingerTouched) {
            Toast.makeText(requireContext(), "测试成功", Toast.LENGTH_SHORT).show()
        }
        handler.removeCallbacks(countDownTask)
        requireFragmentManager().popBackStack()
    }
}