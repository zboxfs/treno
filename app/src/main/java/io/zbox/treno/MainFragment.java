package io.zbox.treno;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import io.zbox.treno.databinding.FragmentMainBinding;
import io.zbox.treno.dialog.DestroyRepoDialog;
import io.zbox.treno.dialog.PasswordDialog;

public class MainFragment extends Fragment implements
        PasswordDialog.PasswordDialogListener,
        DestroyRepoDialog.DestroyRepoDialogListener
{
    private static final String TAG = MainFragment.class.getSimpleName();

    private RepoViewModel model;
    private UriListAdapter adapter;
    private View layout;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // obtain view model
        model = new ViewModelProvider(getActivity()).get(RepoViewModel.class);

        // create uri list adapter
        adapter = new UriListAdapter(uri -> {
            DialogFragment dlg = new PasswordDialog(uri,this);
            dlg.show(getActivity().getSupportFragmentManager(), "password");
        });

        // set up model observers
        model.getUris().observe(this, adapter::submitList);
        model.getRepo().observe(this, repo -> {
            if (repo == null) return;

            // save repo uri to shared preference if it is not memory storage based
            String uri = repo.info().uri;
            if (!uri.startsWith("mem://")) {
                SharedPreferences pref = getActivity().getPreferences(Context.MODE_PRIVATE);
                if (!pref.getAll().containsKey(uri)) {
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(uri, uri);
                    editor.apply();
                }
            }

            // navigate to explorer fragment
            NavDirections directions = MainFragmentDirections.actionMainFragmentToExplorerFragment();
            NavHostFragment.findNavController(this).navigate(directions);
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentMainBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main,
                container,false);
        binding.setLifecycleOwner(this);
        binding.setHandlers(this);
        binding.setUris(model.getUris());
        View view = binding.getRoot();

        // set app bar title
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle("Treno");

        // set up recycler view
        RecyclerView rvList = view.findViewById(R.id.frg_main_rv_repo_list);
        rvList.setHasFixedSize(true);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvList.setAdapter(adapter);

        // enable swipe to delete for uri entry
        Fragment self = this;
        layout = view.findViewById(R.id.frg_main_layout);
        ItemTouchHelper.Callback swiper = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags (RecyclerView recyclerView,
                                         RecyclerView.ViewHolder viewHolder)
            {
                return makeMovementFlags(0, ItemTouchHelper.LEFT);
            }

            @Override
            public boolean onMove (RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                   RecyclerView.ViewHolder target)
            {
                return false;
            }

            @Override
            public void onSwiped (RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                Log.d(TAG, "====> onSwiped " + position + "," +adapter.getCurrentList().size());
                final String uri = adapter.getCurrentList().get(position);

                DialogFragment dlg = new DestroyRepoDialog(uri, position,
                        (DestroyRepoDialog.DestroyRepoDialogListener)self);
                dlg.show((getActivity()).getSupportFragmentManager(), "destroyRepo");
            }

            @Override
            public float getSwipeThreshold (RecyclerView.ViewHolder viewHolder) {
                return 0.7f;
            }
        };
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swiper);
        itemTouchhelper.attachToRecyclerView(rvList);

        return view;
    }

    public void createNewRepo(View view) {
        model.createRepo("mem", "pwd");
    }

    public void onPasswordEntered(String uri, String pwd) {
        model.openRepo(uri, pwd);
    }

    public void onRepoDestroyOk(String uri, int position) {
        LiveData<Boolean> repoDeleted = model.deleteRepo(uri);

        repoDeleted.observe(this, result -> {
            String msg = result ? "Repo was removed successfully." : "Failed to remove repo.";
            Snackbar snackbar = Snackbar.make(layout, msg, Snackbar.LENGTH_LONG);
            snackbar.show();
            repoDeleted.removeObservers(this);

            // restore uri if deletion failed
            if (!result) {
                adapter.notifyItemChanged(position);
            } else {
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void onRepoDestroyCancel(String uri, int position) {
        // restore uri if destroy repo is canceled
        adapter.notifyItemChanged(position);
    }
}
