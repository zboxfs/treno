package io.zbox.treno;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Metadata;
import io.zbox.zboxfs.Path;

@BindingMethods({
        @BindingMethod(type = android.widget.ImageView.class,
                attribute = "srcCompat",
                method = "setImageDrawable")})
public class DirEntryListAdapter extends ListAdapter<DirEntry, DirEntryViewHolder> {

    private static final String TAG = DirEntryListAdapter.class.getSimpleName();

    private RepoViewModel model;
    private Context context;

    private boolean isInActionMode = false;
    private SelectionTracker<String> tracker;

    DirEntryListAdapter(Context context, RepoViewModel model) {
        super(new DiffUtil.ItemCallback<DirEntry>() {
            @Override
            public boolean areItemsTheSame(@NonNull DirEntry oldItem, @NonNull DirEntry newItem) {
                return oldItem.path.equals(newItem.path);
            }

            @Override
            public boolean areContentsTheSame(@NonNull DirEntry oldItem, @NonNull DirEntry newItem) {
                return oldItem.equals(newItem);
            }
        });
        setHasStableIds(false);
        this.context = context;
        this.model = model;
    }

    @NonNull
    @Override
    public DirEntryViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_dent_list, parent,
                false);
        return new DirEntryViewHolder(context, model, binding);
    }

    @Override
    public void onBindViewHolder(@NotNull DirEntryViewHolder holder, int position) {
        DirEntry dent = getItem(position);
        holder.bind(dent, isInActionMode, tracker.isSelected(dent.path.toString()));
    }


    void setTracker(SelectionTracker<String> tracker) {
        this.tracker = tracker;
    }

    void enterSelectionMode() {
        isInActionMode = true;
        notifyItemRangeChanged(0, getItemCount(), "selection-mode");
    }

    void exitSelectionMode() {
        isInActionMode = false;
        tracker.clearSelection();
        notifyItemRangeChanged(0, getItemCount(), "");
    }

    void selectAll() {
        List<DirEntry> list = getCurrentList();

        if (tracker.getSelection().size() == list.size()) {
            for (DirEntry ent : list) {
                tracker.deselect(ent.path.toString());
            }
        } else {
            for (DirEntry ent : list) {
                tracker.select(ent.path.toString());
            }
        }
    }
}
