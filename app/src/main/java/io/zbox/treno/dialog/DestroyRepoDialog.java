package io.zbox.treno.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.DialogFragment;

import io.zbox.treno.R;

public class DestroyRepoDialog extends DialogFragment {
    public interface DestroyRepoDialogListener {
        void onRepoDestroyOk(String uri, int position);
        void onRepoDestroyCancel(String uri, int position);
    }

    private String uri;
    private int position;
    private DestroyRepoDialogListener listener;

    public DestroyRepoDialog(String uri, int position, DestroyRepoDialogListener listener) {
        this.uri = uri;
        this.position = position;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_destroy_repo,
                null, false);

        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    listener.onRepoDestroyOk(uri, position);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = DestroyRepoDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                    listener.onRepoDestroyCancel(uri, position);
                })
                .create();
    }
}
