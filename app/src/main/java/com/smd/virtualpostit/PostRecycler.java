package com.smd.virtualpostit;

import static com.smd.virtualpostit.Constants.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smd.virtualpostit.DataModel.DataConverter;
import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;

import java.io.File;
import java.util.List;

public class PostRecycler extends RecyclerView.Adapter<PostViewHolder> {

    List<Post> data;
    DBHelper database;
    Context context;

    Bitmap myBitmap;

    public PostRecycler(List<Post> posts) {
        this.data = posts;
    }

    public PostRecycler(List<Post> data, Context context) {
        this.data = data;
        this.context = context;
        database = new DBHelper(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_of_posts,
                parent,
                false
        );
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        final Post post = data.get(position);
        String imageNamePath = post.getImagePath() + "/" + post.getImageText();
        File imgFile = new File(imageNamePath);

        myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        holder.imageView.setImageBitmap(myBitmap);
        holder.name.setText(post.getName());
        holder.comment.setText(post.getComment());
        holder.btnRemove.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Confirmation")
                    .setMessage("Are you sure that you want to delete this post ?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        Log.d(TAG, "onClick: delete post  ");
                        boolean deleted = imgFile.delete();
                        database.deletePostById(post.getUid());
                        data.remove(position);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Post was deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel());
            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
