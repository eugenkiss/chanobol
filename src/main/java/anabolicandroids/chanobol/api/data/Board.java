package anabolicandroids.chanobol.api.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Board implements Parcelable {
    @SerializedName("board")
    public String name;
    public String title;

    @Override public String toString() { return name + " - " + title; }
    @Override public int hashCode() { return name != null ? name.hashCode() : 0; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return !(name != null ? !name.equals(board.name) : board.name != null);
    }

    // Made parcelable with http://www.parcelabler.com/

    protected Board(Parcel in) {
        name = in.readString();
        title = in.readString();
    }
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(title);
    }
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Board> CREATOR = new Parcelable.Creator<Board>() {
        @Override public Board createFromParcel(Parcel in) { return new Board(in); }
        @Override public Board[] newArray(int size) { return new Board[size]; }
    };
}
