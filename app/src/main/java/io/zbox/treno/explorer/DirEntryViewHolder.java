package io.zbox.treno.explorer;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import io.zbox.treno.RepoViewModel;
import io.zbox.treno.util.Utils;
import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Metadata;

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

        binding.setVariable(io.zbox.treno.BR.dent, dent);
        binding.setVariable(io.zbox.treno.BR.icon, icon);
        binding.setVariable(io.zbox.treno.BR.size, size);
        binding.setVariable(io.zbox.treno.BR.mtime, mtime);
        binding.setVariable(io.zbox.treno.BR.handlers, this);

        binding.setVariable(io.zbox.treno.BR.isInSelect, isInSelect);
        binding.setVariable(io.zbox.treno.BR.isSelected, isSelected);

        binding.executePendingBindings();
    }
}
