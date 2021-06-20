package top.xjunz.automator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import top.xjunz.library.automator.impl.RootAutomator

/**
 *    @author xjunz 2021/6/20 21:05
 */
class MainActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RootAutomator.INSTANCE.performClick(0f,0f)
    }
}