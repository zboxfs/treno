package io.zbox.treno;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.ZboxException;

public class SelectionMode implements ActionMode.Callback, RenameDialog.RenameDialogListener {

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

    void init(RecyclerView rvList) {
        tracker = new SelectionTracker.Builder<>(
                "dir-entry-selection",
                rvList,
                new DirEntryKeyProvider(adapter),
                new DirEntryDetailsLookup(rvList),
                StorageStrategy.createStringStorage()
        )
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .withOnItemActivatedListener((ItemDetailsLookup.ItemDetails<String> item, MotionEvent e) -> {
                    Log.d(TAG, "==item clicked " + item.getSelectionKey());

                    String key = item.getSelectionKey();
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

                    // if clicked dir, enter it
                    if (dent.metadata.isDir()) {
                        model.setPath(path);
                        return true;
                    }

                    // otherwise, open the file by navigating to new fragment
                    NavDirections directions;
                    if (model.isImageFile(path)) {
                        // navigate to image viewer fragment
                        directions = ExplorerFragmentDirections.actionExplorerFragmentToViewerFragment(path);
                    } else if (model.isVideoFile(path)) {
                        // navigate to video player fragment
                        directions = ExplorerFragmentDirections.actionExplorerFragmentToPlayerFragment(path);
                    } else {
                        return false;
                    }
                    Navigation.findNavController(rvList).navigate(directions);

                    return true;
                })
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

    boolean isInMode() {
        return isInSelection.get();
    }

    // implement ActionMode.Callback.onCreateActionMode
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "==onCreateActionMode");
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

        switch (item.getItemId()) {
            case R.id.menu_sel_select_all:
                adapter.selectAll();
                return true;

            case R.id.menu_sel_rename:
                if (selection.size() != 1) return false; // only one item can be selected
                DialogFragment dlg = new RenameDialog(this);
                dlg.show(((FragmentActivity)context).getSupportFragmentManager(), "rename");
                return true;

            case R.id.menu_sel_move_to:
                return true;

            case R.id.menu_sel_copy_to:
                return true;

            case R.id.menu_sel_remove:
                List<Path> paths = new ArrayList<>();
                try {
                    for (String pathStr : selection) {
                        paths.add(new Path(pathStr));
                        tracker.deselect(pathStr);
                    }
                } catch (ZboxException ignore) {}

                model.remove(paths);
                return true;

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
        Selection<String> selection = tracker.getSelection();

        Log.d(TAG, newName + ", selected: "+selection.size());

        String old = selection.iterator().next();
        tracker.deselect(old);

        try {
            Path from = new Path(old);
            model.rename(from, newName);
        } catch (ZboxException err) {
            Log.e(TAG, err.toString());
        }
    }
}
