package com.example.smartbin_2020_2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Mantenimiento extends AppCompatActivity {

    private Button btcbuttonMan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mantenimiento);


        btcbuttonMan = (Button) findViewById(R.id.buttonMan);


        btcbuttonMan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BTHandler.getInstance().sendMsg(new Message(Command.DETENER_RIEGO));
            }
        });
    }


}