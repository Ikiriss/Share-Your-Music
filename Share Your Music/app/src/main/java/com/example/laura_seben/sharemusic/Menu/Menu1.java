package com.example.laura_seben.sharemusic.Menu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.MainActivity;
import com.example.laura_seben.sharemusic.R;

/**
 * Created by Sébastien on 22/03/2016.
 */
public class Menu1 extends Activity {

    private TextView t_menu = null;
    private Button b_ecouter = null;
    private Button b_bibliotheque = null;
    private Button b_connexion = null;
    private Button b_parametre = null;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)  {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }

        t_menu = (TextView) findViewById(R.id.t_menu);
        Typeface ttype = Typeface.createFromAsset(getAssets(), "fonts/WhiteLarch_PERSONAL_USE.ttf");
        t_menu.setTypeface(ttype);
        Typeface btype = Typeface.createFromAsset(getAssets(), "fonts/MonoSpatial.ttf");
        b_ecouter = (Button) findViewById(R.id.ecouter);
        b_ecouter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu1.this, com.example.laura_seben.sharemusic.Musique.test.MainActivity.class);
                startActivity(intent);
            }
        });
        b_ecouter.setTypeface(btype);

        b_bibliotheque = (Button) findViewById(R.id.bibliotheque);
        b_bibliotheque.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(Menu1.this, MenuBibliotheque.class);
                startActivity(intent);
            }
        });
        b_bibliotheque.setTypeface(btype);

        b_connexion = (Button) findViewById(R.id.connexion);
        b_connexion.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(Menu1.this, com.example.laura_seben.sharemusic.Connexion.WifiDirectActivity.class);
                startActivity(intent);
            }
        });
        b_connexion.setTypeface(btype);
        b_parametre = (Button) findViewById(R.id.parametre);
        b_parametre.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
            }
        });
        b_parametre.setTypeface(btype);


    }

    /**
     * Demande la permission d'accéder à la carte mémoire du téléphone (nécessaire
     * pour les téléphone android au délà de API 21)
     * @param requestCode : La permission demandée
     * @param permissions : La permission accordée
     * @param grantResults : Le résultat de la demande
     */

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {

                }
                return;
            }
        }
    }


}
