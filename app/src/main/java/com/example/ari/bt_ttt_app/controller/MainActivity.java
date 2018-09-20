package com.example.ari.bt_ttt_app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.ari.bt_ttt_app.R;
import com.example.ari.bt_ttt_app.model.User;

public class MainActivity extends AppCompatActivity {
    Button clearText;
    EditText name;
    Button play;
    public User mMyUser;
    public User mOpponent;
    public static String MyName = "";
    public static String Opponent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMyUser = new User();
        mOpponent = new User();

        play = findViewById(R.id.play);
        play.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        name = findViewById(R.id.myName);
                        MyName = name.getText().toString();

                        if (MyName.trim().equals("")) {
                            name.setError("Enter Name");
                        } else {
                            Intent intent;
                            mMyUser.setFirstname(MyName);
                            intent = new Intent(MainActivity.this, BT_TTT_names.class);
                            startActivity(intent);
                        }
                    }
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
    }
}

