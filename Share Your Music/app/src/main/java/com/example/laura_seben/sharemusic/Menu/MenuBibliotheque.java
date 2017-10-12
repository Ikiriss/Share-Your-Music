package com.example.laura_seben.sharemusic.Menu;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.R;

/**
 * Created by Laura-seben on 14/03/2016.
 */
public class MenuBibliotheque extends Activity {

    private TextView t_menu_bibliotheque = null;
    private Button b_creerplaylist = null;
    private Button b_playlist = null;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_bibliotheque);

        b_creerplaylist = (Button) findViewById(R.id.b_creerplaylist);
        Typeface btype = Typeface.createFromAsset(getAssets(), "fonts/MonoSpatial.ttf");
        b_creerplaylist.setTypeface(btype);

        b_playlist = (Button) findViewById(R.id.b_playlist);
        b_playlist.setTypeface(btype);


        t_menu_bibliotheque = (TextView)    findViewById(R.id.menu_bibliotheque);
        Typeface ttype = Typeface.createFromAsset(getAssets(), "fonts/WhiteLarch_PERSONAL_USE.ttf");
        t_menu_bibliotheque.setTypeface(ttype);



    }
}

