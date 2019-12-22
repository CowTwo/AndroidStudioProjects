package com.example.jsplayer_v03;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener{
    private MediaPlayer mediaPlayer;
    private double startTime;
    private double finalTime;
    private Button btn_play, btn_pause, btn_repeat, btn_cancel_repeat, btn_prev, btn_favor, btn_next, btn_forward;
    private TextView txt_title, txt_start_time, txt_end_time;
    private SeekBar seekbar;
    private LinkedList<Song> songList;
    private int index;
    private boolean isPause;
    private boolean isRepeat;
    public static int oneTimeOnly;
    private Handler myHandler;
    private int forwardTime;
    private boolean isCellPlay;
    private TelephonyManager phoneyMana;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer=null;
        startTime = 0;
        finalTime = 0;
        index = 0;
        isPause=false;
        isRepeat=false;
        oneTimeOnly = 0;
        forwardTime = 5000;
        myHandler = new Handler();
        forwardTime = 5000;

        Log.d("JsWan", "before initView");
        initView();
        Log.d("JsWan", "before getMusics");
        getMusics();
        Log.d("JsWan", "before prioritizeMusic");
        prioritizeMusic();

        //偵聽來電事件
        phoneyMana = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        phoneyMana.listen(new myPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            myHandler.removeCallbacks(UpdateSongTime);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        phoneyMana.listen(null, PhoneStateListener.LISTEN_NONE);

        super.onDestroy();
    }

    private void initView() {
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_pause = (Button) findViewById(R.id.btn_pause);
        btn_repeat = (Button) findViewById(R.id.btn_repeat);
        btn_cancel_repeat = (Button) findViewById(R.id.btn_cancel_repeat);
        btn_prev = (Button) findViewById(R.id.btn_prev);
        btn_next = (Button) findViewById(R.id.btn_next);
        btn_favor = (Button) findViewById(R.id.btn_favor);
        btn_forward=(Button) findViewById(R.id.btn_forward);

        btn_pause.setEnabled(false);
        btn_cancel_repeat.setEnabled(false);

        btn_play.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_repeat.setOnClickListener(this);
        btn_cancel_repeat.setOnClickListener(this);
        btn_prev.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_favor.setOnClickListener(this);
        btn_forward.setOnClickListener(this);

        txt_title = (TextView) findViewById(R.id.txt_title);
        txt_start_time=(TextView) findViewById(R.id.txt_start_time);
        txt_end_time=(TextView) findViewById(R.id.txt_end_time);

        seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setClickable(false);

    }

    /* 來電事件處理 */
    private class myPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING://來電時，要處理的動作
                    if (mediaPlayer!=null) {
                        if (mediaPlayer.isPlaying()) {

                            mediaPlayer.pause(); //音樂暫停
                            isCellPlay = true;//標記來電時暫停

                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE://掛斷電話時要處理的動作
                    if (isCellPlay) {
                        if (mediaPlayer!=null) {
                            isCellPlay = false;
                            mediaPlayer.start(); //音樂繼續播放
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                doPlay();
                break;
            case R.id.btn_pause:
                doPause();
                break;
            case R.id.btn_repeat:
                isRepeat=true;
                btn_repeat.setEnabled(false);
                btn_cancel_repeat.setEnabled(true);
                break;
            case R.id.btn_cancel_repeat:
                isRepeat=false;
                btn_repeat.setEnabled(true);
                btn_cancel_repeat.setEnabled(false);
                break;
            case R.id.btn_prev:
                doPrev();
                break;
            case R.id.btn_next:
                doNext();
                break;
            case R.id.btn_favor:
                doFavor();
                break;
            case R.id.btn_forward:
                doForward();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (isRepeat){
            doPause();
            doPlay();
        }
        else {
            doNext();
        }
    }

    private void doPause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPause=true;
            btn_play.setEnabled(true);
            btn_pause.setEnabled(false);
        }

    }

    private void doPlay() {
        if (songList == null || songList.size() == 0) {
            return;
        }
        btn_play.setEnabled(false);
        btn_pause.setEnabled(true);

        playing();
    }

    private void doForward() {
        int temp = (int) startTime;

        if ((temp + forwardTime) <= finalTime) {
            startTime = startTime + forwardTime;
            mediaPlayer.seekTo((int) startTime);
            Toast.makeText(getApplicationContext(), "You have Jumped forward 5 seconds", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();
        }

    }

    private void doNext() {
        if (songList == null || songList.size() == 0) {
            return;
        }

        if (index < songList.size() - 1) {
            index++;
        }
        else{
            index=0;
        }
        isPause=false;
        doPlay();
    }

    private void doPrev() {
        if (songList == null || songList.size() == 0) {
            return;
        }

        if (index > 0) {
            index--;
        }
        else{
            index=songList.size()-1;
        }
        isPause=false;
        doPlay();
    }

    private void doFavor(){
        if (songList == null || songList.size() == 0) {
            return;
        }
        index = 0;
        isPause=false;
        doPlay();
    }

    private void playing(){
        if (mediaPlayer != null && !isPause) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (mediaPlayer == null) {
            long id = songList.get(index).getId();
            Uri songUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

            mediaPlayer = MediaPlayer.create(this, songUri);
            mediaPlayer.setOnCompletionListener(this);
        }

        isPause=false;
        mediaPlayer.start();
        doDisplayProgress();
        txt_title.setText(songList.get(index).getTitle());
    }

    private void doDisplayProgress(){
        finalTime = mediaPlayer.getDuration();
        startTime = mediaPlayer.getCurrentPosition();

        if (oneTimeOnly == 0) {
            seekbar.setMax((int) finalTime);
            oneTimeOnly = 1;
        }
        txt_end_time.setText(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))
        );

        txt_start_time.setText(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) startTime)))
        );

        seekbar.setProgress((int) startTime);
        myHandler.postDelayed(UpdateSongTime, 100);

    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            txt_start_time.setText(String.format("%d min, %d sec",

                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            seekbar.setProgress((int) startTime);
            myHandler.postDelayed(this, 100);
        }
    };

    private void getMusics() {
        songList = new LinkedList<Song>();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            txt_title.setText("查詢錯誤");
            //Log.d("=======>", "查詢錯誤");
        } else if (!cursor.moveToFirst()) {
            txt_title.setText("沒有媒體檔");
            //Log.d("=======>", "沒有媒體檔");
        } else {
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM);
            do {
                long thisId = cursor.getLong(idColumn);
                String thisTitle = cursor.getString(titleColumn);
                String thisAlbum = cursor.getString(albumColumn);
                Log.d("=======>", "id: " + thisId + ", title: " + thisTitle);
                Song song = new Song();
                song.setId(thisId);
                song.setTitle(thisTitle);
                song.setAlbum(thisAlbum);

                songList.add(song);
            } while (cursor.moveToNext());
        }
        txt_title.setText("共有 " + songList.size() + " 首歌曲");
    }
    private void prioritizeMusic(){
        int totalSongCnt = songList.size();
        int currentPos = 0;
        int ii;

        String regularExpression = "(_JsPri)([0-9]+)(_)";
        Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);

        for (currentPos=0;currentPos<totalSongCnt;currentPos++){
            Matcher matcher = pattern.matcher(songList.get(currentPos).getTitle());
            int songPri = 0;
            if (matcher.find()){
                songPri = parseInt(matcher.group(2));
            }

            for (ii=0;ii<currentPos;ii++){
                Matcher matcher1 = pattern.matcher(songList.get(ii).getTitle());
                int tmpPri = 0;
                if (matcher1.find()){
                    tmpPri = parseInt(matcher1.group(2));
                }
                if (songPri>tmpPri){
                    Song song = songList.get(currentPos);
                    songList.remove(currentPos);
                    songList.add(ii, song);
                    break;
                }
            }
        }
    }
}

