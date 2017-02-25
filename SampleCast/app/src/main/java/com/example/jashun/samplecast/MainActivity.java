package com.example.jashun.samplecast;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private MenuItem mediaRouteMenuItem;
    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private Button btn_play, btn_stop;
    private TextView txt_info;
    private VideoServer mVideoServer;
    //private String mVideoPath=Environment.getExternalStorageDirectory().getPath();
    private String mVideoPath="/storage/sdcard1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        mVideoServer = new VideoServer(mVideoPath+"/dummy.mp4",
                320, 240, VideoServer.DEFAULT_SERVER_PORT);
        txt_info.setText("请在远程浏览器中输入:\n\n"+getLocalIpStr(this)+":"+VideoServer.DEFAULT_SERVER_PORT+"External SD path:"+Environment.getExternalStorageDirectory().getPath());
        try {
            mVideoServer.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            txt_info.setText(e.getMessage());
        }

        setupCastListener();
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);

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
    protected void onDestroy() {
        mVideoServer.stop();
        super.onDestroy();
    }

    private void initView() {
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(this);
        txt_info = (TextView)findViewById(R.id.txt_info);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                mVideoServer.setVideoFilePath(mVideoPath+"/錦江秀玉.mp4");
                loadRemoteMedia(/*mSeekbar.getProgress()*/ 0, true);
                //finish();

                break;
            case R.id.btn_stop:
                final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
                if (remoteMediaClient == null) {
                    return;
                }
                remoteMediaClient.pause();
                break;
            default:
                break;
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
        remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                //Intent intent = new Intent(LocalPlayerActivity.this, ExpandedControlsActivity.class);
                //startActivity(intent);
                //remoteMediaClient.removeListener(this);
                MediaStatus mediaStatus = remoteMediaClient.getMediaStatus();
                if (mediaStatus == null) return;
                int playerState = mediaStatus.getPlayerState();
                Log.d("JS_Tag", "playerState="+String.valueOf(playerState));
                //int idleReason = mediaStatus.getIdleReason();
                //Log.d("JS_Tag", "idleReason="+String.valueOf(playerState));
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
        //remoteMediaClient.load(buildMediaInfo(), autoPlay, position);
        remoteMediaClient.queueLoad(buildQueueItems(),0, MediaStatus.REPEAT_MODE_REPEAT_OFF, null);
        Toast.makeText(getApplicationContext(), "position="+String.valueOf(position), Toast.LENGTH_SHORT).show();
    }
    private MediaQueueItem[] buildQueueItems(){
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        //movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "JS Test");
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(0))));
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))));

        //Log.d("JS_Tag", "mSelectedMedia.getUrl()="+mSelectedMedia.getUrl());
        MediaInfo mediaInfo = new MediaInfo.Builder("http://"+getLocalIpStr(this)+":"+VideoServer.DEFAULT_SERVER_PORT+"/dummy.mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                //.setStreamDuration(100 * 1000) // In ms
                .build();

        // Create a queue with single queue item
        MediaQueueItem mediaQueueItem = new MediaQueueItem.Builder(mediaInfo).setAutoplay(true).setPreloadTime(20).build();

        MediaQueueItem[] queueItems = new MediaQueueItem[] { mediaQueueItem,  mediaQueueItem};
        return queueItems;
    }

    private MediaInfo buildMediaInfo() {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        //movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "JS Test");
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

}
