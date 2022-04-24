package com.smd.virtualpostit.DataModel;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

@Database(
        entities = Post.class,
        version = 2,
        exportSchema = false
)
@TypeConverters(DataConverter.class)
public abstract class PostDatabase extends RoomDatabase {
    private static PostDatabase postDB = null;

    public abstract PostDAO postDAO();

    public static synchronized PostDatabase getDBInstance(Context context) {
        if (postDB == null) {
            postDB = Room.databaseBuilder(
                    context.getApplicationContext(),
                    PostDatabase.class,
                    "post19b2"
            )
                    .allowMainThreadQueries()
//                    .fallbackToDestructiveMigration()
                    .build();
        }
        return postDB;
    }
}
