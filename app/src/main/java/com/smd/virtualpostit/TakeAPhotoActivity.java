package com.smd.virtualpostit;

import static com.smd.virtualpostit.Constants.TAG;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.*;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;

import java.io.File;
import java.util.*;


public class TakeAPhotoActivity extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationProviderClient;
    Bitmap bmpImage;
    ImageView imageView;
    EditText editName;
    EditText editComment;
    int PERMISSION_ID = 44;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_aphoto);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        editName = findViewById(R.id.postName);
        editComment = findViewById(R.id.postComment);
        imageView = findViewById(R.id.postImage);
        ImageButton btnBack = findViewById(R.id.btnBack1);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnPost = findViewById(R.id.btnPost);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btnReset.setOnClickListener(reset -> {
            editName.setText(null);
            editComment.setText(null);
            imageView.setImageResource(R.drawable.select_image);
        });
        btnBack.setOnClickListener(back -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnPost.setOnClickListener(view -> {
            this.savePost(this);
        });

        imageView.setOnClickListener(element -> {
            takePicture(this);
        });
    }

    private String pictureImagePath = "";

    String imagePath = "";
    String imageName = "";

    private void takePicture(Context context) {
        if (!Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/VirtualPostIt").exists()) {
            File storageDirPicture = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/VirtualPostIt");
            storageDirPicture.mkdirs();
        }
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/VirtualPostIt");
        pictureImagePath = getRandomFilename(String.valueOf(storageDir), ".jpg");
        imagePath = String.valueOf(storageDir);
        imageName =getImageNameFromPath(pictureImagePath);
        File file = new File(pictureImagePath);
        Uri outputFileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", (file));
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, 1);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            File imgFile = new File(pictureImagePath);
            if (imgFile.exists()) {
                bmpImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imageView.setImageBitmap(bmpImage);


            }
        }

    }

    private void savePost(Context context) {
        if (editName.getText().toString().isEmpty() || editComment.getText().toString().isEmpty() || bmpImage == null) {
            Log.d(TAG, "Data is missing ---- savePost()");
            Toast.makeText(this, "Post data is missing", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Data are OK ---- savePost()");
            getLocation(context);

        }
    }

    public double lat;
    public double lon;

    private void getLocation(Context context) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled()) {

                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Location locationPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                if (location != null) {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                } else if (locationNetwork != null) {
                    lat = locationNetwork.getLatitude();
                    lon = locationNetwork.getLongitude();
                } else if (locationPassive != null) {
                    lon = locationPassive.getLongitude();
                    lat = locationPassive.getLatitude();
                }

                Post post = new Post();

                post.setName(editName.getText().toString());
                post.setComment(editComment.getText().toString());
                post.setDob((new Date().toString()));
                post.setLon(lon);
                post.setLat(lat);
                post.setImageText(imageName);
                post.setImagePath(imagePath);
                post.setDeviceId(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                DBHelper dbHelper = new DBHelper(context);
                boolean check = dbHelper.insertPostData(post);
                if (check) {
                    Log.d(TAG, "insert into table" + post);
                    Toast.makeText(this, "Post was inserted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Cannot insert into table" + post);
                    Toast.makeText(this, "Unable to insert the post", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "Required to turn on location");
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            Log.d(TAG, "Required permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ID);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "Check isLocationEnabled");
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static String getRandomFilename(String filePath, String extension) {
        String randomGuid = UUID.randomUUID().toString().replace("-", "");
        String directory = filePath;
        if (!directory.endsWith(File.separator)) {
            directory += File.separator;
        }
        if ((extension != null) && extension.startsWith(".") && (extension.length() > 1)) {
            extension = extension.substring(1);
        }
        return String.format("%s%s.%s", directory, randomGuid, extension);
    }

    public String getImageNameFromPath(String name) {
        String[] splitted = name.split("/");
        List<String> arrayList = new ArrayList<>(Arrays.asList(splitted));
        return arrayList.get(arrayList.size() - 1);
    }
}