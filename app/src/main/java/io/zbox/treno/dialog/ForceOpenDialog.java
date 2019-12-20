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

public class ForceOpenDialog extends DialogFragment {
    public interface ForceOpenDialogListener {
        void onForceOpenDialogOk();
    }

    private ForceOpenDialogListener listener;

    public ForceOpenDialog(ForceOpenDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_force_open, null,
                false);

        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    listener.onForceOpenDialogOk();
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = ForceOpenDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
