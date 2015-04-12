/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 * Eugen Kiss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package anabolicandroids.chanobol.util;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.ui.scaffolding.Prefs;
import timber.log.Timber;

// From Clover

public class ImageSaver {
    /*
    private static final ImageSaver instance = new ImageSaver();

    public static ImageSaver getInstance() {
        return instance;
    }
    */

    private final Prefs prefs;
    private final Ion ion;
    private final FileCache fileCache;

    public ImageSaver(Prefs prefs, Ion ion, FileCache fileCache) {
        this.prefs = prefs;
        this.ion = ion;
        this.fileCache = fileCache;
    }

    private BroadcastReceiver receiver;

    public void saveAll(final Context context, String folderName, final List<DownloadPair> list) {
        // TODO: I've never seen the plus operator used for type File... but it works...
        final File subFolder = new File(prefs.imageDir() + File.separator + filterName(folderName));

        String text = context.getString(R.string.download_confirm, Integer.toString(list.size()), subFolder.getAbsolutePath());

        new AlertDialog.Builder(context).setMessage(text).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listDownload(context, subFolder, list);
                    }
                }).show();
    }

    public void saveImage(final Context context, String imageUrl, final String name, final String extension, final boolean share) {
        Future<?> ionRequest = fileCache.downloadFile(context, imageUrl, new FileCache.DownloadedCallback() {
            @Override
            @SuppressWarnings("deprecation")
            public void onProgress(long downloaded, long total, boolean done) {
            }

            @Override
            public void onSuccess(final File source) {
                onFileDownloaded(context, name, extension, source, share);
            }

            @Override
            public void onFail(boolean notFound) {
                Toast.makeText(context, R.string.image_open_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onFileDownloaded(final Context context, final String name, final String extension, final File downloaded, boolean share) {
        if (share) {
            scanFile(context, downloaded, true);
        } else {
            /*if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(context, R.string.image_save_not_mounted, Toast.LENGTH_LONG).show();
                return;
            }*/

            new Thread(new Runnable() {
                @Override
                public void run() {
                    File saveDir = prefs.imageDir();

                    if (!saveDir.isDirectory() && !saveDir.mkdirs()) {
                        showToast(context, context.getString(R.string.image_save_directory_error));
                        return;
                    }

                    String fileName = filterName(name + "." + extension);
                    File destination = findUnused(new File(saveDir, fileName), false);

                    final boolean success = storeImage(downloaded, destination);
                    if (success) {
                        scanFile(context, destination, false);
                        showToast(context, context.getString(R.string.image_save_succeeded) + " " + destination.getAbsolutePath());
                    } else {
                        showToast(context, context.getString(R.string.image_save_failed));
                    }
                }
            }).start();
        }
    }

    private void showToast(final Context context, final String message) {
        Util.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void listDownload(Context context, File subFolder, final List<DownloadPair> list) {
        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        if (!subFolder.isDirectory() && !subFolder.mkdirs()) {
            Toast.makeText(context, R.string.image_save_directory_error, Toast.LENGTH_LONG).show();
            return;
        }

        final List<Pair> files = new ArrayList<>(list.size());
        for (DownloadPair uri : list) {
            File destination = new File(subFolder, filterName(uri.imageName));
            if (destination.exists()) continue;

            Pair p = new Pair();
            p.uri = Uri.parse(uri.imageUrl);
            p.file = destination;
            files.add(p);
        }

        final AtomicBoolean stopped = new AtomicBoolean(false);
        final List<Long> ids = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Pair pair : files) {
                    if (stopped.get()) {
                        break;
                    }

                    DownloadManager.Request request;
                    try {
                        request = new DownloadManager.Request(pair.uri);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    request.setDestinationUri(Uri.fromFile(pair.file));
                    request.setVisibleInDownloadsUi(false);
                    request.allowScanningByMediaScanner();

                    synchronized (stopped) {
                        if (stopped.get()) {
                            break;
                        }
                        ids.add(dm.enqueue(request));
                    }
                }
            }
        }).start();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                    synchronized (stopped) {
                        stopped.set(true);

                        for (Long id : ids) {
                            dm.remove(id);
                        }
                    }
                } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    if (receiver != null) {
                        context.unregisterReceiver(receiver);
                        receiver = null;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        context.registerReceiver(receiver, filter);
    }

    private String filterName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.]", "_");
    }

    private File findUnused(File start, boolean isDir) {
        String base;
        String extension;

        if (isDir) {
            base = start.getAbsolutePath();
            extension = null;
        } else {
            String[] splitted = start.getAbsolutePath().split("\\.(?=[^\\.]+$)");
            if (splitted.length == 2) {
                base = splitted[0];
                extension = "." + splitted[1];
            } else {
                base = splitted[0];
                extension = ".";
            }
        }

        File test;
        if (isDir) {
            test = new File(base);
        } else {
            test = new File(base + extension);
        }
        int index = 0;
        int tries = 0;
        while (test.exists() && tries++ < 100) {
            if (isDir) {
                test = new File(base + "_" + index);
            } else {
                test = new File(base + "_" + index + extension);
            }
            index++;
        }

        return test;
    }

    private boolean storeImage(final File source, final File destination) {
        boolean res = true;

        try {
            copy(new FileInputStream(source), new FileOutputStream(destination));
        } catch (IOException e) {
            res = false;
        }

        return res;
    }

    private void scanFile(final Context context, final File file, final boolean shareAfterwards) {
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String unused, final Uri uri) {
                        Util.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Timber.i("Media scan succeeded: " + uri);

                                if (shareAfterwards) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("image/*");
                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)));
                                }
                            }
                        });
                    }
                }
        );
    }

    /**
     * Copies the inputstream to the outputstream and closes both streams.
     *
     * @param is
     * @param os
     * @throws IOException
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        int read;
        byte[] buffer = new byte[4096];
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }

        is.close();
        os.close();
    }

    public static void copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    public static class DownloadPair {
        public String imageUrl;
        public String imageName;

        public DownloadPair(String uri, String name) {
            this.imageUrl = uri;
            this.imageName = name;
        }
    }

    private static class Pair {
        public Uri uri;
        public File file;
    }
}
