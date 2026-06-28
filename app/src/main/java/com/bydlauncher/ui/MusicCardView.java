package com.bydlauncher.ui;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bydlauncher.R;

import java.util.List;

public class MusicCardView {

    private static final String TAG = "MusicCardView";

    private final Context context;
    private final View rootView;

    private ImageView appIcon;
    private TextView appName;
    private ImageView cover;
    private final TextView title;
    private final TextView artist;
    private final ImageView btnPrev;
    private final ImageView btnPlayPause;
    private final ImageView btnNext;

    private MediaController activeController;

    public MusicCardView(View rootView) {
        this.context = rootView.getContext();
        this.rootView = rootView;

        appIcon = null;
        appName = null;
        cover = null;
        title = rootView.findViewById(R.id.music_title);
        artist = rootView.findViewById(R.id.music_artist);
        btnPrev = rootView.findViewById(R.id.music_prev);
        btnPlayPause = rootView.findViewById(R.id.music_play_pause);
        btnNext = rootView.findViewById(R.id.music_next);

        btnPrev.setOnClickListener(v -> sendMediaCommand(PlaybackState.ACTION_SKIP_TO_PREVIOUS));
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> sendMediaCommand(PlaybackState.ACTION_SKIP_TO_NEXT));

        showNoMedia();
    }

    public void refreshMediaState() {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) {
                showNoMedia();
                return;
            }

            List<MediaController> controllers = msm.getActiveSessions(null);
            if (controllers.isEmpty()) {
                showNoMedia();
                return;
            }

            activeController = controllers.get(0);
            updateFromController(activeController);
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to access media sessions, showing simulation", e);
            showSimulatedMedia();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get media sessions", e);
            showNoMedia();
        }
    }

    private void updateFromController(MediaController controller) {
        MediaMetadata metadata = controller.getMetadata();
        if (metadata == null) {
            showNoMedia();
            return;
        }

        String titleText = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artistText = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);

        if (titleText == null) titleText = context.getString(R.string.music_unknown_title);
        if (artistText == null) artistText = context.getString(R.string.music_unknown_artist);

        title.setText(titleText);
        artist.setText(artistText);
        artist.setVisibility(View.VISIBLE);

        if (cover != null) {
            if (art != null) {
                cover.setImageBitmap(art);
            } else {
                cover.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        String packageName = controller.getPackageName();
        if (appIcon != null) {
            try {
                appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageName));
            } catch (Exception ignored) {}
        }
        if (appName != null) {
            try {
                appName.setText(context.getPackageManager().getApplicationLabel(
                        context.getPackageManager().getApplicationInfo(packageName, 0)));
            } catch (Exception e) {
                appName.setText(packageName);
            }
        }

        PlaybackState state = controller.getPlaybackState();
        boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
        btnPlayPause.setImageResource(playing
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
    }

    private void togglePlayPause() {
        if (activeController == null) return;
        PlaybackState state = activeController.getPlaybackState();
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
            activeController.getTransportControls().pause();
        } else if (activeController != null) {
            activeController.getTransportControls().play();
        }
    }

    private void sendMediaCommand(long action) {
        if (activeController == null) return;
        if (action == PlaybackState.ACTION_SKIP_TO_PREVIOUS) {
            activeController.getTransportControls().skipToPrevious();
        } else if (action == PlaybackState.ACTION_SKIP_TO_NEXT) {
            activeController.getTransportControls().skipToNext();
        }
    }

    private void showNoMedia() {
        title.setText(R.string.not_playing);
        artist.setVisibility(View.GONE);
        if (appName != null) appName.setText("");
        if (cover != null) cover.setImageResource(android.R.drawable.ic_menu_gallery);
        activeController = null;
    }

    private void showSimulatedMedia() {
        if (appName != null) appName.setText("Music");
        title.setText("Simulated Track");
        artist.setText("BYD Audio");
        artist.setVisibility(View.VISIBLE);
        if (cover != null) cover.setImageResource(android.R.drawable.ic_menu_gallery);
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    public String getMediaSourceName() {
        if (activeController != null) {
            try {
                return context.getPackageManager().getApplicationLabel(
                        context.getPackageManager().getApplicationInfo(activeController.getPackageName(), 0)).toString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }
}
