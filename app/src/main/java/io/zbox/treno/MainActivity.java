package io.zbox.treno;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import android.content.ContentProviderClient;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        // create view model
        model = ViewModelProviders.of(this).get(RepoViewModel.class);
        model.setResources(getResources());

        // set content provider model
        ContentProviderClient client = getContentResolver().acquireContentProviderClient("io.zbox.treno.provider");
        ZboxFileProvider provider = (ZboxFileProvider)client.getLocalContentProvider();
        provider.setModel(model);
        client.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return model.getRepo().getValue() != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                model.goUp();
                return true;
            }

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

            case R.id.menu_main_close: {
                model.closeRepo();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
