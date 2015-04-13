package anabolicandroids.chanobol.ui.scaffolding;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Singleton;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.api.data.Thread;

/** Utility class for saving and restoring persistent data like a list of favorites */
@Singleton public class PersistentData {
    public static final String favoriteBoardsId = "favoriteBoards";
    public static final String watchlistId = "watchlist";

    Context context;
    SharedPreferences prefs;
    Gson gson = new Gson();

    public PersistentData(App context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    ArrayList<FavoritesCallback> favoritesCallbacks = new ArrayList<>();

    private void saveFavorites(Set<Board> favorites) {
        String jsonFavorites = gson.toJson(favorites);
        prefs.edit().putString(favoriteBoardsId, jsonFavorites).apply();
        for (FavoritesCallback cb : favoritesCallbacks) {
            cb.onChanged(favorites);
        }
    }

    public void addFavorite(Board board) {
        Set<Board> favorites = getFavorites();
        favorites.add(board);
        saveFavorites(favorites);
    }

    public void removeFavorite(Board board) {
        Set<Board> favorites = getFavorites();
        favorites.remove(board);
        saveFavorites(favorites);
    }

    public Set<Board> getFavorites() {
        String jsonFavorites = prefs.getString(favoriteBoardsId, "[]");
        Board[] boards;
        try {
            boards = gson.fromJson(jsonFavorites, Board[].class);
        } catch(JsonSyntaxException e){
            boards = new Board[0];
        }
        return new TreeSet<>(Arrays.asList(boards));
    }

    public void addFavoritesChangedCallback(FavoritesCallback callback) {
        favoritesCallbacks.add(callback);
    }

    public void removeFavoritesChangedCallback(FavoritesCallback callback) {
        favoritesCallbacks.remove(callback);
    }

    public static interface FavoritesCallback {
        public void onChanged(Set<Board> newFavorites);
    }


    public static class WatchlistEntry implements Comparable<WatchlistEntry> {
        public String id;
        public String title;
        @SuppressWarnings("UnusedDeclaration") WatchlistEntry(){}
        WatchlistEntry(String id) { this.id = id; }
        WatchlistEntry(String id, String title) {
            this.id = id;
            this.title = title;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WatchlistEntry that = (WatchlistEntry) o;
            return id.equals(that.id);
        }
        @Override public int hashCode() { return id.hashCode(); }
        @Override public int compareTo(@NonNull WatchlistEntry another) {
            return this.id.compareTo(another.id);
        }
    }

    ArrayList<WatchlistCallback> watchlistCallbacks = new ArrayList<>();

    @SuppressLint("CommitPrefEdits")
    private void saveWatchlist(Set<WatchlistEntry> watchlist) {
        String jsonWatchlist = gson.toJson(watchlist);
        prefs.edit().putString(watchlistId, jsonWatchlist).commit();
        for (WatchlistCallback cb : watchlistCallbacks) {
            cb.onChanged(watchlist);
        }
    }

    public boolean isInWatchlist(String id) {
        return getWatchlist().contains(new WatchlistEntry(id));
    }

    public void addWatchlistThread(Thread thread) {
        Set<WatchlistEntry> watchlist = getWatchlist();
        WatchlistEntry e = new WatchlistEntry(thread.id, thread.title());
        // Why remove? In order to force updating of the entry (e.g. new title with skull)
        watchlist.remove(e);
        watchlist.add(e);
        saveWatchlist(watchlist);

        String jsonThread = gson.toJson(thread);
        writeToInternalFile(thread.id, jsonThread);
    }

    public void removeWatchlistThread(WatchlistEntry entry) {
        Set<WatchlistEntry> watchlist = getWatchlist();
        watchlist.remove(entry);
        saveWatchlist(watchlist);

        context.deleteFile(escapePathSeparator(entry.id));
    }
    public void removeWatchlistThread(Thread thread) {
        removeWatchlistThread(new WatchlistEntry(thread.id));
    }

    public Set<WatchlistEntry> getWatchlist() {
        String jsonWatchlist = prefs.getString(watchlistId, "[]");
        WatchlistEntry[] threads;
        try {
            threads = gson.fromJson(jsonWatchlist, WatchlistEntry[].class);
        } catch (JsonSyntaxException e) {
            threads = new WatchlistEntry[0];
        }
        return new TreeSet<>(Arrays.asList(threads));
    }

    public void clearWatchList() {
        Set<WatchlistEntry> watchlist = getWatchlist();
        for (WatchlistEntry entry : watchlist) {
            removeWatchlistThread(entry);
        }
    }

    public Thread getWatchlistThread(String id) {
        String jsonThread = readFromInternalFile(id);
        return gson.fromJson(jsonThread, Thread.class);
    }

    public void addWatchlistChangedCallback(WatchlistCallback callback) {
        watchlistCallbacks.add(callback);
    }

    public void removeWatchlistChangedCallback(WatchlistCallback callback) {
        watchlistCallbacks.remove(callback);
    }

    public static interface WatchlistCallback {
        public void onChanged(Set<WatchlistEntry> newWatchlist);
    }


    // http://stackoverflow.com/a/14377185/283607

    private String escapePathSeparator(String path) {
        return path.replace("/", "_");
    }

    private void writeToInternalFile(String name, String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(escapePathSeparator(name), Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromInternalFile(String name) {
        String ret = "";
        try {
            InputStream inputStream = context.openFileInput(escapePathSeparator(name));
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
        return ret;
    }

}
