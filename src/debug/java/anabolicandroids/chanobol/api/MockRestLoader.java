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

// TODO: This doesn't work currently. See MockImageLoader.
// TODO: Make it similarly configurable as Retrofit's MockRestAdapter (setErrorPercentage...)
public class MockRestLoader extends SimpleLoader {

    @SuppressWarnings("unchecked")
    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, final FutureCallback<LoaderEmitter> callback) {
        String uri = request.getUri().toString();
        if (!uri.startsWith(ApiModule.endpoint))
            return null;
        Timber.i("Mocked Rest API Request!");
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
