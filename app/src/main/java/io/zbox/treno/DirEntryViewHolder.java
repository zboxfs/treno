package io.zbox.treno;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Metadata;
import io.zbox.zboxfs.Path;

public class DirEntryViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = DirEntryViewHolder.class.getSimpleName();

    private RepoViewModel model;
    private DirEntry dent;
    private Context context;
    private ViewDataBinding binding;

    DirEntryViewHolder(Context context, RepoViewModel model, ViewDataBinding binding) {
        super(binding.getRoot());
        this.context = context;
        this.model = model;
        this.binding = binding;
    }

    DirEntryDetailsLookup.DirEntryDetails getItemDetails() {
        return new DirEntryDetailsLookup.DirEntryDetails(dent) {
            @Override
            public int getPosition() {
                return getAdapterPosition();
            }

            @Override
            public String getSelectionKey() {
                return dent.path.toString();
            }
        };
    }

    void bind(DirEntry dent, boolean isInSelect, boolean isSelected) {
        this.dent = dent;

        Metadata metadata = dent.metadata;
        Drawable icon = AppCompatResources.getDrawable(context, Utils.fileIcon(dent));
        String size = metadata.isDir() ? "" : Utils.prettySize(metadata.contentLen, true) + ",";
        String mtime = metadata.isDir() ? "" : Utils.prettyTime(metadata.modifiedAt);

        binding.setVariable(BR.dent, dent);
        binding.setVariable(BR.icon, icon);
        binding.setVariable(BR.size, size);
        binding.setVariable(BR.mtime, mtime);
        binding.setVariable(BR.handlers, this);

        binding.setVariable(BR.isInSelect, isInSelect);
        binding.setVariable(BR.isSelected, isSelected);

        binding.executePendingBindings();
    }
}
