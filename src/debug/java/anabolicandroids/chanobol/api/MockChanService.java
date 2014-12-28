package anabolicandroids.chanobol.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import anabolicandroids.chanobol.annotations.ForApplication;
import anabolicandroids.chanobol.util.Util;
import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Path;
import retrofit.mime.TypedByteArray;

@Singleton
public class MockChanService implements ChanService {

    private final Context appContext;

    @Inject MockChanService(@ForApplication Context context) {
        appContext = context;
    }

    @Override
    public void listBoards(Callback<Boards> cb) {
        String responseString = Util.loadJSONFromAsset(appContext, "boards.json");
        Response response =  new Response("", 200, "nothing", Collections.EMPTY_LIST,
                new TypedByteArray("application/json", responseString.getBytes()));
        Type type = new TypeToken<Boards>() {}.getType();
        Boards boards = new Gson().fromJson(responseString, type);
        cb.success(boards, response);
    }

    @Override
    public void listThreads(@Path("board") String board, Callback<List<Threads>> cb) {
        String responseString = Util.loadJSONFromAsset(appContext, "catalog.json");
        Response response =  new Response("", 200, "nothing", Collections.EMPTY_LIST,
                new TypedByteArray("application/json", responseString.getBytes()));
        Type type = new TypeToken<List<Threads>>() {}.getType();
        List<Threads> threads = new Gson().fromJson(responseString, type);
        cb.success(threads, response);
    }

    @Override
    public void listPosts(@Path("board") String board, @Path("number") String number, Callback<Posts> cb) {
        String responseString = Util.loadJSONFromAsset(appContext, "thread.json");
        Response response =  new Response("", 200, "nothing", Collections.EMPTY_LIST,
                new TypedByteArray("application/json", responseString.getBytes()));
        Type type = new TypeToken<Posts>() {}.getType();
        Posts posts = new Gson().fromJson(responseString, type);
        cb.success(posts, response);
    }
}
