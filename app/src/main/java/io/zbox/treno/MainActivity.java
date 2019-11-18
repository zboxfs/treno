package io.zbox.treno;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import android.content.ContentProviderClient;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import io.zbox.zboxfs.File;
import io.zbox.zboxfs.OpenOptions;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.Repo;
import io.zbox.zboxfs.RepoInfo;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RepoViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // use toolbar as action bar
        Toolbar toolbar = findViewById(R.id.act_main_toolbar);
        setSupportActionBar(toolbar);


        // create view model
        model = ViewModelProviders.of(this).get(RepoViewModel.class);

        // add test files and directories, for testing
        try {
            Repo repo = model.getRepo().getValue();

            // add dir
            repo.createDirAll(new Path("/dir/sub2/sub3"));
            repo.createDir(new Path("/dir2"));

            // add text file
            File file = new OpenOptions().create(true).write(true).open(repo, new Path("/text.txt"));
            file.writeOnce("Hello, world!".getBytes());
            file.close();

            // add video file
            InputStream is = getResources().openRawResource(R.raw.video);
            ByteBuffer fileBytes = ByteBuffer.allocateDirect(is.available());
            ReadableByteChannel channel = Channels.newChannel(is);
            channel.read(fileBytes);
            channel.close();
            is.close();
            file = new OpenOptions().create(true).write(true).open(repo, new Path("/video.mp4"));
            file.writeOnce(fileBytes);
            file.close();

            // add image files
            is = getResources().openRawResource(R.raw.image);
            fileBytes = ByteBuffer.allocateDirect(is.available());
            channel = Channels.newChannel(is);
            channel.read(fileBytes);
            channel.close();
            is.close();
            file = new OpenOptions().create(true).write(true).open(repo, new Path("/image.png"));
            file.writeOnce(fileBytes);
            file.close();

            is = getResources().openRawResource(R.raw.image2);
            fileBytes = ByteBuffer.allocateDirect(is.available());
            channel = Channels.newChannel(is);
            channel.read(fileBytes);
            channel.close();
            is.close();
            file = new OpenOptions().create(true).write(true).open(repo, new Path("/image2.jpg"));
            file.writeOnce(fileBytes);
            file.close();

            is = getResources().openRawResource(R.raw.image3);
            fileBytes = ByteBuffer.allocateDirect(is.available());
            channel = Channels.newChannel(is);
            channel.read(fileBytes);
            channel.close();
            is.close();
            file = new OpenOptions().create(true).write(true).open(repo, new Path("/image3.jpg"));
            file.writeOnce(fileBytes);
            file.close();

        } catch (Exception err) {
            Log.e(TAG, err.toString());
        }

        // set content provider model
        ContentProviderClient client = getContentResolver().acquireContentProviderClient("io.zbox.treno.provider");
        ZboxFileProvider provider = (ZboxFileProvider)client.getLocalContentProvider();
        provider.setModel(model);
        client.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_info: {
                RepoInfo info = model.getInfo();
                String txt = "Repo info:\n";
                txt += "=======================\n\n";
                txt += Utils.formatTabStr("Volume ID:", Utils.bytesToHex(Arrays.copyOfRange(info.volumeId, 0, 8)));
                txt += Utils.formatTabStr("Version:", info.version);
                txt += Utils.formatTabStr("Uri:", info.uri);
                txt += Utils.formatTabStr("Cipher:", info.cipher.name());
                txt += Utils.formatTabStr("Ops limit:", info.opsLimit.name());
                txt += Utils.formatTabStr("Mem limit:", info.memLimit.name());
                txt += Utils.formatTabStr("Compress:", String.valueOf(info.compress));
                txt += Utils.formatTabStr("Version limit:", String.valueOf(info.versionLimit));
                txt += Utils.formatTabStr("Dedup chunk:", String.valueOf(info.dedupChunk));
                txt += Utils.formatTabStr("Read only:", String.valueOf(info.isReadOnly));
                txt += Utils.formatTabStr("Created at:", String.valueOf(Utils.prettyTime(info.createdAt)));

                DialogFragment dlg = new InfoDialog(txt);
                dlg.show(getSupportFragmentManager(), "info");
                return true;
            }

            case R.id.menu_main_close:
                Log.d(TAG, "main menu close");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
