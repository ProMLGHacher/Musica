package com.example.musica;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;


public class ConnectionEvent {


    private OnConnectionListener onConnectionListener;

    public void setOnConnectionListener(OnConnectionListener listener) {
        onConnectionListener = listener;
    }

    public void doEvent(Context context) {

        while (true) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting())
            {

            } else {
                onConnectionListener.onEvent();
                Toast.makeText(context, "Нет подключения к интернету!", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
}
