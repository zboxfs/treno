package io.zbox.treno;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.zbox.treno.databinding.ActivityMainBinding;
import io.zbox.zboxfs.RepoInfo;

public class MainActivity extends AppCompatActivity implements ChangePwdDialog.ChangePwdDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int READ_EXTERNAL_FILE_REQUEST = 42;
    public static final int OPEN_FILE_REQUEST = 43;

    private RepoViewModel model;
    private View layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        // create view model
        model = ViewModelProviders.of(this).get(RepoViewModel.class);
        model.setResources(getResources());
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        List<String> uris = new ArrayList<>(pref.getAll().keySet());
        model.setUris(uris);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setLoading(model.getLoading());

        // get root layout
        layout = binding.getRoot().findViewById(R.id.act_main_layout);

        // use toolbar as action bar
        Toolbar toolbar = findViewById(R.id.act_main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

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

            case R.id.menu_main_change_pwd: {
                DialogFragment dlg = new ChangePwdDialog(this);
                dlg.show(getSupportFragmentManager(), "changePwd");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) return;

        Uri uri = data.getData();

        // returned from open file activity, revoke temporary access for content provider
        if (requestCode == OPEN_FILE_REQUEST) {
            revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return;
        }

        // returned from add file activity, add files to repo
        if (requestCode == READ_EXTERNAL_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            ContentResolver resolver = getContentResolver();

            // get file name
            String fileName;
            try(Cursor cursor = resolver.query(uri, null, null, null,
                    null, null))
            {
                if (cursor == null) return;

                while (cursor.moveToNext()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    try {
                        InputStream stream = resolver.openInputStream(uri);
                        model.addFile(fileName, stream);
                    } catch (FileNotFoundException err) {
                        Log.e(TAG, err.toString());
                        break;
                    }
                }
            }
        }
    }

    public void onPasswordChange(String oldPwd, String newPwd) {
        LiveData<Boolean> pwdChanged = model.changePwd(oldPwd, newPwd);

        pwdChanged.observe(this, result -> {
            String msg = result ? "Password was changed successfully." : "Failed to change password.";
            Snackbar snackbar = Snackbar.make(layout, msg, Snackbar.LENGTH_LONG);
            snackbar.show();
            pwdChanged.removeObservers(this);
        });
    }
}
