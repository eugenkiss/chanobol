package anabolicandroids.chanobol.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.List;

import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.api.data.ThreadPreview;

public class ChanService {
    Ion ion;
    Context cxt;
    Gson gson;

    public ChanService(Context context, Ion ion) {
        this.cxt = context;
        this.ion = ion;
        gson = new Gson();
    }

    public void listThreads(Object group, final String board, final FutureCallback<List<ThreadPreview>> cb) {
        String path = String.format("/%s/catalog.json", board);
        ion.build(cxt)
                .load(ApiModule.endpoint + path)
                .group(group)
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override public void onCompleted(Exception e, JsonArray result) {
                        if (e != null) {
                            cb.onCompleted(e, null);
                            return;
                        }
                        List<ThreadPreview> threadPreviews = new ArrayList<>(50);
                        for (JsonElement x : result) {
                            for (JsonElement y : x.getAsJsonObject().get("threads").getAsJsonArray()) {
                                ThreadPreview t = gson.fromJson(y, ThreadPreview.class);
                                t.board = board;

                                // Generate excerpt here instead of ThreadView for efficiency reasons
                                t.generateExcerpt();

                                threadPreviews.add(t);
                            }
                        }
                        cb.onCompleted(null, threadPreviews);
                    }
                });
    }

    public void listPosts(Object group, final String board, String number, final FutureCallback<List<Post>> cb) {
        String path = String.format("/%s/thread/%s.json", board, number);
        ion.build(cxt)
                .load(ApiModule.endpoint + path)
                .group(group)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            cb.onCompleted(e, null);
                            return;
                        }
                        List<Post> posts = new ArrayList<>();
                        for (JsonElement x : result.get("posts").getAsJsonArray()) {
                            Post p = gson.fromJson(x, Post.class);
                            p.board = board;
                            // Generate parsed text here instead of in PostView for efficiency reasons
                            // Although not perfect because on activity restoration the parsed result
                            // gets lost but infrequent case so still worthwhile.
                            p.generateParsedTextCache();
                            posts.add(p);
                        }
                        cb.onCompleted(null, posts);
                    }
                });
    }
}
