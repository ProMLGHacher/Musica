package com.example.musica;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


public class ConnectionEvent {


    private OnConnectionListener onConnectionListener;

    public void setOnConnectionListener(OnConnectionListener listener) {
        onConnectionListener = listener;
    }

    Thread myThread;

    public void doEvent(Context context) {

        myThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    ConnectivityManager cm =
                            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo netInfo = cm.getActiveNetworkInfo();
                    if (netInfo != null && netInfo.isConnectedOrConnecting())
                    {

                    } else {
                        destroyThread();
                        break;
                    }
                }
            }
        };

        myThread.start();

    }

    Thread thread;

    public void destroyThread() {
        myThread.interrupt();
        Looper.prepare();
        onConnectionListener.onEvent();
    }
}
