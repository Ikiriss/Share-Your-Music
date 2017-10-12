package com.example.laura_seben.sharemusic.Musique.test;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.laura_seben.sharemusic.Menu.Menu1;
import com.example.laura_seben.sharemusic.Musique.test.MusicService.MusicBinder;
import com.example.laura_seben.sharemusic.R;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    protected ArrayList<Song> songList;
    protected ListView songView;
    protected MusicController controller;
    protected MusicService musicSrv;
    protected Intent playIntent;
    protected boolean musicBound=false;
    public boolean paused=false, playbackPaused=false;
    protected int Clienttype=5;
    protected String Hostadresse;
    protected ArrayList<String> addressList;
    protected int PORT;
    private Menu menu;
    protected TextView t_current_song = null;
    protected Song previoussong= null;

    /**
     * Permet de récuperer les informations de la musique en cours de lecture
     * @param path : Le chemin de la musique en cours de lecture
     * @return : renvoie les informations de la musique en cours
     */

    public Song getSonginformation(String path){
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = Uri.parse(path);
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if(musicCursor!=null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            long thisId = musicCursor.getLong(idColumn);
            String thisTitle = musicCursor.getString(titleColumn);
            String thisArtist = musicCursor.getString(artistColumn);
            Song song = new Song(thisId,thisTitle,thisArtist);
            return(song);
        }else{
            Song song = new Song(0,"bug","bug");
            return(song);
        }
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player);
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();



        t_current_song = (TextView)    findViewById(R.id.t_current_song);
        Typeface ttype = Typeface.createFromAsset(getAssets(), "fonts/AppleGaramond.ttf");
        t_current_song.setTypeface(ttype);
        new TextViewRefresh(this).execute();

    }
    //connect to the service
    protected ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            //changer le service selon le lecteur
            if(Clienttype==1){
                //host
                musicSrv.setClienttypeToHost();
                //musicSrv.setAdressetosend(Hostadresse);
                musicSrv.setPORT(PORT);
            }else if (Clienttype==0){
                //client
                musicSrv.setClienttypeToClient();
            }

            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    /**
     * Permet de récuperer et afficher la liste de musique disponible sur le téléphone
     */

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    /**
     * Permet de mettre en lecture la musique sélectionnée
     * @param view
     */
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused) {
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    /**
     * Permet de lancer la vue de l'obet menu (barre d'action)
     * @param menu : Objet menu
     * @return
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Permet de réaliser une action en fonction du clique sur la barre d'action
     * @param item
     * @return : renvoie l'item selectionné
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_settings:
                stopService(playIntent);
                musicSrv=null;
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.action_shuffle_off:
                if (!musicSrv.getShuffle()){
                    musicSrv.setShuffle();
                    menu.getItem(1).setIcon(R.drawable.ic_shuffle_blue);
                    break;
                }
                else{
                    musicSrv.setShuffle();
                    menu.getItem(1).setIcon(R.drawable.ic_shuffle_black);
                    break;
                }
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onPause(){
        super.onPause();
        paused=true;


    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }



    public void setController(){
        //set the controller up
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    public class TextViewRefresh extends AsyncTask<Void, Void, String> {

        private final Context context;

        //private final TextView statusText;

        /**
         * @param context
        //* @param statusText
         */
        public TextViewRefresh(Context context /*, View statusText*/) {
            this.context = context;

            //this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            int a=0;
            Log.d("TextView", "Thread created");
            if(musicSrv!=null){
                Log.d("Text View", "musicSrv wasn't null !");
                Song songtoshow = musicSrv.getSongtoshow();
                if(songtoshow!=previoussong) {
                    String result = songtoshow.getTitle() + "-" + songtoshow.getArtist();
                    previoussong=songtoshow;
                    return (result);
                }
                else{
                    return(null);
                }
            }else{
                return(null);
            }




        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {

            if (result != null) {
                t_current_song.setText(result);
                Log.d("TextView", "Text set :" + result);

            }else{
                Log.d("TextView", "result null");

            }
            /*try {
                wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            new TextViewRefresh(context).execute();

        }


        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

        }

    }

    /**
     * Passe à la musique suivante
     */
    protected void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    /**
     * Passe à la musique précédente
     */
    protected void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    /**
     * Met le lecteur en pause
     */
    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
        /*Intent intent = new Intent(this,Menu1.class);
        startActivity(intent);*/
        //Envoyer le pause au client
    }

    /**
     * Permet de mettre le curseur de la seekbar à la position voulue
     * @param pos : position voulue
     */
    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
        //Envoyer la pos au client
    }

    /**
     * Permet d'envoyer le message "ready"
     */
    @Override
    public void start() {
        musicSrv.go();
        //ENvoyer la go au client
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    /**
     * Permet d'obtenir la position du curseur musique (quelle musique on a choisit)
     * @return : renvoie la position du curseur musique
     */
    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }



    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }
    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

}
