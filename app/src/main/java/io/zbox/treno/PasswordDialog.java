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

public class PasswordDialog extends DialogFragment {
    public interface PasswordDialogListener {
        void onPasswordEntered(String uri, String pwd);
    }

    private String uri;
    private PasswordDialogListener listener;

    PasswordDialog(String uri, PasswordDialogListener listener) {
        this.uri = uri;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_password, null,
                false);

        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    TextView view = rootView.findViewById(R.id.dlg_pwd_txt_pwd);
                    String pwd = view.getText().toString();
                    listener.onPasswordEntered(uri, pwd);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = PasswordDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
