package com.smd.virtualpostit;

import androidx.annotation.ColorInt;

public class Constants {

    private Constants() {
    }

    public static final String TAG = "VirtualPostIt";

    public static final String SERVICE_ID =
            "com.smd.postIt.automatic.SERVICE_ID";
    @ColorInt
    protected static final int[] COLORS =
            new int[]{
                    0xFFF44336 /* red */,
                    0xFF9C27B0 /* deep purple */,
                    0xFF00BCD4 /* teal */,
                    0xFF4CAF50 /* green */,
                    0xFFFFAB00 /* amber */,
                    0xFFFF9800 /* orange */,
                    0xFF795548 /* brown */
            };
    public static final int CAMERA_INTENT = 51;
}
