package com.smd.virtualpostit;

import static com.smd.virtualpostit.Constants.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ViewImagesActivity extends AppCompatActivity {

    private ViewPager mSlideViewPager;
    private SliderAdapter sliderAdapter;
    private Button mNextBtn;
    private Button mBackBtn;
    private int mCurrentPage;
    LocationManager locationManager;
    int PERMISSION_ID = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_images);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String[] deviceId = new String[]{Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)};
        mSlideViewPager = findViewById(R.id.slideviewpager);
        DBHelper dbHelper = new DBHelper(this);
        List<Post> posts = dbHelper.getAllImgExceptMine(deviceId);
        List<Post> postList = new ArrayList<>();
        for (Post post : posts) {
            if (checkDistanceBetween(post.getLat(), post.getLon())) {
                postList.add(post);
            }
        }
        sliderAdapter = new SliderAdapter(this, postList);
        mSlideViewPager.setAdapter(sliderAdapter);
        mSlideViewPager.addOnPageChangeListener(viewListener);
        mNextBtn = findViewById(R.id.next);
        mBackBtn = findViewById(R.id.previous);
        ImageButton mPrevBtn = findViewById(R.id.btnBack1);
        mNextBtn.setOnClickListener(view -> mSlideViewPager.setCurrentItem(mCurrentPage + 1));
        mBackBtn.setOnClickListener(view -> mSlideViewPager.setCurrentItem(mCurrentPage - 1));
        mPrevBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            mCurrentPage = i;
            if (i == 0) {

                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(false);
                mBackBtn.setVisibility(View.INVISIBLE);
                mNextBtn.setText(R.string.next);
                mBackBtn.setText("");

            } else if (i == getCount() - 1) {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);
                mNextBtn.setText("");
                mBackBtn.setText(R.string.previous);

            } else {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);
                mNextBtn.setText(R.string.next);
                mBackBtn.setText(R.string.previous);
            }
        }

        @Override
        public void onPageScrollStateChanged(int sitate) {
        }
    };

    private int getCount() {
        return databaseList().length;
    }


    public double lat;
    public double lon;

    private boolean checkDistanceBetween(Double startLat, Double startLog) {
        Location locationStart = new Location("Location A");
        Location locationEnd = new Location("Location B");

        locationStart.setLatitude(startLat);
        locationStart.setLongitude(startLog);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

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


        locationEnd.setLatitude(lat);
        locationEnd.setLongitude(lon);
        double distance = locationStart.distanceTo(locationEnd);


        if (distance >= 0 && distance <= 1000) {
            return true;
        }
        return false;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "Check isLocationEnabled");
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}

