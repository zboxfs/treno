package io.zbox.treno;

import android.content.Context;
import android.net.Uri;
import android.text.Selection;
import android.util.Log;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import io.zbox.zboxfs.RepoInfo;
import io.zbox.zboxfs.RepoOpener;
import io.zbox.zboxfs.ZboxException;

public class RepoViewModel extends ViewModel {
    private static final String TAG = RepoViewModel.class.getSimpleName();

    private MutableLiveData<Repo> repo = new MutableLiveData<>();
    private MutableLiveData<List<DirEntry>> dents;
    private MutableLiveData<Path> path = new MutableLiveData<>();
    private MutableLiveData<Boolean> loading = new MutableLiveData<>();

    public RepoViewModel() {
        Log.d(TAG, "RepoViewModel created");

        Env.init(Env.LOG_DEBUG);

        try {
            Repo repo = new RepoOpener().create(true).open("mem://foo", "pwd");
            this.repo.setValue(repo);
        } catch (ZboxException err) {
            Log.e(TAG, err.toString());
        }

        path.setValue(Path.root());
        loading.setValue(false);
    }

    @Override
    public void onCleared() {
        Log.d(TAG, "RepoViewModel cleared");
        repo.getValue().close();
    }

    LiveData<Repo> getRepo() {
        return repo;
    }

    LiveData<List<DirEntry>> getDirEntries() {
        if (dents == null) {
            this.dents = new MutableLiveData<>();
            new Thread(this::readDirEntries).start();
        }
        return dents;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    void setPath(Path path) {
        this.path.setValue(path);
        new Thread(this::readDirEntries).start();
    }

    public LiveData<Path> getPath() {
        return path;
    }

    void goUp() {
        Path curr = this.path.getValue();
        if (!curr.isRoot()) setPath(curr.parent());
    }

    public RepoInfo getInfo() {
        Repo repo = this.repo.getValue();
        return repo.info();
    }

    void addDir(String name) {
        loading.postValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path curr = this.path.getValue();
                Path path = curr.join(name);
                repo.createDir(path);

                Thread.sleep(800);
                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    void addFile(String name, InputStream stream) {
        loading.postValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path curr = this.path.getValue();
                Path path = curr.join(name);

                File file = new OpenOptions().create(true).write(true).open(repo, path);
                file.writeOnce(stream);
                file.close();

                Thread.sleep(800);
                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                try {
                    stream.close();
                } catch (IOException err) {
                    Log.e(TAG, err.toString());
                }
                loading.postValue(false);
            }
        }).start();
    }

    LiveData<File> openFile(Path path) {
        MutableLiveData<File> ret = new MutableLiveData<>();

        loading.postValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                File file = repo.openFile(path);
                Thread.sleep(500);
                ret.postValue(file);
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                loading.postValue(false);
            }
        }).start();

        return ret;
    }

    File openFile(String path) throws ZboxException {
        Repo repo = this.repo.getValue();
        return repo.openFile(new Path(path));
    }

    void writeToStream(String path, FileOutputStream output) {
        Log.d(TAG, "===>writeToStream " + path );
        try {
            Repo repo = this.repo.getValue();
            File file = repo.openFile(new Path(path));
            //Thread.sleep(500);
            byte[] buf = new byte[200000];
            //long read = file.read(output);
            int read = file.read(buf);
            Log.d(TAG, "===>read " + read + " bytes");
            output.write(buf, 0, read);
            output.flush();
            Log.d(TAG, "===>write " + read + " bytes to stream");
            file.close();
        } catch (Exception err) {
            Log.e(TAG, err.toString());
        }
    }

    void rename(String from, String newName) {
        loading.postValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path path = new Path(from);
                path.setFileName(newName);
                repo.rename(new Path(from), path);

                Thread.sleep(500);
                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    void move(List<String> fromStrs, String toStr) {
        loading.postValue(true);
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

                Thread.sleep(500);
                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    void copy(List<String> fromStrs, String toStr) {
        loading.postValue(true);
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

                Thread.sleep(1000);
                if (to.parent().equals(this.path.getValue())) readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    void remove(List<String> paths) {
        new Thread(() -> {
            loading.postValue(true);
            try {
                Repo repo = this.repo.getValue();

                for (String pathStr : paths) {
                    Path path = new Path(pathStr);
                    if (repo.isFile(path)) repo.removeFile(path);
                    if (repo.isDir(path)) repo.removeDirAll(path);
                }

                Thread.sleep(800);
                readDirEntries();
            } catch (Exception err) {
                Log.e(TAG, err.toString());
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    private void readDirEntries() {
        loading.postValue(true);
        try {
            Repo repo = this.repo.getValue();
            Path path = this.path.getValue();
            DirEntry[] dirs = repo.readDir(path);
            List<DirEntry> dents = new ArrayList<>(Arrays.asList(dirs));
            Thread.sleep(500);
            this.dents.postValue(dents);
        } catch (Exception err) {
            Log.e(TAG, err.toString());
        } finally {
            loading.postValue(false);
        }
    }
}
