package io.zbox.treno;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ProxyFileDescriptorCallback;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import io.zbox.zboxfs.File;
import io.zbox.zboxfs.Metadata;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.SeekFrom;
import io.zbox.zboxfs.ZboxException;

public class ZboxFileProvider extends ContentProvider {

    private static final String TAG = ZboxFileProvider.class.getSimpleName();

    private RepoViewModel model;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "=======> ZboxFileProvider.onCreate");
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
        Log.d(TAG, "=======> ZboxFileProvider.query");
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "=======> ZboxFileProvider.insert");
        return null;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs)
    {
        Log.d(TAG, "=======> ZboxFileProvider.update");
        return 0;
    }

    @Override
    public int delete(Uri uri,
                      String selection,
                      String[] selectionArgs)
    {
        Log.d(TAG, "=======> ZboxFileProvider.update");
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(TAG, "=======> ZboxFileProvider.getType");
        String pathStr = uri.getPath();
        return Utils.detectMimeType(pathStr);
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        Log.d(TAG, "=======> ZboxFileProvider.getStreamTypes");
        return new String[] {getType(uri)};
    }

    private static class FileReadCallback extends ProxyFileDescriptorCallback {

        private File file;

        FileReadCallback(File file) {
            this.file = file;
        }

        @Override
        public long onGetSize () throws ErrnoException {
            long size = 0;
            try {
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
            file.close();
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        String path = uri.getPath();

        Log.d(TAG, "=======> ZboxFileProvider.openFile: " + path);

        StorageManager mgr = (StorageManager)getContext().getSystemService(Context.STORAGE_SERVICE);

        try {
            FileReadCallback callback = new FileReadCallback(model.openFile(path));
            Handler handler = new Handler(Looper.getMainLooper());
            return mgr.openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY, callback, handler);
        } catch (Exception err) {
            Log.e(TAG, err.toString());
        }

        return null;
    }
}
