package io.zbox.treno.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.DialogFragment;

import java.util.Arrays;
import java.util.List;

import io.zbox.treno.BR;
import io.zbox.treno.R;

public class ZboxStorageDialog extends DialogFragment {
    private static final List<String> regions = Arrays.asList(
            "us-east-1 (N. Virginia)",
            "us-east-2 (Ohio)",
            "us-west-1 (N. California)",
            "us-west-2 (Oregon)",
            "ca-central-1 (Central)",
            "eu-west-1 (Ireland)",
            "eu-west-2 (London)",
            "eu-west-3 (Paris)",
            "eu-central-1 (Frankfurt)",
            "ap-northeast-1 (Tokyo)",
            "ap-northeast-2 (Seoul)",
            "ap-southeast-1 (Singapore)",
            "ap-southeast-2 (Sydney)",
            "ap-south-1 (Mumbai)",
            "sa-east-1 (São Paulo)"
    );

    private static final List<String> cacheTypes = Arrays.asList("mem", "file");

    private static final List<String> cacheSizes = Arrays.asList("1MB", "5MB", "50MB", "100MB");

    public interface RegionDialogListener {
        void onRegionDialogOk(String region, String cacheType, String cacheSize);
    }

    private RegionDialogListener listener;

    public ZboxStorageDialog(RegionDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_zbox_storage, null,
                false);
        binding.setVariable(BR.regions, regions);
        binding.setVariable(BR.cacheTypes, cacheTypes);
        binding.setVariable(BR.cacheSizes, cacheSizes);
        View rootView = binding.getRoot();
        return builder
                .setView(rootView)
                .setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                    Spinner spinner = rootView.findViewById(R.id.dlg_region_spi_region);
                    String region = regions.get(spinner.getSelectedItemPosition());
                    region = region.substring(0, region.indexOf(" "));
                    spinner = rootView.findViewById(R.id.dlg_region_spi_cache_type);
                    String cacheType = cacheTypes.get(spinner.getSelectedItemPosition());
                    spinner = rootView.findViewById(R.id.dlg_region_spi_cache_size);
                    String cacheSize = cacheSizes.get(spinner.getSelectedItemPosition());
                    listener.onRegionDialogOk(region, cacheType, cacheSize);
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                    Dialog dlg = ZboxStorageDialog.this.getDialog();
                    if (dlg != null) dlg.cancel();
                })
                .create();
    }
}
