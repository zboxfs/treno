package io.zbox.treno.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import io.zbox.treno.R;

public class UriListAdapter extends ListAdapter<String, UriListAdapter.UriListViewHolder> {

    private final OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        void onUriItemClicked(String uri);
    }

    static class UriListViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;

        UriListViewHolder(View view, ViewDataBinding binding) {
            super(view);
            this.binding = binding;
        }

        void bind(String uri, OnItemClickListener listener) {
            binding.setVariable(io.zbox.treno.BR.uri, uri);
            binding.executePendingBindings();
            binding.getRoot().setOnClickListener(view -> listener.onUriItemClicked(uri));
        }
    }

    UriListAdapter(OnItemClickListener itemClickListener) {
        super(new DiffUtil.ItemCallback<String>() {
            @Override
            public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public UriListViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_uri_list, parent,
                false);
        return new UriListViewHolder(binding.getRoot(), binding);
    }

    @Override
    public void onBindViewHolder(@NotNull UriListViewHolder holder, int position) {
        String uri = getItem(position);
        holder.bind(uri, itemClickListener);
    }
}
