package com.smd.virtualpostit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.smd.virtualpostit.DataModel.DataConverter;
import com.smd.virtualpostit.DataModel.Post;

import java.io.File;
import java.util.List;

public class SliderAdapter extends PagerAdapter {
    Context context;
    LayoutInflater layoutInflater;
    List<Post> data;
    Bitmap myBitmap;

    public SliderAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.data = posts;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Post post = data.get(position);
        String imageNamePath = post.getImagePath() + "/" + post.getImageText();
        File imgFile = new File(imageNamePath);
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout, container, false);
        ImageView slideImageView = view.findViewById(R.id.imageViewSliderImage);
        TextView slideHeading = view.findViewById(R.id.textName);
        TextView slideDescription = view.findViewById(R.id.textDescription);

        myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        slideImageView.setImageBitmap(myBitmap);
        slideHeading.setText(post.getName());
        slideDescription.setText(post.getComment());
        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}

