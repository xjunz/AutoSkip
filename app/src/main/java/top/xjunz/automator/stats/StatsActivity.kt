package top.xjunz.automator.stats

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import top.xjunz.automator.R
import top.xjunz.automator.app.RECORD_FILE_NAME
import top.xjunz.automator.databinding.ActivityStatsBinding
import top.xjunz.automator.databinding.ItemRecordBinding
import top.xjunz.automator.stats.model.SortBy
import top.xjunz.automator.util.desaturatedMyIconDrawable
import top.xjunz.automator.util.formatTime

/**
 * @author xjunz 2021/8/11
 */
class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding

    private val statsViewModel by lazy {
        ViewModelProvider(this).get(StatsViewModel::class.java)
    }

    private var adapter: RecordAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_stats)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            finishAfterTransition()
        }

        statsViewModel.recordList.observe(this) {
            updateList()
        }
        statsViewModel.readRecordsWhenNecessary(openFileInput(RECORD_FILE_NAME).fd)
    }

    private fun updateList() {
        if (adapter == null) {
            adapter = RecordAdapter()
            binding.rvRecord.adapter = adapter
        } else {
            statsViewModel.dispatchRecordsChangeTo(adapter!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.stats, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val sortBy = statsViewModel.getSortBy()
        menu?.findItem(R.id.item_ascending)?.isChecked = sortBy.isAscending()
        menu?.findItem(sortBy.menuItemId)?.isChecked = true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_ascending -> statsViewModel.revertOrder()
            R.id.item_by_count -> statsViewModel.setSortBy(SortBy.Count)
            R.id.item_by_freq -> statsViewModel.setSortBy(SortBy.Frequency)
            R.id.item_by_latest_timestamp -> statsViewModel.setSortBy(SortBy.LatestTimestamp)
            R.id.item_by_first_timestamp -> statsViewModel.setSortBy(SortBy.FirstTimestamp)
            R.id.item_by_label -> statsViewModel.setSortBy(SortBy.Label)
        }
        return super.onOptionsItemSelected(item)
    }


    inner class RecordAdapter : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
            return RecordViewHolder(ItemRecordBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            holder.binding.run {
                val record = statsViewModel.requireRecords()[position]
                statsViewModel.viewModelScope.launch {
                    val icon = record.loadIcon()
                    if (icon != null) {
                        ivAppIcon.setImageBitmap(icon)
                    } else {
                        ivAppIcon.setImageDrawable(desaturatedMyIconDrawable)
                    }
                    val label = record.getLabel()
                    if (label.isNullOrBlank()) {
                        tvAppName.text = getString(R.string.unknown_app)
                    } else {
                        tvAppName.text = label
                    }
                }
                tvCount.text = getString(R.string.format_count, record.source.count)
                tvTimestamp.text = formatTime(record.source.latestTimestamp)
            }
        }

        private val detailViewModel by lazy {
            ViewModelProvider(this@StatsActivity).get(DetailFragment.DetailViewModel::class.java)
        }

        inner class RecordViewHolder constructor(val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    detailViewModel.apply {
                        appIcon = binding.ivAppIcon.drawable
                        appName = binding.tvAppName.text
                        setRecordWrapper(statsViewModel.requireRecords()[adapterPosition])
                    }
                    DetailFragment().show(supportFragmentManager, "detail")
                }
            }
        }

        override fun getItemCount() = statsViewModel.requireRecords().size
    }
}