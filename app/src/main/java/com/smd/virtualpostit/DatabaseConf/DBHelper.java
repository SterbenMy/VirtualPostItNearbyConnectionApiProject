package com.smd.virtualpostit.DatabaseConf;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.smd.virtualpostit.DataModel.Post;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DBNAME = "VirtualPostIt.db";
    private static final String TNAME = "Posts";

    public DBHelper(@Nullable Context context) {
        super(context, DBNAME, null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + TNAME + "(uid Integer primary key autoincrement not null, "
                + "name Text, "
                + "comment Text, "
                + "lon Real, "
                + "lat Real, "
                + "dob Text, "
                + "imagePath Text, "
                + "imageText Text, "
                + "deviceId Text)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop Table if exists " + TNAME);
        onCreate(sqLiteDatabase);
    }

    public boolean insertPostData(Post post) {
        // to get Database connection
        SQLiteDatabase database = this.getWritableDatabase();
        //to write in database content
        ContentValues contentValues = new ContentValues();
        //assign values  to content variable
        contentValues.put("name", post.getName());
        contentValues.put("comment", post.getComment());
        contentValues.put("lon", post.getLon());
        contentValues.put("lat", post.getLat());
        contentValues.put("dob", post.getDob().toString());
        contentValues.put("imageText", post.getImageText());
        contentValues.put("imagePath", post.getImagePath());
        contentValues.put("deviceId", post.getDeviceId());

        //execute query
        long result = database.insert(TNAME, null, contentValues);

        return result != -1;
    }

    public void insertPostDataa(Post post) {
        // to get Database connection
        SQLiteDatabase database = this.getWritableDatabase();
        //to write in database content
        ContentValues contentValues = new ContentValues();
        //assign values  to content variable
        contentValues.put("name", post.getName());
        contentValues.put("comment", post.getComment());
        contentValues.put("lon", post.getLon());
        contentValues.put("lat", post.getLat());
        contentValues.put("dob", post.getDob().toString());
        contentValues.put("imageText", post.getImageText());
        contentValues.put("imagePath", post.getImagePath());
        contentValues.put("deviceId", post.getDeviceId());

        //execute query
        database.insert(TNAME, null, contentValues);
    }

    public void deletePostById(int id) {
        // to get Database connection
        SQLiteDatabase database = this.getWritableDatabase();
        //to write in database content
        database.delete(TNAME, "uid = ? ", new String[]{String.valueOf(id)});
    }


    public List<Post> getAllPost() {
        String sql = "select * from " + TNAME;
        // to get Database connection                          
        SQLiteDatabase database = this.getWritableDatabase();
        List<Post> posts = new ArrayList<>();
        Cursor cursor = database.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(0));
                String name = cursor.getString(1);
                String comment = cursor.getString(2);
                Double lon = cursor.getDouble(3);
                Double lat = cursor.getDouble(4);
                String date = cursor.getString(5);
                String imageText = cursor.getString(6);
                String imagePath = cursor.getString(7);
                String deviceId = cursor.getString(8);

                posts.add(new Post(id, name, comment, lon, lat, date, imageText, imagePath, deviceId));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return posts;
    }

    public List<Post> getAllImgExceptMine(String[] device) {
        String sql = "select * from " + TNAME + " where deviceId != ?";
        // to get Database connection
        SQLiteDatabase database = this.getWritableDatabase();
        List<Post> posts = new ArrayList<>();
        Cursor cursor = database.rawQuery(sql, device);
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(0));
                String name = cursor.getString(1);
                String comment = cursor.getString(2);
                Double lon = cursor.getDouble(3);
                Double lat = cursor.getDouble(4);
                String date = cursor.getString(5);
                String imageText = cursor.getString(6);
                String imagePath = cursor.getString(7);
                String deviceId = cursor.getString(8);

                posts.add(new Post(id, name, comment, lon, lat, date, imageText, imagePath, deviceId));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return posts;
    }

    public List<Post> getAllImgMine(String[] device) {
        String sql = "select * from " + TNAME + " where deviceId = ?";
        // to get Database connection
        SQLiteDatabase database = this.getWritableDatabase();
        List<Post> posts = new ArrayList<>();
        Cursor cursor = database.rawQuery(sql, device);
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(0));
                String name = cursor.getString(1);
                String comment = cursor.getString(2);
                Double lon = cursor.getDouble(3);
                Double lat = cursor.getDouble(4);
                String date = cursor.getString(5);
                String imageText = cursor.getString(6);
                String imagePath = cursor.getString(7);
                String deviceId = cursor.getString(8);

                posts.add(new Post(id, name, comment, lon, lat, date, imageText, imagePath, deviceId));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return posts;
    }

    public List<Post> getAllExpiredPosts(String[] currentDate) {
        String sql = "select * from " + TNAME + " where dob <= ?";
        // to get Database connection
        SQLiteDatabase database = this.getWritableDatabase();
        List<Post> posts = new ArrayList<>();
        Cursor cursor = database.rawQuery(sql, currentDate);
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(0));
                String name = cursor.getString(1);
                String comment = cursor.getString(2);
                Double lon = cursor.getDouble(3);
                Double lat = cursor.getDouble(4);
                String date = cursor.getString(5);
                String imageText = cursor.getString(6);
                String imagePath = cursor.getString(7);
                String deviceId = cursor.getString(8);

                posts.add(new Post(id, name, comment, lon, lat, date, imageText, imagePath, deviceId));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return posts;
    }

}
