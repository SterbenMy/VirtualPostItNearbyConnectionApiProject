package com.smd.virtualpostit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.smd.virtualpostit.DataModel.PostDAO;
import com.smd.virtualpostit.DataModel.PostDatabase;

public class ViewMyImagesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PostDAO postDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_images);

        recyclerView = findViewById(R.id.postRecyclerView);

        postDAO = PostDatabase.getDBInstance(this).postDAO();

        PostRecycler postRecycler = new PostRecycler(postDAO.getAllPosts());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(postRecycler);
    }
}