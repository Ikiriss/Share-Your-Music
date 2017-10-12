package com.example.laura_seben.sharemusic.Musique.test;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.IBinder;

import android.app.Service;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.Connexion.FileTransferService;
import com.example.laura_seben.sharemusic.R;


/**
 * Classe qui contient l'objet mediaplayer qui va permettre la lecture de la musique
 * Contient également tous les méthodes utiles au fonctionnalités de base d'une application de lecture de musique
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {
    //media player
    protected MediaPlayer player;
    //song list
    protected ArrayList<Song> songs;
    //current position
    protected int songPosn;
    protected Song songtoshow;
    protected final IBinder musicBind = new MusicBinder();
    protected String songTitle="";
    protected static final int NOTIFY_ID=1;
    protected boolean shuffle=false;
    protected Random rand;
    protected int type=5; // type = 1 pour host, 0 pour client
    protected String Hostadresse;
    protected int PORT;
    protected ArrayList<String> addressList;
    protected int tailleliste;

    public void setClienttypeToHost(){
        type=1;
    }
    public void setClienttypeToClient(){
        type=0;
    }
    public void setAdressetosend(ArrayList<String> addressList){
        this.addressList = addressList;
        tailleliste = addressList.size();
    }
    public void setPORT(int PORT){
        this.PORT=PORT;
    }

    /**
     * Permet d'envoyer la musique de l'host au client
     * @param address : adresse du client
     */
    public void sendSong(String address){
        Uri uri = getUri();

        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                address);

        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        this.startService(serviceIntent);
    }

    /**
     * Permet d'envoyer par socket une action qui sera réalisé par les clients qui
     * seront connectés à l'host
     * @param action : action à envoyer
     * @param order : ordre à envoyer, 0 pour pause, 1 pour play et 5 pour seek to
     */
    public void sendAction(String action,int order){
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_ACTION);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                Hostadresse);
        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        serviceIntent.putExtra("action",action);
        serviceIntent.putExtra("order",order);
        serviceIntent.putExtra("pos", getPosn());
        this.startService(serviceIntent);
    }
    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPosn=0;
        //create player
        player = new MediaPlayer();
        initMusicPlayer();
        rand=new Random();
    }

    /**
     * Initialise le lecteur
     */
    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    /**
     * initialise la vue des musiques
     * @param theSongs : Les musiques à afficher
     */
    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
    }


    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // ici peut-être erreur et changer le > en >=
        if (type==0) {

        }else {
            if (player.getCurrentPosition() > 0) {
                mp.reset();
                playNext();
            }
        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    /**
     * Permet de récuperer les informations de la musique en cours de lecture
     * @return : Renvoie les informations de la musique en cours de lecture
     */
    public Song getSongtoshow(){return (songtoshow);}

    public void setSongtoshow(Song songtoshow){this.songtoshow=songtoshow;}

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void playSong(){
        //play a song
        player.reset();
        //get song
        Song playSong = songs.get(songPosn);
        songTitle=playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
        songtoshow=playSong;

    }

    /**
     * Permet de lire la musique dont on connait le chemin
     */
    public void playSong(String path){
        //play a song
        player.reset();
        try{
            player.setDataSource(path);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public Uri getUri(){
        Song playSong = songs.get(songPosn);
        long currSong = playSong.getID();
//set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        return(trackUri);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }
    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer() {
        /*if(type==1) {
            sendAction("pause", 0);
        }*/
        try {
            player.pause();
        }catch(Exception e){
            Log.d("MusicService","pause exception "+ e);
        }
    }

    public void seek(int posn){
        try {
            player.seekTo(posn);
        }catch(Exception e){
            Log.d("MusicService","seekTo exception "+ e);
        }
    }

    public void go(){
        try {
            player.start();
        }catch(Exception e){
            Log.d("MusicService","go exception "+ e);
        }
    }

    /**
     * Permet de passer à la musique précedente
     */
    public void playPrev(){
        if(type==0) {

        }else {
            songPosn--;
            if (songPosn < 0) songPosn = songs.size() - 1;
            if(type==1) {
                for(int i=0; i<tailleliste;i++) {
                    sendSong((String)addressList.get(i));
                }
            }
            playSong();
        }
    }

    /**
     * Permet de passer à la musique suivante
     */
    public void playNext(){
        if(type==0){

        }
        else {

            if (shuffle) {
                int newSong = songPosn;
                while (newSong == songPosn) {
                    newSong = rand.nextInt(songs.size());
                }
                songPosn = newSong;
            } else {
                songPosn++;
                if (songPosn >= songs.size()) songPosn = 0;
            }
            if (type == 1) {
                for(int i=0; i<tailleliste;i++) {
                    sendSong((String)addressList.get(i));
                }
            }
            playSong();
        }

    }

    /**
     * Permet d'activer ou de désactiver le mode aléatoire
     */
    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    /**
     * permet de récuperer la valeur du boolean shuffle
     * @return renvoie la valeur du boolean shuffle
     */
    public boolean getShuffle(){
        return shuffle;
    }


}
