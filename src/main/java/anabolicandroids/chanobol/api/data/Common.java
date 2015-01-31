package anabolicandroids.chanobol.api.data;

import com.google.gson.annotations.SerializedName;

public abstract class Common {
    @SerializedName("no")
    public String number;

    @SerializedName("now")
    public String date;

    public int time;

    public int sticky; // 1 = true, 0 = false

    public int closed;

    public String name;

    public int replies;

    public int images;

    @SerializedName("sub")
    public String subject;

    @SerializedName("com")
    public String text;

    @SerializedName("tim")
    public String imageId;

    @SerializedName("ext")
    public String imageExtension;

    @SerializedName("w")
    public int imageWidth;

    @SerializedName("h")
    public int imageHeight;

    @SerializedName("tn_w")
    public int thumbnailWidth;

    @SerializedName("tn_h")
    public int thumbnailHeight;

    public String filename;

    @SerializedName("fsize")
    public long filesize;

    @SerializedName("semantic_url")
    public String semanticUrl;

    @SerializedName("trip")
    public String tripCode;
}
