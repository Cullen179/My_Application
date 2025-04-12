package com.example.myapplication.model;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class ScreenRecordConfig implements Parcelable {
    private final int resultCode;
    private final Intent data;

    public ScreenRecordConfig(int resultCode, Intent data) {
        this.resultCode = resultCode;
        this.data = data;
    }

    protected ScreenRecordConfig(Parcel in) {
        resultCode = in.readInt();
        data = in.readParcelable(Intent.class.getClassLoader());
    }

    public static final Creator<ScreenRecordConfig> CREATOR = new Creator<ScreenRecordConfig>() {
        @Override
        public ScreenRecordConfig createFromParcel(Parcel in) {
            return new ScreenRecordConfig(in);
        }

        @Override
        public ScreenRecordConfig[] newArray(int size) {
            return new ScreenRecordConfig[size];
        }
    };

    public int getResultCode() {
        return resultCode;
    }

    public Intent getData() {
        return data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(resultCode);
        parcel.writeParcelable(data, flags);
    }
}
