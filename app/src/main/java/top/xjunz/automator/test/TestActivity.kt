package top.xjunz.automator.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import top.xjunz.automator.OnCheckResultListener
import top.xjunz.automator.R
import top.xjunz.automator.app.AutomatorViewModel
import top.xjunz.automator.databinding.ActivityTestBinding
import top.xjunz.automator.model.Result

/**
 * @author xjunz 2021/7/10
 */
class TestActivity : AppCompatActivity() {
    private val viewModel by lazy {
        AutomatorViewModel.get()
    }
    private lateinit var binding: ActivityTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test)
        binding.host = this
    }


    private val resultListener: OnCheckResultListener by lazy {
        object : OnCheckResultListener.Stub() {
            override fun onCheckResult(result: Result?) = runOnUiThread {
                val text = if (result != null) {
                    if (result.passed) {
                        getString(R.string.test_ok)
                    } else {
                        getString(R.string.test_not_ok) + ":" + when (result.getReason()) {
                            Result.REASON_ILLEGAL_LOCATION -> getString(R.string.illegal_location)
                            Result.REASON_ILLEGAL_SIZE -> getString(R.string.illegal_size)
                            Result.REASON_ERROR -> getString(R.string.error)
                            Result.REASON_NONE -> getString(R.string.cannot_find)
                            else -> getString(R.string.unknown_problem)
                        }
                    }
                } else {
                    getString(R.string.test_not_ok)
                }
                blinkTextIfNecessary(text)
            }
        }
    }

    private fun blinkTextIfNecessary(newText: String) {
        binding.tvHint.run {
            if (text?.toString() != newText) {
                text = newText
                return
            }
            animate().alpha(0f).setDuration(62).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    animate().alpha(1f).setListener(null).start()
                }
            }).start()
        }
    }

    fun test(view: View) {
        viewModel.launchStandaloneCheck(resultListener)
    }
}