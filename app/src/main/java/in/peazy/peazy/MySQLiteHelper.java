package in.peazy.peazy;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.Timestamp;

/**
 * Created by MB on 6/10/2017.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_LOCATIONS = "locations";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_MARKER = "marker";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LON = "longitude";

    private static final String DATABASE_NAME = "locations.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TAG = "PeazySQLHelper";

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_LOCATIONS + "( " + COLUMN_TIMESTAMP
            + " text not null, " + COLUMN_LAT
            + " text not null, " + COLUMN_LON
            + " text primary key not null, " + COLUMN_MARKER
            + " text not null);";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }
}
