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

import io.zbox.zboxfs.Path;

public class EditDialog extends DialogFragment {
    public interface EditDialogListener {
        void onEditDialogOk(Path path, String text);
    }

    private Path path;
    private String text;
    private EditDialogListener listener;

    EditDialog(Path path, String text, EditDialogListener listener) {
        this.path = path;
        this.text = text;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_edit, null,
                false);
        binding.setVariable(BR.text, text);

        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("Save", (DialogInterface dialog, int id) -> {
                    TextView view = rootView.findViewById(R.id.dlg_edit_txt_text);
                    String text = view.getText().toString();
                    listener.onEditDialogOk(path, text);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = EditDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
