package io.zbox.treno;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import io.zbox.treno.databinding.FragmentExplorerBinding;
import io.zbox.treno.databinding.FragmentPlayerBinding;
import io.zbox.zboxfs.Path;

public class PlayerFragment extends Fragment {

    private static final String TAG = PlayerFragment.class.getSimpleName();

    private Path filePath;
    private RepoViewModel model;

    private MediaSessionCompat mediaSession;
    private MediaControllerCompat mediaController;
    private MediaControllerCompat.TransportControls controls;
    private VideoPlayer player;

    private SurfaceView surfaceView;

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get file path from arguments
        filePath = PlayerFragmentArgs.fromBundle(getArguments()).getFilePath();

        // obtain view model
        model = ViewModelProviders.of(getActivity()).get(RepoViewModel.class);

        // create video player
        player = new VideoPlayer(getActivity());

        // create media session
        mediaSession = new MediaSessionCompat(getContext(), TAG);
        mediaSession.setMediaButtonReceiver(null);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder().build());
        mediaSession.setCallback(player);

        // create media controller
        mediaController = new MediaControllerCompat(getContext(), mediaSession);
        MediaControllerCompat.setMediaController(getActivity(), mediaController);
        controls = mediaController.getTransportControls();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate view
        FragmentPlayerBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_player, container, false);
        binding.setLifecycleOwner(this);
        binding.setHandlers(this);
        View view = binding.getRoot();

        // get surface and set its callback
        Fragment self = this;
        surfaceView = view.findViewById(R.id.frg_player_sv_surface);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surface is ready");
                player.setSurface(surfaceView);

                // open file
                model.openFile(filePath).observe(self, player);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
        mediaSession.release();
        mediaSession = null;
        Log.d(TAG, "PlayerFragment destroyed");
    }

    /* ------------------------------------------
       playback control
       ------------------------------------------ */
    public void startPlayback(View view) {
        controls.play();
    }

    public void stopPlayback(View view) {
        controls.stop();
    }
}
