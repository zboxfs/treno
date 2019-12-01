package io.zbox.treno.explorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.zbox.treno.R;
import io.zbox.treno.RepoViewModel;
import io.zbox.zboxfs.DirEntry;

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
