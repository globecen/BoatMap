package com.globecen.boatmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import com.globecen.boatmap.MapTile;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.util.StreamUtils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayInputStream;
import java.io.File;

public class MBTilesTileSource extends BitmapTileSourceBase implements ITileSource {

    private SQLiteDatabase database;

    public MBTilesTileSource(Context context, String name, int minZoom, int maxZoom, int tileSizePixels, String fileExtension) {
        super(name, minZoom, maxZoom, tileSizePixels, fileExtension);
        try {
            File mbtilesFile = new File(context.getFilesDir(), name);
            database = SQLiteDatabase.openOrCreateDatabase(mbtilesFile, null);
        } catch (SQLiteException e) {
            // handle the exception
        }
    }


    public Drawable getDrawable(byte[] data) {
        if (data == null) {
            return null;
        } else {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            return new BitmapDrawable(Resources.getSystem(), bitmap);
        }
    }


    public Drawable getDrawable(MapTile tile) {
        byte[] tileBytes = getTileImage(tile);
        return getDrawable(tileBytes);
    }

    private byte[] getTileImage(MapTile tile) {
        Cursor cursor = null;
        byte[] image = null;
        try {
            int zoom = tile.getZoomLevel();
            int tileX = tile.getX();
            int tileY = (1 << zoom) - tile.getY() - 1; // TMS style flip
            String[] columns = {"tile_data"};
            String selection = "zoom_level = ? AND tile_column = ? AND tile_row = ?";
            String[] selectionArgs = new String[]{
                    String.valueOf(zoom),
                    String.valueOf(tileX),
                    String.valueOf(tileY)
            };
            cursor = database.query("tiles", columns, selection, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                image = cursor.getBlob(0);
            }
        } catch (Exception e) {
            // handle exception
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return image;
    }

    @Override
    public void finalize() throws Throwable {
        if (database != null && database.isOpen()) {
            database.close();
        }
        super.finalize();
    }
}
