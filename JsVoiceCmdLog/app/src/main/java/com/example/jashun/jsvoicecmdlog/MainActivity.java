package com.example.jashun.jsvoicecmdlog;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private int index;
    private Button btn_pre_sentence, btn_next_sentence, btn_say, btn_chk_algo;
    private TextView txt_recognized_sentence, txt_input_sentence;
    private File ktvDir = new File("/sdcard/JsKtv/");
    private File[] ktvFiles;
    private LinkedList<Song> songList;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        index = 0;
        initView();
        getKtvFiles();
        prioritizeMusic();
        txt_input_sentence.setText(songList.get(index).getSongName());

    }

    private void initView() {
        btn_pre_sentence = (Button) findViewById(R.id.btn_pre_sentence);
        btn_next_sentence = (Button) findViewById(R.id.btn_next_sentence);
        btn_say = (Button) findViewById(R.id.btn_say);
        btn_chk_algo = (Button) findViewById(R.id.btn_chk_algo);
        txt_input_sentence = (TextView) findViewById(R.id.txt_input_sentence);
        txt_recognized_sentence = (TextView) findViewById(R.id.txt_recognized_sentence);

        btn_pre_sentence.setOnClickListener(this);
        btn_next_sentence.setOnClickListener(this);
        btn_say.setOnClickListener(this);
        btn_chk_algo.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pre_sentence:
                if (index > 0) {
                    index--;
                }
                else{
                    index=songList.size()-1;
                }
                txt_input_sentence.setText(songList.get(index).getSongName());
                break;
            case R.id.btn_next_sentence:
                if (index < songList.size() - 1) {
                    index++;
                }
                else{
                    index=0;
                }
                txt_input_sentence.setText(songList.get(index).getSongName());
                break;
            case R.id.btn_say:
                promptSpeechInput();
                break;
            case R.id.btn_chk_algo:
                chkAlgoByLogFile();
                txt_input_sentence.setText("Algo Done");
                break;
            default:
                break;
        }
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
                    txt_recognized_sentence.setText(result.get(0));
                    String fpath = "/sdcard/JsKtv/VoiceCmdLog.txt";
                    logToFile(fpath, songList.get(index).getSongName()+",    ");
                    logToFile(fpath, result.get(0)+"\n");
                }
                break;
            }

        }
    }
    private Boolean logToFile(String fpath, String fcontent){
        try {
            File file = new File(fpath);

            // If file does not exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true); //the true will append the new data
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(fcontent);
            bw.close();

            //Log.d("Suceess","Sucess");
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void chkAlgoByLogFile(){
        try {
            String fpath = "/sdcard/JsKtv/VoiceCmdLog.txt";
            String line = "";

            BufferedReader br = new BufferedReader(new FileReader(fpath));
            while ((line = br.readLine()) != null) {
                String regularExpression = "(.*)(,)(.*)";
                Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
                String targetSongName, voiceCmd;
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()){
                    targetSongName = matcher.group(1);
                    voiceCmd = matcher.group(3);

                    int maxMatchedSongIdx = findSongIdxByVoiceCmd(voiceCmd);

                    String resultfpath =  "/sdcard/JsKtv/ChkAlgoByLog.txt";
                    if (maxMatchedSongIdx!=-1){
                        logToFile(resultfpath, targetSongName+",    "+voiceCmd+",    "+
                                songList.get(maxMatchedSongIdx).getSongName()+"\n");
                    }
                    else{
                        logToFile(resultfpath, targetSongName+",    "+voiceCmd+",    "+
                                "NOT FOUND"+"\n");
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();

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
}

