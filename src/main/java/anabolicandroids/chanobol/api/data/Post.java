package anabolicandroids.chanobol.api.data;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;
import org.parceler.Transient;

import anabolicandroids.chanobol.ui.posts.parsing.CommentParser;

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

    // TODO: should be handled by threadmanager...
    // Transient helper state
    public int thumbMutedColor = -1;

    // Used internally to speed up rendering by temporary caching
    // It would be better to persist the parsedText but SpannableString is not parcelable by default
    @Transient transient private CharSequence parsedText;
    public void generateParsedTextCache() {
        if (parsedText == null) {
            String s = subject; if (s == null) s = ""; else s = "<h5>" + s + "</h5>";
            String t = text; if (t == null) t = "";
            parsedText = CommentParser.getInstance().parseComment(this, s + t);
        }
    }
    public CharSequence parsedText() {
        generateParsedTextCache();
        return parsedText;
    }
}
