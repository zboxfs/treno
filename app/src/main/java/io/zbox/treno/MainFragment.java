package io.zbox.treno;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.zbox.treno.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentMainBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main,
                container,false);
        binding.setLifecycleOwner(this);
        binding.setHandlers(this);
        return binding.getRoot();
    }

    public void navToExplorer(View view) {
        Log.d(TAG, "navToExplorer");
        NavDirections directions = MainFragmentDirections.actionMainFragmentToExplorerFragment();
        Navigation.findNavController(view).navigate(directions);
    }
}
