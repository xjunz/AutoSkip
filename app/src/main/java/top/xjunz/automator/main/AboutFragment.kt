package top.xjunz.automator.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import me.zhanghai.android.appiconloader.AppIconLoader
import top.xjunz.automator.APP_DOWNLOAD_URL
import top.xjunz.automator.BuildConfig
import top.xjunz.automator.R
import top.xjunz.automator.databinding.FragmentAboutBinding
import top.xjunz.automator.util.donate
import top.xjunz.automator.util.dp2px
import top.xjunz.automator.util.viewUrl

/**
 * @author xjunz 2021/9/2
 */
class AboutFragment : DialogFragment() {
    private lateinit var binding: FragmentAboutBinding
    private val iconLoader by lazy {
        AppIconLoader(dp2px(48).toInt(), true, requireContext())
    }
    private val pm by lazy {
        requireActivity().packageManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Base_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            binding.ivIcon.setImageBitmap(iconLoader.loadIcon(pm.getApplicationInfo(BuildConfig.APPLICATION_ID, 0)))
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