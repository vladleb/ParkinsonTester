package com.vlad.parkinsontester;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private float coeffRezist;
    private int timeGame = 10;
    private int multiplier = 0;
    private TextView tvCoeffRezistChange;
    private TextView tvTimeGame;
    private Button btnStart;
    private Button btnExit;
    private Button btnIncrementCoeffRezist;
    private Button btnDecrement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        tvCoeffRezistChange = (TextView) findViewById(R.id.textViewKoeffReistValue);
        tvTimeGame = (TextView) findViewById(R.id.tvTimeGame);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnExit = (Button) findViewById(R.id.btnExit);
        btnIncrementCoeffRezist = (Button) findViewById(R.id.btnIncrementCoeffRezist);
        btnDecrement = (Button) findViewById(R.id.btnDecrementCoeffRezist);

    }

    public void clickBtnIncrement(View view) {

        if (multiplier < 10) {
            multiplier++;
            coeffRezist = (float) (0.1 * multiplier);
            tvCoeffRezistChange.setText("Коэффициент сопротивления: " + coeffRezist);
        }
    }

    public void clickBtnDecrement(View view) {

        if (multiplier > 0) {
            multiplier--;
            coeffRezist = (float) (0.1 * multiplier);
            tvCoeffRezistChange.setText("Коэффициент сопротивления: " + coeffRezist);
        }
    }

    public void clickBtnExit(View view) {
        finish();
    }

    public void clickBtnStart(View view) {
        Intent intentStart = new Intent(this, BalanceBall.class);
        intentStart.putExtra("timeGame", timeGame);
        intentStart.putExtra("coeffRezist", coeffRezist);

        startActivity(intentStart);
    }

    public void clickBtnDecrementTimeGame(View view) {
        if (timeGame > 1) {
            timeGame--;
            tvTimeGame.setText("Время тестирвания: " + timeGame + " секунд");
        }
    }

    public void clickBtnIncrementTimeGame(View view) {
        timeGame++;
        tvTimeGame.setText("Время тестирвания: " + timeGame + " секунд");
    }
}