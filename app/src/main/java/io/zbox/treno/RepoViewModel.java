package io.zbox.treno;

import android.content.res.Resources;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.zbox.treno.util.Utils;
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

    private MutableLiveData<List<String>> uris = new MutableLiveData<>();

    private MutableLiveData<Repo> repo = new MutableLiveData<>();
    private MutableLiveData<List<DirEntry>> dents = new MutableLiveData<>();
    private MutableLiveData<Path> path = new MutableLiveData<>();
    private MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    private Resources res;

    public RepoViewModel() {
        Env.init(Env.LOG_DEBUG);

        uris.setValue(new ArrayList<>());
        repo.setValue(null);
        dents.setValue(new ArrayList<>());
        path.setValue(Path.root());
        loading.setValue(false);
    }

    @Override
    public void onCleared() {
        closeRepo();
    }

    // Resource is to load sample files from raw resources
    void setResources(Resources res) {
        this.res = res;
    }

    public LiveData<Repo> getRepo() {
        return repo;
    }

    public void createRepo(String uri, String pwd) {
        loading.postValue(true);
        new Thread(() -> {
            try {
                Repo repo = new RepoOpener().createNew(true).open(uri, pwd);
                addSampleFiles(repo);
                this.repo.postValue(repo);

                // add repo uri to uri list
                addUri(uri);
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public void openRepo(String uri, String pwd) {
        loading.setValue(true);
        this.repo.setValue(null);
        new Thread(() -> {
            try {
                Repo repo = new RepoOpener().open(uri, pwd);
                this.repo.postValue(repo);
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public void closeRepo() {
        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                if (repo != null) {
                    List<DirEntry> dents = this.dents.getValue();
                    dents.clear();
                    this.dents.postValue(dents);

                    repo.close();
                    this.repo.postValue(null);
                }
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public LiveData<Boolean> deleteRepo(String uri) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo.destroy(uri);

                // remove uri
                List<String> uris = this.uris.getValue();
                int idx = uris.indexOf(uri);
                if (idx >= 0) {
                    uris.remove(idx);
                    this.uris.postValue(uris);
                }

                result.postValue(true);
            } catch (Exception err) {
                OutputErrorMsg(err);
                result.postValue(false);
            } finally {
                loading.postValue(false);
            }
        }).start();

        return result;
    }

    LiveData<Boolean> changePwd(String oldPwd, String newPwd) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                RepoInfo info = repo.info();
                repo.resetPassword(oldPwd, newPwd, info.opsLimit, info.memLimit);

                result.postValue(true);
            } catch (Exception err) {
                OutputErrorMsg(err);
                result.postValue(false);
            } finally {
                loading.postValue(false);
            }
        }).start();

        return result;
    }

    public LiveData<List<String>> getUris() { return uris; }

    void setUris(List<String> uris) {
        this.uris.setValue(uris);
    }

    void addUri(String uri) {
        List<String> uris = this.uris.getValue();
        uris.add(uri);
        this.uris.postValue(uris);
    }

    public LiveData<List<DirEntry>> getDirEntries() {
        new Thread(this::readDirEntries).start();
        return dents;
    }

    LiveData<Boolean> getLoading() {
        return loading;
    }

    LiveData<String> getError() {
        return error;
    }

    public LiveData<Path> getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path.setValue(path);
        new Thread(this::readDirEntries).start();
    }

    public boolean goUp() {
        Path curr = this.path.getValue();
        if (curr.isRoot()) return false;
        setPath(curr.parent());
        return true;
    }

    public RepoInfo getInfo() {
        Repo repo = this.repo.getValue();
        return repo.info();
    }

    public void addDir(String name) {
        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path curr = this.path.getValue();
                Path path = curr.join(name);
                repo.createDir(path);

                readDirEntries();
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    void addFile(String name, InputStream stream) {
        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path curr = this.path.getValue();
                Path path = curr.join(name);

                File file = new OpenOptions().create(true).write(true).open(repo, path);
                file.writeOnce(stream);
                file.close();

                readDirEntries();
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                try {
                    stream.close();
                } catch (IOException err) {
                    OutputErrorMsg(err);
                }
                loading.postValue(false);
            }
        }).start();
    }

    LiveData<File> openFile(Path path) {
        MutableLiveData<File> ret = new MutableLiveData<>();

        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                File file = repo.openFile(path);
                ret.postValue(file);
            } catch (Exception err) {
                OutputErrorMsg(err);
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

    public LiveData<String> readTextFile(Path path) {
        MutableLiveData<String> ret = new MutableLiveData<>();

        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                File file = repo.openFile(path);
                String text = file.readAllString();
                file.close();
                ret.postValue(text);
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();

        return ret;
    }

    public void updateTextFile(Path path, String text) {
        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                File file = new OpenOptions().write(true).open(repo, path);
                file.writeOnce(text);
                file.close();

                readDirEntries();
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public void rename(String from, String newName) {
        loading.setValue(true);
        new Thread(() -> {
            try {
                Repo repo = this.repo.getValue();
                Path path = new Path(from);
                path.setFileName(newName);
                repo.rename(new Path(from), path);

                readDirEntries();
            } catch (Exception err) {
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public void move(List<String> fromStrs, String toStr) {
        loading.setValue(true);
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
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public void copy(List<String> fromStrs, String toStr) {
        loading.setValue(true);
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
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    public void remove(List<String> paths) {
        loading.setValue(true);
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
                OutputErrorMsg(err);
            } finally {
                loading.postValue(false);
            }
        }).start();
    }

    private void OutputErrorMsg(Exception err) {
        Log.e(TAG, err.toString());
        error.postValue(err.getMessage());
    }

    private void readDirEntries() {
        loading.postValue(true);
        try {
            Repo repo = this.repo.getValue();
            Path path = this.path.getValue();
            DirEntry[] dirs = repo.readDir(path);
            List<DirEntry> dents = new ArrayList<>(Arrays.asList(dirs));
            this.dents.postValue(dents);
        } catch (Exception err) {
            OutputErrorMsg(err);
        } finally {
            loading.postValue(false);
        }
    }

    private void addSampleFiles(Repo repo) {
        try {
            // add dir
            repo.createDirAll(new Path("/dir/sub2/sub3"));
            repo.createDir(new Path("/dir2"));

            // add text file
            File file = new OpenOptions().create(true).write(true).open(repo, new Path("/hello_world.txt"));
            file.writeOnce("Hello, world!");
            file.close();

            // add video file
            InputStream is = res.openRawResource(R.raw.video);
            ByteBuffer fileBytes = ByteBuffer.allocateDirect(is.available());
            ReadableByteChannel channel = Channels.newChannel(is);
            channel.read(fileBytes);
            channel.close();
            is.close();
            file = new OpenOptions().create(true).write(true).open(repo, new Path("/video.mp4"));
            file.writeOnce(fileBytes);
            file.close();

            // add image file
            is = res.openRawResource(R.raw.image);
            fileBytes = ByteBuffer.allocateDirect(is.available());
            channel = Channels.newChannel(is);
            channel.read(fileBytes);
            channel.close();
            is.close();
            file = new OpenOptions().create(true).write(true).open(repo, new Path("/image.jpg"));
            file.writeOnce(fileBytes);
            file.close();

        } catch (Exception err) {
            OutputErrorMsg(err);
        }
    }
}
