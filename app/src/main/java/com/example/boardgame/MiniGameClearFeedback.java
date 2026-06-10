package com.example.boardgame;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class MiniGameClearFeedback {
    private static final long DISPLAY_MILLIS = 1_800L;
    private static final String OVERLAY_TAG = "mini_game_clear_feedback";

    private MiniGameClearFeedback() {
    }

    static void show(Activity activity, String title, String message) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        View previousOverlay = content.findViewWithTag(OVERLAY_TAG);
        if (previousOverlay != null) {
            content.removeView(previousOverlay);
        }

        FrameLayout overlay = new FrameLayout(activity);
        overlay.setTag(OVERLAY_TAG);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Color.argb(225, 232, 245, 233));
        overlay.setAlpha(0f);
        overlay.setElevation(dp(activity, 48));

        LinearLayout messageLayout = new LinearLayout(activity);
        messageLayout.setOrientation(LinearLayout.VERTICAL);
        messageLayout.setGravity(Gravity.CENTER);
        messageLayout.setPadding(
                (int) dp(activity, 36),
                (int) dp(activity, 28),
                (int) dp(activity, 36),
                (int) dp(activity, 28)
        );
        messageLayout.setScaleX(0.82f);
        messageLayout.setScaleY(0.82f);

        GradientDrawable messageBackground = new GradientDrawable();
        messageBackground.setColor(Color.WHITE);
        messageBackground.setCornerRadius(dp(activity, 8));
        messageBackground.setStroke((int) dp(activity, 2), Color.rgb(76, 175, 80));
        messageLayout.setBackground(messageBackground);
        messageLayout.setElevation(dp(activity, 12));

        TextView checkText = new TextView(activity);
        checkText.setGravity(Gravity.CENTER);
        checkText.setText("✓");
        checkText.setTextColor(Color.rgb(46, 125, 50));
        checkText.setTextSize(72);
        checkText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        checkText.setIncludeFontPadding(false);

        TextView titleText = new TextView(activity);
        titleText.setGravity(Gravity.CENTER);
        titleText.setText(title);
        titleText.setTextColor(Color.rgb(27, 94, 32));
        titleText.setTextSize(28);
        titleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView messageText = new TextView(activity);
        messageText.setGravity(Gravity.CENTER);
        messageText.setText(message);
        messageText.setTextColor(Color.rgb(55, 71, 79));
        messageText.setTextSize(16);

        messageLayout.addView(checkText, wrapContent());
        LinearLayout.LayoutParams titleParams = wrapContent();
        titleParams.topMargin = (int) dp(activity, 8);
        messageLayout.addView(titleText, titleParams);
        LinearLayout.LayoutParams messageParams = wrapContent();
        messageParams.topMargin = (int) dp(activity, 8);
        messageLayout.addView(messageText, messageParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        cardParams.leftMargin = (int) dp(activity, 24);
        cardParams.rightMargin = (int) dp(activity, 24);
        overlay.addView(messageLayout, cardParams);
        content.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        overlay.animate()
                .alpha(1f)
                .setDuration(180L)
                .start();
        messageLayout.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        overlay.postDelayed(() -> overlay.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction(() -> {
                    if (overlay.getParent() == content) {
                        content.removeView(overlay);
                    }
                })
                .start(), DISPLAY_MILLIS);
    }

    private static LinearLayout.LayoutParams wrapContent() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private static float dp(Activity activity, float value) {
        return value * activity.getResources().getDisplayMetrics().density;
    }
}
