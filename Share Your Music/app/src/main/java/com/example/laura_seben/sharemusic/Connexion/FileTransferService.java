package com.example.laura_seben.sharemusic.Connexion;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Service qui se lance lorsque l'on veut communiquer entre 2 appareils connectés entre eux
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 15000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_PORT = "go_port";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String ACTION_SEND_ACTION = "com.example.android.wifidirect.SEND_ACTION";
    public static final String ACTION_SEND_READY="com.example.android.wifidirect.SEND_READY";
    public String ACTION = "go_action";// peut prendre play,pause,seekto

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * Ouverture d'un socket pour envoyer des informations entre host et clients
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            // Système d'entête
            /*  Entête flux pour client
                1 -> fichier
                0 -> action
                    1 -> play/pause
                    0 -> seekto
                Entête flux pour host
                1 -> ready
                    1 -> ready to play music (la musique a bien été reçue)
                    0 -> ready to play (première connexion)
            * */

            try {
                Log.d("Network :", "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d("Network :", "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d("Network :", e.toString());
                }
                stream.write(1);
                //entête fichier

                DeviceDetailFragment.copyFile(is, stream);

                Log.d("Network :", "Client: Data written");
            } catch (IOException e) {
                Log.e("Network :", e.getMessage());

            } finally{
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        else if (intent.getAction().equals(ACTION_SEND_ACTION)){
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            int pos = intent.getExtras().getInt("pos");
            try {
                Log.d("Network :", "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d("Network :", "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(stream);

                out.write(0);
                //entête action
                ACTION=intent.getStringExtra("action");
                int order=intent.getIntExtra("order",5);
                Log.d("Network :", "order="+order);
                Log.d("Network :", "ACTION="+ACTION);

                if ( order==1 || order==0){
                    out.write(1);
                    //entête play/pause
                    if ( order==1){
                        out.write(1);
                        Log.d("Network :", "sending play");
                    }
                    else{
                        out.write(0);
                        Log.d("Network :", "sending pause");
                    }
                    //out.writeInt(pos);

                }
                else{
                    out.write(0);
                    Log.d("Network :", "sending seekto");
                    //entête seekto

                }
                out.writeInt(pos);

                Log.d("Network :", "Client: Action sent");
            } catch (IOException e) {
                Log.e("Network :", e.getMessage());

            } finally{
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }else if(intent.getAction().equals(ACTION_SEND_READY)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            int ready = intent.getIntExtra("ready", 0);
            // 1 pour ready musique, 0 pour ready lecteur
            // On recommence tant qu'on a pas réussi à l'envoyer
            while (true) {
                try {
                    Log.d(WifiDirectActivity.TAG, "Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), 0);

                    Log.d(WifiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                    OutputStream stream = socket.getOutputStream();
                    stream.write(1);
                    stream.write(ready);
                    Log.d("Client: Ready Sent", "" + ready);
                    break;

                } catch (IOException e) {
                    Log.e("Client", "Client failed to send ready :" + e.getMessage());

                }
            }
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                        Log.d("Client :", "socket closed");
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }




    }
}

