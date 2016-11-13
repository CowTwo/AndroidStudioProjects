package com.example.jashun.jsktv;

import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener{
    private Button btn_play, btn_stop, btn_repeat, btn_cancel_repeat, btn_prev, btn_favor, btn_next, btn_type_normal, btn_type_karaoke;
    private TextView txt_title;
    private File ktvDir = new File("/sdcard/JsKtv/");
    private File[] ktvFiles;
    VideoView myVideoView;
    private LinkedList<Song> songList;
    private int index;
    private boolean isRepeat;
    private boolean isTypeKaraoke;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        index = 0;
        isRepeat = false;
        isTypeKaraoke = true;

        initView();
        getKtvFiles();
        prioritizeMusic();
    }

    @Override
    protected void onDestroy() {
        doStop();

        super.onDestroy();
    }

    private void initView() {
        myVideoView = (VideoView) findViewById(R.id.myvideoview);
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_stop = (Button) findViewById(R.id.btn_stop);

        btn_repeat = (Button) findViewById(R.id.btn_repeat);
        btn_cancel_repeat = (Button) findViewById(R.id.btn_cancel_repeat);
        btn_prev = (Button) findViewById(R.id.btn_prev);
        btn_next = (Button) findViewById(R.id.btn_next);
        btn_type_normal = (Button) findViewById(R.id.btn_type_normal);
        btn_type_karaoke = (Button) findViewById(R.id.btn_type_karaoke);
        btn_favor = (Button) findViewById(R.id.btn_favor);

        btn_stop.setEnabled(false);
        btn_cancel_repeat.setEnabled(false);
        btn_type_karaoke.setEnabled(false);

        btn_play.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_repeat.setOnClickListener(this);
        btn_cancel_repeat.setOnClickListener(this);
        btn_prev.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_type_normal.setOnClickListener(this);
        btn_type_karaoke.setOnClickListener(this);
        btn_favor.setOnClickListener(this);

        txt_title = (TextView) findViewById(R.id.txt_title);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                doPlay();
                break;
            case R.id.btn_stop:
                doStop();
                break;
            case R.id.btn_next:
                doNext();
                break;
            case R.id.btn_prev:
                doPrev();
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
            case R.id.btn_type_normal:
                doTypeNormal();
                break;
            case R.id.btn_type_karaoke:
                doTypeKaraoke();
                break;
            case R.id.btn_favor:
                doFavor();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (isRepeat){
            doPlay();
        }
        else {
            doNext();
        }
    }


    private void doPlay() {
        String songFilePath;
        if (songList == null || songList.size() == 0) {
            return;
        }
        btn_play.setEnabled(false);
        btn_stop.setEnabled(true);

        if (myVideoView!=null) {
            if (myVideoView.isPlaying()) {
                myVideoView.stopPlayback();
                myVideoView.clearFocus();
            }
        }

        if (isTypeKaraoke) {
            songFilePath = songList.get(index).getKtvFilePath();
        }
        else{
            songFilePath = songList.get(index).getNormalFilePath();
        }
        if (songFilePath != null) {
            myVideoView.setVideoPath(songFilePath);
            //myVideoView.setMediaController(new MediaController(this));  // Display the controls in the VideoView
            myVideoView.setMediaController(null); // Hide the controls in the VideoView
            myVideoView.setOnCompletionListener(this);
            myVideoView.requestFocus();
            myVideoView.start();
            txt_title.setText(songList.get(index).getSongName());
        }
        else{
            txt_title.setText("Invalid FilePath");
        }
    }

    private void doStop() {
        if (myVideoView!=null) {
            if (myVideoView.isPlaying()) {
                myVideoView.stopPlayback();
                myVideoView.clearFocus();
            }
        }
        btn_play.setEnabled(true);
        btn_stop.setEnabled(false);

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
        doPlay();
    }
    private void doTypeNormal(){
        isTypeKaraoke = false;
        btn_type_normal.setEnabled(false);
        btn_type_karaoke.setEnabled(true);
        doPlay();
    }
    private void doTypeKaraoke(){
        isTypeKaraoke = true;
        btn_type_normal.setEnabled(true);
        btn_type_karaoke.setEnabled(false);
        doPlay();
    }
    private void doFavor(){
        if (songList == null || songList.size() == 0) {
            return;
        }
        index = 0;
        doPlay();
    }


    private void getKtvFiles() {
        if (ktvDir.exists() && ktvDir.isDirectory()) {
            ktvFiles = ktvDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".mp4");
                }
            });

            if (ktvFiles.length==0){
                return;
            }

            songList = new LinkedList<Song>();
            String regularExpression = "(.*)(_Type)(.*)";
            Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
            for (int i = 0; i < ktvFiles.length; i++) {

                String songName="Empty";
                Matcher matcher = pattern.matcher(ktvFiles[i].getName());
                if (matcher.find()){
                    songName = matcher.group(1);
                }

                String songPath = ktvFiles[i].getAbsolutePath();

                boolean isSongNameExisted = false;
                for (int j=0;j<songList.size();j++){
                    Song oldSong = songList.get(j);
                    if (oldSong.getSongName().equals(songName)){
                        isSongNameExisted = true;

                        if (songPath.contains("_TypeKaraoke_")){
                            oldSong.setKtvFilePath(songPath);
                        }
                        else{
                            oldSong.setNormalFilePath(songPath);
                        }

                        break;
                    }
                }
                if (!isSongNameExisted) {
                    Song song = new Song();

                    song.setSongName(songName);
                    if (songPath.contains("_TypeKaraoke_")){
                        song.setKtvFilePath(songPath);
                    }
                    else{
                        song.setNormalFilePath(songPath);
                    }
                    songList.add(song);
                }
            }
        }
        return;
    }
    private void prioritizeMusic(){
        int totalSongCnt = songList.size();
        int currentPos = 0;
        int ii;

        String regularExpression = "(_JsPri)([0-9]+)(_)";
        Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);

        for (currentPos=0;currentPos<totalSongCnt;currentPos++){
            if (songList.get(currentPos).getKtvFilePath()==null){
                continue;
            }

            Matcher matcher = pattern.matcher(songList.get(currentPos).getKtvFilePath());
            int songPri = 0;
            if (matcher.find()){
                songPri = parseInt(matcher.group(2));
            }

            for (ii=0;ii<currentPos;ii++){
                if (songList.get(ii).getKtvFilePath()==null){
                    continue;
                }
                Matcher matcher1 = pattern.matcher(songList.get(ii).getKtvFilePath());
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

