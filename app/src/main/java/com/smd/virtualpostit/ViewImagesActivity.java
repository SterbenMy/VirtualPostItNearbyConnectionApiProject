package com.smd.virtualpostit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;
import java.util.List;

public class ViewImagesActivity extends AppCompatActivity {

    private ViewPager mSlideViewPager;
    private SliderAdapter sliderAdapter;
    private Button mNextBtn;
    private Button mBackBtn;
    private int mCurrentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_images);

        String[] deviceId = new String[]{ Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID) };
        mSlideViewPager = (ViewPager) findViewById(R.id.slideviewpager);
        DBHelper dbHelper = new DBHelper(this);
        List<Post> posts =  dbHelper.getAllImgExceptMine(deviceId);
        sliderAdapter = new SliderAdapter(this, posts);
        mSlideViewPager.setAdapter(sliderAdapter);
        mNextBtn = (Button) findViewById(R.id.next);
        mBackBtn = (Button) findViewById(R.id.previous);
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
                mNextBtn.setText("Next");
                mBackBtn.setText("");

            } else if (i == getCount() - 1) {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);
                mNextBtn.setText( "Finish");
                mBackBtn.setText("Previous");

            } else {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);
                mNextBtn.setText("Next");
                mBackBtn.setText("Previous");
            }
        }

        @Override
        public void onPageScrollStateChanged(int sitate) {
        }
    };

    private int getCount() {
        return databaseList().length;
    }
}