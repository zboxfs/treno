package io.zbox.treno;

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

public class CopyToDialog extends DialogFragment {
    public interface CopyToDialogListener {
        void onCopyToDialogOk(String dest);
    }

    private String src;
    private CopyToDialog.CopyToDialogListener listener;

    CopyToDialog(String src, CopyToDialog.CopyToDialogListener listener) {
        this.src = src;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_copy_to, null,
                false);
        binding.setVariable(BR.dest, src);
        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    TextView view = rootView.findViewById(R.id.dlg_add_txt_path);
                    String name = view.getText().toString();
                    listener.onCopyToDialogOk(name);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = CopyToDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
