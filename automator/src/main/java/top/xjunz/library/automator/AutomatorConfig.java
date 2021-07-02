package top.xjunz.library.automator;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author xjunz 2021/6/30
 */
public class AutomatorConfig implements Parcelable {
    boolean fallbackInjectingEvents = true;
    boolean checkRegion = true;
    boolean checkSize = true;
    boolean checkDigit = true;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.fallbackInjectingEvents ? (byte) 1 : (byte) 0);
        dest.writeByte(this.checkRegion ? (byte) 1 : (byte) 0);
        dest.writeByte(this.checkSize ? (byte) 1 : (byte) 0);
    }

    public void readFromParcel(Parcel source) {
        this.fallbackInjectingEvents = source.readByte() != 0;
        this.checkRegion = source.readByte() != 0;
        this.checkSize = source.readByte() != 0;
    }

    public AutomatorConfig() {
    }

    protected AutomatorConfig(Parcel in) {
        this.fallbackInjectingEvents = in.readByte() != 0;
        this.checkRegion = in.readByte() != 0;
        this.checkSize = in.readByte() != 0;
    }

    public static final Creator<AutomatorConfig> CREATOR = new Creator<AutomatorConfig>() {
        @Override
        public AutomatorConfig createFromParcel(Parcel source) {
            return new AutomatorConfig(source);
        }

        @Override
        public AutomatorConfig[] newArray(int size) {
            return new AutomatorConfig[size];
        }
    };
}
