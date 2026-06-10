package com.example.boardgame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class BoardDiceController {
    interface ResultListener {
        void onDiceResult(int result);
    }

    private static final long MAX_TIME_MS = 30_000L;
    private static final long TIMEOUT_DISPLAY_DELAY_MS = 500L;
    private static final int COLOR_YELLOW = Color.rgb(255, 202, 40);
    private static final int COLOR_RED = Color.rgb(211, 47, 47);
    private static final int COLOR_NORMAL_TEXT = Color.rgb(51, 51, 51);
    private static final String[] DICE_FACES = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};

    private final AppCompatActivity activity;
    private final ResultListener resultListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View overlay;
    private final View selectionContent;
    private final View resultContent;
    private final TextView timerText;
    private final TextView instructionText;
    private final TextView diceVisual;
    private final TextView finalResultText;
    private final TextView boardTimerText;
    private final Button rollButton;
    private final Button boardViewButton;
    private final Button[] diceButtons;
    private final OnBackPressedCallback backPressedCallback;

    private long startTime;
    private boolean timerRunning;
    private boolean yabawiActive;
    private boolean locked;
    private boolean animating;
    private boolean active;
    private int animationGeneration;
    private Button selectedButton;
    private CountDownTimer rollTimer;

    BoardDiceController(AppCompatActivity activity, ResultListener resultListener) {
        this.activity = activity;
        this.resultListener = resultListener;
        overlay = activity.findViewById(R.id.diceOverlay);
        selectionContent = activity.findViewById(R.id.diceSelectionContent);
        resultContent = activity.findViewById(R.id.diceResultContent);
        timerText = activity.findViewById(R.id.diceOverlayTimer);
        instructionText = activity.findViewById(R.id.diceOverlayInstruction);
        diceVisual = activity.findViewById(R.id.diceOverlayVisual);
        finalResultText = activity.findViewById(R.id.diceOverlayResult);
        boardTimerText = activity.findViewById(R.id.txtDiceCountdown);
        rollButton = activity.findViewById(R.id.diceOverlayRollButton);
        boardViewButton = activity.findViewById(R.id.diceOverlayBoardButton);

        Button dice12 = activity.findViewById(R.id.diceOverlay12);
        Button dice34 = activity.findViewById(R.id.diceOverlay34);
        Button dice56 = activity.findViewById(R.id.diceOverlay56);
        Button diceRandom = activity.findViewById(R.id.diceOverlayRandom);
        diceButtons = new Button[]{dice12, dice34, dice56, diceRandom};
        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (active && !animating) {
                    minimize();
                } else {
                    Toast.makeText(activity, "지금은 보드로 돌아갈 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, backPressedCallback);

        dice12.setTag("12");
        dice34.setTag("34");
        dice56.setTag("56");
        diceRandom.setTag("RANDOM");

        for (Button button : diceButtons) {
            button.setOnClickListener(view -> handleSelection((Button) view));
        }
        boardViewButton.setOnClickListener(view -> minimize());
        rollButton.setOnClickListener(view -> {
            if (animating || yabawiActive) {
                return;
            }
            if (selectedButton == null) {
                Toast.makeText(activity, "먼저 주사위를 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            startRollAnimation();
        });
    }

    void show() {
        if (!active) {
            beginTurn();
        }
        showOverlay();
    }

    void beginTurn() {
        if (active) {
            return;
        }
        resetState();
        active = true;
        overlay.setVisibility(View.GONE);
        backPressedCallback.setEnabled(false);
        startTimer();
    }

    private void showOverlay() {
        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();
        backPressedCallback.setEnabled(true);
    }

    private void minimize() {
        if (!active || animating) {
            return;
        }
        overlay.setVisibility(View.GONE);
        backPressedCallback.setEnabled(false);
    }

    void cancel() {
        animationGeneration++;
        stopTimer();
        handler.removeCallbacksAndMessages(null);
        if (rollTimer != null) {
            rollTimer.cancel();
            rollTimer = null;
        }
        overlay.setVisibility(View.GONE);
        boardTimerText.setVisibility(View.GONE);
        backPressedCallback.setEnabled(false);
        animating = false;
        active = false;
    }

    boolean isShowing() {
        return overlay.getVisibility() == View.VISIBLE;
    }

    boolean isActive() {
        return active;
    }

    private void resetState() {
        animationGeneration++;
        stopTimer();
        handler.removeCallbacksAndMessages(null);
        if (rollTimer != null) {
            rollTimer.cancel();
            rollTimer = null;
        }
        selectedButton = null;
        yabawiActive = false;
        locked = false;
        animating = false;
        selectionContent.setVisibility(View.VISIBLE);
        resultContent.setVisibility(View.GONE);
        timerText.setVisibility(View.VISIBLE);
        rollButton.setVisibility(View.VISIBLE);
        rollButton.setEnabled(true);
        boardViewButton.setVisibility(View.VISIBLE);
        boardViewButton.setEnabled(true);
        instructionText.setText("주사위 버튼을 선택하고 굴리기를 누르세요.");
        instructionText.setTextColor(COLOR_NORMAL_TEXT);
        timerText.setText("30");
        diceVisual.setText("⚀");
        diceVisual.setTextColor(COLOR_NORMAL_TEXT);

        for (Button button : diceButtons) {
            button.setEnabled(true);
            button.setAlpha(0.6f);
            button.setScaleX(1f);
            button.setScaleY(1f);
            button.setTranslationX(0f);
        }
        revealOriginalButtons();
    }

    private final Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning || !active) {
                return;
            }

            long remainingTime = MAX_TIME_MS - (SystemClock.uptimeMillis() - startTime);
            if (remainingTime <= 0) {
                timerText.setText("0");
                timerText.setTextColor(COLOR_RED);
                updateBoardTimer(0, true);
                timerRunning = false;
                showOverlay();
                animating = true;
                boardViewButton.setVisibility(View.INVISIBLE);
                rollButton.setEnabled(false);
                for (Button button : diceButtons) {
                    button.setEnabled(false);
                }
                handler.postDelayed(
                        BoardDiceController.this::handleTimeout,
                        TIMEOUT_DISPLAY_DELAY_MS
                );
                return;
            }

            int secondsLeft = (int) Math.ceil(remainingTime / 1000.0);
            timerText.setText(String.valueOf(secondsLeft));
            timerText.setTextColor(remainingTime <= 3000L ? COLOR_RED : COLOR_YELLOW);
            updateBoardTimer(secondsLeft, remainingTime <= 3000L);
            handler.postDelayed(this, 50L);
        }
    };

    private void startTimer() {
        timerRunning = true;
        startTime = SystemClock.uptimeMillis();
        boardTimerText.setVisibility(View.VISIBLE);
        handler.post(updateTimerRunnable);
    }

    private void stopTimer() {
        timerRunning = false;
        handler.removeCallbacks(updateTimerRunnable);
        boardTimerText.setVisibility(View.GONE);
    }

    private void updateBoardTimer(int secondsLeft, boolean urgent) {
        boardTimerText.setText("주사위 선택 " + secondsLeft + "초");
        boardTimerText.setTextColor(urgent ? COLOR_RED : COLOR_YELLOW);
    }

    private void handleSelection(Button clickedButton) {
        if (animating || locked) {
            return;
        }
        if (!yabawiActive && random.nextFloat() < 0.2f) {
            triggerYabawiGimmick();
            return;
        }

        selectedButton = clickedButton;
        for (Button button : diceButtons) {
            button.setAlpha(0.6f);
            button.setScaleX(1f);
            button.setScaleY(1f);
        }
        clickedButton.setAlpha(1f);
        clickedButton.setScaleX(1.08f);
        clickedButton.setScaleY(1.08f);

        if (yabawiActive) {
            locked = true;
            revealOriginalButtons();
            startRollAnimation();
        }
    }

    private void triggerYabawiGimmick() {
        stopTimer();
        animating = true;
        yabawiActive = true;
        int generation = animationGeneration;
        selectedButton = null;
        timerText.setVisibility(View.INVISIBLE);
        rollButton.setVisibility(View.INVISIBLE);
        boardViewButton.setVisibility(View.INVISIBLE);
        instructionText.setText("다크 패턴 발동! 3초 후 버튼들이 가려지고 섞입니다.");
        instructionText.setTextColor(COLOR_RED);

        for (Button button : diceButtons) {
            button.setAlpha(1f);
            button.setScaleX(1f);
            button.setScaleY(1f);
        }

        handler.postDelayed(() -> {
            if (!active || generation != animationGeneration) {
                return;
            }
            showOverlay();
            for (Button button : diceButtons) {
                button.setText("?");
                button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(158, 158, 158)));
            }
            instructionText.setText("주사위 버튼들을 섞는 중...");
            shuffleButtons(5, generation);
        }, 3000L);
    }

    private void shuffleButtons(int remainingShuffles, int generation) {
        if (!active || generation != animationGeneration) {
            return;
        }
        if (remainingShuffles <= 0) {
            animating = false;
            timerText.setVisibility(View.VISIBLE);
            boardViewButton.setVisibility(View.VISIBLE);
            instructionText.setText("가려진 주사위 버튼을 선택하세요.");
            startTimer();
            return;
        }

        List<Float> targetPositions = new ArrayList<>();
        for (Button button : diceButtons) {
            targetPositions.add(button.getX());
        }
        Collections.shuffle(targetPositions);

        for (int i = 0; i < diceButtons.length; i++) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(
                    diceButtons[i],
                    View.X,
                    targetPositions.get(i)
            );
            animator.setDuration(500L);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            if (i == 0) {
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        shuffleButtons(remainingShuffles - 1, generation);
                    }
                });
            }
            animator.start();
        }
    }

    private void revealOriginalButtons() {
        for (Button button : diceButtons) {
            String tag = (String) button.getTag();
            if ("12".equals(tag)) {
                styleButton(button, "1~2\n(30%)", "#42A5F5");
            } else if ("34".equals(tag)) {
                styleButton(button, "3~4\n(20%)", "#66BB6A");
            } else if ("56".equals(tag)) {
                styleButton(button, "5~6\n(20%)", "#FFA726");
            } else {
                styleButton(button, "랜덤\n(1/6)", "#AB47BC");
            }
        }
    }

    private void styleButton(Button button, String text, String color) {
        button.setText(text);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)));
    }

    private void handleTimeout() {
        showOverlay();
        if (selectedButton == null) {
            selectedButton = diceButtons[3];
            Toast.makeText(activity, "시간 초과! 랜덤으로 굴립니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "시간 초과! 선택된 주사위로 굴립니다.", Toast.LENGTH_SHORT).show();
        }
        if (yabawiActive) {
            revealOriginalButtons();
        }
        startRollAnimation();
    }

    private void startRollAnimation() {
        stopTimer();
        animating = true;
        boardViewButton.setVisibility(View.INVISIBLE);
        rollButton.setEnabled(false);
        for (Button button : diceButtons) {
            button.setEnabled(false);
        }

        int finalResult = calculateDiceResult((String) selectedButton.getTag());
        instructionText.setText("주사위를 굴리는 중...");
        instructionText.setTextColor(COLOR_NORMAL_TEXT);
        diceVisual.setTextColor(Color.rgb(25, 118, 210));

        rollTimer = new CountDownTimer(1500L, 100L) {
            @Override
            public void onTick(long millisUntilFinished) {
                diceVisual.setText(DICE_FACES[random.nextInt(6)]);
            }

            @Override
            public void onFinish() {
                diceVisual.setText(DICE_FACES[finalResult - 1]);
                showResult(finalResult);
            }
        }.start();
    }

    private void showResult(int result) {
        selectionContent.setVisibility(View.GONE);
        resultContent.setVisibility(View.VISIBLE);
        finalResultText.setText(String.valueOf(result));
        handler.postDelayed(() -> {
            overlay.setVisibility(View.GONE);
            boardTimerText.setVisibility(View.GONE);
            backPressedCallback.setEnabled(false);
            animating = false;
            active = false;
            resultListener.onDiceResult(result);
        }, 1800L);
    }

    private int calculateDiceResult(String type) {
        int roll = random.nextInt(100);
        switch (type) {
            case "12":
                if (roll < 30) return 1;
                if (roll < 60) return 2;
                if (roll < 70) return 3;
                if (roll < 80) return 4;
                if (roll < 90) return 5;
                return 6;
            case "34":
                if (roll < 15) return 1;
                if (roll < 30) return 2;
                if (roll < 50) return 3;
                if (roll < 70) return 4;
                if (roll < 85) return 5;
                return 6;
            case "56":
                if (roll < 15) return 1;
                if (roll < 30) return 2;
                if (roll < 45) return 3;
                if (roll < 60) return 4;
                if (roll < 80) return 5;
                return 6;
            default:
                return random.nextInt(6) + 1;
        }
    }
}
