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

public class ChangePwdDialog extends DialogFragment {
    public interface ChangePwdDialogListener {
        void onPasswordChange(String oldPwd, String newPwd);
    }

    private ChangePwdDialogListener listener;

    public ChangePwdDialog(ChangePwdDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_change_pwd, null,
                false);

        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    TextView view = rootView.findViewById(R.id.dlg_chg_pwd_txt_old);
                    String oldPwd = view.getText().toString();
                    view = rootView.findViewById(R.id.dlg_chg_pwd_txt_new);
                    String newPwd = view.getText().toString();
                    listener.onPasswordChange(oldPwd, newPwd);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = ChangePwdDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
