package com.smd.virtualpostit;

import static com.smd.virtualpostit.Constants.CAMERA_INTENT;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.smd.virtualpostit.DataModel.DataConverter;
import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DataModel.PostDAO;
import com.smd.virtualpostit.DataModel.PostDatabase;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TakeAPhotoActivity extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationProviderClient;
    Bitmap bmpImage;
    ImageView imageView;
    EditText editName;
    EditText editComment;
    PostDAO postDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_aphoto);

        editName = findViewById(R.id.postName);
        editComment = findViewById(R.id.postComment);
        imageView = findViewById(R.id.postImage);
        ImageButton btnBack = findViewById(R.id.btnBack1);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnPost = findViewById(R.id.btnPost);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        postDAO = PostDatabase.getDBInstance(this).postDAO();

        btnReset.setOnClickListener(reset -> {
            editName.setText(null);
            editComment.setText(null);
            imageView.setImageResource(R.drawable.select_image);
        });


        btnBack.setOnClickListener(back -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnPost.setOnClickListener(this::savePost);

        imageView.setOnClickListener(this::takePicture);
    }

    private void takePicture(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_INTENT);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA_INTENT:
                bmpImage = (Bitmap) data.getExtras().get("data");
                if (bmpImage != null) {
                    imageView.setImageBitmap(bmpImage);
                } else {
                    Toast.makeText(this,
                            "BitMap is NULL",
                            Toast.LENGTH_SHORT)
                            .show();
                }
                break;
        }
    }

    private void savePost(View view) {
        if (editName.getText().toString().isEmpty() || editComment.getText().toString().isEmpty()
                || bmpImage == null) {
            Toast.makeText(
                    this,
                    "Post data is missing",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            getLocation();

        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(TakeAPhotoActivity.this, Locale.getDefault());


                        List<Address> addressList = geocoder.getFromLocation(
                                location.getLatitude(),
                                location.getLongitude(),
                                1);
                        Post post = new Post();

                        post.setName(editName.getText().toString());
                        post.setComment(editComment.getText().toString());
                        post.setImage(DataConverter.convertImage2ByteArray(bmpImage));
                        post.setDob(new Date());
                        post.setLon(addressList.get(0).getLongitude());
                        post.setLat(addressList.get(0).getLatitude());
                        postDAO.insertPost(post);
                        Toast.makeText(
                                this,
                                "Post was inserted successfully",
                                Toast.LENGTH_SHORT
                        ).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }
}