package com.example.documentsscanner;

import static com.example.documentsscanner.ImagePreview.position;
import static com.example.documentsscanner.ImagePreview.save;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String FULL_MODE = "FULL";
    static final int REQUEST_WRITE_STORAGE = 112;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 112;
    private static final String BASE_MODE = "BASE";
    private static final String BASE_MODE_WITH_FILTER = "BASE_WITH_FILTER";
    private String selectedMode = FULL_MODE;
    private static MainActivity instance;
    ConstraintLayout selectedImagesLayout, menuLayout;
    ImageView  selectedCancel,shareButton,homeButton,saveButton,selectedSave,selectedShare, selectedDelete,menuButton;
    ConstraintLayout mainLayout;
    private static final int MAX_WIDTH_DOC = 300;
    private static final int MAX_WIDTH_PDF = 600;
    private static final int MAX_HEIGHT_PDF = 800;
    private static final int MAX_HEIGHT_DOC = 400;
    TextView selectedImagesText, shareAsImage, shareAsPdf,shareAsWord;
    LinearLayout shareAs, saveAs, selectedNavigation;
    ImagePreview imagePreview;
    RecyclerView recyclerView;
    ScannedRecycleAdapter adapter;
    int editPosition = -1;
    public String folderName;



    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    boolean editImage = false;
    private Uri pdfUri;
    private GmsDocumentScanningResult lastScanResult;
    public static   List<Uri> images1 = new ArrayList<>();

    List<Uri> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        recyclerView = findViewById(R.id.result_info_recycle);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.setLayoutDirection(LinearLayout.HORIZONTAL);
