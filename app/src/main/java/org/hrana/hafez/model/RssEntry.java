package org.hrana.hafez.model;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Item to hold RSS Feeds
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class RssEntry implements Parcelable {
    private String title;
    private String summary;
    private String expandedText;
    private String date;
    private String url;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(date);
        out.writeString(summary);
        out.writeString(title);
        out.writeString(url);
        out.writeString(expandedText);
    }

    public static final Parcelable.Creator<RssEntry> CREATOR = new Parcelable.Creator<RssEntry>() {
        public RssEntry createFromParcel(Parcel source) {
            return new RssEntry(source);
        }

        public RssEntry[] newArray(int size) {
            return new RssEntry[size];
        }
    };
    protected RssEntry(Parcel in) {
        RssEntry.builder()
                .date(in.readString())
                .title(in.readString())
                .summary(in.readString())
                .url(in.readString())
                .expandedText(in.readString())
                .build();
    }

}
