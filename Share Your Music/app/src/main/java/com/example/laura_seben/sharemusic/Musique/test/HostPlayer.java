package com.example.laura_seben.sharemusic.Musique.test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.Connexion.FileTransferService;
import com.example.laura_seben.sharemusic.Connexion.Utils;
import com.example.laura_seben.sharemusic.Connexion.WifiDirectActivity;
import com.example.laura_seben.sharemusic.R;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HostPlayer extends MainActivity{
    ArrayList<String> addressList ;
    String addresstosend;
    int listtaille=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player);
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        addressList = new ArrayList<String>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();
        Intent ancien = getIntent();
        PORT = ancien.getIntExtra("PORT", 8988);
        Clienttype=1;
        new HostAsyncTask(this).execute();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

    }

    /**
     * Methode qui permet d'envoyer une musique à l'adresse indiqué
     * @param address : adresse du client qui va recevoir la musique
     */
    public void sendSong(String address){

        Uri uri = musicSrv.getUri();
        Log.d("Sending Song to ", "addresstosend"+address);

        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                address);

        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        this.startService(serviceIntent);
    }

    /**
     * Permet d'envoyer au client une action du type mettre pause ou mettre play
     * @param address : adresse du client
     * @param action : action à envoyer
     * @param order : ordre à envoyer, 0 pour pause, 1 pour play et 5 pour seek to
     */
    public void sendAction(String address, String action,int order){
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_ACTION);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                address);
        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        serviceIntent.putExtra("action", action);
        serviceIntent.putExtra("order", order);
        serviceIntent.putExtra("pos", getCurrentPosition());
        this.startService(serviceIntent);
    }

    @Override
    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.setAdressetosend(addressList);
        for(int i=0; i < listtaille; i++) {
            sendSong(addressList.get(i));
        }

        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);

    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
        for(int i=0; i<listtaille; i++) {
            sendAction( (addressList.get(i)), "pause", 0);
        }
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
        for(int i=0; i<listtaille; i++) {
            sendAction((addressList.get(i)),"seekto", 5);
        }
    }

    @Override
    public void start() {
        musicSrv.go();
        for(int i=0; i<listtaille; i++) {
            sendAction((addressList.get(i)),"play", 1);
        }
    }

    //Listener de réseau du HostPlayer

    public class HostAsyncTask extends AsyncTask<Void, Void, String> {

        private final Context context;
        //private final TextView statusText;

        /**
         * @param context
        //* @param statusText
         */
        public HostAsyncTask(Context context/*, View statusText*/) {
            this.context = context;
            //this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d("Network :", "Server: Socket opened");
                String address = "";
                int a=0;
                while (true) {


                    Socket client = serverSocket.accept();
                    address = client.getInetAddress().getHostAddress();

                    Log.d("Network :", "Server: connection done");
                    InputStream inputstream = client.getInputStream();
                    DataInputStream dis = new DataInputStream(inputstream);
                    Log.d("Network :", "Adresse =" + address);

                    int byte1 = dis.read();
                    if (byte1 == 1) {
                        int byte2= dis.read();
                        if (byte2 == 1) {
                            sendAction(address,"seekto", 5);
                            Log.d("Network :", "Sending seekto to :" + address);
                        }else{
                            Log.d("Network :","Enter the byte2==0 ok");
                            Hostadresse=address;
                            addresstosend=address;
                            Log.d("Network :","addresstosend="+ addresstosend);

                            if(!addressList.contains(address)) {
                                addressList.add(listtaille, address);
                                listtaille++;
                                for(int i=0;i<listtaille;i++){
                                    Log.d("Network :","Listaddress :"+i +" "+ addressList.get(i));
                                }
                                if(musicSrv!=null){
                                    musicSrv.setAdressetosend(addressList);
                                }
                            }

                            Log.d("Network :","Dernier élément ajouté:"+address);
                        }


                    } else if (byte1 == 0) {
                    } else {
                        a++;
                        if (a == 100000) {
                            break;
                        }
                    }
                }
                serverSocket.close();
                return "stop";

            } catch (IOException e) {
                Log.e("Network :", e.getMessage());
                return null;
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {


            }

        }


        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

        }

    }
}