package io.zbox.treno;

import android.media.MediaDataSource;
import android.util.Log;

import java.io.IOException;

import io.zbox.zboxfs.File;
import io.zbox.zboxfs.Metadata;
import io.zbox.zboxfs.SeekFrom;
import io.zbox.zboxfs.ZboxException;

public class ZboxMediaSource extends MediaDataSource {

    private static final String TAG = ZboxMediaSource.class.getSimpleName();

    private final File file;

    ZboxMediaSource(File file) {
        this.file = file;
    }

    @Override
    public long getSize () throws IOException {
        long size;

        try {
            Metadata md = file.metadata();
            size = md.contentLen;
        } catch (ZboxException err) {
            throw new IOException(err.getMessage());
        }

        return size;
    }

    @Override
    public int readAt (long position, byte[] buffer,  int offset, int size) throws IOException {
        if (size == 0) return 0;

        int read;

        try {
            file.seek(position, SeekFrom.START);
            read = file.read(buffer, offset, size);
        } catch (ZboxException err) {
            throw new IOException(err.getMessage());
        }

        return read == 0 ? -1 : read;
    }

    @Override
    public void close() {
        file.close();
        Log.d(TAG, "file closed");
    }
}
