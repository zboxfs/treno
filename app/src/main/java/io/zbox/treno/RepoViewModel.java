package io.zbox.treno;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Env;
import io.zbox.zboxfs.File;
import io.zbox.zboxfs.OpenOptions;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.Repo;
import io.zbox.zboxfs.RepoOpener;
import io.zbox.zboxfs.ZboxException;

public class RepoViewModel extends ViewModel {
    private static final String TAG = RepoViewModel.class.getSimpleName();

    private MutableLiveData<Repo> repo = new MutableLiveData<>();
    private MutableLiveData<List<DirEntry>> dents;
    private MutableLiveData<Path> path;

    public RepoViewModel() {
        Log.d(TAG, "RepoViewModel created");

        Env.init(Env.LOG_DEBUG);

        try {
            Repo repo = new RepoOpener().create(true).open("mem://foo", "pwd");
            this.repo.setValue(repo);
        } catch (ZboxException err) {
            Log.e(TAG, err.toString());
        }

        path = new MutableLiveData<>();
        path.setValue(Path.root());
    }

    @Override
    public void onCleared() {
        Log.d(TAG, "RepoViewModel cleared");
        repo.getValue().close();
    }

    public LiveData<Repo> getRepo() {
        return repo;
    }

    LiveData<List<DirEntry>> getDirEntries() {
        if (dents == null) {
            this.dents = new MutableLiveData<>();
            new Thread(this::readDirEntries).start();
        }
        return dents;
    }

    public LiveData<Path> getPath() {
        return path;
    }

    void setPath(Path path) {
        this.path.setValue(path);
        new Thread(this::readDirEntries).start();
    }

    boolean isImageFile(Path path) {
        String ext = path.extension().toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif");
    }

    boolean isVideoFile(Path path) {
        String ext = path.extension().toLowerCase();
        return ext.equals("avi") || ext.equals("mpg") || ext.equals("mp4");
    }

    void goUp() {
        Path curr = this.path.getValue();
        if (!curr.isRoot()) setPath(curr.parent());
    }

    void addDir(String name) {
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path curr = this.path.getValue();
                Path path = curr.join(name);
                repo.createDir(path);

                readDirEntries();
            } catch (ZboxException err) {
                Log.e(TAG, err.toString());
            }
        }).start();
    }

    void addFile(String name, InputStream stream) {
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path curr = this.path.getValue();
                Path path = curr.join(name);

                File file = new OpenOptions().create(true).write(true).open(repo, path);
                file.writeOnce(stream);
                file.close();

                readDirEntries();
            } catch (ZboxException err) {
                Log.e(TAG, err.toString());
            } finally {
                try {
                    stream.close();
                } catch (IOException err) {
                    Log.e(TAG, err.toString());
                }
            }
        }).start();
    }

    LiveData<File> openFile(Path path) {
        MutableLiveData<File> ret = new MutableLiveData<>();

        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                File file = repo.openFile(path);
                ret.postValue(file);
            } catch (ZboxException err) {
                Log.e(TAG, err.toString());
            }
        }).start();

        return ret;
    }

    void rename(String from, String newName) {
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path path = new Path(from);
                path.setFileName(newName);
                repo.rename(new Path(from), path);

                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            }
        }).start();
    }

    void move(List<String> fromStrs, String toStr) {
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path to = new Path(toStr);

                for (String pathStr : fromStrs) {
                    Path from = new Path(pathStr);

                    if (repo.isFile(from) && repo.pathExists(to) && repo.isDir(to)) {
                        Path tgt = to.join(from.fileName());
                        repo.move(from, tgt);
                    } else {
                        repo.move(from, to);
                    }
                }

                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            }
        }).start();
    }

    void copy(List<String> fromStrs, String toStr) {
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path to = new Path(toStr);

                for (String pathStr : fromStrs) {
                    Path from = new Path(pathStr);

                    if (repo.isFile(from)) {
                        if (repo.pathExists(to) && repo.isDir(to)) {
                            Path tgt = to.join(from.fileName());
                            repo.copy(from, tgt);
                        } else {
                            repo.copy(from, to);
                        }
                    } else {
                        repo.copyDirAll(from, to);
                    }
                }

                if (to.parent().equals(this.path.getValue())) readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            }
        }).start();
    }

    void remove(List<String> paths) {
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();

                for (String pathStr : paths) {
                    Path path = new Path(pathStr);
                    if (repo.isFile(path)) repo.removeFile(path);
                    if (repo.isDir(path)) repo.removeDirAll(path);
                }

                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            }
        }).start();
    }

    private void readDirEntries() {
        try {
            Repo repo = this.repo.getValue();
            Path path = this.path.getValue();
            DirEntry[] dirs = repo.readDir(path);
            List<DirEntry> dents = new ArrayList<>(Arrays.asList(dirs));
            this.dents.postValue(dents);
        } catch (ZboxException err) {
            Log.e(TAG, err.toString());
        }
    }
}
