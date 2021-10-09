package com.vlad.parkinsontester;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class BalanceBall extends AppCompatActivity implements SensorEventListener {

    private Context mContext = BalanceBall.this;

    private static final int REQUEST = 112;
    private String EMAIL = "vladis90@bk.ru";
    private String SUBJECT = "Результаты тестирования от ";
    private String TEXT = "Отправляю Вам результаты тестирования" + "\n" + "Измерение проведено в ";
    private StringBuilder sb = new StringBuilder();

    private SensorManager mSensorManager;
    private Sensor mAcceleration;

    private TextView tv;
    private TextView tvG;
    private TextView tvTimer;
    private ImageView ball;
    private ImageView point;
    private SensorManager sensorManager;
    private Sensor sensorAccel;
    private Button btnRestart;
    private Button btnSendLog;

    private int startTimer = 3;
    private int gameTimer = 10;
    private long time;
    private long start = 0;
    private double seconds;

    private int coordinateBallY = 0; // координата шара по высоте (dpBallHeight)
    private int coordinateBallX = 0; // координата шара по ширине (dpBallWidth)
    private int coordinateBallYMax; // максимальная координата по высоте с учетом размера шара (dpHeightMax)
    private int coordinateBallXMax; // максимальная координата по ширине с учетом размера шара (dpWidthMax)

    private float aX = 0; // ускорение по оси X
    private float aY = 0; // ускорение по оси Y
    private float vX = 0;
    private float vY = 0;

    private float aXLog = 0; // ускорение по оси X
    private float aYLog = 0; // ускорение по оси Y
    private float vXLog = 0;
    private float vYLog = 0;

    private double deltaT = 0.02;
    private float coeffRezist;

    private float dpHeight = 0; // число dp экрана по высоте
    private float dpWidth = 0; // число dp экрана по ширине

    private boolean yMoveDown = false;
    private boolean yMoveUp = false;
    private boolean xMoveLeft = false;
    private boolean xMoveRight = false;

    private boolean ballMoveBlock = true;
    private boolean flag = true;

    private float[] valuesAccelNew = new float[3]; // новые значения с датчиков
    private float[] valuesAccelOld = new float[3]; // старые значения с датчиков

    private int ballSize = 100; // размер шара
    private int pointSize = 150; // размер точки

    private int pointLeftDefault;
    private int pointTopDefault;
    private int ballLeftDefault;
    private int ballTopDefault;

    private enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        NONE
    }

    @SuppressLint({"Range", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance_ball);
        getSupportActionBar().hide();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //блокировка ориентации
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //не гасить экран

        Intent intent = getIntent();
        coeffRezist = intent.getFloatExtra("coeffRezist", (float) 0.0);
        gameTimer = intent.getIntExtra("timeGame", 10);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        mAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // Получаем датчик ускорения

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        dpHeight = (displayMetrics.heightPixels / 2) * 2;
        dpWidth = (displayMetrics.widthPixels / 2) * 2;

        coordinateBallY = (int) (displayMetrics.heightPixels / 2) - ballSize / 2; //Исходное положение шара по высоте
        coordinateBallX = (int) (displayMetrics.widthPixels / 2) - ballSize / 2; //Исходное положение шара по ширине

        ballTopDefault = coordinateBallY;
        ballLeftDefault = coordinateBallX;
        pointTopDefault = coordinateBallY - ((pointSize - ballSize) / 2);
        pointLeftDefault = coordinateBallX - ((pointSize - ballSize) / 2);

        coordinateBallYMax = 2 * (int) (displayMetrics.heightPixels / 2) - ballSize - 50;
        coordinateBallXMax = 2 * (int) (displayMetrics.widthPixels / 2) - ballSize;

        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);


        tv = new TextView(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(400, 200);
        lp.setMargins(20, 50, 0, 0);
        tv.setText("Время: " + Integer.toString(gameTimer));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(23);

        tv.setLayoutParams(lp);
        relativeLayout.addView(tv);

        tvG = new TextView(this);
        RelativeLayout.LayoutParams lpG = new RelativeLayout.LayoutParams(600, 100);
        lpG.setMargins(0, 200, 0, 0);

        tvG.setLayoutParams(lpG);
        relativeLayout.addView(tvG);

        tvTimer = new TextView(this);
        RelativeLayout.LayoutParams lpTimer = new RelativeLayout.LayoutParams(60, 110);
        lpTimer.setMargins((int) ((dpWidth / 2) - 30), 200, 0, 0);
        tvTimer.setText("3");
        tvTimer.setTextSize(25);
        tvTimer.setTypeface(Typeface.DEFAULT_BOLD);
        tvTimer.setLayoutParams(lpTimer);
        relativeLayout.addView(tvTimer);

        point = new ImageView(BalanceBall.this);
        point.setImageResource(R.drawable.point);
        RelativeLayout.LayoutParams pointLayoutParams = new RelativeLayout.LayoutParams(pointSize, pointSize);
        pointLayoutParams.setMargins(coordinateBallX - ((pointSize - ballSize) / 2),
                coordinateBallY - ((pointSize - ballSize) / 2), 0, 0);
        point.setLayoutParams(pointLayoutParams);
        relativeLayout.addView(point);

        ball = new ImageView(BalanceBall.this);
        ball.setImageResource(R.drawable.sphere);
        RelativeLayout.LayoutParams ballLayoutParams = new RelativeLayout.LayoutParams(ballSize, ballSize);
        ballLayoutParams.setMargins(coordinateBallX, coordinateBallY, 0, 0);
        ball.setLayoutParams(ballLayoutParams);
        relativeLayout.addView(ball);

        btnRestart = new Button(this);
        RelativeLayout.LayoutParams btnExitLayoutParams = new RelativeLayout.LayoutParams(250, 150);
        btnExitLayoutParams.setMargins((int) (dpWidth - 250), 50, 0, 0);
        btnRestart.setLayoutParams(btnExitLayoutParams);
        btnRestart.setAlpha((float) 0.5);
        btnRestart.setText("Меню");
        btnRestart.setOnClickListener(v -> {
            sb.setLength(0);
            finish();
        });
        relativeLayout.addView(btnRestart);

        btnSendLog = new Button(this);
        RelativeLayout.LayoutParams btnSendLogLayoutParams = new RelativeLayout.LayoutParams(350, 200);
        btnSendLogLayoutParams.setMargins((int) (dpWidth / 2) - 150, (int) (dpHeight - 200), 0, 0);
        btnSendLog.setLayoutParams(btnSendLogLayoutParams);
        btnSendLog.setText("Отправить отчет");
        btnSendLog.setEnabled(false);
        btnSendLog.setAlpha((float) 0.0);
        btnSendLog.setOnClickListener(v -> sendLog());
        relativeLayout.addView(btnSendLog);

        setContentView(relativeLayout, rlp);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        startTimers();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSensorManager.registerListener(this, this.mAcceleration,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mSensorManager.unregisterListener(this);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!ballMoveBlock) {

            if (flag) {

                time = System.currentTimeMillis();
                start = System.nanoTime();
                flag = false;
            }

            Direction directionX = Direction.NONE;
            Direction directionY = Direction.NONE;
            int reverseX = 1;
            int reverseY = 1;

            for (int i = 0; i < 3; i++) {
                valuesAccelNew[i] = event.values[i];

                Double truncatedDouble = BigDecimal.valueOf(valuesAccelNew[i])
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
                valuesAccelNew[i] = truncatedDouble.floatValue();
            }

            aX = valuesAccelNew[0] * (1 - coeffRezist);
            aY = valuesAccelNew[1] * (1 - coeffRezist);

            if (!xMoveRight && !xMoveLeft) { // Если шарик НЕ двигается по оси X
                if ((aX < 0) && coordinateBallX != coordinateBallXMax) { // Если ускорение направлено ВПРАВО по оси X и шарик НЕ уперся в ПРАВУЮ стенку
                    xMoveRight = true;
                    directionX = Direction.RIGHT;
                } else if ((aX > 0) && coordinateBallX != 0) { // Если ускорение направлено ВЛЕВО по оси X и шарик НЕ уперся в ЛЕВУЮ стенку
                    xMoveLeft = true;
                    directionX = Direction.LEFT;
                }

            } else if (xMoveRight) { // Если шарик двигается ВПРАВО по оси X

                if (aX < 0) // Если ускорение направлено ВПРАВО по оси X
                    directionX = Direction.RIGHT;

                if (aX > 0) { // Если ускорение направлено ВЛЕВО по оси X
                    directionX = Direction.RIGHT;
                    reverseX = -1;
                }
            } else if (xMoveLeft) { // Если шарик двигается ВЛЕВО по оси X

                if (aX > 0) // Если ускорение направлено ВЛЕВО по оси X
                    directionX = Direction.LEFT;

                if (aX < 0) { // Если ускорение направлено ВПРАВО по оси X
                    directionX = Direction.LEFT;
                    reverseX = -1;
                }
            }


            if (!yMoveUp && !yMoveDown) { // Если шарик НЕ двигается по оси Y
                if ((aY < 0) && coordinateBallY != 0) { // Если ускорение направлено ВВЕРХ по оси Y и шарик НЕ уперся в ВЕРХНЮЮ стенку
                    yMoveUp = true;
                    directionY = Direction.UP;
                } else if ((aY > 0) && coordinateBallY != coordinateBallYMax) { // Если ускорение направлено ВНИЗ по оси Y и шарик НЕ уперся в НИЖНЮЮ стенку
                    yMoveDown = true;
                    directionY = Direction.DOWN;
                }

            } else if (yMoveDown) { // Если шарик двигается ВНИЗ по оси Y

                if (aY > 0) // Если ускорение направлено ВНИЗ по оси Y
                    directionY = Direction.DOWN;

                if (aY < 0) { // Если ускорение направлено ВВЕРХ по оси Y
                    directionY = Direction.DOWN;
                    reverseY = -1;
                }
            } else if (yMoveUp) { // Если шарик двигается ВВЕРХ по оси Y

                if (aY < 0) // Если ускорение направлено ВВЕРХ по оси Y
                    directionY = Direction.UP;

                if (aY > 0) { // Если ускорение направлено ВНИЗ по оси Y
                    directionY = Direction.UP;
                    reverseY = -1;
                }
            }

            aX = Math.abs(aX) * reverseX;
            aY = Math.abs(aY) * reverseY;
            aXLog = aX;
            aYLog = aY;

            countingValues(aX, directionX, aY, directionY);

            long elapsedTime = System.nanoTime() - start;
            deltaT = (double) elapsedTime / 1_000_000_000.0;
            start = System.nanoTime();
            // System.out.println(deltaT + " seconds");

            for (int i = 0; i < 3; i++) {
                valuesAccelOld[i] = valuesAccelNew[i];
            }

            double timeD = (double) ((System.currentTimeMillis() - time) / 10) / 100;

            sb.append("    {" + "\n" +
                    "       \"coordinateBallY\": " + coordinateBallY + ",\n" +
                    "       \"coordinateBallX\": " + coordinateBallX + ",\n" +
                    "       \"aX\": " + aXLog + ",\n" +
                    "       \"aY\": " + aYLog + ",\n" +
                    "       \"vX\": " + vXLog + ",\n" +
                    "       \"vY\": " + vYLog + ",\n" +
                    "       \"Time\": " + timeD + "\n" +
                    "    }," + '\n');

        }
    }

    public void countingValues(float aXCurrent, Direction directionX, float aYCurrent, Direction directionY) {

        int deltaX;
        int deltaY;

        vXLog = (float) (vXLog + (aXCurrent * deltaT));
        vYLog = (float) (vYLog + (aYCurrent * deltaT));

        if(vXLog <= 0.0) {
            vXLog = 0;
        }
        if(vYLog <= 0.0) {
            vYLog = 0;
        }

        aXCurrent = (float) (aXCurrent * 3793.627); // from m/s^2 to px/s^2
        aYCurrent = (float) (aYCurrent * 3793.627);

        deltaX = (int) Math.round((float) ((vX * deltaT + ((convertPixelsToDp(aXCurrent, BalanceBall.this) * Math.pow(deltaT, 2)) / 2))));
        vX = (float) (vX + (convertPixelsToDp(aXCurrent, BalanceBall.this) * deltaT));

        if (vX <= 0.0) {
            vX = 0;
            vXLog = 0;
            xMoveRight = false;
            xMoveLeft = false;
        }

        deltaY = (int) Math.round((float) ((vY * deltaT + ((convertPixelsToDp(aYCurrent, BalanceBall.this) * Math.pow(deltaT, 2)) / 2))));
        vY = (float) (vY + (convertPixelsToDp(aYCurrent, BalanceBall.this) * deltaT));

        if (vY <= 0.0) {
            vY = 0;
            vXLog = 0;
            yMoveUp = false;
            yMoveDown = false;
        }

        ballMoving(deltaX, directionX, deltaY, directionY);
    }

    public void ballMoving(int deltaMovementX, Direction directionX, int deltaMovementY, Direction directionY) {

        List<Integer> idMovementX = new ArrayList<>();
        List<Integer> idMovementY = new ArrayList<>();
        for (int i = 0; i < Math.max(deltaMovementX, deltaMovementY); i++) {
            idMovementX.add(i);
            idMovementY.add(i);
        }
        randomize(idMovementX);
        randomize(idMovementY);

        RelativeLayout.LayoutParams ballLayoutParams = new RelativeLayout.LayoutParams(100, 100);

        for (int i = 0; i < Math.max(deltaMovementX, deltaMovementY); i++) {

            if (idMovementX.get(i) < deltaMovementX) {
                if (directionX.equals(Direction.RIGHT)) {
                    if (coordinateBallX + 1 <= coordinateBallXMax) {
                        coordinateBallX = coordinateBallX + 1;
                        ballLayoutParams.setMargins(coordinateBallX, coordinateBallY, 0, 0);
                        ball.setLayoutParams(ballLayoutParams);
                    }
                    if (coordinateBallX == coordinateBallXMax) {
                        vX = 0;
                        xMoveRight = false;
                    }
                }

                if (directionX.equals(Direction.LEFT)) {
                    if (coordinateBallX - 1 >= 0) {
                        coordinateBallX = coordinateBallX - 1;
                        ballLayoutParams.setMargins(coordinateBallX, coordinateBallY, 0, 0);
                        ball.setLayoutParams(ballLayoutParams);
                    }
                    if (coordinateBallX == 0) {
                        vX = 0;
                        xMoveLeft = false;
                    }
                }
            }

            if (idMovementY.get(i) < deltaMovementY) {
                if (directionY.equals(Direction.DOWN)) {
                    if (coordinateBallY + 1 <= coordinateBallYMax) {
                        coordinateBallY = coordinateBallY + 1;
                        ballLayoutParams.setMargins(coordinateBallX, coordinateBallY, 0, 0);
                        ball.setLayoutParams(ballLayoutParams);
                    }
                    if (coordinateBallY == coordinateBallYMax) {
                        vY = 0;
                        yMoveDown = false;
                    }
                }

                if (directionY.equals(Direction.UP)) {
                    if (coordinateBallY - 1 >= 0) {
                        coordinateBallY = coordinateBallY - 1;
                        ballLayoutParams.setMargins(coordinateBallX, coordinateBallY, 0, 0);
                        ball.setLayoutParams(ballLayoutParams);
                    }
                    if (coordinateBallY == 0) {
                        vY = 0;
                        yMoveUp = false;
                    }
                }
            }
        }
    }

    public static float convertPixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void randomize(List<Integer> arr) {
        // Creating a object for Random class
        Random r = new Random();

        // Start from the last element and swap one by one. We don't
        // need to run for the first element that's why i > 0
        for (int i = arr.size() - 1; i > 0; i--) {

            // Pick a random index from 0 to i
            int j = r.nextInt(i);

            // Swap arr[i] with the element at random index
            int temp = arr.get(i);
            arr.set(i, arr.get(j));
            arr.set(j, temp);
        }

        // Prints the random array
        // System.out.println(arr);
    }

    public void startTimers() {

        new CountDownTimer((startTimer + 1) * 1000, 1000) {

            @Override
            public void onTick(long millis) {
                tvTimer.setText(Integer.toString((int) (millis / 1000)));
                System.out.println(millis);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("");
                ballMoveBlock = false;
                sb.append("  [" + "\n");
                new CountDownTimer((gameTimer + 1) * 1000, 1000) {
                    int time = gameTimer;

                    @Override
                    public void onTick(long millis) {
                        tv.setText("Время: " + Integer.toString((int) (millis / 1000)));
                        time--;
                    }

                    @Override
                    public void onFinish() {
                        tv.setText("Конец" + "\n" + "измерения");
                        ballMoveBlock = true;
                        sb.setLength(sb.length() - 2);
                        sb.append('\n' + "  ]" + '\n' + "}");
                        btnSendLog.setEnabled(true);
                        btnSendLog.setAlpha((float) 1.0);
                    }
                }.start();
            }
        }.start();
    }

    public void sendLog() {

        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS, REQUEST);
        }

        int permissionStatusWrite = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionStatusRead = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        // you should access local path in scoped storage mode.
        File localStorage = getExternalFilesDir(null);
        if (localStorage == null) {
            return;
        }

        String storagePath = localStorage.getAbsolutePath();
        String rootPath = storagePath + "/test";
        String fileName = "/Log.json";

        File root = new File(rootPath);
        if (!root.mkdirs()) {
            Log.i("Test", "This path is already exist: " + root.getAbsolutePath());
        }

        File file = new File(rootPath + fileName);
        try {
            int permissionCheck = ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                if (!file.createNewFile()) {
                    Log.i("Test", "This file is already exist: " + file.getAbsolutePath());
                    try {
                        FileOutputStream flogs = new FileOutputStream(file);
                        flogs.write((writeParamJSON() + sb.toString()).getBytes());
                        flogs.flush();
                        flogs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (permissionStatusWrite == PackageManager.PERMISSION_GRANTED &&
                permissionStatusRead == PackageManager.PERMISSION_GRANTED) {

            Uri path = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);

            Date dateNowSending = new Date();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat formatForDateNow = new SimpleDateFormat("E dd.MM.yyyy 'в' HH:mm:ss");
            TEXT += formatForDateNow.format(dateNowSending);

            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("application/json");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{EMAIL});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, SUBJECT + formatForDateNow.format(dateNowSending));
            if (path != null) {
                emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            }

            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, TEXT);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            emailIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            this.startActivityForResult(Intent.createChooser(emailIntent,
                    "Sending email..."), 1);

        } else {
            System.out.println("permissionStatusWrite = " + permissionStatusWrite + "\n" +
                    "permissionStatusRead = " + permissionStatusRead);
        }

    }

    public String writeParamJSON() {

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String strDate = "\"" + dateFormat.format(date) + "\"";

        String param = new String("{" + "\n" +
                "  \"dateNowSending\": " + strDate + ",\n" +
                "  \"coeffRezist\": " + coeffRezist + ",\n" +
                "  \"gameTime\": " + gameTimer + ",\n" +
                "  \"dpHeight\": " + dpHeight + ",\n" +
                "  \"dpWidth\": " + dpWidth + ",\n" +
                "  \"ballSize\": " + ballSize + ",\n" +
                "  \"pointSize\": " + pointSize + ",\n" +
                "  \"coordinateBallYMax\": " + coordinateBallYMax + ",\n" +
                "  \"coordinateBallXMax\": " + coordinateBallXMax + ",\n" +
                "  \"pointLeftDefault\": " + pointLeftDefault + ",\n" +
                "  \"pointTopDefault\": " + pointTopDefault + ",\n" +
                "  \"ballLeftDefault\": " + ballLeftDefault + ",\n" +
                "  \"ballTopDefault\": " + ballTopDefault + ",\n" +
                "  \"Measurement\": " + '\n');

        return param;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}