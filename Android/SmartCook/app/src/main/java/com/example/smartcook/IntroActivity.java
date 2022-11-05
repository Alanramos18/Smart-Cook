package com.example.smartcook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class IntroActivity extends AppCompatActivity {

    private ConstraintLayout layout;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        textView = findViewById(R.id.textView);
        layout   = (ConstraintLayout)findViewById(R.id.introLayout);

        textView.setText("Pulse en cualquier lugar para comenzar");

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                intent.putExtra("First", true);

                startActivity(intent);

                finish();
            }
        });
    }
}