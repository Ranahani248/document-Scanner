package com.example.documentsscanner;

import static java.util.ResourceBundle.clearCache;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BitmapSaver {
    private static final String DEFAULT_FOLDER_PREFIX = "draft";
    private static final String IMAGE_EXTENSION = ".jpg";
    private static final String TAG = "BitmapSaver";

    private Context context;

    public BitmapSaver(Context context) {
        this.context = context;
    }

    public void saveBitmaps(List<Uri> uris, @Nullable String folderName) {
        Log.d(TAG, "Saving bitmap to folder: " + folderName);
        if (folderName == null || folderName.isEmpty()) {
            folderName = generateFolderName(DEFAULT_FOLDER_PREFIX);
        }
        Log.d(TAG, "Saving bitmap to folder1: " + folderName);

        File folder = new File(context.getFilesDir(), folderName);
        if(!folder.exists()){
            folder.mkdirs();
        }
        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            Bitmap bitmap = getBitmapFromUri(uri);
            if (bitmap != null) {
                saveBitmapToFile(bitmap, folder, "image_" + (i + 1) + IMAGE_EXTENSION);
            }
        }

        HomeActivity homeActivity = HomeActivity.getInstance();
        if(homeActivity != null){
                homeActivity.refreshAdapter();


        }
        else{
            Log.d(TAG, "DataSet Effect1" + folderName);
        }


    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmap from URI: " + e.getMessage(), e);
            return null;
        }
    }

    private void saveBitmapToFile(Bitmap bitmap, File folder, String fileName) {
        File file = new File(folder, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Log.d(TAG, "Saved bitmap to: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file: " + e.getMessage(), e);
        }
    }


    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    private String generateFolderName(String baseName) {
        File baseDir = context.getFilesDir();
        int count = 1;
        String folderName = baseName;
        File folder = new File(baseDir, folderName);
        while (folder.exists()) {
            count++;
            folderName = baseName + " " + count;
            folder = new File(baseDir, folderName);
        }
        return folderName;
    }
}
