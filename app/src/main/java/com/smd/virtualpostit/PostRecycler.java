package com.smd.virtualpostit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smd.virtualpostit.DataModel.DataConverter;
import com.smd.virtualpostit.DataModel.Post;

import java.util.List;

public class PostRecycler extends RecyclerView.Adapter<PostViewHolder> {

    List<Post> data;

    public PostRecycler(List<Post> posts) {
        data = posts;
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
        Post post = data.get(position);
        holder.imageView.setImageBitmap(DataConverter.convertByteArray2Image(post.getImage()));
        holder.name.setText(post.getName());
        holder.comment.setText(post.getComment());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
