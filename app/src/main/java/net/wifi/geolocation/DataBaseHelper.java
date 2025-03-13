package net.wifi.geolocation;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DatabaseHelper extends SQLiteAssetHelper {
    private static final String DB_DIR = "/data/data/net.wifi.geolocation/databases/"; // полный путь к базе данных
    private static final String DB_NAME = "db1.sqlite";
    private static final int SCHEMA = 3; // версия базы данных
    private static String DB_PATH;
    /*static final String TABLE = "network"; // название таблицы в бд
    // названия столбцов
    static final String COLUMN_BSIID = "bssid";
    static final String COLUMN_BESTLAT = "bestlan";
    static final String COLUMN_BESTLON = "bestlon";*/
    private final Context myContext;

    DatabaseHelper(Context context) {
        super(context, DB_NAME, null, SCHEMA);
        //setForcedUpgrade();
        this.myContext=context;
        DB_PATH = DB_DIR + DB_NAME;
    }


    /*@Override
    public void setForcedUpgrade() {
        super.setForcedUpgrade();
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        setForcedUpgrade();
    }*/

    void create_db(){
        InputStream myInput;
        OutputStream myOutput;
        try {
            File file = new File(DB_PATH);
            if (!file.exists()) {
                this.getReadableDatabase();
                this.close();
                //получаем локальную бд как поток
                myInput = myContext.getAssets().open(DB_NAME);
                // Путь к новой бд
                String outFileName = DB_PATH;

                // Открываем пустую бд
                myOutput = new FileOutputStream(outFileName);

                // побайтово копируем данные
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }

                myOutput.flush();
                myOutput.close();
                myInput.close();
            }
        }
        catch(IOException ex){
            Log.d("DatabaseHelper", ex.getMessage());
        }
    }
    public SQLiteDatabase open()throws SQLException {
            return SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);


    }
}