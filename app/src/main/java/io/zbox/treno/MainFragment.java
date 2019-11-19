package io.zbox.treno;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.zbox.treno.databinding.FragmentMainBinding;
import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Metadata;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();

    private RepoViewModel model;

    private static class RepoListAdapter extends RecyclerView.Adapter<RepoListAdapter.RepoListViewHolder> {
        private List<String> repoList;

        static class RepoListViewHolder extends RecyclerView.ViewHolder {
            private ViewDataBinding binding;

            RepoListViewHolder(View view, ViewDataBinding binding) {
                super(view);
                this.binding = binding;
            }

            void bind(String uri) {
                binding.setVariable(BR.uri, uri);
                binding.executePendingBindings();
            }
        }

        RepoListAdapter(List<String> repoList) {
            this.repoList = repoList;
        }

        @NonNull
        @Override
        public RepoListViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_repo_list, parent,
                    false);
            return new RepoListViewHolder(binding.getRoot(), binding);
        }

        @Override
        public void onBindViewHolder(@NotNull RepoListViewHolder holder, int position) {
            String uri = repoList.get(position);
            holder.bind(uri);
        }

        @Override
        public int getItemCount() {
            return repoList.size();
        }
    }

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // obtain view model
        model = new ViewModelProvider(getActivity()).get(RepoViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SharedPreferences pref = getActivity().getPreferences(Context.MODE_PRIVATE);
        List<String> repoList = new ArrayList<>(pref.getAll().keySet());

        // Inflate the layout for this fragment
        FragmentMainBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main,
                container,false);
        binding.setLifecycleOwner(this);
        binding.setHandlers(this);
        View view = binding.getRoot();

        RecyclerView rvList = view.findViewById(R.id.frg_main_rv_repo_list);
        rvList.setHasFixedSize(true);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvList.setAdapter(new RepoListAdapter(repoList));

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle("Treno");

        return view;
    }

    public void createNewRepo(View view) {
        model.getRepo().observe(this, repo -> {
            if (repo != null) navToExplorer(view);
        });
        model.createRepo("mem://foo", "pwd");
    }

    public void navToExplorer(View view) {
        NavDirections directions = MainFragmentDirections.actionMainFragmentToExplorerFragment();
        Navigation.findNavController(view).navigate(directions);
    }
}
