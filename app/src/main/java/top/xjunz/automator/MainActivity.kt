package top.xjunz.automator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import top.xjunz.library.automator.AutomatorFactory

/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        object :Thread(){
            override fun run() {
                super.run()
                sleep(3000)
                AutomatorFactory.getAutomator(AutomatorFactory.Mode.ROOT).performClick(1000f,1000f)
            }
        }.start()
       // Toast.makeText(this,Hyper::class.java.name,Toast.LENGTH_SHORT).show()
    }
}