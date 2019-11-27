package io.zbox.treno.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.DialogFragment;

import io.zbox.treno.R;

public class RenameDialog extends DialogFragment {

    private String name;
    private RenameDialog.RenameDialogListener listener;

    public interface RenameDialogListener {
        void onRenameDialogOk(String newName);
    }

    public RenameDialog(String name, RenameDialog.RenameDialogListener listener) {
        this.name = name;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_rename, null,
                false);
        binding.setVariable(io.zbox.treno.BR.name, name);
        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    TextView view = rootView.findViewById(R.id.dlg_rename_txt_path);
                    String newName = view.getText().toString();
                    listener.onRenameDialogOk(newName);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = RenameDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
