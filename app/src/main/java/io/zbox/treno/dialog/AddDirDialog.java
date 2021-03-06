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

public class AddDirDialog extends DialogFragment {
    public interface AddDirDialogListener {
        void onAddDirDialogOk(String name);
    }

    private AddDirDialogListener listener;

    public AddDirDialog(AddDirDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_add_dir, null,
                false);

        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    TextView view = rootView.findViewById(R.id.dlg_add_dir_txt_name);
                    String name = view.getText().toString();
                    listener.onAddDirDialogOk(name);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = AddDirDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}