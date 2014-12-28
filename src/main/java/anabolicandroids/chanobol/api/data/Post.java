package anabolicandroids.chanobol.api.data;

import com.google.gson.annotations.SerializedName;

public class Post extends Common {
    @SerializedName("resto")
    public String replyTo;

    public boolean isOp() {
        return "0".equals(replyTo);
    }

    public int postReplies;
}
