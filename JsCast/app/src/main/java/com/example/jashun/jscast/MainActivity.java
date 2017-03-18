package com.example.jashun.jscast;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    enum BtnPlayState{
        PLAY_KARAOKE,
        PLAY_FAMILY,
        PLAY_OTHER,
        STOP
    }

    private class MatchedResult{
        int matchedCharCnt=0;
        int matchedSongIdx=-1;
    }

    private Button btn_play_karaoke, btn_play_family,btn_play_other, btn_stop;
    private Button btn_type_normal, btn_type_karaoke;
    private Button btn_repeat, btn_cancel_repeat;
    private Button btn_next, btn_prev, btn_find_by_voice;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView txt_info;
    private boolean isRepeat;

    private File karaokeDir = new File("/sdcard/JsCast/Karaoke/");
    private File familyDir = new File("/sdcard/JsCast/Family/");
    private File otherDir = new File("/sdcard/JsCast/Other/");

    private File[] videoFiles;
    private LinkedList<Song> karaokeList, familyList, otherList;

    private VideoServer mVideoServer;

    private boolean isTypeKaraoke;
    private int index;
    private String m_SongDesc="";
    private BtnPlayState btnPlayState=BtnPlayState.STOP;

    private MenuItem mediaRouteMenuItem;
    private CastContext mCastContext=null;
    private CastSession mCastSession=null;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private int m_PreviousState = MediaStatus.PLAYER_STATE_IDLE;
    private boolean isCastConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setTitle("My Title");

        index = 0;
        isTypeKaraoke = false;
        isRepeat = false;

        initView();

        startDebugLog2File();

        karaokeList = new LinkedList<Song>();
        getVideoFiles(karaokeList, karaokeDir);
        prioritizeMusic(karaokeList);
        Log.d("JS_Tag", "onCreate: TotalKaraokeFileNum ="+String.valueOf(karaokeList.size()));

        familyList = new LinkedList<Song>();
        getVideoFiles(familyList, familyDir);
        prioritizeMusic(familyList);
        Log.d("JS_Tag", "onCreate: TotalFamilyFileNum ="+String.valueOf(familyList.size()));

        otherList = new LinkedList<Song>();
        getVideoFiles(otherList, otherDir);
        prioritizeMusic(otherList);
        Log.d("JS_Tag", "onCreate: TotalOtherFileNum ="+String.valueOf(otherList.size()));

        startVideoServer();

        //++  Google Cast Related
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        Log.d("JS_Tag", "mCastSession (onCreate) ="+String.valueOf(mCastSession));

        setupCastListener();
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        //--
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.option_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);
        Log.d("JS_Tag", "onCreateOptionsMenu");

        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        isCastConnected = false;
        if (mCastSession!=null){
            isCastConnected = true;
        }

        if (isCastConnected) {
            Log.d("JS_Tag", "menu.setGroupVisible(0,false);");
            //menu.findItem(R.id.media_route_menu_item).setEnabled(false);
            menu.setGroupVisible(0,false);
            this.setTitle("連線成功");
            showAllButtonsAfterHide();
        }
        else{
            Log.d("JS_Tag", "menu.setGroupVisible(0,true);");
            //menu.findItem(R.id.media_route_menu_item).setEnabled(true);
            menu.setGroupVisible(0,true);
            this.setTitle("XXX, 請連線... ==>>");
            hideAllButtons();
        }
        return true;
    }

    private void hideAllButtons(){
        btn_play_karaoke.setEnabled(false);
        btn_play_other.setEnabled(false);
        btn_play_family.setEnabled(false);
        btn_stop.setEnabled(false);
        btn_type_normal.setEnabled(false);
        btn_type_karaoke.setEnabled(false);
        btn_repeat.setEnabled(false);
        btn_cancel_repeat.setEnabled(false);
        btn_prev.setEnabled(false);
        btn_next.setEnabled(false);
        btn_find_by_voice.setEnabled(false);
    }
    private void showAllButtonsAfterHide(){
        btn_play_karaoke.setEnabled(true);
        btn_play_other.setEnabled(true);
        btn_play_family.setEnabled(true);
        btn_stop.setEnabled(false);
        if (isRepeat){
            btn_repeat.setEnabled(false);
            btn_cancel_repeat.setEnabled(true);
        } else{
            btn_repeat.setEnabled(true);
            btn_cancel_repeat.setEnabled(false);
        }
        if (isTypeKaraoke){
            btn_type_normal.setEnabled(true);
            btn_type_karaoke.setEnabled(false);
        }
        else{
            btn_type_normal.setEnabled(false);
            btn_type_karaoke.setEnabled(true);
        }
        btn_prev.setEnabled(true);
        btn_next.setEnabled(true);
        btn_find_by_voice.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        Log.d("JS_Tag", "onDestroy");
        doStop();
        mVideoServer.stop();
        super.onDestroy();
    }

    private void startDebugLog2File(){
        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder" );
            File logDirectory = new File( appDirectory + "/log" );
            File logFile = new File( logDirectory, "logcat_JsCast.txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Runtime.getRuntime().exec("rm "+logFile);
                Process process = Runtime.getRuntime().exec( "logcat -c");
                // 為了知道log發生的時間點，可以利用-v time這個參數
                // logcat -s TAG 印出特定TAG的訊息
                process = Runtime.getRuntime().exec( "logcat -f " + logFile + " -v time -s JS_Tag");
                //process = Runtime.getRuntime().exec( "logcat -f " + logFile + " *:S MyActivity:D MyActivity2:D");
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }

    private void startVideoServer(){
        //++ Start Video Server
        mVideoServer = new VideoServer("",
                320, 240, VideoServer.DEFAULT_SERVER_PORT);
        txt_info.setText("请在远程浏览器中输入:\n\n"+getLocalIpStr(this)+":"+VideoServer.DEFAULT_SERVER_PORT);
        try {
            mVideoServer.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            txt_info.setText(e.getMessage());
        }
        //--
    }

    private void initView() {
        btn_play_karaoke = (Button) findViewById(R.id.btn_play_karaoke);
        btn_play_other = (Button) findViewById(R.id.btn_play_other);
        btn_play_family = (Button) findViewById(R.id.btn_play_family);
        btn_type_normal = (Button) findViewById(R.id.btn_type_normal);
        btn_type_karaoke = (Button) findViewById(R.id.btn_type_karaoke);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_next = (Button) findViewById(R.id.btn_next);
        btn_prev = (Button) findViewById(R.id.btn_prev);
        btn_repeat = (Button) findViewById(R.id.btn_repeat);
        btn_cancel_repeat = (Button) findViewById(R.id.btn_cancel_repeat);
        btn_find_by_voice = (Button) findViewById(R.id.btn_find_by_voice);
        txt_info = (TextView) findViewById(R.id.txt_info);

        btn_play_karaoke.setOnClickListener(this);
        btn_play_other.setOnClickListener(this);
        btn_play_family.setOnClickListener(this);
        btn_type_normal.setOnClickListener(this);
        btn_type_karaoke.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_prev.setOnClickListener(this);
        btn_repeat.setOnClickListener(this);
        btn_cancel_repeat.setOnClickListener(this);
        btn_find_by_voice.setOnClickListener(this);

        btn_prev.setBackgroundColor(0xffffccff);
        btn_next.setBackgroundColor(0xffffccff);
        btn_find_by_voice.setBackgroundColor(0xffffccff);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play_karaoke:
                btnPlayState = BtnPlayState.PLAY_KARAOKE;
                doPlay();
                break;
            case R.id.btn_play_family:
                btnPlayState = BtnPlayState.PLAY_FAMILY;
                doPlay();
                break;
            case R.id.btn_play_other:
                btnPlayState = BtnPlayState.PLAY_OTHER;
                doPlay();
                break;
            case R.id.btn_type_normal:
                if (btnPlayState==BtnPlayState.STOP){
                    btnPlayState = BtnPlayState.PLAY_KARAOKE;
                }
                isTypeKaraoke = false;
                btn_type_karaoke.setEnabled(true);
                btn_type_normal.setEnabled(false);
                doPlay();
                break;
            case R.id.btn_type_karaoke:
                if (btnPlayState==BtnPlayState.STOP){
                    btnPlayState = BtnPlayState.PLAY_KARAOKE;
                }
                isTypeKaraoke = true;
                btn_type_karaoke.setEnabled(false);
                btn_type_normal.setEnabled(true);
                doPlay();
                break;
            case R.id.btn_stop:
                btnPlayState = BtnPlayState.STOP;
                doStop();
                break;
            case R.id.btn_next:
                if (btnPlayState==BtnPlayState.STOP){
                    btnPlayState = BtnPlayState.PLAY_KARAOKE;
                }
                doNext();
                break;
            case R.id.btn_prev:
                if (btnPlayState==BtnPlayState.STOP){
                    btnPlayState = BtnPlayState.PLAY_KARAOKE;
                }
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
            case R.id.btn_find_by_voice:
                // Disable WiFi to make voice recognition more stable by using google off-line recognition. Get rid of  possible unstable link issue.
                //turnOffWiFi();
                //btn_find_by_voice.setEnabled(false);
                promptSpeechInput();
                //myHandler.postDelayed(DebounceFindButton, 3000);
                break;
            default:
                break;
        }
    }

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
                    MatchedResult karaokeMatchedResult = findSongIdxByVoiceCmd(karaokeList, result.get(0));
                    MatchedResult otherMatchedResult = findSongIdxByVoiceCmd(otherList, result.get(0));
                    int matchedSongIdx=-1;
                    if (karaokeMatchedResult.matchedCharCnt >= otherMatchedResult.matchedCharCnt){
                        btnPlayState = BtnPlayState.PLAY_KARAOKE;
                        matchedSongIdx = karaokeMatchedResult.matchedSongIdx;
                    }
                    else{
                        btnPlayState = BtnPlayState.PLAY_OTHER;
                        matchedSongIdx = otherMatchedResult.matchedSongIdx;
                    }
                    if (matchedSongIdx!=-1){
                        index = matchedSongIdx;
                        doPlay();
                    }else{
                        txt_info.setText("無:"+result.get(0));
                    }
                }
                break;
            }

        }
    }
    private MatchedResult findSongIdxByVoiceCmd(LinkedList<Song> songList, String voiceCmd){
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
                    maxMatchedCnt=songNo;
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

        MatchedResult matchedResult= new MatchedResult();
        matchedResult.matchedCharCnt=maxMatchedCnt;
        matchedResult.matchedSongIdx=maxMatchedSongIdx;

        return matchedResult;
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

    private void doStop() {
        Log.d("JS_Tag", "doStop");
        btn_play_karaoke.setEnabled(true);
        btn_play_family.setEnabled(true);
        btn_play_other.setEnabled(true);
        btn_stop.setEnabled(false);

        if (mCastSession==null){
            return;
        }
        RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }

        //++ Workaround "remoteMediaClient.stop();" unstable issue by load empty media (By JaShun)
        //remoteMediaClient.stop();
        //Log.d("JS_Tag", "doStop, remoteMediaClient.stop(); remoteMediaClient="+String.valueOf(remoteMediaClient));
        remoteMediaClient.load(buildEmptyMediaInfo(), true, 0);
        Log.d("JS_Tag", "doStop, remoteMediaClient.load(buildEmptyMediaInfo(), true, 0); remoteMediaClient="+String.valueOf(remoteMediaClient));
        //--
    }

    private void doPlay(){
        LinkedList<Song> playList;
        String songFilePath=null;
        Log.d("JS_Tag", "doPlay");

        if (btnPlayState==BtnPlayState.PLAY_KARAOKE) {
            btn_play_karaoke.setEnabled(false);
            btn_play_family.setEnabled(true);
            btn_play_other.setEnabled(true);
            btn_stop.setEnabled(true);
            playList = karaokeList;
            if (playList == null || playList.size() == 0) {return;}
            if (index>=playList.size()){index = playList.size()-1;}
            if (isTypeKaraoke) {
                songFilePath = playList.get(index).getKtvFilePath();
            }
            else{
                songFilePath = playList.get(index).getNormalFilePath();
            }
        }
        else if (btnPlayState==BtnPlayState.PLAY_FAMILY){
            btn_play_family.setEnabled(false);
            btn_play_karaoke.setEnabled(true);
            btn_play_other.setEnabled(true);
            btn_stop.setEnabled(true);
            playList = familyList;
            if (playList == null || playList.size() == 0) {return;}
            if (index>=playList.size()){index = playList.size()-1;}
            songFilePath = playList.get(index).getNormalFilePath();
        }
        else if (btnPlayState==BtnPlayState.PLAY_OTHER){
            btn_play_family.setEnabled(true);
            btn_play_karaoke.setEnabled(true);
            btn_play_other.setEnabled(false);
            btn_stop.setEnabled(true);
            playList = otherList;
            if (playList == null || playList.size() == 0) {return;}
            if (index>=playList.size()){index = playList.size()-1;}
            songFilePath = playList.get(index).getKtvFilePath();
        }
        else{
            Log.d("JS_Tag", "doPlay: Invalid PlayState!!");
            return;
        }

        if (songFilePath != null) {
            int songNo = playList.get(index).getSongNo();
            if (songNo>0) {
                m_SongDesc = playList.get(index).getSongName() + "(" + String.valueOf(songNo) + ")";
            }else{
                m_SongDesc = playList.get(index).getSongName();
            }
            txt_info.setText(m_SongDesc);

            Log.d("JS_Tag", "doPlay, index="+String.valueOf(index));
            Log.d("JS_Tag", "doPlay, songFilePath="+songFilePath);
            mVideoServer.setVideoFilePath(songFilePath);
            loadRemoteMedia(/*mSeekbar.getProgress()*/ 0, true);
            //finish();
        }
        else{
            Log.d("JS_Tag", "doPlay: songFilePath="+"Invalid FilePath");
            txt_info.setText("Invalid FilePath");
        }

    }

    private LinkedList<Song> getActPlayList(){
        LinkedList<Song> playList = null;
        if (btnPlayState==BtnPlayState.PLAY_KARAOKE) {
            playList = karaokeList;
        }
        else if (btnPlayState==BtnPlayState.PLAY_FAMILY){
            playList = familyList;
        }
        else if (btnPlayState==BtnPlayState.PLAY_OTHER){
            playList = otherList;
        }
        return playList;
    }

    private void doPrev(){
        Log.d("JS_Tag", "doPrev");

        LinkedList<Song> playList = getActPlayList();

        if (playList == null || playList.size() == 0) {
            return;
        }

        if (index >0) {
            index--;
        }
        else{
            index=playList.size()-1;
        }

        doPlay();
    }

    private void doNext() {
        Log.d("JS_Tag", "doNext,  btnPlayState="+String.valueOf(btnPlayState));

        LinkedList<Song> playList = getActPlayList();

        if (playList == null || playList.size() == 0) {
            return;
        }

        if (index < playList.size() - 1) {
            index++;
        }
        else{
            index=0;
        }

        doPlay();
    }

    private void setupCastListener() {
        Log.d("JS_Tag", "setupCastListener");

        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                Log.d("JS_Tag", "onSessionEnded");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                Log.d("JS_Tag", "onSessionResumed");
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                Log.d("JS_Tag", "onSessionResumeFailed");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                Log.d("JS_Tag", "onSessionStarted");
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                Log.d("JS_Tag", "onSessionStartFailed");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {
                Log.d("JS_Tag", "onSessionStarting");
            }

            @Override
            public void onSessionEnding(CastSession session) {
                Log.d("JS_Tag", "onSessionEnding");
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
                Log.d("JS_Tag", "onSessionResuming");
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
                Log.d("JS_Tag", "onSessionSuspended");
            }

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                Log.d("JS_Tag", "mCastSession (onApplicationConnected)="+String.valueOf(mCastSession));
                //loadRemoteMedia(/*mSeekbar.getProgress()*/ 0, true);
                //finish();

                invalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
                mCastSession = null;
                Log.d("JS_Tag", "mCastSession (onApplicationDisconnected)="+String.valueOf(mCastSession));

                doStop();

                invalidateOptionsMenu();
            }
        };
    }

    private void loadRemoteMedia(int position, boolean autoPlay) {
        if (mCastSession==null){
            return;
        }
        RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mCastSession.getRemoteMediaClient().getMediaStatus();
                if (mediaStatus == null) return;
                int currentState = mediaStatus.getPlayerState();
                Log.d("JS_Tag", "preState="+String.valueOf(m_PreviousState)+", currentState="+String.valueOf(currentState));
                if ((m_PreviousState!=MediaStatus.PLAYER_STATE_IDLE)&&(currentState==MediaStatus.PLAYER_STATE_IDLE)){
                    int idleReason = mediaStatus.getIdleReason();
                    Log.d("JS_Tag", "idleStateReason="+String.valueOf(idleReason));
                    if ((btnPlayState!=BtnPlayState.STOP) && (idleReason==MediaStatus.IDLE_REASON_FINISHED)) {
                        if (isRepeat){
                            doPlay();
                        }
                        else {
                            doNext();
                        }
                    }
                }
                m_PreviousState = currentState;
            }

            @Override
            public void onMetadataUpdated() {
            }

            @Override
            public void onQueueStatusUpdated() {
            }

            @Override
            public void onPreloadStatusUpdated() {
            }

            @Override
            public void onSendingRemoteMediaRequest() {
            }
        });

        Log.d("JS_Tag", "loadRemoteMedia, remoteMediaClient="+String.valueOf(remoteMediaClient));
        remoteMediaClient.load(buildMediaInfo(), autoPlay, position);
        //remoteMediaClient.queueLoad(buildQueueItems(),0, MediaStatus.REPEAT_MODE_REPEAT_OFF, null);
        Toast.makeText(getApplicationContext(), "position="+String.valueOf(position), Toast.LENGTH_SHORT).show();
    }

    private MediaInfo buildMediaInfo() {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        //movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
        movieMetadata.putString(MediaMetadata.KEY_TITLE, m_SongDesc);
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(0))));
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))));

        //Log.d("JS_Tag", "mSelectedMedia.getUrl()="+mSelectedMedia.getUrl());
        return new MediaInfo.Builder("http://"+getLocalIpStr(this)+":"+VideoServer.DEFAULT_SERVER_PORT+"/dummy.mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                //.setStreamDuration(100 * 1000) // In ms
                .build();
    }
    private MediaInfo buildEmptyMediaInfo() {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        //movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "Empty");
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(0))));
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))));

        //Log.d("JS_Tag", "mSelectedMedia.getUrl()="+mSelectedMedia.getUrl());

        //++ Use Invalid Port to stop Remote Media (workaround for remoteMediaClient.stop unstable issue)
        return new MediaInfo.Builder("http://"+getLocalIpStr(this))
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                //.setStreamDuration(100 * 1000) // In ms
                .build();
        //--
    }

    private void getVideoFiles(LinkedList<Song> songList, File videoDir) {
        if (videoDir.exists() && videoDir.isDirectory()) {
            videoFiles = videoDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".mp4");
                }
            });

            if (videoFiles.length==0){
                return;
            }

            //songList = new LinkedList<Song>();
            for (int i = 0; i < videoFiles.length; i++) {
                String songName= getSongNameFromFilename(videoFiles[i].getName());
                String songPath = videoFiles[i].getAbsolutePath();

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
    private void prioritizeMusic(LinkedList<Song> songList){
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
    public static String getLocalIpStr(Context context) {
        WifiManager wifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return intToIpAddr(wifiInfo.getIpAddress());
    }
    private static String intToIpAddr(int ip) {
        return (ip & 0xff) + "." + ((ip>>8)&0xff) + "." + ((ip>>16)&0xff) + "." + ((ip>>24)&0xff);
    }
}