//        recyclerView.setLayoutParams(layoutParams);
        adapter = new ScannedRecycleAdapter(images, this);
        recyclerView.setAdapter(adapter);
        mainLayout = findViewById(R.id.mainLayout);
        selectedImagesLayout = findViewById(R.id.selectedImagesLayout);
        selectedDelete = findViewById(R.id.selectedDelete);
        selectedCancel = findViewById(R.id.selectedCancel);
        selectedImagesText = findViewById(R.id.selectedImagesText);
        homeButton = findViewById(R.id.homeButton);
        shareButton = findViewById(R.id.shareButton);
        saveButton = findViewById(R.id.saveButton);
        shareAsImage = findViewById(R.id.shareAsImage);
        shareAsPdf = findViewById(R.id.shareAsPdf);
        shareAsWord = findViewById(R.id.shareAsWord);
        saveAs = findViewById(R.id.saveAs);
        shareAs = findViewById(R.id.shareAs);
        selectedSave = findViewById(R.id.selectedSave);
        selectedShare = findViewById(R.id.selectedShare);
        menuLayout = findViewById(R.id.menuLayout);
        menuButton = findViewById(R.id.menuButton);
        folderName = getIntent().getStringExtra("name");
        Log.d("MainActivity", "onCreate: " + folderName);

        mainLayout.setOnClickListener(v -> {
            if(shareAs.getVisibility() == View.VISIBLE){
                shareAs.setVisibility(View.GONE);
                Log.d("MainActivity", "share: ");

            }
            if(saveAs.getVisibility() == View.VISIBLE){
                saveAs.setVisibility(View.GONE);
                Log.d("MainActivity", "save: ");

            }
            if ( menuLayout.getVisibility() == View.VISIBLE) {
                menuLayout.setVisibility(View.GONE);

            };
            Log.d("MainActivity", "onCreate: ");
        });
        homeButton.setOnClickListener(v -> {
            finish();
        });
        selectedSave.setOnClickListener(v -> {
            saveAs.setVisibility(View.VISIBLE);
        });
        selectedShare.setOnClickListener(v -> {
            shareAs.setVisibility(View.VISIBLE);
        });

        selectedNavigation = findViewById(R.id.selectedNavigation);

        saveButton.setOnClickListener(v -> {
            saveAs.setVisibility(View.VISIBLE);
            shareAs.setVisibility(View.GONE);
        });
        shareButton.setOnClickListener(v -> {
            shareAs.setVisibility(View.VISIBLE);
            saveAs.setVisibility(View.GONE);
        });
        menuButton.setOnClickListener(v -> {
            if(menuLayout.getVisibility() == View.VISIBLE){
                menuLayout.setVisibility(View.GONE);
            }else{
                menuLayout.setVisibility(View.VISIBLE);
            }
            saveAs.setVisibility(View.GONE);
            shareAs.setVisibility(View.GONE);
        });

        selectedCancel.setOnClickListener(v -> {
            selectedImagesLayout.setVisibility(View.GONE);
            selectedNavigation.setVisibility(View.GONE);
            if(adapter != null){
                adapter.unselect();
            }
            if(shareAs.getVisibility() == View.VISIBLE){
                shareAs.setVisibility(View.GONE);

            }
            if(saveAs.getVisibility() == View.VISIBLE){
                saveAs.setVisibility(View.GONE);

            }
        });
        selectedDelete.setOnClickListener(v -> {
            selectedImagesLayout.setVisibility(View.GONE);
            selectedNavigation.setVisibility(View.GONE);
            if(adapter != null){
                adapter.list.removeAll(adapter.selectedList);
                adapter.notifyDataSetChanged();
                adapter.selectedList.clear();
                adapter.selection = false;
            }
            saveDraft();
        });
        if(images1 != null && !images1.isEmpty()){
            images.addAll(images1);
            images1.clear();
            adapter.notifyDataSetChanged();
        }
        scannerLauncher = registerForActivityResult(new StartIntentSenderForResult(), this::handleActivityResult);
    }


    public void onScanButtonClicked(View view) {

        GmsDocumentScannerOptions.Builder options = new GmsDocumentScannerOptions.Builder()
                .setResultFormats(
                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                       .setGalleryImportAllowed(true);

        options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL);
        options.setPageLimit(1);

        GmsDocumentScanning.getClient(options.build())
                .getStartScanIntent(this)
                .addOnSuccessListener(
                        intentSender ->
                                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(
                        e -> Log.e(TAG, "onScanButtonClicked: " + e.getMessage()));
    }


    @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode + " " + data);
            if (requestCode == 20 && save) {

                if(position != -1){
                    Log.d(TAG, "onActivityResult: bitmapApplied" );
                        adapter.updateImage(position, ImagePreview.currentBitmap);
                        adapter.notifyDataSetChanged();
                    }
                save = false;
                saveDraft();

            }


        }
    private void handleActivityResult(ActivityResult activityResult) {
        int resultCode = activityResult.getResultCode();
        lastScanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.getData());
        if (resultCode == Activity.RESULT_OK && lastScanResult != null) {
                Log.d(TAG, "handleActivityResult: " + lastScanResult.getPages().size());
            if (!lastScanResult.getPages().isEmpty()) {
                if (editImage) {
//                    images.set(editPosition, lastScanResult.getPages().get(0).getImageUri());
//                    adapter.notifyItemChanged(editPosition);
                    Intent intent = new Intent(this, ImagePreview.class);
                    intent.putExtra("uri", lastScanResult.getPages().get(0).getImageUri());
                    intent.putExtra("position", editPosition);
                    intent.putExtra("editImage", true);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, 20);
                    editImage = false;
                    editPosition = -1;

                } else {
                    for (int i = 0; i < lastScanResult.getPages().size(); i++) {
                        images.add(lastScanResult.getPages().get(i).getImageUri());
                    }
                    adapter.notifyDataSetChanged();
                    saveDraft();
                    Intent intent = new Intent(this, ImagePreview.class);
                    intent.putExtra("uri", lastScanResult.getPages().get(0).getImageUri());
                    intent.putExtra("position", images.size() - 1);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, 20);

                }
            }
            if (lastScanResult.getPdf() != null) {
                File file = new File(lastScanResult.getPdf().getUri().getPath());
                pdfUri = lastScanResult.getPdf().getUri();

            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if(editImage && imagePreview != null){
            }
        Log.d(TAG, "handleActivityResult: Canceled");
        } else {
            Log.d(TAG, "handleActivityResult: Failed");
        }
    }

    public void onSavePdfButtonClicked(View view) {
        if(!adapter.selection) {
            saveFiles.savePdfFile(images,this);
        }
        else{
            saveFiles.savePdfFile(adapter.selectedList,this);
        }
        saveAs.setVisibility(View.GONE);
    }
    public void onSharePdfButtonClicked(View view){
        if(!adapter.selection) {
            shareFiles.sharePdfFile(images,this);
        }
        else{
            shareFiles.sharePdfFile(adapter.selectedList,this);
        }
        shareAs.setVisibility(View.GONE);
    }
    public void EmptyClickListener(View view) {

    }
    public void onSaveImageButtonClicked(View view) {
        if(!adapter.selection) {
            for (int i = 0; i < images.size(); i++) {
                saveFiles.saveImageFile("image/jpeg", "saved_scanned_image " + System.currentTimeMillis() + ".jpg", images.get(i),this);
            }
        }
        else{
            for (int i = 0; i < adapter.selectedList.size(); i++) {
                saveFiles.saveImageFile("image/jpeg", "saved_scanned_image " + System.currentTimeMillis() + ".jpg", adapter.selectedList.get(i),this);
            }
        }
        saveAs.setVisibility(View.GONE);

    }
    public static MainActivity getInstance() {
        return instance;
    }

    public void onSaveDocButtonClicked(View view) {
        if(!adapter.selection) {
            saveFiles.saveDocFile(images,this);
        }
        else{
            saveFiles.saveDocFile(adapter.selectedList,this);

        }
        saveAs.setVisibility(View.GONE);
    }
    public void onShareDocButtonClicked(View view){
        if(!adapter.selection) {
           shareFiles.shareDocFile(images,this);
        }
        else{
            shareFiles.shareDocFile(adapter.selectedList,this);
        }
        shareAs.setVisibility(View.GONE);
    }



    public void onShareButtonClicked(View view) {
        ArrayList<Uri> imageUris = new ArrayList<>();

        if (!adapter.selection) {
            for (int i = 0; i < images.size(); i++) {
                Uri tempUri = createTemporaryFile(images.get(i), "shared_image_" + System.currentTimeMillis() + ".jpg");
                if (tempUri != null) {
                    imageUris.add(tempUri);
                }
            }
        } else {
            for (int i = 0; i < adapter.selectedList.size(); i++) {
                Uri tempUri = createTemporaryFile(adapter.selectedList.get(i), "shared_image_" + System.currentTimeMillis() + ".jpg");
                if (tempUri != null) {
                    imageUris.add(tempUri);
                }
            }
        }

        if (!imageUris.isEmpty()) {
            shareFiles.shareImages(imageUris,this);
        }
    }

    Uri createTemporaryFile(Uri sourceUri, String fileName) {
        try {
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(sourceUri);

            if (inputStream == null) {
                throw new IOException("Unable to open input stream from URI");
            }

            File tempFile = new File(getCacheDir(), fileName);
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                inputStream.close();
            }

            return FileProvider.getUriForFile(this, getPackageName() + ".provider", tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Tag", "Error creating temporary file: " + e.getMessage());
            return null;
        }
    }
    private void clearCache(Context context) {
        File cacheDir = context.getCacheDir();
        File externalCacheDir = context.getExternalCacheDir();
        if (cacheDir != null) {
            deleteRecursively(cacheDir);
        }
        if (externalCacheDir != null) {
            deleteRecursively(externalCacheDir);
        }
        Log.d(TAG, "Cleared cache: " + cacheDir.getAbsolutePath());
    }
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child); // Recursively delete subdirectories and files
                }
            }
        }

        if (file.delete()) {
            Log.d(TAG, "Deleted file: " + file.getAbsolutePath());
        } else {
            Log.d(TAG, "Failed to delete file: " + file.getAbsolutePath());
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();

          clearCache(this);

    }

    public void saveDraft(){
        if (images != null && !images.isEmpty()) {

            BitmapSaver bitmapSaver = new BitmapSaver(this);
            if(folderName != null  && !folderName.equals("")){
                bitmapSaver.saveBitmaps(images, folderName);
            }
            else {
                bitmapSaver.saveBitmaps(images, null);
            }
        }
    }

}
