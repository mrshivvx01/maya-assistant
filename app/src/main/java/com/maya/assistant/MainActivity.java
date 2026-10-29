package com.maya.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(20, 16, 38));

        TextView character = new TextView(this);
        character.setText("🌸");
        character.setTextSize(90);
        character.setGravity(Gravity.CENTER);
        layout.addView(character);

        TextView title = new TextView(this);
        title.setText("MAYA AI");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Your anime AI companion");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        TextView status = new TextView(this);
        status.setText("\nHello! I'm Maya 🌷\nYour assistant is getting ready.");
        status.setTextSize(18);
        status.setTextColor(Color.WHITE);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        setContentView(layout);
    }
}
