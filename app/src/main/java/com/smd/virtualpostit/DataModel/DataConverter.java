package com.smd.virtualpostit.DataModel;

import androidx.room.TypeConverter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.Date;

public class DataConverter {

    private DataConverter() {
    }

    @TypeConverter
    public static Date toDate(Long dateLong) {
        return dateLong == null ? null : new Date(dateLong);
    }

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    public static String convertImage2ByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArrayImage = stream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
        return encodedImage;
    }

    public static Bitmap convertByteArray2Image(byte[] arr) {
        return BitmapFactory.decodeByteArray(arr, 0, arr.length);
    }

    public static Bitmap convertByteArray2ImageFromString(String image) {
        byte[] byteArrayImage = Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(byteArrayImage, 0, byteArrayImage.length);
    }
}
