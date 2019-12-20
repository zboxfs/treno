package io.zbox.treno;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ProxyFileDescriptorCallback;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;

import java.io.IOException;

import io.zbox.treno.util.Utils;
import io.zbox.zboxfs.File;
import io.zbox.zboxfs.Metadata;
import io.zbox.zboxfs.SeekFrom;
import io.zbox.zboxfs.ZboxException;

public class ZboxFileProvider extends ContentProvider {

    private static final String TAG = ZboxFileProvider.class.getSimpleName();

    private RepoViewModel model;

    @Override
    public boolean onCreate() {
        return true;
    }

    public void setModel(RepoViewModel model) {
        this.model = model;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder)
    {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public int delete(Uri uri,
                      String selection,
                      String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        String pathStr = uri.getPath();
        return Utils.detectMimeType(pathStr);
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        return new String[] {getType(uri)};
    }

    private class FileReadWorker extends HandlerThread {
        private Handler handler;

        FileReadWorker(String name) {
            super(name);
        }

        synchronized void waitUntilReady() {
            handler = new Handler(getLooper());
        }

        Handler getHandler() {
            return handler;
        }
    }

    private class FileReadCallback extends ProxyFileDescriptorCallback {

        private FileReadWorker worker;
        private String path;
        private File file = null;

        FileReadCallback(String path, FileReadWorker worker) {
            this.path = path;
            this.worker = worker;
        }

        // open file if it is not opened yet
        private void ensureFileOpened() throws ZboxException {
            if (file == null) file = model.openFile(path);
        }

        @Override
        public long onGetSize () throws ErrnoException {
            long size = 0;
            try {
                ensureFileOpened();
                Metadata md = file.metadata();
                size = md.contentLen;
            } catch (ZboxException err) {
                Log.e(TAG, err.toString());
                throw new ErrnoException("onGetSize", OsConstants.EIO);
            }
            return size;
        }

        @Override
        public int onRead(long offset, int size, byte[] data) throws ErrnoException {
            int read = 0;
            try {
                ensureFileOpened();
                file.seek(offset, SeekFrom.START);
                read = file.read(data, 0, size);
            } catch (ZboxException err) {
                Log.e(TAG, err.toString());
                throw new ErrnoException("onRead", OsConstants.EIO);
            }
            return read;
        }

        @Override
        public void onRelease () {
            if (file != null) file.close();
            file = null;
            worker.quitSafely();
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        String path = uri.getPath();

        StorageManager mgr = (StorageManager)getContext().getSystemService(Context.STORAGE_SERVICE);

        FileReadWorker worker = new FileReadWorker("zbox-file-reader");
        worker.start();
        worker.waitUntilReady();

        FileReadCallback callback = new FileReadCallback(path, worker);

        try {
            return mgr.openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY, callback, worker.getHandler());
        } catch (IOException err) {
            Log.e(TAG, err.toString());
        }

        return null;
    }
}
