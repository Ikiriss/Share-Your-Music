package com.example.laura_seben.sharemusic;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.laura_seben.sharemusic.Menu.Menu1;

public class MainActivity extends Activity {
    private Button b = null;
    private TextView t_mainmenu = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        b = (Button) findViewById(R.id.screen);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Menu1.class);
                startActivity(intent);
            }
        });

        t_mainmenu = (TextView)    findViewById(R.id.t_mainmenu);
        Typeface ttype = Typeface.createFromAsset(getAssets(), "fonts/WhiteLarch_PERSONAL_USE.ttf");
        t_mainmenu.setTypeface(ttype);
        /*Intent intent = new Intent(MainActivity.this, Menu1.class);
        startActivity(intent);*/


    }

}




