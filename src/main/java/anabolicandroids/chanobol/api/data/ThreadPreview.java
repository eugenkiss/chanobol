package anabolicandroids.chanobol.api.data;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.parceler.Parcel;

import java.lang.reflect.Type;

// ThreadPreview is not a list of posts but so to say a preview/pointer to the list of posts contained
// in the corresponding thread. ThreadPreview represents the tile you see in the catalog and can also
// be understood as simply the first (OP - original poster) post in the thread.
@Parcel
public class ThreadPreview extends Common {

    public boolean isSticky() {
        return sticky == 1;
    }

    // Only used internally, no counterpart in 4Chan API
    public boolean dead;

    // Used internally to speed up rendering
    public String strippedSubject;
    public String excerpt;

    public void generateExcerpt() {
        if (subject == null) subject = "";
        if (text == null) text = "";
        strippedSubject = android.text.Html.fromHtml(subject).toString();
        String strippedText = android.text.Html.fromHtml(text).toString();
        excerpt = strippedSubject
                + (strippedSubject.isEmpty() ? "" : "\n")
                + strippedText.substring(0, Math.min(160, strippedText.length()));
    }

    public Post toOpPost() {
        Gson gson = new Gson();
        Type type = new TypeToken<Post>() {}.getType();
        return gson.fromJson(gson.toJson(this), type);
    }

    // From Clover
    public static String generateTitle(Board board, ThreadPreview threadPreview) {
        return generateTitle(board, threadPreview, 100);
    }

    public static String generateTitle(Board board, ThreadPreview threadPreview, int maxLength) {
        if (!TextUtils.isEmpty(threadPreview.subject)) {
            return threadPreview.subject;
        } else if (!TextUtils.isEmpty(threadPreview.text)) {
            return "/" + board + "/ - " + threadPreview.text.subSequence(0, Math.min(threadPreview.text.length(), maxLength)).toString();
        } else {
            return "/" + board + "/" + threadPreview.number;
        }
    }

    public static String generateTitle(String boardName, String threadNumber) {
            return "/" + boardName + "/" + threadNumber;
    }
}
