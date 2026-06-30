package com.bydlauncher.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bydlauncher.R;
import com.bydlauncher.model.VehicleStatus;

public class UnboundedPage {

    public interface ModeSwitch {
        void switchToStandard();
        void switchToStandardSettings();
    }

    private final View rootView;
    private final Context context;
    private final AggregateCardView aggregateCard;
    private final MusicCardView musicCard;
    private final LinearLayout cardColumn;
    private final View clockCard;
    private final View mapArea;
    private final TextView mapHint;
    private final View dragZone;
    private final ThreeFingerGestureDetector gestureDetector;

    private ModeSwitch modeSwitch;
    private AppSlotManager appSlotManager;
    private com.bydlauncher.api.BydAcApi acApi;
    private com.bydlauncher.model.VehicleStatus lastStatus;
    private boolean cardsVisible = true;

    private int[] cardOrder = {0, 1, 2};
    private View[] cards;

    public UnboundedPage(View rootView) {
        this.rootView = rootView;
        this.context = rootView.getContext();

        cardColumn = rootView.findViewById(R.id.unbounded_card_column);
        clockCard = rootView.findViewById(R.id.unbounded_clock_card);
        mapArea = rootView.findViewById(R.id.unbounded_map_area);
        mapHint = rootView.findViewById(R.id.unbounded_map_hint);
        dragZone = rootView.findViewById(R.id.unbounded_drag_zone);

        View aggRoot = findCardRoot(rootView.findViewById(R.id.agg_speed));
        aggregateCard = new AggregateCardView(aggRoot);

        View musicRoot = rootView.findViewById(R.id.music_card_mini);
        musicCard = new MusicCardView(musicRoot);

        cards = new View[]{clockCard, aggRoot, musicRoot};

        gestureDetector = new ThreeFingerGestureDetector(new ThreeFingerGestureDetector.Callback() {
            @Override public void onSwipeDown() { hideCards(); }
            @Override public void onSwipeUp() { showCards(); }
            @Override public void onSwipeLeft() { rotateCards(-1); }
            @Override public void onSwipeRight() { rotateCards(1); }
        }, context.getResources().getDisplayMetrics().density);

        rootView.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) return true;
            return false;
        });

        mapArea.setOnClickListener(v -> {
            if (appSlotManager != null) {
                appSlotManager.launch(AppSlotManager.SLOT_NAV);
            }
        });

        setupDragZone();
        setupMiniNavbar();
    }

    private View findCardRoot(View child) {
        View current = child;
        while (current.getParent() instanceof View) {
            View parent = (View) current.getParent();
            if (parent.getId() == R.id.unbounded_card_column || parent == rootView) return current;
            current = parent;
        }
        return child;
    }

    public void setModeSwitch(ModeSwitch modeSwitch) {
        this.modeSwitch = modeSwitch;
    }

    public void setAcApi(com.bydlauncher.api.BydAcApi acApi) {
        this.acApi = acApi;
    }

    public void setAppSlotManager(AppSlotManager appSlotManager) {
        this.appSlotManager = appSlotManager;
        updateMapHint();
    }

    private void updateMapHint() {
        if (appSlotManager == null || !appSlotManager.isConfigured(AppSlotManager.SLOT_NAV)) {
            mapHint.setText(R.string.unbounded_no_nav);
        } else {
            mapHint.setText(R.string.unbounded_tap_nav);
        }
    }

    public void updateStatus(VehicleStatus status) {
        this.lastStatus = status;
        aggregateCard.updateStatus(status);
    }

    public void refreshMusic() {
        musicCard.refreshMediaState();
    }

    private void hideCards() {
        if (!cardsVisible) return;
        cardsVisible = false;
        cardColumn.animate()
                .translationX(-cardColumn.getWidth())
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> cardColumn.setVisibility(View.GONE))
                .start();
    }

    private void showCards() {
        if (cardsVisible) return;
        cardsVisible = true;
        cardColumn.setVisibility(View.VISIBLE);
        cardColumn.setTranslationX(-cardColumn.getWidth());
        cardColumn.setAlpha(0f);
        cardColumn.animate()
                .translationX(0)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void rotateCards(int direction) {
        if (direction > 0) {
            int last = cardOrder[cardOrder.length - 1];
            System.arraycopy(cardOrder, 0, cardOrder, 1, cardOrder.length - 1);
            cardOrder[0] = last;
        } else {
            int first = cardOrder[0];
            System.arraycopy(cardOrder, 1, cardOrder, 0, cardOrder.length - 1);
            cardOrder[cardOrder.length - 1] = first;
        }
        reorderCards();
    }

    private void reorderCards() {
        cardColumn.removeAllViews();
        for (int idx : cardOrder) {
            View card = cards[idx];
            if (card.getParent() != null) {
                ((ViewGroup) card.getParent()).removeView(card);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
            cardColumn.addView(card, params);
        }
    }

    private void setupDragZone() {
        // 底部拖拽区域不触发模式切换，模式只能通过设置页切换
        dragZone.setClickable(true);
    }

    private void setupMiniNavbar() {
        View navbar = rootView.findViewById(R.id.mini_navbar);
        if (navbar != null) {
            navbar.setClickable(true);
        }

        View navAc = rootView.findViewById(R.id.mini_nav_ac);
        View navHome = rootView.findViewById(R.id.mini_nav_home);
        View navSettings = rootView.findViewById(R.id.mini_nav_settings);

        if (navAc != null) {
            navAc.setOnClickListener(v -> showAcPopup());
        }
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                // 无界模式已经是主页，切换卡片显隐
                if (cardsVisible) hideCards();
                else showCards();
            });
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                if (modeSwitch != null) modeSwitch.switchToStandardSettings();
            });
        }
    }

    public View getView() { return rootView; }

    private void showAcPopup() {
        if (acApi == null) return;
        AcPanelDialog.show(context, acApi, lastStatus);
    }
}
