package top.xjunz.automator.stats.model

import androidx.annotation.IdRes
import top.xjunz.automator.R

/**
 * @author xjunz 2021/8/23
 */
sealed class SortBy constructor(var order: Int, @IdRes val menuItemId: Int) {
    companion object {
        const val ASCENDING = 1
        const val DESCENDING = -1
    }

    fun isAscending() = order == ASCENDING

    fun revertOrder() {
        order = -order
    }

    object Count : SortBy(DESCENDING, R.id.item_by_count)
    object Frequency : SortBy(DESCENDING, R.id.item_by_freq)
    object LatestTimestamp : SortBy(DESCENDING, R.id.item_by_latest_timestamp)
    object FirstTimestamp : SortBy(ASCENDING, R.id.item_by_first_timestamp)
    object Label : SortBy(ASCENDING, R.id.item_by_label)

}
