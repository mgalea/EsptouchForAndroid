package com.espressif.esptouch.android.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.esptouch.android.R;
import com.espressif.esptouch.android.v1.EspTouchActivity;
import com.espressif.esptouch.android.v2.EspTouch2Activity;

public class EspMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_title);
    }
     /** Called when the user touches the button */
    public void openV1(View view) {
        startActivity(new Intent(EspMainActivity.this, EspTouchActivity.class));
    }
    /** Called when the user touches the button */
    public void openV2(View view) {
        startActivity(new Intent(EspMainActivity.this, EspTouch2Activity.class));
    }
    /** Called when the user touches the button */
    public void logOut(View view) {
        startActivity(new Intent(EspMainActivity.this, LoginActivity.class));
    }
}
