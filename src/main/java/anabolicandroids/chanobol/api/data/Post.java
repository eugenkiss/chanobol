package anabolicandroids.chanobol.api.data;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class Post extends Common {
    @SerializedName("resto")
    public String replyTo;

    @SuppressWarnings("UnusedDeclaration")
    public boolean isOp() { return "0".equals(replyTo); }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isWebm() { return ".webm".equals(mediaExtension); }

    // Only used internally, no counterpart in 4Chan API
    public int replyCount;
}
