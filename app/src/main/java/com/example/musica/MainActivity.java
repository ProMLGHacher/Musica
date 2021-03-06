package com.example.musica;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    ArrayList<String> previousTracksArrayList = new ArrayList<>();

    static String coverURL;

    MediaPlayer mPlayer;

    Random random = new Random();

    MyAdapter adapter = new MyAdapter(MainActivity.this);
    RecyclerView previewRecyclerView;

    FloatingActionButton playButton;
    SeekBar seekBar;
    Button nextTrack;
    Button previousTrack;
    ImageView like;
    ImageView disLike;
    ImageView reply;
    TextView trackName;
    TextView artistName;

    Thread myThread;

    FirebaseStorage storage;
    FirebaseDatabase firebaseDatabase;

    int musicPosition;
    int playButtonPos = 0;
    int musicPosForPlaying = 0;
    boolean getReady = false;
    boolean isLike = false;
    boolean isDisLike = false;
    String lastMusicName;

    public static int rvPos;


    MusicInfo musicInfo;


    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        ConnectionEvent connectionEvent = new ConnectionEvent();
//
//        connectionEvent.setOnConnectionListener(new OnConnectionListener() {
//            @Override
//            public void onEvent() {
//                makeToastNotInternetConnection();
//            }
//        });
//
//        connectionEvent.doEvent(this);

        firebaseDatabase = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        trackName = findViewById(R.id.trackName);
        artistName = findViewById(R.id.artistName);
        reply = findViewById(R.id.reply);
        disLike = findViewById(R.id.disLike);
        like = findViewById(R.id.like);
        nextTrack = findViewById(R.id.nextTrack);
        previousTrack = findViewById(R.id.previousTrack);
        playButton = findViewById(R.id.playButton);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        previewRecyclerView = findViewById(R.id.coverRV);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        previewRecyclerView.setLayoutManager(linearLayoutManager);

        adapter = new MyAdapter(MainActivity.this);
        previewRecyclerView.setAdapter(adapter);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(previewRecyclerView);

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        DatabaseReference lastFileNameFromFirebase = firebaseDatabase.getReference("lastMusicName");
        lastFileNameFromFirebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                lastMusicName = dataSnapshot.getValue().toString();

                previousTracksArrayList.add(lastMusicName);
                musicPosForPlaying = previousTracksArrayList.size()-1;

                System.out.println(previousTracksArrayList);
                System.out.println(musicPosForPlaying);

                firebaseDatabase.getReference("tracksInfo").child(lastMusicName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        musicInfo = dataSnapshot.getValue(MusicInfo.class);

                        String setDataSourceURL = musicInfo.getMusicURL();

                        coverURL = musicInfo.getCoverURL();
                        adapter.notifyDataSetChanged();
                        previewRecyclerView.scrollToPosition(rvPos);

                        trackName.setText(musicInfo.getMusicName());
                        artistName.setText(musicInfo.getArtist());

                        try {
                            mPlayer.setDataSource(setDataSourceURL);
                            mPlayer.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                getReady = true;
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (!mPlayer.isLooping()) {
                    nextTrack();
                }
            }
        });

        nextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextTrack();
            }
        });

        previousTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousTrack();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLike) {
                    takeAwayLike();
                } else {
                    setLike();
                }
            }
        });

        disLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDisLike) {
                    takeAwayDisLike();
                } else {
                    setDisLike();
                }
            }
        });

        reply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer.isLooping()) {
                    mPlayer.setLooping(false);
                    reply.setImageResource(R.drawable.replay_disable);
                } else {
                    mPlayer.setLooping(true);
                    reply.setImageResource(R.drawable.replay_enable);
                }
            }
        });



    }

    private void setDisLike() {
        disLike.setImageResource(R.drawable.dislike_enable);
        isDisLike = true;
    }

    private void takeAwayDisLike() {
        disLike.setImageResource(R.drawable.dislike_disble);
        isDisLike = false;
    }


    public void setLike() {
        like.setImageResource(R.drawable.like_enable);
        isLike = true;
    }

    public void takeAwayLike() {
        like.setImageResource(R.drawable.like_disble);
        isLike = false;
    }




    int randomTrackCount;
    int count;
    boolean getWhile;
    int childrenCount;
    public void nextTrack() {

        Log.e("FIRST", previousTracksArrayList.toString());
        Log.e ("FIRST", String.valueOf(musicPosForPlaying));

        DatabaseReference nextTrackDataBase = firebaseDatabase.getReference("tracksInfo");

        nextTrackDataBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childrenCount = (int)snapshot.getChildrenCount();


                if (childrenCount == previousTracksArrayList.size()) {
                    Toast.makeText(MainActivity.this, "?? ?????????????????? ?????????? ???? ?????????????? ??????????????????????", Toast.LENGTH_LONG).show();
                } else {
                    if (musicPosForPlaying != previousTracksArrayList.size() - 1) {
                        if (previousTracksArrayList.size() > 1) {

                            musicPosForPlaying += 1;

                            String fileNamesGet = previousTracksArrayList.get(musicPosForPlaying);
                            firebaseDatabase.getReference("lastMusicName").setValue(fileNamesGet);

                            firebaseDatabase.getReference("tracksInfo").child(fileNamesGet).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    musicInfo = dataSnapshot.getValue(MusicInfo.class);

                                    coverURL = musicInfo.getCoverURL();
                                    adapter.notifyDataSetChanged();
                                    previewRecyclerView.scrollToPosition(rvPos);

                                    try {
                                        stop();
                                        mPlayer.reset();

                                        mPlayer.setDataSource(musicInfo.getMusicURL());

                                        trackName.setText(musicInfo.getMusicName());
                                        artistName.setText(musicInfo.getArtist());

                                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            @Override
                                            public void onPrepared(MediaPlayer mp) {
                                                play();
                                            }
                                        });

                                        mPlayer.prepareAsync();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    Log.e("SECOND", previousTracksArrayList.toString());
                                    Log.e("SECOND", String.valueOf(musicPosForPlaying));

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    //CODE
                                }
                            });

                        }
                    } else {
                        firebaseDatabase.getReference("tracksInfo").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                randomTrackCount = 0;
                                count = 0;
                                getWhile = true;

                                while (getWhile) {

                                    count = 0;
                                    randomTrackCount = random.nextInt((int) dataSnapshot.getChildrenCount());

                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        if (count != randomTrackCount) {
                                            count += 1;
                                        } else {
                                            MusicInfo musicInfoInWhile;
                                            musicInfoInWhile = child.getValue(MusicInfo.class);
                                            String musicNameInWhile = musicInfoInWhile.getMusicName() + "_" + musicInfoInWhile.getArtist();

                                            if (!previousTracksArrayList.contains(musicNameInWhile)) {
                                                getWhile = false;

                                                previousTracksArrayList.add(musicNameInWhile);

                                                musicPosForPlaying = previousTracksArrayList.size() - 1;

                                                musicInfo = child.getValue(MusicInfo.class);

                                                String setDataSourceURL = musicInfo.getMusicURL();

                                                coverURL = musicInfo.getCoverURL();
                                                adapter.notifyDataSetChanged();
                                                previewRecyclerView.scrollToPosition(rvPos);

                                                firebaseDatabase.getReference("lastMusicName").setValue(musicInfo.getMusicName() + "_" + musicInfo.getArtist());

                                                try {
                                                    stop();
                                                    mPlayer.reset();

                                                    trackName.setText(musicInfo.getMusicName());
                                                    artistName.setText(musicInfo.getArtist());

                                                    mPlayer.setDataSource(setDataSourceURL);

                                                    mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                        @Override
                                                        public void onPrepared(MediaPlayer mp) {
                                                            play();
                                                        }
                                                    });

                                                    mPlayer.prepareAsync();

                                                    break;

                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                            } else {
                                                break;
                                            }
                                        }
                                    }
                                }

                                Log.e("SECOND2", previousTracksArrayList.toString());
                                Log.e("SECOND2", String.valueOf(musicPosForPlaying));

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void previousTrack() {

        if (musicPosForPlaying <= 0) {
            //code
        } else if (previousTracksArrayList.size() <= 1) {
            //code
        } else {

            Log.e("LAST", previousTracksArrayList.toString());
            Log.e ("LAST", String.valueOf(musicPosForPlaying));

            musicPosForPlaying -= 1;


            String fileNamesGet = previousTracksArrayList.get(musicPosForPlaying);
            firebaseDatabase.getReference("lastMusicName").setValue(fileNamesGet);

            firebaseDatabase.getReference("tracksInfo").child(fileNamesGet).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    musicInfo = dataSnapshot.getValue(MusicInfo.class);

                    coverURL = musicInfo.getCoverURL();
                    adapter.notifyDataSetChanged();
                    previewRecyclerView.scrollToPosition(rvPos);

                    try {
                        stop();
                        mPlayer.reset();

                        mPlayer.setDataSource(musicInfo.getMusicURL());

                        trackName.setText(musicInfo.getMusicName());
                        artistName.setText(musicInfo.getArtist());

                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                play();
                            }
                        });

                        mPlayer.prepareAsync();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    public void stop() {
        mPlayer.stop();
        mPlayer.seekTo(0);
        playButtonPos = 0;

    }

    public void play() {
        try {
            if (getReady) {
                if (playButtonPos == 0) {
                    mPlayer.start();
                    seekBar.setMax(mPlayer.getDuration() / 1000);
                    startThread();
                    playButtonPos = 1;
                } else if (playButtonPos == 1) {
                    pause();
                    playButtonPos = 0;
                }
            }
        } catch (Exception ignored) { }
    }

    public void pause() {
        mPlayer.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer.isPlaying()) {
            stop();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //code
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //code
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String seekTo = seekBar.getProgress() + "000";
        mPlayer.seekTo(Integer.parseInt(seekTo));
    }

    public void seekBarSetProgress() {
        seekBar.setProgress(musicPosition);
    }

    public void startThread() {
        myThread = new Thread() {
            @Override
            public void run() {
                while(mPlayer.isPlaying()) {
                    try{
                        Thread.sleep(1000);
                        musicPosition = mPlayer.getCurrentPosition() / 1000;
                        seekBarSetProgress();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        };

        myThread.start();

    }

    public static boolean isOnline(Context context)
    {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return true;
        }
        return false;
    }

    public void makeToastNotInternetConnection() {
        Toast.makeText(MainActivity.this, "?????? ?????????????????????? ?? ??????????????????!", Toast.LENGTH_LONG).show();
    }


}