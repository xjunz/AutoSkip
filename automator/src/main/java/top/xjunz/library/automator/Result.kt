package top.xjunz.library.automator

import android.os.Parcel
import android.os.Parcelable
import android.view.accessibility.AccessibilityNodeInfo

/**
 * @author xjunz 2021/7/12
 */
data class Result(var passed: Boolean, var maskedReason: Int, var node: AccessibilityNodeInfo) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readParcelable(AccessibilityNodeInfo::class.java.classLoader)!!) {
    }

    fun getReason() = maskedReason and REASON_MASK_PARENT.inv()
    fun dueToChild() = maskedReason ushr 31 == 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (passed) 1 else 0)
        parcel.writeInt(maskedReason)
        parcel.writeParcelable(node, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Result> {
        const val REASON_MASK_CHILD = 0
        const val REASON_MASK_PARENT = 1 shl 31
        const val REASON_ILLEGAL_SIZE = 1 shl 1
        const val REASON_ILLEGAL_LOCATION = 1 shl 2
        const val REASON_NOT_CLICKABLE = 1 shl 3
        const val REASON_ILLEGAL_TEXT = 1 shl 4
        override fun createFromParcel(parcel: Parcel): Result {
            return Result(parcel)
        }

        override fun newArray(size: Int): Array<Result?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "passed: $passed, reason: ${getReason()}, due2child: ${dueToChild()}\nnode: $node"
    }

}
