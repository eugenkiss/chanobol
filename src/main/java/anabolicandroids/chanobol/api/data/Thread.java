package anabolicandroids.chanobol.api.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.parceler.Parcel;

import java.lang.reflect.Type;

@Parcel
public class Thread extends Common {

    // Only used internally, no counterpart in 4Chan API
    public boolean dead;

    public Post toOpPost() {
        Gson gson = new Gson();
        Type type = new TypeToken<Post>() {}.getType();
        return gson.fromJson(gson.toJson(this), type);
    }
}
