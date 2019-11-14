package io.zbox.treno;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate view
        FragmentViewerBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_viewer, container, false);
        binding.setLifecycleOwner(this);
        binding.setHandlers(this);
        View view = binding.getRoot();

        Fragment self = this;


        img = view.findViewById(R.id.frg_viewer_img);
        img.setOnTouchListener(new ViewerSwipeTouchListener(getActivity()) {
            void onSwipeTop() {
               Log.d(TAG, "top");
            }
            void onSwipeRight() {
                Log.d(TAG, "right");
            }
            void onSwipeLeft() {
                Log.d(TAG, "left");
                Path next = null;
                try {
                    next = new Path("/image2.jpg");
                } catch (ZboxException ignore) {

                }
                model.openFile(next).observe(self, (Observer<File>)self);
            }
            void onSwipeBottom() {
                Log.d(TAG, "bottom");
            }
        });

        // open file
        model.openFile(filePath).observe(this, this);

        // enter full screen
        hideSystemUI(true);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideSystemUI(false);
    }

    private void hideSystemUI(boolean hide) {
        View decorView = getActivity().getWindow().getDecorView();
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        int flag;

        if (hide) {
            flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    |View.SYSTEM_UI_FLAG_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_IMMERSIVE
                    |View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            actionBar.hide();
        } else {
            flag = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            |View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            actionBar.show();
        }
        decorView.setSystemUiVisibility(flag);
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
        } finally {
            file.close();
        }
    }
}
