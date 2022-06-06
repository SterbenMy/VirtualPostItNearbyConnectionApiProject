package com.smd.virtualpostit;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PostViewHolder extends RecyclerView.ViewHolder {
    ImageView imageView;
    TextView name, comment;
    Button btnRemove;

    public PostViewHolder(@NonNull View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.cardName);
        comment = itemView.findViewById(R.id.cardComment);
        imageView = itemView.findViewById(R.id.cardImage);
        btnRemove = itemView.findViewById(R.id.btnRemove);
    }
}
