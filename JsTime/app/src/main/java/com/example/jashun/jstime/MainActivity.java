package com.example.jashun.jstime;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private TextView txt_time;
    private Handler myHandler;
    private int countNum=0;
    private Calendar c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myHandler = new Handler();
        initView();

    }

    private void initView() {
        txt_time = (TextView) findViewById(R.id.txt_time);

        displayCurrentTime();
        myHandler.postDelayed(updateCurrentTime, 3000);
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacks(updateCurrentTime);

        super.onDestroy();
    }

    private Runnable updateCurrentTime = new Runnable() {
        public void run() {
            displayCurrentTime();
            myHandler.postDelayed(updateCurrentTime, 3000);
        }
    };
    private void displayCurrentTime(){
        c = Calendar.getInstance();
        String formatStr = "%02d";
        int seconds = c.get(Calendar.SECOND);
        String str_seconds=String.format(formatStr, seconds);
        int minutes = c.get(Calendar.MINUTE);
        String str_minutes=String.format(formatStr, minutes);
        int hour = c.get(Calendar.HOUR);
        String str_hour=String.format(formatStr, hour);
        String time = str_hour + ":" + str_minutes;
        txt_time.setText(time);
    }
}
