package com.example.jashun.jsktv;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener{
    private Button btn_play, btn_stop, btn_repeat, btn_cancel_repeat, btn_prev, btn_favor, btn_next, btn_type_normal, btn_type_karaoke;
    private Button btn_find_by_voice;
    private TextView txt_title;
    private File ktvDir = new File("/sdcard/JsKtv/");
    private File[] ktvFiles;
    private VideoView myVideoView;
    private LinkedList<Song> songList;
    private int index;
    private boolean isRepeat;
    private boolean isTypeKaraoke;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private Handler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        index = 0;
        isRepeat = false;
        isTypeKaraoke = true;
        myHandler = new Handler();

        initView();
        getKtvFiles();
        prioritizeMusic();
        printSongName2File();
    }

    @Override
    protected void onDestroy() {
        doStop();
        myHandler.removeCallbacks(DebounceFindButton);

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
        btn_find_by_voice = (Button) findViewById(R.id.btn_find_by_voice);

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
        btn_find_by_voice.setOnClickListener(this);

        txt_title = (TextView) findViewById(R.id.txt_title);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                doPlay();
                break;
            case R.id.btn_stop:
                turnOnWiFi();
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
            case R.id.btn_find_by_voice:
                // Disable WiFi to make voice recognition more stable by using google off-line recognition. Get rid of  possible unstable link issue.
                turnOffWiFi();
                btn_find_by_voice.setEnabled(false);
                promptSpeechInput();
                myHandler.postDelayed(DebounceFindButton, 3000);
                break;
            default:
                break;
        }
    }

    private Runnable DebounceFindButton = new Runnable() {
        public void run() {
            btn_find_by_voice.setEnabled(true);
        }
    };

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
            int songNo = songList.get(index).getSongNo();
            if (songNo>0) {
                txt_title.setText(songList.get(index).getSongName() + "(" + String.valueOf(songNo) + ")");
            }else{
                txt_title.setText(songList.get(index).getSongName());
            }
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

    private String getSongNameFromFilename(String fileName){
        String regularExpression = "(.*)(_Type)(.*)";
        Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
        String songName="Empty";
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()){
            songName = matcher.group(1);
        }
        return songName;
    }
    private int getSongNoFromKtvFilename(String ktvFileName){
        String regularExpression = "(_JsNo)([0-9]+)(_)";
        Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
        int songNo = 0;
        Matcher matcher = pattern.matcher(ktvFileName);
        if (matcher.find()){
            songNo = parseInt(matcher.group(2));
        }
        return songNo;
    }
    private int getSongPriFromKtvFilename(String ktvFileName){
        String regularExpression = "(_JsPri)([0-9]+)(_)";
        Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
        int songPri = 0;
        Matcher matcher = pattern.matcher(ktvFileName);
        if (matcher.find()){
            songPri = parseInt(matcher.group(2));
        }
        return songPri;
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
            for (int i = 0; i < ktvFiles.length; i++) {
                String songName= getSongNameFromFilename(ktvFiles[i].getName());
                String songPath = ktvFiles[i].getAbsolutePath();

                boolean isSongNameExisted = false;
                Song oldSong=null, song=null;
                for (int j=0;j<songList.size();j++){
                    oldSong = songList.get(j);
                    if (oldSong.getSongName().equals(songName)){
                        isSongNameExisted = true;
                        break;
                    }
                }
                if (isSongNameExisted){
                    song = oldSong;
                } else {
                    song = new Song();
                    song.setSongName(songName);
                    song.setSongNo(0);
                    song.setSongPri(0);
                    songList.add(song);
                }
                if (songPath.contains("_TypeKaraoke_")){
                    song.setKtvFilePath(songPath);
                    song.setSongNo(getSongNoFromKtvFilename(songPath));
                    song.setSongPri(getSongPriFromKtvFilename(songPath));
                }
                else{
                    song.setNormalFilePath(songPath);
                }
            }
        }
    }
    private void prioritizeMusic(){
        int totalSongCnt = songList.size();
        int currentPos = 0;
        int ii;

        for (currentPos=0;currentPos<totalSongCnt;currentPos++){
            int songPri = songList.get(currentPos).getSongPri();
            for (ii=0;ii<currentPos;ii++){
                int tmpPri = songList.get(ii).getSongPri();
                if (songPri>tmpPri){
                    Song song = songList.get(currentPos);
                    songList.remove(currentPos);
                    songList.add(ii, song);
                    break;
                }
            }
        }
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        doStop();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "say input sentence");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Speech not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    int matchedSongIdx = findSongIdxByVoiceCmd(result.get(0));
                    if (matchedSongIdx!=-1){
                        index = matchedSongIdx;
                        doPlay();
                    }else{
                        txt_title.setText("無:"+result.get(0));
                    }
                }
                break;
            }

        }
    }

    private int findSongIdxByVoiceCmd(String voiceCmd){
        // 去除空白
        voiceCmd = voiceCmd.replaceAll("\\s+", "");

        // Algo
        int maxMatchedCnt = 0;
        int maxMatchedSongIdx = -1;

        if (isNumeric(voiceCmd)){
            int songNo = parseInt(voiceCmd);
            for (int j = 0; j < songList.size(); j++) {
                if (songList.get(j).getSongNo()==songNo){
                    maxMatchedSongIdx = j;
                }
            }
        } else {
            for (int j = 0; j < songList.size(); j++) {
                int matchedCharCnt = 0;
                String songName = songList.get(j).getSongName();
                int voiceCmdLen = voiceCmd.length();

                String tmpSongName = songName;
                for (int k = 0; k <voiceCmdLen; k++) {
                    if (tmpSongName.indexOf(voiceCmd.charAt(k)) != -1) {
                        matchedCharCnt += 1;
                        tmpSongName = tmpSongName.replaceFirst(Character.toString(voiceCmd.charAt(k)), "");
                    }
                }
                if (matchedCharCnt > maxMatchedCnt) {
                    maxMatchedCnt = matchedCharCnt;
                    maxMatchedSongIdx = j;
                }
                if (maxMatchedCnt==voiceCmdLen){
                    break;
                }
            }
        }
        return maxMatchedSongIdx;
    }
    public boolean isNumeric(String str)
    {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() )
        {
            return false;
        }
        return true;
    }

    private void printSongName2File(){
        try {
            File file = new File("/sdcard/JsKtv/SongNameList.txt");

            // If file does not exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), false); //the true will append the new data
            BufferedWriter bw = new BufferedWriter(fw);

            int maxSongNo=0;
            for (int j = 0; j < songList.size(); j++) {
                int songNo = songList.get(j).getSongNo();
                if (songNo>0) {
                    bw.write(songList.get(j).getSongName() + "(" + String.valueOf(songNo) + ")"+"\n");
                }else{
                    bw.write(songList.get(j).getSongName()+"\n");
                }
                if (songNo>maxSongNo){
                    maxSongNo = songNo;
                }
            }
            bw.write("maxSongNo (numeric ID for Song) is"+String.valueOf(maxSongNo)+"\n");
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void turnOffWiFi(){
        WifiManager wifi =(WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(wifi.isWifiEnabled()){
            wifi.setWifiEnabled(false);
        }
    }
    private void turnOnWiFi(){
        WifiManager wifi =(WifiManager)getSystemService(Context.WIFI_SERVICE);
        if( ! wifi.isWifiEnabled()){
            wifi.setWifiEnabled(true);
        }
    }
}

