package top.xjunz.library.automator;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author xjunz 2021/6/30
 */
public class AutomatorConfig implements Parcelable {
    boolean fallbackInjectingEvents = false;
    boolean detectRegion = true;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        dest.writeByte(this.fallbackInjectingEvents ? (byte) 1 : (byte) 0);
        dest.writeByte(this.detectRegion ? (byte) 1 : (byte) 0);
    }

    public void readFromParcel(@NotNull Parcel source) {
        this.fallbackInjectingEvents = source.readByte() != 0;
        this.detectRegion = source.readByte() != 0;
    }

    public AutomatorConfig() {
    }

    protected AutomatorConfig(@NotNull Parcel in) {
        this.fallbackInjectingEvents = in.readByte() != 0;
        this.detectRegion = in.readByte() != 0;
    }

    public static final Parcelable.Creator<AutomatorConfig> CREATOR = new Parcelable.Creator<AutomatorConfig>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public AutomatorConfig createFromParcel(Parcel source) {
            return new AutomatorConfig(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public AutomatorConfig[] newArray(int size) {
            return new AutomatorConfig[size];
        }
    };
}
