package anabolicandroids.chanobol.api.data;

import com.google.gson.annotations.SerializedName;

public class Post extends Common {
    @SerializedName("resto")
    public String replyTo;

    public String country;

    @SerializedName("country_name")
    public String countryName;

    public boolean isOp() {
        return "0".equals(replyTo);
    }

    // Only used internally, no counterpart in 4Chan API
    public int replyCount;
}
