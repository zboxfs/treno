package io.zbox.treno;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

import io.zbox.treno.databinding.FragmentViewerBinding;
import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.File;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.Repo;
import io.zbox.zboxfs.FileInputStream;
import io.zbox.zboxfs.ZboxException;

public class ViewerFragment extends Fragment implements Observer<File> {

    private static final String TAG = ViewerFragment.class.getSimpleName();

    private Path filePath;
    private RepoViewModel model;

    private ImageView img;

    public ViewerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get file path from arguments
        filePath = PlayerFragmentArgs.fromBundle(getArguments()).getFilePath();

        // obtain view model
        model = ViewModelProviders.of(getActivity()).get(RepoViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate view
        FragmentViewerBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_viewer, container, false);
        binding.setLifecycleOwner(this);
        View view = binding.getRoot();

        img = view.findViewById(R.id.frg_viewer_img);

        // open file
        model.openFile(filePath).observe(this, this);

        return view;
    }

    /* =====================================
       Observer<File>, this is called when file is opened
       ===================================== */
    public void onChanged(File file) {
        Log.d(TAG, "file is opened");


        try {
            InputStream stream = new FileInputStream(file);

            BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inJustDecodeBounds = true;
            Bitmap bm = BitmapFactory.decodeStream(stream, null, options);

            stream.close();

            /*int imageHeight = options.outHeight;
            int imageWidth = options.outWidth;
            String imageType = options.outMimeType;
            Log.d(TAG, "imageHeight: " + imageHeight + ", imageWidth: " + imageWidth + ", imageType: " + imageType);
*/
            img.setImageBitmap(bm);

        } catch (IOException err) {
            Log.e(TAG, err.toString());
        }
    }
}
