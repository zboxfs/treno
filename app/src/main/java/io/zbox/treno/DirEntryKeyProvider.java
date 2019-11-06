package io.zbox.treno;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.zbox.zboxfs.DirEntry;

public class DirEntryKeyProvider extends ItemKeyProvider<String> {

    private final DirEntryListAdapter adapter;

    DirEntryKeyProvider() {
        super(ItemKeyProvider.SCOPE_MAPPED);
        adapter = null;
    }

    DirEntryKeyProvider(DirEntryListAdapter adapter) {
        super(ItemKeyProvider.SCOPE_MAPPED);
        this.adapter = adapter;
    }

    @Override
    public String getKey(int position) {
        if (adapter == null) return null;
        return adapter.getCurrentList().get(position).path.toString();
    }

    @Override
    public int getPosition(@NonNull String key) {
        if (adapter != null) {
            List<DirEntry> list = adapter.getCurrentList();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).path.equals(key)) return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }
}