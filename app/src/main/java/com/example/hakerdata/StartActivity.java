package com.example.hakerdata;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    public void onClick(View view){
        if(view.getId() == R.id.card_message){ }
        else if(view.getId() == R.id.card_picture){ }
        else if(view.getId() == R.id.card_video){ }
        else if(view.getId() == R.id.card_audio){ }
    }
}