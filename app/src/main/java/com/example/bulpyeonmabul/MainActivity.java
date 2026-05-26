package com.example.bulpyeonmabul;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.example.bulpyeonmabul.score.BoardScoreController;
import com.example.bulpyeonmabul.score.BoardTileType;
import com.example.bulpyeonmabul.score.ScorePlayerSeed;
import com.example.bulpyeonmabul.score.ScoreSnapshot;
import com.example.bulpyeonmabul.score.ScoreViewRow;
import com.example.bulpyeonmabul.integration.LocalTestServerBridge;
import com.example.bulpyeonmabul.integration.MainGameServerBridge;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 16;
    private static final int LOCAL_PLAYER_INDEX = 0;

    private final int[] tileIds = {
            R.id.tile0, R.id.tile1, R.id.tile2, R.id.tile3, R.id.tile4,
            R.id.tile5, R.id.tile6, R.id.tile7, R.id.tile8, R.id.tile9,
            R.id.tile10, R.id.tile11, R.id.tile12, R.id.tile13, R.id.tile14, R.id.tile15
    };

    private final String[] playerIds = {"p1", "p2", "p3", "p4"};
    private final String[] playerNames = {"1P", "2P", "3P", "4P"};
    private final ImageView[] players = new ImageView[4];
    private final TextView[] scorePanels = new TextView[4];
    private final int[] playerPositions = {0, 0, 0, 0};
    private final double[] playerDiceMultipliers = {1.0, 1.0, 1.0, 1.0};
    private final String[] playerCards = {null, null, null, null};
    private final Random random = new Random();

    private int currentPlayerTurn = 0;
    private int completedTurnsInRound = 0;
    private BoardScoreController scoreController;
    private TextView txtDiceVisual;
    private TextView txtTurnInfo;
    private Button btnDice;
    private TextView localPlayerBadge;
    private MainGameServerBridge serverBridge;
    private ActivityResultLauncher<Intent> diceLauncher;
    private ActivityResultLauncher<Intent> adGameLauncher;
    private ActivityResultLauncher<Intent> roundMiniGameLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerGameLaunchers();
        applyImmersiveSystemBars();
        setContentView(R.layout.activity_main);

        txtDiceVisual = findViewById(R.id.txtDiceVisual);
        txtTurnInfo = findViewById(R.id.txtTurnInfo);

        players[0] = findViewById(R.id.player1);
        players[1] = findViewById(R.id.player2);
        players[2] = findViewById(R.id.player3);
        players[3] = findViewById(R.id.player4);

        createTemporaryScorePanels();
        serverBridge = new LocalTestServerBridge();

        scoreController = new BoardScoreController(Arrays.asList(
                new ScorePlayerSeed(playerIds[0], playerNames[0]),
                new ScorePlayerSeed(playerIds[1], playerNames[1]),
                new ScorePlayerSeed(playerIds[2], playerNames[2]),
                new ScorePlayerSeed(playerIds[3], playerNames[3])
        ));

        btnDice = findViewById(R.id.btnDice);
        btnDice.setOnClickListener(v -> rollDice(btnDice));
        Button btnVolumeMazeTest = findViewById(R.id.btnVolumeMazeTest);
        btnVolumeMazeTest.setOnClickListener(v -> {
            Intent intent = new Intent(this, VolumeMazeActivity.class);
            intent.putExtra(GameContract.EXTRA_ROUND, GameContract.ROUND_VOLUME_MAZE);
            intent.putExtra(GameContract.EXTRA_PLAYER_INDEX, LOCAL_PLAYER_INDEX);
            startActivity(intent);
        });
        renderScoreBoard();
    }

    private void registerGameLaunchers() {
        diceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            int diceValue = random.nextInt(6) + 1;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                diceValue = result.getData().getIntExtra(GameContract.EXTRA_DICE_RESULT, diceValue);
            }
            int finalDiceValue = applyDiceMultiplier(diceValue);
            txtDiceVisual.setText(String.valueOf(finalDiceValue));
            handlePlayerMove(finalDiceValue, btnDice);
        });

        adGameLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            boolean success = result.getResultCode() == RESULT_OK
                    && result.getData() != null
                    && result.getData().getBooleanExtra(GameContract.EXTRA_SUCCESS, false);
            scoreController.onAdResult(playerIds[currentPlayerTurn], success);
            serverBridge.submitAdResult(playerIds[currentPlayerTurn], success);
            renderScoreBoard();
            Toast.makeText(this, success ? "광고 미니게임 성공! (+5점)" : "광고 미니게임 실패! (-5점)", Toast.LENGTH_SHORT).show();
            completeTurn(btnDice);
        });

        roundMiniGameLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            int round = scoreController.getSnapshot().getRound();
            int localProgress = 0;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                boolean success = result.getData().getBooleanExtra(GameContract.EXTRA_SUCCESS, false);
                localProgress = result.getData().getIntExtra(GameContract.EXTRA_PROGRESS, success ? 100 : 0);
                serverBridge.submitRoundMiniGameFinished(playerIds[LOCAL_PLAYER_INDEX], round, localProgress, success);
            }
            applyRoundMiniGameResult(round, localProgress);
        });
    }

    private void createTemporaryScorePanels() {
        ViewGroup content = findViewById(android.R.id.content);
        ConstraintLayout rootLayout = (ConstraintLayout) content.getChildAt(0);

        for (int i = 0; i < scorePanels.length; i++) {
            TextView panel = new TextView(this);
            panel.setId(View.generateViewId());
            panel.setGravity(Gravity.CENTER);
            panel.setPadding(dp(8), dp(8), dp(8), dp(8));
            panel.setTextSize(16);
            panel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            panel.setTextColor(i == 3 ? Color.rgb(17, 17, 17) : Color.WHITE);
            panel.setBackground(createScorePanelBackground(i, false));

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(dp(132), dp(72));
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
            scorePanels[i] = panel;

            if (i == LOCAL_PLAYER_INDEX) {
                localPlayerBadge = new TextView(this);
                localPlayerBadge.setId(View.generateViewId());
                localPlayerBadge.setGravity(Gravity.CENTER);
                localPlayerBadge.setText("나");
                localPlayerBadge.setTextSize(13);
                localPlayerBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                localPlayerBadge.setTextColor(Color.rgb(216, 67, 67));
                localPlayerBadge.setBackground(createMeBadgeBackground());
                localPlayerBadge.setElevation(dp(12));

                ConstraintLayout.LayoutParams badgeParams = new ConstraintLayout.LayoutParams(dp(34), dp(24));
                badgeParams.startToStart = panel.getId();
                badgeParams.topToTop = panel.getId();
                badgeParams.setMargins(dp(6), dp(6), 0, 0);
                rootLayout.addView(localPlayerBadge, badgeParams);
            }
        }
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveSystemBars();
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

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fills[playerIndex]);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(active ? dp(5) : dp(2), active ? Color.WHITE : borders[playerIndex]);
        return drawable;
    }

    private GradientDrawable createMeBadgeBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(1), Color.rgb(216, 67, 67));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void rollDice(Button btnDice) {
        btnDice.setEnabled(false);
        diceLauncher.launch(new Intent(this, DiceActivity.class));
    }

    private int applyDiceMultiplier(int rawDiceValue) {
        double multiplier = playerDiceMultipliers[currentPlayerTurn];
        if (multiplier == 1.0) {
            return rawDiceValue;
        }

        playerDiceMultipliers[currentPlayerTurn] = 1.0;
        if (multiplier == 2.0) {
            Toast.makeText(this, "✨ 주사위 값 2배 버프 적용!", Toast.LENGTH_SHORT).show();
            return rawDiceValue * 2;
        }

        Toast.makeText(this, "📉 주사위 값 절반 디버프 적용(올림)!", Toast.LENGTH_SHORT).show();
        return (int) Math.ceil(rawDiceValue / 2.0);
    }

    private void handlePlayerMove(int diceValue, Button btnDice) {
        String playerId = playerIds[currentPlayerTurn];
        String playerName = playerDisplayName(currentPlayerTurn);
        Toast.makeText(this, playerName + " " + diceValue + "칸 전진!", Toast.LENGTH_SHORT).show();

        scoreController.onMoveCompleted(playerId, diceValue);
        playerPositions[currentPlayerTurn] = (playerPositions[currentPlayerTurn] + diceValue) % BOARD_SIZE;
        int targetPos = playerPositions[currentPlayerTurn];
        serverBridge.submitDiceResult(playerId, diceValue, targetPos);

        movePlayerSmoothly(players[currentPlayerTurn], targetPos, currentPlayerTurn);
        renderScoreBoard();

        new Handler().postDelayed(() -> checkTileEffect(targetPos, btnDice), 700);
    }

    private void movePlayerSmoothly(View player, int targetTileIndex, int playerIndex) {
        View targetTile = findViewById(tileIds[targetTileIndex]);
        float offset = playerIndex * 20f;
        float targetX = targetTile.getX() + (targetTile.getWidth() / 4f) + offset;
        float targetY = targetTile.getY() + (targetTile.getHeight() / 4f) + offset;

        player.animate().x(targetX).y(targetY).setDuration(600).start();
    }

    private void checkTileEffect(int position, Button btnDice) {
        String playerId = playerIds[currentPlayerTurn];
        String playerName = playerDisplayName(currentPlayerTurn);

        if (position == 4 || position == 8 || position == 12) {
            scoreController.onTileLanded(playerId, BoardTileType.CHANCE);
            serverBridge.submitTileResolved(playerId, "CHANCE");
            handleRandomEventTile(btnDice);
        } else if (position == 6 || position == 14) {
            scoreController.onTileLanded(playerId, BoardTileType.AD);
            serverBridge.submitTileResolved(playerId, "AD");
            renderScoreBoard();
            Toast.makeText(this, "광고 발생! 마이크로 미니게임을 진행합니다.", Toast.LENGTH_LONG).show();
            adGameLauncher.launch(createRandomAdGameIntent());
        } else if (position == 2 || position == 10) {
            scoreController.onTileLanded(playerId, BoardTileType.CARD);
            serverBridge.submitTileResolved(playerId, "CARD");
            handleCardTile(btnDice);
        } else if (position == 1 || position == 3 || position == 7 || position == 11 || position == 15) {
            scoreController.onTileLanded(playerId, BoardTileType.SCORE_PLUS);
            serverBridge.submitTileResolved(playerId, "SCORE_PLUS");
            renderScoreBoard();
            Toast.makeText(this, "💰 점수 증가 칸! (+3점)", Toast.LENGTH_SHORT).show();
            completeTurn(btnDice);
        } else if (position == 5 || position == 9 || position == 13) {
            scoreController.onTileLanded(playerId, BoardTileType.SCORE_MINUS);
            serverBridge.submitTileResolved(playerId, "SCORE_MINUS");
            renderScoreBoard();
            Toast.makeText(this, "💀 점수 감소 칸! (-3점)", Toast.LENGTH_SHORT).show();
            completeTurn(btnDice);
        } else {
            scoreController.onTileLanded(playerId, BoardTileType.START);
            serverBridge.submitTileResolved(playerId, "START");
            renderScoreBoard();
            if (position == 0) {
                Toast.makeText(this, "시작 칸 보너스! (+5점)", Toast.LENGTH_SHORT).show();
            }
            completeTurn(btnDice);
        }
    }

    private Intent createRandomAdGameIntent() {
        int gameIndex = random.nextInt(3);
        if (gameIndex == 0) {
            return new Intent(this, AdGame1Activity.class);
        }
        if (gameIndex == 1) {
            return new Intent(this, AdGame2Activity.class);
        }
        return new Intent(this, AdGame3Activity.class);
    }

    private void handleRandomEventTile(Button btnDice) {
        int activePlayer = currentPlayerTurn;
        String activePlayerId = playerIds[activePlayer];
        String playerName = playerDisplayName(activePlayer);
        String[] events = getResources().getStringArray(R.array.random_events);

        int eventIdx = random.nextInt(events.length);
        String chosenEvent = events[eventIdx];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("❓ 물음표 돌발 이벤트 발동!");
        builder.setMessage(playerName + "님이 모서리 물음표 칸에 도착했습니다!\n\n[결과]\n" + chosenEvent);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                applyRandomEvent(eventIdx, activePlayer, activePlayerId);
                renderScoreBoard();
                completeTurn(btnDice);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void applyRandomEvent(int eventIdx, int activePlayer, String activePlayerId) {
        switch (eventIdx) {
            case 0:
                String topPlayerId = scoreController.getTopPlayerId();
                int firstPlaceIdx = playerIndexOf(topPlayerId);
                scoreController.swapScores(activePlayerId, topPlayerId, "CHANCE_SWAP_TOP");
                Toast.makeText(this, "🥇 1등(" + (firstPlaceIdx + 1) + "번)과 점수가 바뀌었습니다!", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                String lastPlayerId = scoreController.getLastPlayerId();
                int lastPlaceIdx = playerIndexOf(lastPlayerId);
                scoreController.swapScores(activePlayerId, lastPlayerId, "CHANCE_SWAP_LAST");
                Toast.makeText(this, "📉 꼴등(" + (lastPlaceIdx + 1) + "번)과 점수가 바뀌었습니다!", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                swapWithRandomPlayer(activePlayer);
                break;
            case 3:
                playerDiceMultipliers[activePlayer] = 2.0;
                break;
            case 4:
                playerDiceMultipliers[activePlayer] = 0.5;
                break;
            case 5:
                scoreController.onChanceOrCardScoreChanged(activePlayerId, 7, "CHANCE_SELF_PLUS");
                break;
            case 6:
                scoreController.onChanceOrCardScoreChanged(activePlayerId, -7, "CHANCE_SELF_MINUS");
                break;
            case 7:
                scoreController.multiplyAllScores(2, "CHANCE_ALL_DOUBLE");
                break;
            case 8:
                scoreController.halveAllScoresRoundUp("CHANCE_ALL_HALF");
                break;
            case 9:
                for (int i = 0; i < playerIds.length; i++) {
                    if (i != activePlayer) {
                        scoreController.onChanceOrCardScoreChanged(playerIds[i], -3, "CHANCE_OTHERS_MINUS");
                    }
                }
                Toast.makeText(this, "다른 플레이어들의 점수가 깎입니다!", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    private void swapWithRandomPlayer(int activePlayer) {
        int targetPlayer;
        do {
            targetPlayer = random.nextInt(players.length);
        } while (targetPlayer == activePlayer);

        int tempPos = playerPositions[activePlayer];
        playerPositions[activePlayer] = playerPositions[targetPlayer];
        playerPositions[targetPlayer] = tempPos;

        movePlayerSmoothly(players[activePlayer], playerPositions[activePlayer], activePlayer);
        movePlayerSmoothly(players[targetPlayer], playerPositions[targetPlayer], targetPlayer);
        Toast.makeText(this, (targetPlayer + 1) + "번 플레이어와 위치가 교체되었습니다!", Toast.LENGTH_SHORT).show();
    }

    private void handleCardTile(Button btnDice) {
        int playerIdx = currentPlayerTurn;
        String[] randomCardNames = getResources().getStringArray(R.array.golden_keys);
        String newCard = randomCardNames[random.nextInt(randomCardNames.length)];

        if (playerCards[playerIdx] != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🃏 카드 교체 선택");
            builder.setMessage((playerIdx + 1) + "번 플레이어님은 이미 [" + playerCards[playerIdx] + "]를 가지고 있습니다.\n새로운 [" + newCard + "]로 교체하시겠습니까?");
            builder.setPositiveButton("교체", (dialog, which) -> {
                playerCards[playerIdx] = newCard;
                Toast.makeText(MainActivity.this, "[" + newCard + "](으)로 교체되었습니다.", Toast.LENGTH_SHORT).show();
                completeTurn(btnDice);
            });
            builder.setNegativeButton("유지", (dialog, which) -> {
                Toast.makeText(MainActivity.this, "기존 카드를 유지합니다.", Toast.LENGTH_SHORT).show();
                completeTurn(btnDice);
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            playerCards[playerIdx] = newCard;
            Toast.makeText(this, "🃏 " + (playerIdx + 1) + "번 플레이어 [" + newCard + "] 획득!", Toast.LENGTH_LONG).show();
            completeTurn(btnDice);
        }
    }

    private void renderScoreBoard() {
        ScoreSnapshot snapshot = scoreController.getSnapshot();
        int[] scores = new int[playerIds.length];

        for (ScoreViewRow row : snapshot.getRanking()) {
            int playerIndex = playerIndexOf(row.getPlayerId());
            scores[playerIndex] = row.getScore();
        }

        txtTurnInfo.setText("라운드 " + snapshot.getRound() + " | 현재 턴 " + playerNames[currentPlayerTurn]);
        for (int i = 0; i < scorePanels.length; i++) {
            String turnLabel = i == currentPlayerTurn ? "  턴" : "";
            scorePanels[i].setBackground(createScorePanelBackground(i, i == currentPlayerTurn));
            scorePanels[i].setElevation(i == currentPlayerTurn ? dp(10) : dp(2));
            scorePanels[i].setText(playerNames[i] + turnLabel + "\n" + scores[i] + "점");
            scorePanels[i].setAlpha(i == currentPlayerTurn ? 1.0f : 0.86f);
        }
    }

    private int playerIndexOf(String playerId) {
        for (int i = 0; i < playerIds.length; i++) {
            if (playerIds[i].equals(playerId)) {
                return i;
            }
        }
        return 0;
    }

    private String playerDisplayName(int playerIndex) {
        return (playerIndex + 1) + "번 플레이어";
    }

    private void completeTurn(Button btnDice) {
        completedTurnsInRound++;
        currentPlayerTurn = (currentPlayerTurn + 1) % players.length;
        renderScoreBoard();
        if (completedTurnsInRound >= players.length) {
            btnDice.setEnabled(false);
            startRoundMiniGame();
        } else {
            btnDice.setEnabled(true);
        }
    }

    private void startRoundMiniGame() {
        int round = scoreController.getSnapshot().getRound();
        Intent intent;
        if (round == GameContract.ROUND_CAPTCHA) {
            intent = new Intent(this, CaptchaActivity.class);
            Toast.makeText(this, "1라운드 미니게임: 캡챠", Toast.LENGTH_LONG).show();
        } else if (round == GameContract.ROUND_PASSWORD) {
            intent = new Intent(this, PasswordActivity.class);
            Toast.makeText(this, "2라운드 미니게임: 비밀번호", Toast.LENGTH_LONG).show();
        } else {
            intent = new Intent(this, VolumeMazeActivity.class);
            Toast.makeText(this, "3라운드 미니게임: 볼륨 미로", Toast.LENGTH_LONG).show();
        }
        intent.putExtra(GameContract.EXTRA_ROUND, round);
        intent.putExtra(GameContract.EXTRA_PLAYER_INDEX, LOCAL_PLAYER_INDEX);
        roundMiniGameLauncher.launch(intent);
    }

    private void applyRoundMiniGameResult(int round, int localProgress) {
        List<MiniGameStanding> standings = new ArrayList<>();
        standings.add(new MiniGameStanding(playerIds[LOCAL_PLAYER_INDEX], localProgress));
        for (int i = 0; i < playerIds.length; i++) {
            if (i != LOCAL_PLAYER_INDEX) {
                standings.add(new MiniGameStanding(playerIds[i], random.nextInt(101)));
            }
        }
        Collections.sort(standings, Comparator.comparingInt(MiniGameStanding::getProgress).reversed());

        List<String> ranking = new ArrayList<>();
        for (MiniGameStanding standing : standings) {
            ranking.add(standing.playerId);
        }
        scoreController.onMiniGameFinished(ranking);
        renderScoreBoard();

        if (round >= GameContract.ROUND_VOLUME_MAZE) {
            showFinalRanking();
            btnDice.setEnabled(false);
            return;
        }

        scoreController.nextRound();
        completedTurnsInRound = 0;
        currentPlayerTurn = 0;
        renderScoreBoard();
        btnDice.setEnabled(true);
        Toast.makeText(this, (round + 1) + "라운드를 시작합니다.", Toast.LENGTH_LONG).show();
    }

    private void showFinalRanking() {
        ScoreSnapshot snapshot = scoreController.getSnapshot();
        StringBuilder message = new StringBuilder();
        int rank = 1;
        for (ScoreViewRow row : snapshot.getRanking()) {
            message.append(rank)
                    .append("위 ")
                    .append(row.getNickname())
                    .append(" - ")
                    .append(row.getScore())
                    .append("점\n");
            rank++;
        }

        new AlertDialog.Builder(this)
                .setTitle("최종 순위")
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .setCancelable(false)
                .show();
    }

    private static final class MiniGameStanding {
        private final String playerId;
        private final int progress;

        private MiniGameStanding(String playerId, int progress) {
            this.playerId = playerId;
            this.progress = progress;
        }

        private int getProgress() {
            return progress;
        }
    }
}
