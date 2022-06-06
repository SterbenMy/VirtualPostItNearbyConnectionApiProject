package com.smd.virtualpostit.DataModel;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Arrays;
import java.util.Date;

@Entity(tableName = "Posts")
public class Post {
    public Post() {
    }

    public Post(int uid, String name, String comment, Double lon, Double lat, String dob, String imagePath, String imageText, String deviceId) {
        this.uid = uid;
        this.name = name;
        this.comment = comment;
        this.lon = lon;
        this.lat = lat;
        this.dob = dob;
        this.imageText = imageText;
        this.imagePath = imagePath;
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return name + "," + comment + "," + lon + "," + lat + "," + dob + "," + imagePath + "," + imageText + "," + deviceId;
    }

    @PrimaryKey(autoGenerate = true)
    int uid;

    @ColumnInfo(name = "name")
    String name;

    @ColumnInfo(name = "comment")
    String comment;

    @ColumnInfo(name = "longitude")
    Double lon;

    @ColumnInfo(name = "latitude")
    Double lat;

    String dob;

    @ColumnInfo(name = "imageText")
    String imageText;

    @ColumnInfo(name = "imagePath")
    String imagePath;

    @ColumnInfo(name = "deviceId")
    String deviceId;

    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }


    public String getImageText() {
        return imageText;
    }

    public String getImagePath() {
        return imagePath;
    }


    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public int getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public String getDob() {
        return dob;
    }

    public Double getLon() {
        return lon;
    }

    public Double getLat() {
        return lat;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


}
