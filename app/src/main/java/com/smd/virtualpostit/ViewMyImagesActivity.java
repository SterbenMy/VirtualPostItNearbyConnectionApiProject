package com.smd.virtualpostit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.provider.Settings;

import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;

import java.util.List;

public class ViewMyImagesActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_images);

        recyclerView = findViewById(R.id.postRecyclerView);
        String[] deviceId = new String[]{ Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID) };
        DBHelper dbHelper = new DBHelper(this);
        List<Post> posts = dbHelper.getAllImgMine(deviceId);

        PostRecycler postRecycler = new PostRecycler(posts, ViewMyImagesActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(postRecycler);
    }


}