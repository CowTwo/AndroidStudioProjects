package com.example.jashun.jscast;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
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

    enum BtnActionState{
        PLAY_KARAOKE,
        PLAY_FAMILY,
        STOP
    }

    private Button btn_play_karaoke, btn_stop;
    private TextView txt_info;

    private File ktvDir = new File("/sdcard/JsCast/Karaoke/");
    private File[] ktvFiles;
    private LinkedList<Song> songList;

    private VideoServer mVideoServer;

    private boolean isTypeKaraoke;
    private int index;
    private String m_SongDesc="";
    private BtnActionState btnActionState=BtnActionState.STOP;

    private MenuItem mediaRouteMenuItem;
    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private int m_PreviousState = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        index = 0;
        isTypeKaraoke = false;

        initView();
        getKtvFiles();
        prioritizeMusic();

        startVideoServer();

        //++  Google Cast Related
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();

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
        btn_stop = (Button) findViewById(R.id.btn_stop);

        //btn_repeat = (Button) findViewById(R.id.btn_repeat);
        //btn_cancel_repeat = (Button) findViewById(R.id.btn_cancel_repeat);
        //btn_prev = (Button) findViewById(R.id.btn_prev);
        //btn_next = (Button) findViewById(R.id.btn_next);
        //btn_type_normal = (Button) findViewById(R.id.btn_type_normal);
        //btn_type_karaoke = (Button) findViewById(R.id.btn_type_karaoke);
        //btn_find_by_voice = (Button) findViewById(R.id.btn_find_by_voice);

        btn_stop.setEnabled(false);
        //btn_cancel_repeat.setEnabled(false);
        //btn_type_karaoke.setEnabled(false);

        btn_play_karaoke.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        //btn_repeat.setOnClickListener(this);
        //btn_cancel_repeat.setOnClickListener(this);
        //btn_prev.setOnClickListener(this);
        //btn_next.setOnClickListener(this);
        //btn_type_normal.setOnClickListener(this);
        //btn_type_karaoke.setOnClickListener(this);
        //btn_favor.setOnClickListener(this);
        //btn_find_by_voice.setOnClickListener(this);

        txt_info = (TextView) findViewById(R.id.txt_info);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play_karaoke:
                doPlayKaraoke();
                break;
            case R.id.btn_stop:
                doStop();
                break;
            default:
                break;
        }
    }
    private void doStop() {
        btn_play_karaoke.setEnabled(true);
        btn_stop.setEnabled(false);
        btnActionState = BtnActionState.STOP;

        if (mCastSession==null){
            return;
        }
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        remoteMediaClient.pause();


    }
    private void doPlayKaraoke() {
        String songFilePath;
        if (songList == null || songList.size() == 0) {
            return;
        }
        btn_play_karaoke.setEnabled(false);
        btn_stop.setEnabled(true);
        btnActionState = BtnActionState.PLAY_KARAOKE;

        if (isTypeKaraoke) {
            songFilePath = songList.get(index).getKtvFilePath();
        }
        else{
            songFilePath = songList.get(index).getNormalFilePath();
        }
        if (songFilePath != null) {
            int songNo = songList.get(index).getSongNo();
            if (songNo>0) {
                m_SongDesc = songList.get(index).getSongName() + "(" + String.valueOf(songNo) + ")";
            }else{
                m_SongDesc = songList.get(index).getSongName();
            }
            txt_info.setText(m_SongDesc);

            mVideoServer.setVideoFilePath(songFilePath);
            loadRemoteMedia(/*mSeekbar.getProgress()*/ 0, true);
            //finish();
        }
        else{
            txt_info.setText("Invalid FilePath");
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
        doPlayKaraoke();
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
            public void onSessionStarting(CastSession session) {}

            @Override
            public void onSessionEnding(CastSession session) {}

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {}

            @Override
            public void onSessionSuspended(CastSession session, int reason) {}

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                //loadRemoteMedia(/*mSeekbar.getProgress()*/ 0, true);
                //finish();
                if (mCastSession==null){
                    return;
                }
                final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
                if (remoteMediaClient == null) {
                    return;
                }
                remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = remoteMediaClient.getMediaStatus();
                        if (mediaStatus == null) return;
                        int currentState = mediaStatus.getPlayerState();
                        Log.d("JS_Tag", "preState="+String.valueOf(m_PreviousState)+", currentState="+String.valueOf(currentState));
                        if ((m_PreviousState==MediaStatus.PLAYER_STATE_PLAYING)&&(currentState==MediaStatus.PLAYER_STATE_IDLE)){
                            doNext();
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

                invalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
                invalidateOptionsMenu();
            }
        };
    }

    private void loadRemoteMedia(int position, boolean autoPlay) {
        if (mCastSession == null) {
            return;
        }
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
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
    public static String getLocalIpStr(Context context) {
        WifiManager wifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return intToIpAddr(wifiInfo.getIpAddress());
    }
    private static String intToIpAddr(int ip) {
        return (ip & 0xff) + "." + ((ip>>8)&0xff) + "." + ((ip>>16)&0xff) + "." + ((ip>>24)&0xff);
    }
}
