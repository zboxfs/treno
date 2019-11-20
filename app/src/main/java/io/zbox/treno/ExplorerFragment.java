package io.zbox.treno;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Observer;

import io.zbox.treno.databinding.FragmentExplorerBinding;

public class ExplorerFragment extends Fragment implements
        AddDirDialog.AddDirDialogListener,
        CloseRepoDialog.CloseRepoDialogListener
{
    private static final String TAG = ExplorerFragment.class.getSimpleName();

    private static final int READ_EXTERNAL_FILE_REQUEST = 42;

    private RepoViewModel model;
    private RecyclerView rvList;
    private DirEntryListAdapter adapter;
    private SelectionMode selectionMode;
    private OnBackPressedCallback backCallback;

    private ObservableBoolean showAddButtons = new ObservableBoolean(false);

    public ExplorerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // obtain view model
        model = new ViewModelProvider(getActivity()).get(RepoViewModel.class);

        // create list adapter
        adapter = new DirEntryListAdapter(this.getContext(), model);

        // create selection mode
        selectionMode = new SelectionMode(this.getActivity(), model, adapter);

        // set action show/hide callback
        ObservableBoolean isInSelection = selectionMode.getIsInSelection();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        isInSelection.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged (Observable sender, int propertyId) {
                if (isInSelection.get()) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
            }
        });

        // create customised back navigation callback
        ExplorerFragment self = this;
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!model.goUp()) {
                    DialogFragment dlg = new CloseRepoDialog(self);
                    dlg.show(getActivity().getSupportFragmentManager(), "close");
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);

        // set up model observers
        model.getDirEntries().observe(this, adapter::submitList);
        model.getPath().observe(this, path -> {
            // set up back button and title in action bar
            actionBar.setDisplayHomeAsUpEnabled(!path.isRoot());
            actionBar.setTitle(path.isRoot() ? "Repo" : path.fileName());
        });
        model.getRepo().observe(this, repo -> {
            if (repo == null) {
                NavHostFragment.findNavController(this).popBackStack();
                getActivity().invalidateOptionsMenu();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate view
        FragmentExplorerBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_explorer, container,false);
        binding.setLifecycleOwner(this);
        binding.setModel(model);
        binding.setHandlers(this);
        binding.setIsInSelection(selectionMode.getIsInSelection());
        binding.setShowAddButtons(showAddButtons);
        View view = binding.getRoot();

        // set up recycler view
        rvList = view.findViewById(R.id.frg_explorer_rv_list);
        rvList.setHasFixedSize(true);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvList.setAdapter(adapter);

        // initialise selection mode
        selectionMode.init(rvList);

        // show app bar menu
        getActivity().invalidateOptionsMenu();

        return view;
    }

    public void toggleAddButtons(View view) {
        showAddButtons.set(!showAddButtons.get());
    }

    public void showAddDirDialog(View view) {
        DialogFragment dlg = new AddDirDialog(this);
        dlg.show(getActivity().getSupportFragmentManager(), "addDir");
        showAddButtons.set(false);
    }

    // implements AddDirDialog.AddDirDialogListener
    public void onAddDirDialogOk(String name) {
        Log.d(TAG, name);
        model.addDir(name);
    }

    public void startAddFileActivity(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_EXTERNAL_FILE_REQUEST);
        showAddButtons.set(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != READ_EXTERNAL_FILE_REQUEST
                || resultCode != Activity.RESULT_OK
                || data == null)
        {
            return;
        }

        Uri uri = data.getData();
        Log.i(TAG, "Uri: " + uri.toString());

        ContentResolver resolver = getActivity().getContentResolver();

        // get file name
        String fileName;
        try(Cursor cursor = resolver.query(uri, null, null, null,
                null, null))
        {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            Log.i(TAG, "Display Name: " + fileName);
        }

        try {
            InputStream stream = resolver.openInputStream(uri);
            model.addFile(fileName, stream);
        } catch (FileNotFoundException err) {
            Log.e(TAG, err.toString());
        }

    }

    // implements CloseRepoDialog.onRepoClosed
    public void onRepoClosed() {
        model.closeRepo();
    }

}
