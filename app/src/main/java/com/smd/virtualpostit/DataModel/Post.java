package com.smd.virtualpostit.DataModel;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "Posts")
public class Post {

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

    Date dob;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    byte[] image;

    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public void setImage(byte[] image) {
        this.image = image;
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

    public Date getDob() {
        return dob;
    }

    public byte[] getImage() {
        return image;
    }

    public Double getLon() {
        return lon;
    }

    public Double getLat() { return lat; }

}
