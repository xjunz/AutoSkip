package top.xjunz.automator.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import top.xjunz.automator.APP_DOWNLOAD_URL
import top.xjunz.automator.R
import top.xjunz.automator.databinding.FragmentAboutBinding
import top.xjunz.automator.util.donate
import top.xjunz.automator.util.myIcon
import top.xjunz.automator.util.viewUrl

/**
 * @author xjunz 2021/9/2
 */
class AboutFragment : DialogFragment() {
    private lateinit var binding: FragmentAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            binding.ivIcon.setImageBitmap(myIcon)
        }.onFailure {
            binding.ivIcon.setImageResource(R.mipmap.ic_launcher)
        }
        binding.btnDonate.setOnClickListener {
            donate(requireActivity())
        }
        binding.btnUpdate.setOnClickListener {
            viewUrl(requireActivity(), APP_DOWNLOAD_URL)
        }
    }


}