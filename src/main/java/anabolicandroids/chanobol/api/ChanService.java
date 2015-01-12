package anabolicandroids.chanobol.api;

import java.util.List;

import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.api.data.Thread;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

public interface ChanService {
    @GET("/{board}/catalog.json")
    void listThreads(
            @Path("board") String board,
            Callback<List<Threads>> cb
    );

    @GET("/{board}/thread/{number}.json")
    void listPosts(
            @Path("board") String board,
            @Path("number") String number,
            Callback<Posts> cb
    );

    public static class Boards {
        public List<Board> boards;
    }

    public static class Threads {
        public List<Thread> threads;
    }

    public static class Posts {
        public List<Post> posts;
    }
}
