package io.zbox.treno.explorer;

import android.content.Intent;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import io.zbox.treno.MainActivity;
import io.zbox.treno.R;
import io.zbox.treno.RepoViewModel;
import io.zbox.treno.databinding.FragmentExplorerBinding;
import io.zbox.treno.dialog.AddDirDialog;
import io.zbox.treno.dialog.CloseRepoDialog;

public class ExplorerFragment extends Fragment implements
        AddDirDialog.AddDirDialogListener,
        CloseRepoDialog.CloseRepoDialogListener
{
    private static final String TAG = ExplorerFragment.class.getSimpleName();

    private RepoViewModel model;
    private RecyclerView rvList;
    private DirEntryListAdapter adapter;
    private ExplorerPresenter presenter;
    private OnBackPressedCallback backCallback;

    private ObservableBoolean showAddButtons = new ObservableBoolean(false);

    public ExplorerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentActivity activity = getActivity();

        // obtain view model
        model = new ViewModelProvider(activity).get(RepoViewModel.class);

        // create list adapter
        adapter = new DirEntryListAdapter(this.getContext(), model);

        // create selection mode
        presenter = new ExplorerPresenter(this, model, adapter);

        // set action show/hide callback
        ObservableBoolean isInSelection = presenter.getIsInSelection();
        ActionBar actionBar = ((AppCompatActivity)activity).getSupportActionBar();
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
                    dlg.show(activity.getSupportFragmentManager(), "close");
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
        binding.setIsInSelection(presenter.getIsInSelection());
        binding.setShowAddButtons(showAddButtons);
        View view = binding.getRoot();

        // refresh to show app bar menu
        getActivity().invalidateOptionsMenu();

        // set up recycler view
        rvList = view.findViewById(R.id.frg_explorer_rv_list);
        rvList.setHasFixedSize(true);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvList.setAdapter(adapter);

        // initialise presenter
        presenter.init(rvList);

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
        model.addDir(name);
    }

    public void startAddFileActivity(View view) {
        MainActivity activity = (MainActivity)getActivity();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        activity.startActivityForResult(intent, MainActivity.READ_EXTERNAL_FILE_REQUEST);
        showAddButtons.set(false);
    }

    // implements CloseRepoDialog.onRepoClosed
    public void onRepoClosed() {
        model.closeRepo();
    }
}
