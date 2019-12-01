package io.zbox.treno.explorer;

import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

import io.zbox.zboxfs.DirEntry;

public class DirEntryDetailsLookup extends ItemDetailsLookup<String> {

    private final RecyclerView recyclerView;

    static class DirEntryDetails extends ItemDetails<String> {

        DirEntry dent;

        DirEntryDetails(DirEntry dent) {
            super();
            this.dent = dent;
        }

        @Override
        public int getPosition () { return 0; }

        @Override
        public String getSelectionKey() { return null; }

        DirEntry getDent() { return dent; }
    }

    DirEntryDetailsLookup(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public DirEntryDetails getItemDetails(MotionEvent e) {
        View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
            if (holder instanceof DirEntryViewHolder) {
                return ((DirEntryViewHolder)holder).getItemDetails();
            }
        }
        return null;
    }
}
