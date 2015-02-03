package anabolicandroids.chanobol.ui.images;

import android.os.Parcel;
import android.os.Parcelable;

import anabolicandroids.chanobol.api.data.Post;

public class ImgIdExt implements Parcelable {

    public static ImgIdExt from(Post post) {
        return new ImgIdExt(post.imageId, post.imageExtension);
    }

    public String id;
    public String ext;

    public ImgIdExt(String imageId, String imageExtension) {
        id = imageId;
        ext = imageExtension;
    }

    // Made parcelable with http://www.parcelabler.com/

    protected ImgIdExt(Parcel in) {
        id = in.readString();
        ext = in.readString();
    }
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(ext);
    }
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ImgIdExt> CREATOR = new Parcelable.Creator<ImgIdExt>() {
        @Override public ImgIdExt createFromParcel(Parcel in) { return new ImgIdExt(in); }
        @Override public ImgIdExt[] newArray(int size) { return new ImgIdExt[size]; }
    };
}
