package com.example.bulpyeonmabul;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class CaptchaActivity extends AppCompatActivity {

    private GridLayout captchaGrid;
    private TextView txtStatus;
    private int currentStage = 1;
    private final int MAX_STAGE = 5;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);

        captchaGrid = findViewById(R.id.captchaGrid);
        txtStatus = findViewById(R.id.txtStatus);

        startNewStage();
    }

    private void startNewStage() {
        captchaGrid.removeAllViews();
        txtStatus.setText("단계: " + currentStage + " / " + MAX_STAGE);

        // 1. 기본 색상 생성
        int r = random.nextInt(200);
        int g = random.nextInt(200);
        int b = random.nextInt(200);
        int baseColor = Color.rgb(r, g, b);

        // 2. 난이도 조절 (단계가 높을수록 차이가 줄어듦)
        // 1단계: 차이 30, 5단계: 차이 4 (거의 안보임)
        int diff = 35 - (currentStage * 6);
        int targetColor = Color.rgb(r + diff, g + diff, b + diff);

        // 3. 정답 칸 위치 결정 (0~24)
        int answerIndex = random.nextInt(25);

        // 4. 5x5 버튼 생성
        for (int i = 0; i < 25; i++) {
            Button btn = new Button(this);
            // 버튼 크기 설정
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i % 5, 1f);
            params.rowSpec = GridLayout.spec(i / 5, 1f);
            params.setMargins(5, 5, 5, 5);
            btn.setLayoutParams(params);

            if (i == answerIndex) {
                btn.setBackgroundColor(targetColor);
                btn.setOnClickListener(v -> {
                    if (currentStage < MAX_STAGE) {
                        currentStage++;
                        startNewStage();
                    } else {
                        Toast.makeText(this, "광고 제거 완료!", Toast.LENGTH_SHORT).show();
                        finish(); // 게임 종료 후 보드판으로 복귀
                    }
                });
            } else {
                btn.setBackgroundColor(baseColor);
                btn.setOnClickListener(v -> {
                    Toast.makeText(this, "틀렸습니다! 1단계부터 다시 시작!", Toast.LENGTH_SHORT).show();
                    currentStage = 1; // 다크 패턴: 틀리면 처음부터
                    startNewStage();
                });
            }
            captchaGrid.addView(btn);
        }
    }
}