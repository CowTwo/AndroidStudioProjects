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

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private int index;
    private Button btn_pre_sentence, btn_next_sentence, btn_say;
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
        txt_input_sentence.setText(songList.get(index).getSongName());

    }

    private void initView() {
        btn_pre_sentence = (Button) findViewById(R.id.btn_pre_sentence);
        btn_next_sentence = (Button) findViewById(R.id.btn_next_sentence);
        btn_say = (Button) findViewById(R.id.btn_say);
        txt_input_sentence = (TextView) findViewById(R.id.txt_input_sentence);
        txt_recognized_sentence = (TextView) findViewById(R.id.txt_recognized_sentence);

        btn_pre_sentence.setOnClickListener(this);
        btn_next_sentence.setOnClickListener(this);
        btn_say.setOnClickListener(this);

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
            default:
                break;
        }
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
                    logToFile(songList.get(index).getSongName()+",    ");
                    logToFile(result.get(0)+"\n");
                }
                break;
            }

        }
    }
    private Boolean logToFile(String fcontent){
        try {

            String fpath = "/sdcard/JsKtv/VoiceCmdLog.txt";

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

    // Pseudo code
    // Procedure / API Plan:
    //		1. VoiceCmd Log file  (SongName, Speech2TextLog)
    //		2. Create songList from VoiceCmd log file
    //		3. Extract Speech2TextLog one by one
    //		4. Pass Speech2TxtLog string to API findSongByVoiceString(string)to match the most possible
    //			SongName in songList
    //
    // Matching Algorithm:
    //		For each_song in songList{
    //			For each_char in Speech2TextLog{
    //				if (char also in songName){
    //					matchCnt++;
    //					replace that char in songName w/ unused char;
    //				}
    //			}
    //			Calculate matchCnt/charLenOfSongName
    //			Record max matchRate and corresponding songIndex
    //		}

    private void chkAlgoByLogFile(){
        BufferedReader br = null;

        try {
            String fpath = "/sdcard/JsKtv/VoiceCmdLog.txt";

            br = new BufferedReader(new FileReader(fpath));
            String line = "";

            while ((line = br.readLine()) != null) {
                
            }

        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
