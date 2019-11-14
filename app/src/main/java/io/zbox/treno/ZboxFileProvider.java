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
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import io.zbox.zboxfs.ZboxException;

public class ZboxFileProvider extends ContentProvider implements ContentProvider.PipeDataWriter<String> {

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
        //return "text/plain";
        return "image/jpeg";
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        Log.d(TAG, "=======> ZboxFileProvider.getStreamTypes");
        return new String[] {"image/jpeg"};
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Log.d(TAG, "=======> ZboxFileProvider.openFile2: " + uri.getPath());
        //return null;
        return openPipeHelper(uri, "image/jpeg", null, null, this);

    }

    /*@Override
    public AssetFileDescriptor openAssetFile (Uri uri, String mode) throws FileNotFoundException {
        Log.d(TAG, "=======> ZboxFileProvider.openAssetFile: " + uri.getPath());
        return new AssetFileDescriptor(
                openPipeHelper(uri, "image/jpeg", null, null, this),
                0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }*/

    /*@Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException
    {
        Log.d(TAG, "=======> ZboxFileProvider.openTypedAssetFile: " + uri.getPath());

        return new AssetFileDescriptor(
                openPipeHelper(uri, "image/jpeg", opts, null, this),
                0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }*/

    private static class PipeTask<T> extends AsyncTask<Object, Object, Object> {

        private final ParcelFileDescriptor fd;
        private final Uri uri;
        private final String mimeType;
        private final Bundle opts;
        private final T args;
        private final PipeDataWriter<T> func;

        PipeTask(ParcelFileDescriptor fd, Uri uri, String mimeType, Bundle opts, T args,
                 PipeDataWriter<T> func) {
            this.fd = fd;
            this.uri = uri;
            this.mimeType = mimeType;
            this.opts = opts;
            this.args = args;
            this.func = func;
        }

        @Override
        protected Object doInBackground(Object... params) {
            func.writeDataToPipe(fd, uri, mimeType, opts, args);
            try {
                fd.close();
            } catch (IOException e) {
                Log.w(TAG, "Failure closing pipe", e);
            }
            return null;
        }
    }

    @Override
    public <T> ParcelFileDescriptor openPipeHelper(Uri uri, String mimeType, Bundle opts, T args,
                                               PipeDataWriter<T> func) throws FileNotFoundException
    {
        try {
            final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createReliablePipe();
            AsyncTask<Object, Object, Object> task = new PipeTask(fds[1], uri, mimeType, opts, args, func);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[])null);
            return fds[0];
        } catch (IOException e) {
            throw new FileNotFoundException("failure making pipe");
        }
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output,
                                Uri uri,
                                String mimeType,
                                Bundle opts,
                                String args)
    {
        Log.d(TAG, "=======> ZboxFileProvider.writeDataToPipe "+output.toString());
        FileOutputStream dst = new FileOutputStream(output.getFileDescriptor());

        /*try {
            dst.write(42);
            dst.write(42);
            dst.write(42);
            dst.flush();
        } catch (IOException ignore) {}*/


        InputStream is = getContext().getResources().openRawResource(R.raw.image3);
        byte[] buf = new byte[8192];
        int read = 0;
        long total = 0;

        try {
            while ((read = is.read(buf)) >= 0) {
                dst.write(buf, 0, read);
                total += read;
            }
        } catch (IOException err) {
            Log.e(TAG, err.toString());
        } finally {
            try { is.close(); } catch (IOException err) {Log.e(TAG, err.toString());}
        }
        Log.d(TAG, "total read " + total);


        //model.writeToStream(uri.getPath(), dst);

        try { dst.flush(); dst.close(); } catch (IOException err) {Log.e(TAG, err.toString());}
    }
}
