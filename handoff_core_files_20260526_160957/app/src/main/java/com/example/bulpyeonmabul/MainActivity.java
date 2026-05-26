package com.example.bulpyeonmabul;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final int[] tileIds = {
            R.id.tile0, R.id.tile1, R.id.tile2, R.id.tile3, R.id.tile4,
            R.id.tile5, R.id.tile6, R.id.tile7, R.id.tile8, R.id.tile9,
            R.id.tile10, R.id.tile11, R.id.tile12, R.id.tile13, R.id.tile14, R.id.tile15
    };

    private ImageView[] players = new ImageView[4];
    private int[] playerPositions = {0, 0, 0, 0};

    // [확장] 각 플레이어의 점수판 데이터 (초기 점수 10점으로 세팅, 감점 대비)
    private int[] playerScores = {10, 10, 10, 10};

    // [확장] 각 플레이어의 다음 주사위 배율 보정 변수 (기본값 1.0)
    private double[] playerDiceMultipliers = {1.0, 1.0, 1.0, 1.0};

    // 각 플레이어의 카드 보유 여부
    private String[] playerCards = {null, null, null, null};

    private int currentPlayerTurn = 0;
    private final Random random = new Random();
    private TextView txtDiceVisual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtDiceVisual = findViewById(R.id.txtDiceVisual);
        players[0] = findViewById(R.id.player1);
        players[1] = findViewById(R.id.player2);
        players[2] = findViewById(R.id.player3);
        players[3] = findViewById(R.id.player4);

        Button btnDice = findViewById(R.id.btnDice);

        btnDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnDice.setEnabled(false);
                txtDiceVisual.animate()
                        .rotationBy(720)
                        .scaleX(1.2f).scaleY(1.2f)
                        .setDuration(500)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                txtDiceVisual.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();

                                // 순수 주사위 랜덤값 구하기
                                int rawDiceValue = random.nextInt(6) + 1;
                                int finalDiceValue = rawDiceValue;

                                // [물음표 기믹 반영] 주사위 배율 보정이 걸려있다면 계산 처리
                                double multiplier = playerDiceMultipliers[currentPlayerTurn];
                                if (multiplier != 1.0) {
                                    if (multiplier == 2.0) {
                                        finalDiceValue = rawDiceValue * 2;
                                        Toast.makeText(MainActivity.this, "✨ 주사위 값 2배 버프 적용!", Toast.LENGTH_SHORT).show();
                                    } else if (multiplier == 0.5) {
                                        // 소수점 올림 처리 (Math.ceil)
                                        finalDiceValue = (int) Math.ceil(rawDiceValue / 2.0);
                                        Toast.makeText(MainActivity.this, "📉 주사위 값 절반 디버프 적용(올림)!", Toast.LENGTH_SHORT).show();
                                    }
                                    playerDiceMultipliers[currentPlayerTurn] = 1.0; // 효과 소모 후 초기화
                                }

                                txtDiceVisual.setText(String.valueOf(finalDiceValue));
                                handlePlayerMove(finalDiceValue, btnDice);
                            }
                        }).start();
            }
        });
    }

    private void handlePlayerMove(int diceValue, Button btnDice) {
        String playerName = (currentPlayerTurn + 1) + "번 플레이어";
        Toast.makeText(this, playerName + " " + diceValue + "칸 전진!", Toast.LENGTH_SHORT).show();

        playerPositions[currentPlayerTurn] = (playerPositions[currentPlayerTurn] + diceValue) % 16;
        int targetPos = playerPositions[currentPlayerTurn];

        movePlayerSmoothly(players[currentPlayerTurn], targetPos);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkTileEffect(targetPos, btnDice);
            }
        }, 700);
    }

    private void movePlayerSmoothly(View player, int targetTileIndex) {
        View targetTile = findViewById(tileIds[targetTileIndex]);
        float offset = (currentPlayerTurn * 20);
        float targetX = targetTile.getX() + (targetTile.getWidth() / 4f) + offset;
        float targetY = targetTile.getY() + (targetTile.getHeight() / 4f) + offset;

        player.animate().x(targetX).y(targetY).setDuration(600).start();
    }

    private void checkTileEffect(int position, Button btnDice) {
        String playerName = (currentPlayerTurn + 1) + "번 플레이어";

        // 1. [모서리 칸] 4번, 8번, 12번 = ❓ 초안의 '물음표 돌발 이벤트' 칸
        if (position == 4 || position == 8 || position == 12) {
            handleRandomEventTile(btnDice);
        }
        // 2. [사이드 칸] 6번, 14번 = 📢 캡챠 및 다크패턴 광고 칸
        else if (position == 6 || position == 14) {
            Toast.makeText(this, "📢 [광고/캡챠] 매크로 방지 인증 및 광고를 제거하세요!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, CaptchaActivity.class);
            startActivity(intent);
            completeTurn(btnDice);
        }
        // 3. [사이드 칸] 2번, 10번 = 황금열쇠 카드 칸
        else if (position == 2 || position == 10) {
            handleCardTile(btnDice);
        }
        // 4. 보너스 점수 칸 (1, 3, 7, 11, 15번)
        else if (position == 1 || position == 3 || position == 7 || position == 11 || position == 15) {
            playerScores[currentPlayerTurn] += 3;
            Toast.makeText(this, "💰 " + playerName + " 보너스 점수 +3 획득! (현재: " + playerScores[currentPlayerTurn] + "점)", Toast.LENGTH_SHORT).show();
            completeTurn(btnDice);
        }
        // 5. 감점 함정 칸 (5, 9, 13번)
        else if (position == 5 || position == 9 || position == 13) {
            playerScores[currentPlayerTurn] = Math.max(0, playerScores[currentPlayerTurn] - 3);
            Toast.makeText(this, "💀 " + playerName + " 함정 진입! 점수 -3 감점! (현재: " + playerScores[currentPlayerTurn] + "점)", Toast.LENGTH_SHORT).show();
            completeTurn(btnDice);
        }
        // 6. 시작 칸 (0번)
        else {
            completeTurn(btnDice);
        }
    }

    // [신규 확장] ❓ 물음표 칸 10대 돌발 이벤트 핵심 알고리즘 구현
    private void handleRandomEventTile(Button btnDice) {
        int activePlayer = currentPlayerTurn;
        String playerName = (activePlayer + 1) + "번 플레이어";

        String[] events = {
                "🥇 1등과 점수 교환",
                "📉 꼴등과 점수 교환",
                "🔄 랜덤 플레이어와 위치 교환",
                "⏩ 다음 주사위 값 2배",
                "🐌 다음 주사위 값 절반 (소수점 올림)",
                "💥 내 점수 +7",
                "💔 내 점수 -7",
                "📈 모든 플레이어 점수 2배",
                "📉 모든 플레이어 점수 절반 (소수점 올림)",
                "😈 나를 제외한 모든 플레이어 점수 -3"
        };

        int eventIdx = random.nextInt(events.length);
        String chosenEvent = events[eventIdx];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("❓ 물음표 돌발 이벤트 발동!");
        builder.setMessage(playerName + "님이 모서리 물음표 칸에 도착했습니다!\n\n[결과]\n" + chosenEvent);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (eventIdx) {
                    case 0: // 1등과 점수 교환
                        int firstPlaceIdx = 0;
                        for (int i = 1; i < 4; i++) {
                            if (playerScores[i] > playerScores[firstPlaceIdx]) firstPlaceIdx = i;
                        }
                        int tempScore1 = playerScores[activePlayer];
                        playerScores[activePlayer] = playerScores[firstPlaceIdx];
                        playerScores[firstPlaceIdx] = tempScore1;
                        Toast.makeText(MainActivity.this, "🥇 1등(" + (firstPlaceIdx+1) + "번)과 점수가 바뀌었습니다!", Toast.LENGTH_SHORT).show();
                        break;

                    case 1: // 꼴등과 점수 교환
                        int lastPlaceIdx = 0;
                        for (int i = 1; i < 4; i++) {
                            if (playerScores[i] < playerScores[lastPlaceIdx]) lastPlaceIdx = i;
                        }
                        int tempScore2 = playerScores[activePlayer];
                        playerScores[activePlayer] = playerScores[lastPlaceIdx];
                        playerScores[lastPlaceIdx] = tempScore2;
                        Toast.makeText(MainActivity.this, "📉 꼴등(" + (lastPlaceIdx+1) + "번)과 점수가 바뀌었습니다!", Toast.LENGTH_SHORT).show();
                        break;

                    case 2: // 랜덤 플레이어와 위치 교환 (말 애니메이션 연동)
                        int targetPlayer;
                        do {
                            targetPlayer = random.nextInt(4);
                        } while (targetPlayer == activePlayer);

                        int tempPos = playerPositions[activePlayer];
                        playerPositions[activePlayer] = playerPositions[targetPlayer];
                        playerPositions[targetPlayer] = tempPos;

                        movePlayerSmoothly(players[activePlayer], playerPositions[activePlayer]);
                        movePlayerSmoothly(players[targetPlayer], playerPositions[targetPlayer]);
                        Toast.makeText(MainActivity.this, (targetPlayer + 1) + "번 플레이어와 위치가 교체되었습니다!", Toast.LENGTH_SHORT).show();
                        break;

                    case 3: // 다음 주사위 값 2배 버프 적립
                        playerDiceMultipliers[activePlayer] = 2.0;
                        break;

                    case 4: // 다음 주사위 값 절반 디버프 적립
                        playerDiceMultipliers[activePlayer] = 0.5;
                        break;

                    case 5: // 내 점수 +7
                        playerScores[activePlayer] += 7;
                        break;

                    case 6: // 내 점수 -7
                        playerScores[activePlayer] = Math.max(0, playerScores[activePlayer] - 7);
                        break;

                    case 7: // 모든 플레이어 점수 2배
                        for (int i = 0; i < 4; i++) playerScores[i] *= 2;
                        break;

                    case 8: // 모든 플레이어 점수 절반 (소수점 올림)
                        for (int i = 0; i < 4; i++) {
                            playerScores[i] = (int) Math.ceil(playerScores[i] / 2.0);
                        }
                        break;

                    case 9: // 나 제외 모든 플레이어 점수 -3
                        for (int i = 0; i < 4; i++) {
                            if (i != activePlayer) {
                                playerScores[i] = Math.max(0, playerScores[i] - 3);
                            }
                        }
                        Toast.makeText(MainActivity.this, "다른 플레이어들의 멘탈과 점수가 깎입니다!", Toast.LENGTH_SHORT).show();
                        break;
                }

                // 점수 로그 띄워주기 (디버깅 및 테스트용)
                System.out.println("현재 점수 현황: " + playerScores[0] + ", " + playerScores[1] + ", " + playerScores[2] + ", " + playerScores[3]);
                completeTurn(btnDice);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void handleCardTile(Button btnDice) {
        int playerIdx = currentPlayerTurn;
        String[] randomCardNames = {"방어권 카드", "주사위 홀수 고정", "주사위 짝수 고정", "상대 점수 스틸"};
        String newCard = randomCardNames[random.nextInt(randomCardNames.length)];

        if (playerCards[playerIdx] != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🃏 카드 교체 선택");
            builder.setMessage((playerIdx + 1) + "번 플레이어님은 이미 [" + playerCards[playerIdx] + "]를 가지고 있습니다.\n새로운 [" + newCard + "]로 교체하시겠습니까?");
            builder.setPositiveButton("교체", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    playerCards[playerIdx] = newCard;
                    Toast.makeText(MainActivity.this, "[" + newCard + "](으)로 교체되었습니다.", Toast.LENGTH_SHORT).show();
                    completeTurn(btnDice);
                }
            });
            builder.setNegativeButton("유지", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "기존 카드를 유지합니다.", Toast.LENGTH_SHORT).show();
                    completeTurn(btnDice);
                }
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            playerCards[playerIdx] = newCard;
            Toast.makeText(this, "🃏 " + (playerIdx + 1) + "번 플레이어 [" + newCard + "] 획득!", Toast.LENGTH_LONG).show();
            completeTurn(btnDice);
        }
    }

    private void completeTurn(Button btnDice) {
        currentPlayerTurn = (currentPlayerTurn + 1) % 4;
        btnDice.setEnabled(true);
    }
}