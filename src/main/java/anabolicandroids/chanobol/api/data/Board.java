package anabolicandroids.chanobol.api.data;

import com.google.gson.annotations.SerializedName;

public class Board {
    @SerializedName("board")
    public String name;

    public String title;

    @Override
    public String toString() {
        return name + " - " + title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Board board = (Board) o;

        return !(name != null ? !name.equals(board.name) : board.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
