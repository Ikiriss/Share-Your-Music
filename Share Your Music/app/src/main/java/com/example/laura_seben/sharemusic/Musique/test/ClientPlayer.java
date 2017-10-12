package com.example.laura_seben.sharemusic.Musique.test;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.Connexion.FileTransferService;
import com.example.laura_seben.sharemusic.Connexion.Utils;
import com.example.laura_seben.sharemusic.Connexion.WifiDirectActivity;
import com.example.laura_seben.sharemusic.R;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ClientPlayer extends MainActivity{





    private String pathmusic;
    public static int PORT = 8988;
    private static String address = "";
    private static String Hostaddress="";
    private String file;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player);
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        // On enlève la partie getSongList puisqu'on lui enlève la capacité de choisir
        //getSongList();


        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();
        Clienttype=0;
        Intent intent = getIntent();
        PORT = intent.getIntExtra("PORT",8988);
        Hostaddress = intent.getStringExtra("Hostadresse");
        Log.d("Hostadress=",Hostaddress);
        Log.d("Bonjour","bonjour");


        /**
         * On envoie un message pour prévenir l'host qu'on est prêt, et donc on lui donne notre IP
         */

        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_READY);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                Hostaddress);
        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        serviceIntent.putExtra("ready", 0);
        this.startService(serviceIntent);

        new ClientAsyncTask(this).execute();

    }

        @Override
        protected void onDestroy() {
            stopService(playIntent);
            musicSrv=null;
        boolean deleted = deleteFile(file);
        Log.d("File :", "Last file was : " + pathmusic);
        Log.d("File :", "Last file deleted " + deleted);
        super.onDestroy();

    }


    /**
     * Listener réseau du Client
     */


    public class ClientAsyncTask extends AsyncTask<Void, Void, String> {

        private final Context context;

        /**
         * @param context
         */
        public ClientAsyncTask(Context context/*, View statusText*/) {
            this.context = context;
            //this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d("Network :", "Server: Socket opened");

                int a = 0;
                int passage = 0;
                //String file = "";

                while (true) {

                    Socket client = serverSocket.accept();
                    Log.d("Network :", "Server: connection done");
                    InputStream inputstream = client.getInputStream();
                    DataInputStream dis = new DataInputStream(inputstream);
                    address=client.getInetAddress().getHostAddress();

                    int byte1 = dis.read();
                    if (byte1 == 1) {
                        //entête fichier
                        if (passage>=1){
                            //note on ne peut plus supprimer en ExternalStorage depuis 4.4
                            /*File file = new File(pathmusic);
                            boolean deleted = file.delete();*/
                            boolean deleted = deleteFile(file);
                            Log.d("File :", "Previous file was : "+ pathmusic);
                            Log.d("File :", "Previous file deleted " + deleted);
                        }

                        /* Ancienne méthode pour copier les fichiers
                        final File f = new File(Environment.getExternalStorageDirectory() + "/"
                                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                                + ".mp3");

                        File dirs = new File(f.getParent());
                        if (!dirs.exists())
                            dirs.mkdirs();
                        f.createNewFile();*/
                        long time = System.currentTimeMillis();
                        file = "music"+time+".mp3";
                        FileOutputStream fos = openFileOutput(file,context.MODE_PRIVATE);
                        Log.d("File :", "copying files " + getFilesDir()+"/"+file);

                        copyFile(dis, fos);
                        /* Avant, on copiait dans : new FileOutputStream(f)*/

                        pathmusic = getFilesDir()+"/"+file;
                        /* Ancien path : "file://" + f.getAbsolutePath();*/
                        Thread t1 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                musicSrv.playSong(pathmusic);
                                playbackPaused=false;
                                Intent intent = new Intent(context,FileTransferService.class);
                                intent.setAction(FileTransferService.ACTION_SEND_READY);
                                intent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, address);
                                intent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
                                intent.putExtra("ready", 1);
                                context.startService(intent);

                            }
                        });

                        t1.start();
                        Song songtoshow = getSonginformation(pathmusic);
                        musicSrv.setSongtoshow(songtoshow);
                        passage++;

                    } else if (byte1==0){
                        //entête action
                        int byte2 = dis.read();
                        Log.d("Network :", "byte2="+byte2);
                        if (byte2==1){
                            //entête play/pause
                            int byte3 = dis.read();
                            Log.d("Network :", "byte3="+byte3);
                            if(byte3==1){
                                //entête play
                                int pos = dis.readInt();
                                seekTo(pos);
                                start();
                                Log.d("Network :", "play");

                            }
                            else{
                                //entête pause
                                int pos = dis.readInt();
                                seekTo(pos);
                                pause();
                                Log.d("Network :", "pause");
                            }
                        }
                        else{
                            //entête seekto
                            int pos = dis.readInt();
                            seekTo(pos);
                            start();
                            Log.d("Network :", "seekto"+pos);
                        }
                        //mettre à jour le play/pos
                    }
                    else {
                        a++;
                        //wait(1);
                        if (a==1000000000){
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


        @Override
        protected void onPostExecute(String result) {
            if (result != null) {


            }

        }


        @Override
        protected void onPreExecute() {

        }

    }
    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d("Network :", e.toString());
            return false;
        }
        return true;
    }
}

