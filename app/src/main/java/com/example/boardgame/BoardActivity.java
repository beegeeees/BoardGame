package com.example.boardgame;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class BoardActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYERS = "extra_players";

    private static final int BOARD_SIZE = 16;
    private static final int MAX_PLAYERS = 4;
    private static final int MICRO_GAME_SUCCESS_SCORE = 5;
    private static final int MICRO_GAME_FAILURE_SCORE = -5;
    private static final long TILE_EFFECT_DELAY_MILLIS = 650L;
    private static final long AD_NOTICE_DELAY_MILLIS = 3_000L;
    private static final long MINI_GAME_NOTICE_DELAY_MILLIS = 5_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final int[] tileIds = {
            R.id.tile0, R.id.tile1, R.id.tile2, R.id.tile3, R.id.tile4,
            R.id.tile5, R.id.tile6, R.id.tile7, R.id.tile8, R.id.tile9,
            R.id.tile10, R.id.tile11, R.id.tile12, R.id.tile13, R.id.tile14, R.id.tile15
    };
    private final ImageView[] playerViews = new ImageView[MAX_PLAYERS];
    private final TextView[] scorePanels = new TextView[MAX_PLAYERS];
    private final int[] renderedPositions = new int[MAX_PLAYERS];
    private final int[] movementTargets = new int[MAX_PLAYERS];

    private TextView txtDiceVisual;
    private TextView txtTurnInfo;
    private Button btnDice;
    private BoardDiceController diceController;
    private ActivityResultLauncher<Intent> microGameLauncher;
    private ActivityResultLauncher<Intent> miniGameLauncher;

    private String appliedTileKey = "";
    private String launchedMicroGameKey = "";
    private int requestedMiniGameRound = 0;
    private int scheduledMiniGameRound = 0;
    private int launchedMiniGameRound = 0;
    private int submittedMiniGameRound = 0;
    private boolean finalDialogShown = false;
    private boolean diceSubmissionPending = false;
    private boolean tileEventDialogVisible = false;
    private String pendingTileEventType = "";
    private Runnable pendingTileEffectRunnable;
    private Runnable pendingMiniGameStartRunnable;
    private AlertDialog phaseNoticeDialog;

    private final ServerSession.Listener serverListener = new ServerSession.Listener() {
        @Override
        public void onRoomUpdated(RoomSnapshot room) {
            renderBoard();
            driveGamePhase();
        }

        @Override
        public void onGameUpdated(GameSnapshot game) {
            showPendingTileEventResult(game);
            renderBoard();
            driveGamePhase();
        }

        @Override
        public void onServerError(String errorCode, String details) {
            GameSnapshot game = ServerSession.getLatestGameSnapshot();
            if (game != null
                    && "MINI_GAME_RUNNING".equals(game.getTurnPhase())
                    && isHost()
                    && details != null
                    && details.contains("Mini game is still accepting scores")) {
                scheduleFinishMiniGame();
                return;
            }
            if (diceSubmissionPending
                    && game != null
                    && isMyTurn(game)
                    && "WAITING_FOR_ROLL".equals(game.getTurnPhase())) {
                diceSubmissionPending = false;
                renderBoard();
            }
            pendingTileEventType = "";
            Toast.makeText(BoardActivity.this, details, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerGameLaunchers();
        applyImmersiveSystemBars();
        setContentView(R.layout.activity_board);

        txtDiceVisual = findViewById(R.id.txtDiceVisual);
        txtTurnInfo = findViewById(R.id.txtTurnInfo);
        btnDice = findViewById(R.id.btnDice);
        diceController = new BoardDiceController(this, this::submitDiceResult);
        playerViews[0] = findViewById(R.id.player1);
        playerViews[1] = findViewById(R.id.player2);
        playerViews[2] = findViewById(R.id.player3);
        playerViews[3] = findViewById(R.id.player4);
        Arrays.fill(renderedPositions, -1);
        Arrays.fill(movementTargets, -1);

        createScorePanels();
        btnDice.setOnClickListener(view -> {
            diceController.show();
        });

        renderBoard();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ServerSession.addListener(serverListener);
        ServerSession.connect(this);
        renderBoard();
        driveGamePhase();
    }

    @Override
    protected void onStop() {
        ServerSession.removeListener(serverListener);
        cancelPendingPhaseActions();
        if (diceController != null) {
            diceController.cancel();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        dismissPhaseNotice();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveSystemBars();
        }
    }

    private void registerGameLaunchers() {
        microGameLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            boolean success = result.getResultCode() == RESULT_OK
                    && result.getData() != null
                    && result.getData().getBooleanExtra(GameContract.EXTRA_SUCCESS, false);
            ServerSession.submitMicroGameScore(
                    success ? MICRO_GAME_SUCCESS_SCORE : MICRO_GAME_FAILURE_SCORE
            );
        });

        miniGameLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            GameSnapshot game = ServerSession.getLatestGameSnapshot();
            if (game == null || submittedMiniGameRound == game.getCurrentRound()) {
                return;
            }
            int progress = 0;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                boolean success = result.getData().getBooleanExtra(GameContract.EXTRA_SUCCESS, false);
                progress = result.getData().getIntExtra(GameContract.EXTRA_PROGRESS, success ? 100 : 0);
            }
            submittedMiniGameRound = game.getCurrentRound();
            ServerSession.submitMiniGameScore(progress);
            if (isHost()) {
                scheduleFinishMiniGame();
            }
        });
    }

    private void renderBoard() {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (room == null) {
            diceSubmissionPending = false;
            txtTurnInfo.setText("게임 정보를 기다리는 중");
            btnDice.setEnabled(false);
            btnDice.setAlpha(0.40f);
            return;
        }

        List<PlayerSnapshot> players = room.getPlayers();
        PlayerSnapshot[] playersBySlot = playersBySlot(players);
        for (int i = 0; i < MAX_PLAYERS; i++) {
            PlayerSnapshot player = playersBySlot[i];
            boolean visible = player != null;
            playerViews[i].setVisibility(visible ? View.VISIBLE : View.GONE);
            scorePanels[i].setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                movePlayerToTile(playerViews[i], player.getPosition(), i);
                renderScorePanel(scorePanels[i], player, i, isCurrentPlayer(game, player.getId()));
            } else {
                playerViews[i].animate().cancel();
                renderedPositions[i] = -1;
                movementTargets[i] = -1;
            }
        }

        if (game == null) {
            diceSubmissionPending = false;
            if (diceController != null && diceController.isActive()) {
                diceController.cancel();
            }
            txtDiceVisual.setText("?");
            txtTurnInfo.setText("게임 시작 대기 중");
            btnDice.setEnabled(false);
            btnDice.setAlpha(0.40f);
            return;
        }

        txtDiceVisual.setText(game.getLastDiceRoll() > 0 ? String.valueOf(game.getLastDiceRoll()) : "?");
        txtTurnInfo.setText(turnText(room, game));
        boolean canRollDice = isMyTurn(game)
                && "WAITING_FOR_ROLL".equals(game.getTurnPhase());
        if (!canRollDice) {
            diceSubmissionPending = false;
            if (diceController != null && diceController.isActive()) {
                diceController.cancel();
            }
        } else if (!diceSubmissionPending
                && !tileEventDialogVisible
                && diceController != null
                && !diceController.isActive()) {
            diceController.beginTurn();
        }
        boolean isDiceEnabled = canRollDice
                && !diceSubmissionPending
                && !tileEventDialogVisible;
        btnDice.setEnabled(isDiceEnabled);
        btnDice.setAlpha(isDiceEnabled ? 1.0f : 0.40f);

        if ("FINISHED".equals(game.getTurnPhase())) {
            btnDice.setEnabled(false);
            btnDice.setAlpha(0.40f);
            showFinalRanking(room);
        }
    }

    private void submitDiceResult(int diceRoll) {
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (game != null
                && isMyTurn(game)
                && "WAITING_FOR_ROLL".equals(game.getTurnPhase())
                && diceRoll >= 1
                && diceRoll <= 6) {
            diceSubmissionPending = true;
            btnDice.setEnabled(false);
            btnDice.setAlpha(0.40f);
            txtDiceVisual.setText(String.valueOf(diceRoll));
            ServerSession.rollDice(diceRoll);
            return;
        }
        renderBoard();
    }

    private void driveGamePhase() {
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (game == null) {
            return;
        }

        String phase = game.getTurnPhase();
        if ("WAITING_FOR_TILE_EFFECT".equals(phase) && isMyTurn(game)) {
            String key = game.getCurrentRound() + ":" + game.getCurrentPlayerId() + ":" + game.getLastDiceRoll();
            if (!key.equals(appliedTileKey)) {
                appliedTileKey = key;
                long delayMillis = TILE_EFFECT_DELAY_MILLIS;
                PlayerSnapshot currentPlayer = findPlayer(
                        ServerSession.getLatestRoomSnapshot(),
                        game.getCurrentPlayerId()
                );
                if (currentPlayer != null && isAdTile(currentPlayer.getPosition())) {
                    showPhaseNotice(getString(R.string.board_ad_notice));
                    delayMillis = AD_NOTICE_DELAY_MILLIS;
                }
                String tileEventType = currentPlayer == null
                        ? ""
                        : tileEventType(currentPlayer.getPosition());
                pendingTileEffectRunnable = () -> {
                    pendingTileEffectRunnable = null;
                    dismissPhaseNotice();
                    pendingTileEventType = tileEventType;
                    ServerSession.applyTileEffect();
                };
                handler.postDelayed(pendingTileEffectRunnable, delayMillis);
            }
            return;
        }

        if ("WAITING_FOR_MICRO_GAME".equals(phase) && isMyTurn(game)) {
            String key = game.getCurrentRound() + ":" + game.getCurrentPlayerId() + ":" + game.getLastDiceRoll();
            if (!key.equals(launchedMicroGameKey)) {
                launchedMicroGameKey = key;
                microGameLauncher.launch(randomMicroGameIntent());
            }
            return;
        }

        if ("WAITING_FOR_MINI_GAME".equals(phase)) {
            if (requestedMiniGameRound != game.getCurrentRound()) {
                requestedMiniGameRound = game.getCurrentRound();
                showPhaseNotice(getString(R.string.board_minigame_notice));
            }
            if (!isHost() || scheduledMiniGameRound == game.getCurrentRound()) {
                return;
            }
            scheduledMiniGameRound = game.getCurrentRound();
            pendingMiniGameStartRunnable = () -> {
                pendingMiniGameStartRunnable = null;
                ServerSession.startMiniGame(miniGameType(game.getCurrentRound()));
            };
            handler.postDelayed(pendingMiniGameStartRunnable, MINI_GAME_NOTICE_DELAY_MILLIS);
            return;
        }

        if ("MINI_GAME_RUNNING".equals(phase)
                && launchedMiniGameRound != game.getCurrentRound()) {
            launchedMiniGameRound = game.getCurrentRound();
            dismissPhaseNotice();
            miniGameLauncher.launch(miniGameIntent(game.getCurrentRound()));
        }
    }

    private void createScorePanels() {
        ViewGroup content = findViewById(android.R.id.content);
        ConstraintLayout rootLayout = (ConstraintLayout) content.getChildAt(0);
        rootLayout.setClipChildren(false);
        rootLayout.setClipToPadding(false);

        for (int i = 0; i < scorePanels.length; i++) {
            TextView panel = new TextView(this);
            panel.setId(View.generateViewId());
            panel.setGravity(Gravity.CENTER);
            panel.setIncludeFontPadding(false);
            panel.setMaxLines(2);
            panel.setPadding(dp(8), dp(6), dp(8), dp(6));
            panel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.board_score_panel_text_size));
            panel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            panel.setTextColor(i == 3 ? Color.rgb(17, 17, 17) : Color.WHITE);
            panel.setBackground(createScorePanelBackground(i, false));
            panel.setElevation(dp(14));
            panel.setTranslationZ(dp(14));

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.board_score_panel_width),
                    getResources().getDimensionPixelSize(R.dimen.board_score_panel_height)
            );
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            if (i == 0 || i == 2) {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            }
            if (i == 0 || i == 1) {
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            }

            rootLayout.addView(panel, params);
            panel.bringToFront();
            scorePanels[i] = panel;
        }
    }

    private void renderScorePanel(TextView panel, PlayerSnapshot player, int index, boolean active) {
        String turnLabel = active ? "  ▶ 턴" : "";
        boolean isMe = player.getId().equals(ServerSession.getCurrentPlayerId());
        panel.bringToFront();
        panel.setBackground(createScorePanelBackground(index, active));
        panel.setElevation(active ? dp(22) : dp(14));
        panel.setTranslationZ(active ? dp(22) : dp(14));
        panel.setAlpha(active ? 1.0f : 0.92f);
        panel.animate()
                .scaleX(active ? 1.04f : 1.0f)
                .scaleY(active ? 1.04f : 1.0f)
                .setDuration(160L)
                .start();
        SpannableStringBuilder label = new SpannableStringBuilder(player.getNickname());
        if (isMe) {
            label.append(" ");
            int badgeStart = label.length();
            label.append("나");
            label.setSpan(
                    new MeBadgeSpan(dp(4), dp(3), dp(5)),
                    badgeStart,
                    label.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        label.append(turnLabel)
                .append("\n")
                .append(String.valueOf(player.getScore()))
                .append("점");
        panel.setText(label);
    }

    private static final class MeBadgeSpan extends ReplacementSpan {
        private final int horizontalPadding;
        private final int verticalPadding;
        private final float cornerRadius;

        MeBadgeSpan(int horizontalPadding, int verticalPadding, float cornerRadius) {
            this.horizontalPadding = horizontalPadding;
            this.verticalPadding = verticalPadding;
            this.cornerRadius = cornerRadius;
        }

        @Override
        public int getSize(
                Paint paint,
                CharSequence text,
                int start,
                int end,
                Paint.FontMetricsInt fontMetrics
        ) {
            if (fontMetrics != null) {
                fontMetrics.ascent -= verticalPadding;
                fontMetrics.top = fontMetrics.ascent;
                fontMetrics.descent += verticalPadding;
                fontMetrics.bottom = fontMetrics.descent;
            }
            return Math.round(paint.measureText(text, start, end)) + (horizontalPadding * 2);
        }

        @Override
        public void draw(
                Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                Paint paint
        ) {
            float textWidth = paint.measureText(text, start, end);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float badgeTop = y + metrics.ascent - verticalPadding;
            float badgeBottom = y + metrics.descent + verticalPadding;

            int originalColor = paint.getColor();
            Paint.Style originalStyle = paint.getStyle();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(
                    new RectF(
                            x,
                            badgeTop,
                            x + textWidth + (horizontalPadding * 2),
                            badgeBottom
                    ),
                    cornerRadius,
                    cornerRadius,
                    paint
            );

            paint.setColor(Color.rgb(31, 106, 68));
            canvas.drawText(text, start, end, x + horizontalPadding, y, paint);
            paint.setColor(originalColor);
            paint.setStyle(originalStyle);
        }
    }

    private GradientDrawable createScorePanelBackground(int playerIndex, boolean active) {
        int[] fills = {
                Color.rgb(216, 67, 67),
                Color.rgb(47, 111, 222),
                Color.rgb(46, 157, 98),
                Color.rgb(242, 201, 76)
        };
        int[] borders = {
                Color.rgb(122, 23, 23),
                Color.rgb(24, 59, 122),
                Color.rgb(20, 83, 52),
                Color.rgb(138, 101, 8)
        };

        int safeIndex = Math.max(0, Math.min(playerIndex, fills.length - 1));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fills[safeIndex]);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(active ? dp(4) : dp(2),
                active ? Color.rgb(255, 249, 196) : borders[safeIndex]);
        return drawable;
    }

    private void movePlayerToTile(View player, int targetTileIndex, int playerIndex) {
        int safeTileIndex = Math.floorMod(targetTileIndex, BOARD_SIZE);
        if (movementTargets[playerIndex] == safeTileIndex) {
            return;
        }

        int previousTileIndex = renderedPositions[playerIndex];
        if (previousTileIndex == safeTileIndex) {
            placePlayerOnTile(player, safeTileIndex, playerIndex, false, null);
            return;
        }

        player.bringToFront();
        player.setElevation(dp(18));
        player.setTranslationZ(dp(18));

        int forwardSteps = previousTileIndex < 0
                ? 0
                : Math.floorMod(safeTileIndex - previousTileIndex, BOARD_SIZE);
        if (previousTileIndex < 0 || forwardSteps == 0 || forwardSteps > 6) {
            movementTargets[playerIndex] = -1;
            renderedPositions[playerIndex] = safeTileIndex;
            placePlayerOnTile(player, safeTileIndex, playerIndex, false, null);
            return;
        }

        movementTargets[playerIndex] = safeTileIndex;
        hopPlayerThroughTiles(player, previousTileIndex, safeTileIndex, playerIndex, forwardSteps);
    }

    private void hopPlayerThroughTiles(View player, int currentTileIndex, int targetTileIndex,
                                       int playerIndex, int remainingSteps) {
        if (remainingSteps <= 0) {
            completePlayerMovement(targetTileIndex, playerIndex);
            return;
        }

        int nextTileIndex = Math.floorMod(currentTileIndex + 1, BOARD_SIZE);
        Runnable endAction = remainingSteps == 1
                ? () -> completePlayerMovement(targetTileIndex, playerIndex)
                : () -> hopPlayerThroughTiles(player, nextTileIndex, targetTileIndex,
                        playerIndex, remainingSteps - 1);
        placePlayerOnTile(player, nextTileIndex, playerIndex, true,
                endAction);
    }

    private void completePlayerMovement(int targetTileIndex, int playerIndex) {
        renderedPositions[playerIndex] = targetTileIndex;
        movementTargets[playerIndex] = -1;
    }

    private void placePlayerOnTile(View player, int tileIndex, int playerIndex,
                                   boolean hop, Runnable endAction) {
        View targetTile = findViewById(tileIds[tileIndex]);
        targetTile.post(() -> {
            float[] target = playerTargetPosition(player, targetTile, playerIndex);
            player.animate().cancel();
            if (!hop) {
                player.animate()
                        .x(target[0])
                        .y(target[1])
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180L)
                        .withEndAction(endAction)
                        .start();
                return;
            }

            player.animate()
                    .x(target[0])
                    .y(target[1] - dp(12))
                    .scaleX(1.12f)
                    .scaleY(1.12f)
                    .setDuration(120L)
                    .withEndAction(() -> player.animate()
                            .y(target[1])
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(105L)
                            .withEndAction(endAction)
                            .start())
                    .start();
        });
    }

    private float[] playerTargetPosition(View player, View targetTile, int playerIndex) {
        float offsetX = (playerIndex % 2 == 0 ? -10f : 10f) + playerIndex * 3f;
        float offsetY = (playerIndex < 2 ? -10f : 10f);
        float targetX = targetTile.getX() + (targetTile.getWidth() / 2f)
                - (player.getWidth() / 2f) + offsetX;
        float targetY = targetTile.getY() + (targetTile.getHeight() / 2f)
                - (player.getHeight() / 2f) + offsetY;
        return new float[]{targetX, targetY};
    }

    private Intent randomMicroGameIntent() {
        int gameIndex = random.nextInt(3);
        if (gameIndex == 0) {
            return new Intent(this, AdGame1Activity.class);
        }
        if (gameIndex == 1) {
            return new Intent(this, AdGame2Activity.class);
        }
        return new Intent(this, AdGame3Activity.class);
    }

    private Intent miniGameIntent(int round) {
        Intent intent;
        if (round == GameContract.ROUND_CAPTCHA) {
            intent = new Intent(this, CaptchaActivity.class);
        } else if (round == GameContract.ROUND_PASSWORD) {
            intent = new Intent(this, PasswordActivity.class);
        } else {
            intent = new Intent(this, VolumeMazeActivity.class);
        }
        intent.putExtra(GameContract.EXTRA_ROUND, round);
        intent.putExtra(GameContract.EXTRA_PLAYER_INDEX, playerIndexOf(ServerSession.getCurrentPlayerId()));
        return intent;
    }

    private String miniGameType(int round) {
        if (round == GameContract.ROUND_CAPTCHA) {
            return "CAPTCHA";
        }
        if (round == GameContract.ROUND_PASSWORD) {
            return "PASSWORD";
        }
        return "VOLUME_MAZE";
    }

    private void scheduleFinishMiniGame() {
        handler.postDelayed(ServerSession::finishMiniGame, 3000L);
    }

    private void showPhaseNotice(String message) {
        dismissPhaseNotice();
        phaseNoticeDialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .create();
        phaseNoticeDialog.show();
    }

    private void dismissPhaseNotice() {
        if (phaseNoticeDialog != null) {
            if (phaseNoticeDialog.isShowing()) {
                phaseNoticeDialog.dismiss();
            }
            phaseNoticeDialog = null;
        }
    }

    private void cancelPendingPhaseActions() {
        if (pendingTileEffectRunnable != null) {
            handler.removeCallbacks(pendingTileEffectRunnable);
            pendingTileEffectRunnable = null;
            appliedTileKey = "";
        }
        if (pendingMiniGameStartRunnable != null) {
            handler.removeCallbacks(pendingMiniGameStartRunnable);
            pendingMiniGameStartRunnable = null;
        }
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (game != null && "WAITING_FOR_MINI_GAME".equals(game.getTurnPhase())) {
            requestedMiniGameRound = 0;
            scheduledMiniGameRound = 0;
        }
        dismissPhaseNotice();
    }

    private boolean isAdTile(int position) {
        int tileIndex = Math.floorMod(position, BOARD_SIZE);
        return tileIndex == 6 || tileIndex == 14;
    }

    private String tileEventType(int position) {
        int tileIndex = Math.floorMod(position, BOARD_SIZE);
        if (tileIndex == 2 || tileIndex == 10) {
            return "CARD";
        }
        if (tileIndex == 4 || tileIndex == 8 || tileIndex == 12) {
            return "QUESTION";
        }
        return "";
    }

    private void showPendingTileEventResult(GameSnapshot game) {
        if (game == null
                || pendingTileEventType.isEmpty()
                || "WAITING_FOR_TILE_EFFECT".equals(game.getTurnPhase())) {
            return;
        }

        String eventType = pendingTileEventType;
        pendingTileEventType = "";
        String systemMessage = game.getLastSystemMessage();

        if ("CARD".equals(eventType)) {
            boolean alreadyHasCard = systemMessage != null
                    && systemMessage.contains("already has a card");
            showTileEventDialog(
                    alreadyHasCard ? "카드 보유 중" : "방어 카드 획득!",
                    alreadyHasCard
                            ? "이미 방어 카드를 보유하고 있어 추가로 획득하지 않았습니다."
                            : "방어 카드를 획득했습니다.\n점수 감소 칸에 도착하면 자동으로 사용됩니다."
            );
            return;
        }

        if ("QUESTION".equals(eventType)) {
            boolean gainedScore = systemMessage != null
                    && systemMessage.contains("question result: 5 points");
            showTileEventDialog(
                    "물음표 이벤트",
                    gainedScore
                            ? "행운의 결과!\n점수 5점을 획득했습니다."
                            : "아쉬운 결과!\n점수 5점을 잃었습니다."
            );
        }
    }

    private void showTileEventDialog(String title, String message) {
        tileEventDialogVisible = true;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .create();
        dialog.setOnDismissListener(ignored -> {
            tileEventDialogVisible = false;
            renderBoard();
        });
        dialog.show();
    }

    private String turnText(RoomSnapshot room, GameSnapshot game) {
        PlayerSnapshot current = findPlayer(room, game.getCurrentPlayerId());
        String name = current == null ? "알 수 없음" : current.getNickname();
        String message = game.getLastSystemMessage();
        if (message == null || message.trim().isEmpty()) {
            message = game.getTurnPhase();
        }
        int displayedRound = Math.min(game.getCurrentRound(), game.getFinalRound());
        return "라운드 " + displayedRound + "/" + game.getFinalRound()
                + " | 현재 턴 " + name + "\n" + message;
    }

    private boolean isMyTurn(GameSnapshot game) {
        return game != null && game.getCurrentPlayerId().equals(ServerSession.getCurrentPlayerId());
    }

    private boolean isCurrentPlayer(GameSnapshot game, String playerId) {
        return game != null && game.getCurrentPlayerId().equals(playerId);
    }

    private boolean isHost() {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        return room != null && room.getHostPlayerId().equals(ServerSession.getCurrentPlayerId());
    }

    private PlayerSnapshot findPlayer(RoomSnapshot room, String playerId) {
        if (room == null) {
            return null;
        }
        for (PlayerSnapshot player : room.getPlayers()) {
            if (player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    private int playerIndexOf(String playerId) {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        if (room == null) {
            return 0;
        }
        for (int i = 0; i < room.getPlayers().size(); i++) {
            PlayerSnapshot player = room.getPlayers().get(i);
            if (player.getId().equals(playerId)) {
                return validSlotIndex(player.getSlotIndex()) ? player.getSlotIndex() : i;
            }
        }
        return 0;
    }

    private PlayerSnapshot[] playersBySlot(List<PlayerSnapshot> players) {
        PlayerSnapshot[] result = new PlayerSnapshot[MAX_PLAYERS];
        for (PlayerSnapshot player : players) {
            int slotIndex = player.getSlotIndex();
            if (!validSlotIndex(slotIndex) || result[slotIndex] != null) {
                slotIndex = firstEmptySlot(result);
            }
            if (validSlotIndex(slotIndex)) {
                result[slotIndex] = player;
            }
        }
        return result;
    }

    private int firstEmptySlot(PlayerSnapshot[] players) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private boolean validSlotIndex(int slotIndex) {
        return slotIndex >= 0 && slotIndex < MAX_PLAYERS;
    }

    private void showFinalRanking(RoomSnapshot room) {
        if (finalDialogShown || room == null) {
            return;
        }
        finalDialogShown = true;
        List<PlayerSnapshot> ranking = new ArrayList<>(room.getPlayers());
        ranking.sort(Comparator.comparingInt(PlayerSnapshot::getScore).reversed());

        StringBuilder message = new StringBuilder();
        int rank = 1;
        for (int i = 0; i < ranking.size(); i++) {
            PlayerSnapshot player = ranking.get(i);
            if (i > 0 && player.getScore() < ranking.get(i - 1).getScore()) {
                rank = i + 1;
            }
            boolean isTie = (i > 0 && player.getScore() == ranking.get(i - 1).getScore())
                    || (i < ranking.size() - 1
                    && player.getScore() == ranking.get(i + 1).getScore());
            String meSuffix = player.getId().equals(ServerSession.getCurrentPlayerId())
                    ? " [나]"
                    : "";

            message.append(isTie ? "공동 " : "")
                    .append(rank)
                    .append("위 ")
                    .append(player.getNickname())
                    .append(meSuffix)
                    .append(" - ")
                    .append(player.getScore())
                    .append("점\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("최종 순위")
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .setCancelable(false)
                .show();
    }

    private void applyImmersiveSystemBars() {
        Window window = getWindow();
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
