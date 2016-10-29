package com.example.jashun.jsplayer_v01;

import android.app.Activity;
import android.app.Activity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity implements View.OnClickListener, MediaPlayer.OnCompletionListener {
    private Button b1, b2, bP, b4, bR, bR_C;
    private ImageView iv;
    private MediaPlayer mediaPlayer;
    private double startTime = 0;
    private double finalTime = 0;
    private Handler myHandler = new Handler();
    ;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar seekbar;
    private TextView tx1, tx2, tx3, tx4;
    private LinkedList<Song> songList;
    private boolean bRepeatEnable = false;
    int index = 0;
    private boolean isPause;

    public static int oneTimeOnly = 0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b1 = (Button) findViewById(R.id.button);
        b2 = (Button) findViewById(R.id.button2);
        bP = (Button) findViewById(R.id.button3);
        b4 = (Button) findViewById(R.id.button4);
        bR = (Button) findViewById(R.id.buttonR);
        bR_C = (Button) findViewById(R.id.buttonR_C);
        iv = (ImageView) findViewById(R.id.imageView);

        tx1 = (TextView) findViewById(R.id.textView2);
        tx2 = (TextView) findViewById(R.id.textView3);
        tx3 = (TextView) findViewById(R.id.textView4);
        tx4 = (TextView) findViewById(R.id.textView5);

        if (true) {
            bP.setText("Play");
            getMusics();
            /*
                        long id = songList.get(index).getId();
                        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        mediaPlayer = MediaPlayer.create(this, songUri);
                        tx3.setText(songList.get(index).getTitle());
                        */
        } else {
            ArrayList<HashMap<String, String>> songList = getPlayList("/sdcard/Music");
            if (songList != null) {
                for (int i = 0; i < songList.size(); i++) {
                    String fileName = songList.get(i).get("file_name");
                    String filePath = songList.get(i).get("file_path");
                    //here you will get list of file name and file path that present in your device
                    //log.e("file details "," name ="+fileName +" path = "+filePath);
                    tx4.setText(" name =" + fileName + " path = " + filePath);
                }
            }
            mediaPlayer = MediaPlayer.create(this, R.raw.song);
            tx3.setText("Song.mp3");
        }

        seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setClickable(false);
        b2.setEnabled(false);
        bR_C.setEnabled(false);

        bP.setOnClickListener(this);

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JsdoPause();
            }
        });

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp = (int) startTime;

                if ((temp + forwardTime) <= finalTime) {
                    startTime = startTime + forwardTime;
                    mediaPlayer.seekTo((int) startTime);
                    Toast.makeText(getApplicationContext(), "You have Jumped forward 5 seconds", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();
                }
            }
        });

        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp = (int) startTime;

                if ((temp - backwardTime) > 0) {
                    startTime = startTime - backwardTime;
                    mediaPlayer.seekTo((int) startTime);
                    Toast.makeText(getApplicationContext(), "You have Jumped backward 5 seconds", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot jump backward 5 seconds", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bRepeatEnable) {
                    bRepeatEnable = true;
                    mediaPlayer.setLooping(true);
                    bR.setEnabled(false);
                    bR_C.setEnabled(true);
                }
            }
        });

        bR_C.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bRepeatEnable) {
                    bRepeatEnable = false;
                    mediaPlayer.setLooping(false);
                    bR.setEnabled(true);
                    bR_C.setEnabled(false);
                }
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button3:
                // JsdoPlay();
                doPlay();
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        doNext();
        //JsdoPause();
        //JsdoPlay();
    }

    private void doStop() {
        if (mediaPlayer != null) {
            isPause = false;
            mediaPlayer.stop();
            bP.setText("Play");
        }

    }

    private void doPlay() {
        if (songList == null || songList.size() == 0) {
            return;
        }

        if (bP.getText().toString().equals("Play")) {
            playing();
            bP.setText("Pause");
        }else{
            isPause = true;
            mediaPlayer.pause();
            bP.setText("Play");
        }

    }

    private void doNext() {
        if (songList == null || songList.size() == 0) {
            return;
        }

        if (index < songList.size() - 1) {
            index++;
            isPause = false;
            playing();
            bP.setText("Pause");
        }
    }

    private void doPrev() {
        if (songList == null || songList.size() == 0) {
            return;
        }

        if (index > 0) {
            index--;
            isPause = false;
            playing();
            bP.setText("Pause");
        }
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

        mediaPlayer.start();
        doDisplayProgress();
        tx3.setText(songList.get(index).getTitle());
    }


    private void JsdoPause() {
        Toast.makeText(getApplicationContext(), "Pausing sound", Toast.LENGTH_SHORT).show();
        mediaPlayer.pause();
        b2.setEnabled(false);
        bP.setEnabled(true);
    }

    private void JsdoPlay() {
        long id = songList.get(index).getId();
        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        mediaPlayer = MediaPlayer.create(this, songUri);
        tx3.setText(songList.get(index).getTitle());

        mediaPlayer.setOnCompletionListener(this);

        Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
        mediaPlayer.start();

        doDisplayProgress();

        b2.setEnabled(true);
        bP.setEnabled(false);

    }

    private void doDisplayProgress(){
        finalTime = mediaPlayer.getDuration();
        startTime = mediaPlayer.getCurrentPosition();

        if (oneTimeOnly == 0) {
            seekbar.setMax((int) finalTime);
            oneTimeOnly = 1;
        }
        tx2.setText(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))
        );

        tx1.setText(String.format("%d min, %d sec",
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
            tx1.setText(String.format("%d min, %d sec",

                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            seekbar.setProgress((int) startTime);
            myHandler.postDelayed(this, 100);
        }
    };

    private ArrayList<HashMap<String, String>> getPlayList(String rootPath) {
        ArrayList<HashMap<String, String>> fileList = new ArrayList<>();


        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles(); //here you will get NPE if directory doesn't contains  any file,handle it like this.
            for (File file : files) {
                if (file.isDirectory()) {
                    if (getPlayList(file.getAbsolutePath()) != null) {
                        fileList.addAll(getPlayList(file.getAbsolutePath()));
                    } else {
                        break;
                    }
                } else if (file.getName().endsWith(".mp3")) {
                    HashMap<String, String> song = new HashMap<>();
                    song.put("file_path", file.getAbsolutePath());
                    song.put("file_name", file.getName());
                    fileList.add(song);
                }
            }
            return fileList;
        } catch (Exception e) {
            return null;
        }
    }

    private void getMusics() {
        songList = new LinkedList<Song>();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            tx4.setText("查詢錯誤");
            //Log.d("=======>", "查詢錯誤");
        } else if (!cursor.moveToFirst()) {
            tx4.setText("沒有媒體檔");
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
        tx4.setText("共有 " + songList.size() + " 首歌曲");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_main, menu);  JSWAN
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
   /* JSWAN
      if (id == R.id.action_settings) {
         return true;
      }
      */
        return super.onOptionsItemSelected(item);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
