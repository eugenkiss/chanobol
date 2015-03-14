package anabolicandroids.chanobol.api;

import android.text.TextUtils;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.HttpUtil;
import com.koushikdutta.async.http.cache.ResponseCacheMiddleware;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.ion.HeadersResponse;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ResponseServedFrom;
import com.koushikdutta.ion.loader.SimpleLoader;

import timber.log.Timber;

/*
TODO: This doesn't work currently.
The code is copied from https://github.com/koush/ion/blob/master/ion/src/com/koushikdutta/ion/loader/HttpLoader.java
MockImageLoader is used correctly for image requests in mock mode as can be seen in the logs.
The problem is that currently MockImageLoader doesn't do anything different than the default
HttpLoader so `catalogThumbnail.jpg` is not loaded from the assets folder. I wonder what's the best
way to make it work like
https://github.com/JakeWharton/u2020/blob/0f375d2fde2703b72c85cada2aca0d693a8dcb09/src/debug/java/com/jakewharton/u2020/data/MockRequestHandler.java
*/
public class MockImageLoader extends SimpleLoader {

    @SuppressWarnings("unchecked")
    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, final FutureCallback<LoaderEmitter> callback) {
        String uri = request.getUri().toString();
        if (!uri.startsWith(ApiModule.imgCdn) && !uri.startsWith(ApiModule.thumbCdn))
            return null;
        Timber.i("Mocked Image Request!");
        return (Future<DataEmitter>)(Future)ion.getHttpClient().execute(request, new HttpConnectCallback() {
            @Override public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                long length = -1;
                ResponseServedFrom loadedFrom = ResponseServedFrom.LOADED_FROM_NETWORK;
                HeadersResponse headers = null;
                AsyncHttpRequest request = null;
                if (response != null) {
                    request = response.getRequest();
                    headers = new HeadersResponse(response.code(), response.message(), response.headers());
                    length = HttpUtil.contentLength(headers.getHeaders());
                    String servedFrom = response.headers().get(ResponseCacheMiddleware.SERVED_FROM);
                    if (TextUtils.equals(servedFrom, ResponseCacheMiddleware.CACHE))
                        loadedFrom = ResponseServedFrom.LOADED_FROM_CACHE;
                    else if (TextUtils.equals(servedFrom, ResponseCacheMiddleware.CONDITIONAL_CACHE))
                        loadedFrom = ResponseServedFrom.LOADED_FROM_CONDITIONAL_CACHE;
                }
                callback.onCompleted(ex,
                        new LoaderEmitter(response, length, loadedFrom, headers, request));
            }
        });
    }
}
