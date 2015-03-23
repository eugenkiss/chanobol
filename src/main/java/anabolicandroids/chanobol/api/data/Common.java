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
    public String mediaId;

    @SerializedName("ext")
    public String mediaExtension;

    @SerializedName("w")
    public int mediaWidth;

    @SerializedName("h")
    public int mediaHeight;

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

    // These may make no sense for a thread but it is important that these are here
    // Because of the shared transition from thread image to op post image s.t.
    // the flags are immediately visible and not only after load

    public String country;

    @SerializedName("country_name")
    public String countryName;
}
