package anabolicandroids.chanobol.api.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class Thread extends Common {

    public boolean dead;

    public Post toOpPost() {
        Gson gson = new Gson();
        Type type = new TypeToken<Post>() {}.getType();
        return gson.fromJson(gson.toJson(this), type);
    }
}
