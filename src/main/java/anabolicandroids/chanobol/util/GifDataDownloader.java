package anabolicandroids.chanobol.util;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

// https://github.com/felipecsl/GifImageView/blob/master/app/src/main/java/com/felipecsl/gifimageview/app
public class GifDataDownloader extends AsyncTask<String, Void, byte[]> {

    private static final String TAG = "GifDataDownloader";

    public GifDataDownloader() {
    }

    @Override
    protected byte[] doInBackground(final String... params) {
        final String gifUrl = params[0];

        if (gifUrl == null)
            return null;

        byte[] gif = null;
        try {
            gif = ByteArrayHttpClient.get(gifUrl);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "GifDecode OOM: " + gifUrl, e);
        }

        return gif;
    }
}

class ByteArrayHttpClient {
    private static final String TAG = "ByteArrayHttpClient";
    private static OkHttpClient client = new OkHttpClient();

    public static byte[] get(final String urlString) {
        InputStream in = null;
        try {
            final String decodedUrl = URLDecoder.decode(urlString, "UTF-8");
            final URL url = new URL(decodedUrl);
            final Request request = new Request.Builder().url(url).build();
            final Response response = client.newCall(request).execute();
            in = response.body().byteStream();
            return IOUtils.toByteArray(in);
        } catch (final MalformedURLException e) {
            Log.d(TAG, "Malformed URL", e);
        } catch (final OutOfMemoryError e) {
            Log.d(TAG, "Out of memory", e);
        } catch (final UnsupportedEncodingException e) {
            Log.d(TAG, "Unsupported encoding", e);
        } catch (final IOException e) {
            Log.d(TAG, "IO exception", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ignored) {
                }
            }
        }
        return null;
    }
}