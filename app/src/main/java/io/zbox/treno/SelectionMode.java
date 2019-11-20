package io.zbox.treno;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.OnItemActivatedListener;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.ZboxException;

public class SelectionMode implements
        ActionMode.Callback,
        RenameDialog.RenameDialogListener,
        MoveToDialog.MoveToDialogListener,
        CopyToDialog.CopyToDialogListener
{

    private static final String TAG = SelectionMode.class.getSimpleName();

    private Context context;
    private ActionMode actionMode;
    private RepoViewModel model;
    private DirEntryListAdapter adapter;
    private SelectionTracker<String> tracker;
    private ObservableBoolean isInSelection = new ObservableBoolean(false);

    SelectionMode(Context context, RepoViewModel model, DirEntryListAdapter adapter) {
        this.context = context;
        this.model = model;
        this.adapter = adapter;
    }

    private class ItemActivatedListener implements OnItemActivatedListener<String> {
        @Override
        public boolean onItemActivated(ItemDetailsLookup.ItemDetails<String> item, MotionEvent e) {
            String key = item.getSelectionKey();

            Log.d(TAG, "==item clicked " + key);

            DirEntry dent = ((DirEntryDetailsLookup.DirEntryDetails)item).getDent();
            Path path = dent.path;

            // if it is in selection mode
            if (isInSelection.get()) {
                if (tracker.isSelected(key)) {
                    tracker.deselect(key);
                } else {
                    tracker.select(key);
                }
                return true;
            }

            // if it is dir entry click, enter it
            if (dent.metadata.isDir()) {
                model.setPath(path);
                return true;
            }

            // otherwise, open system activity to view the file
            String pathStr = path.toString();
            Uri uri = Uri.parse("content://io.zbox.treno.provider" + pathStr);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, Utils.detectMimeType(pathStr));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return true;
            }
            return false;
        }
    }

    void init(RecyclerView rvList) {
        tracker = new SelectionTracker.Builder<>(
                "dir-entry-selection",
                rvList,
                new DirEntryKeyProvider(adapter),
                new DirEntryDetailsLookup(rvList),
                StorageStrategy.createStringStorage()
        )
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .withOnItemActivatedListener(new ItemActivatedListener())
                .build();
        adapter.setTracker(tracker);

        // set selection change observer
        SelectionMode self = this;
        tracker.addObserver(new SelectionTracker.SelectionObserver<String>() {
            @Override
            public void onItemStateChanged(@NonNull String key, boolean selected) {
                Log.d(TAG, "===> tracker.onItemStateChanged, key: "+key+", selected: "+selected);
            }

            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();

                int selectedCnt = tracker.getSelection().size();

                Log.d(TAG, "===> tracker.onSelectionChanged, "+selectedCnt);

                if (actionMode != null) {
                    actionMode.setTitle(selectedCnt > 0 ? selectedCnt + " selected" : null);
                    return;
                }

                // start action mode
                actionMode = ((AppCompatActivity)context).startSupportActionMode(self);

                // notify all item to enter action mode
                adapter.enterSelectionMode();
            }
        });
    }

    ObservableBoolean getIsInSelection() { return isInSelection; }

    // implement ActionMode.Callback.onCreateActionMode
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_selection, menu);

        isInSelection.set(true);

        return true;
    }

    // implement ActionMode.Callback.onPrepareActionMode
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // Return false if nothing is done
    }

    // implement ActionMode.Callback.onActionItemClicked
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Selection<String> selection = tracker.getSelection();
        FragmentManager fm = ((FragmentActivity)context).getSupportFragmentManager();

        switch (item.getItemId()) {
            case R.id.menu_sel_select_all:
                adapter.selectAll();
                return true;

            case R.id.menu_sel_rename: {
                if (selection.size() != 1) return false; // only one item can be selected
                DialogFragment dlg = new RenameDialog(selection.iterator().next(), this);
                dlg.show(fm, "rename");
                return true;
            }

            case R.id.menu_sel_move_to: {
                if (selection.size() != 1) return false; // only one item can be selected
                DialogFragment dlg = new MoveToDialog(selection.iterator().next(), this);
                dlg.show(fm, "moveTo");
                return true;
            }

            case R.id.menu_sel_copy_to: {
                DialogFragment dlg = new CopyToDialog(selection.iterator().next(), this);
                dlg.show(fm, "copyTo");
                return true;
            }

            case R.id.menu_sel_remove: {
                List<String> paths = new ArrayList<>();
                for (String pathStr : selection) {
                    paths.add(pathStr);
                }
                tracker.clearSelection();
                model.remove(paths);
                return true;
            }

            default:
                return false;
        }
    }

    // implement ActionMode.Callback.onDestroyActionMode
    public void onDestroyActionMode(ActionMode mode) {
        Log.d(TAG, "==onDestroyActionMode");
        adapter.exitSelectionMode();
        isInSelection.set(false);
        actionMode = null;
    }

    // implements RenameDialog.RenameDialogListener.onRenameDialogOk
    public void onRenameDialogOk(String newName) {
        String from = tracker.getSelection().iterator().next();
        tracker.deselect(from);
        model.rename(from, newName);
    }

    // implements MoveToDialog.MoveToDialogListener.onMoveToDialogOk
    public void onMoveToDialogOk(String to) {
        Selection<String> selection = tracker.getSelection();
        List<String> paths = new ArrayList<>();
        for (String pathStr : selection) {
            paths.add(pathStr);
        }
        tracker.clearSelection();
        model.move(paths, to);
    }

    // implements CopyToDialog.CopyToDialogListener.onCopyToDialogOk
    public void onCopyToDialogOk(String to) {
        Selection<String> selection = tracker.getSelection();
        List<String> paths = new ArrayList<>();
        for (String pathStr : selection) {
            paths.add(pathStr);
        }
        tracker.clearSelection();
        model.copy(paths, to);
    }
}
