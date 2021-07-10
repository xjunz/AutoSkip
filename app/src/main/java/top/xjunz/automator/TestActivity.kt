package top.xjunz.automator

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import top.xjunz.automator.databinding.ActivityTestBinding
import top.xjunz.library.automator.OnTestResultListener
import top.xjunz.library.automator.Result

/**
 * @author xjunz 2021/7/10
 */
class TestActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(application as AutomatorApp).get(AutomatorViewModel::class.java)
    }
    private lateinit var binding: ActivityTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test)
    }

    fun startTesting(view: View) {
        val res = viewModel.startTesting(object : OnTestResultListener.Stub() {
            override fun onTestResult(result: Result?) {
                runOnUiThread {  }
            }
        })
        if (!res) {
            Log.i("automator", "Launch testing  failed!z")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTesting()
    }
}