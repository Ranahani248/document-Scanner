package com.example.documentsscanner;

import static com.example.documentsscanner.ImagePreview.delete;
import static com.example.documentsscanner.ImagePreview.position;
import static com.example.documentsscanner.ImagePreview.retake;
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
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 112;
    private static final String BASE_MODE = "BASE";
    private static final String BASE_MODE_WITH_FILTER = "BASE_WITH_FILTER";
    private String selectedMode = FULL_MODE;
    private static MainActivity instance;
    ConstraintLayout selectedImagesLayout;
    ImageView  selectedCancel;
    private static final int MAX_WIDTH_DOC = 300;
    private static final int MAX_WIDTH_PDF = 600;
    private static final int MAX_HEIGHT_PDF = 800;
    private static final int MAX_HEIGHT_DOC = 400;
    TextView selectedImagesText, selectedDelete, homeButton, shareButton, saveButton, shareAsImage, shareAsPdf,shareAsWord,selectedSave,selectedShare;
    LinearLayout shareAs, saveAs, selectedNavigation;

    RecyclerView recyclerView;
    ScannedRecycleAdapter adapter;
    int editPosition = -1;



    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    boolean editImage = false;
    private Uri pdfUri;
    private GmsDocumentScanningResult lastScanResult;
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


        selectedSave.setOnClickListener(v -> {
            saveAs.setVisibility(View.VISIBLE);
        });
        selectedShare.setOnClickListener(v -> {
            shareAs.setVisibility(View.VISIBLE);
        });

        selectedNavigation = findViewById(R.id.selectedNavigation);

        saveButton.setOnClickListener(v -> {
            saveAs.setVisibility(View.VISIBLE);
        });
        shareButton.setOnClickListener(v -> {
            shareAs.setVisibility(View.VISIBLE);
        });


        selectedCancel.setOnClickListener(v -> {
            selectedImagesLayout.setVisibility(View.GONE);
            selectedNavigation.setVisibility(View.GONE);
            if(adapter != null){
                adapter.unselect();
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
        });

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
            }
            else if (requestCode == 20 && delete) {

                if(position != -1){
                    images.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, images.size());
                }
                delete = false;
            }
            else if (requestCode == 20 && retake) {

//                if(position != -1){
//                    activity.editPosition = i;
//                    activity.editImage = true;
//                    onScanButtonClicked(holder.change);
//                }
                retake = false;
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

                    Log.d(TAG, "adapter: " + adapter.getItemCount());
                }
            }
            if (lastScanResult.getPdf() != null) {
                File file = new File(lastScanResult.getPdf().getUri().getPath());
                pdfUri = lastScanResult.getPdf().getUri();

            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
        Log.d(TAG, "handleActivityResult: Canceled");
        } else {
            Log.d(TAG, "handleActivityResult: Failed");
        }
    }

    public void onSavePdfButtonClicked(View view) {
        if(!adapter.selection) {
            savePdfFile(images);
        }
        else{
            savePdfFile(adapter.selectedList);
        }
        saveAs.setVisibility(View.GONE);
    }
    public void onSharePdfButtonClicked(View view){
        if(!adapter.selection) {
            sharePdfFile(images);
        }
        else{
            sharePdfFile(adapter.selectedList);
        }
        shareAs.setVisibility(View.GONE);
    }
    public void EmptyClickListener(View view) {

    }
    public void onSaveImageButtonClicked(View view) {
        if(!adapter.selection) {
            for (int i = 0; i < images.size(); i++) {
                saveFile("image/jpeg", "saved_scanned_image " + System.currentTimeMillis() + ".jpg", images.get(i));
            }
        }
        else{
            for (int i = 0; i < adapter.selectedList.size(); i++) {
                saveFile("image/jpeg", "saved_scanned_image " + System.currentTimeMillis() + ".jpg", adapter.selectedList.get(i));
            }
        }
        saveAs.setVisibility(View.GONE);

    }
    public static MainActivity getInstance() {
        return instance;
    }

    public void onSaveDocButtonClicked(View view) {
        if(!adapter.selection) {
            saveDocFile(images);
        }
        else{
            saveDocFile(adapter.selectedList);
        }
        saveAs.setVisibility(View.GONE);
    }
    public void onShareDocButtonClicked(View view){
        if(!adapter.selection) {
            shareDocFile(images);
        }
        else{
            shareDocFile(adapter.selectedList);
        }
        shareAs.setVisibility(View.GONE);
    }
    private void savePdfFile(List<Uri> imageUris) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        if (!imageUris.isEmpty()) {
            try {
                PdfDocument document = new PdfDocument();
                ContentResolver contentResolver = getContentResolver();

                for (Uri imageUri : imageUris) {
                    // Handle potential null URIs
                    if (imageUri == null) {
                        continue; // Skip to next image
                    }

                    InputStream inputStream = contentResolver.openInputStream(imageUri);
                    if (inputStream == null) {
                        throw new IOException("Unable to open input stream from URI: " + imageUri);
                    }

                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();

                    // Scale the image down to fit within the maximum dimensions while maintaining the aspect ratio
                    if (width > height) {
                        float ratio = (float) width / height;
                        if (width > MAX_WIDTH_PDF) {
                            width = MAX_WIDTH_PDF;
                            height = (int) (width / ratio);
                        }
                        if (height > MAX_HEIGHT_PDF) {
                            height = MAX_HEIGHT_PDF;
                            width = (int) (height * ratio);
                        }
                    } else {
                        float ratio = (float) height / width;
                        if (height > MAX_HEIGHT_PDF) {
                            height = MAX_HEIGHT_PDF;
                            width = (int) (height / ratio);
                        }
                        if (width > MAX_WIDTH_PDF) {
                            width = MAX_WIDTH_PDF;
                            height = (int) (width * ratio);
                        }
                    }

                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, imageUris.indexOf(imageUri) + 1).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    page.getCanvas().drawBitmap(scaledBitmap, 0, 0, null);
                    document.finishPage(page);
                    inputStream.close();
                }

                // Save the document to storage
                Uri documentUri = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "saved_scanned_document.pdf");
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

                    documentUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                } else {
                    File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    if (!documentsFolder.exists()) {
                        documentsFolder.mkdirs();
                    }
                    File destFile = new File(documentsFolder, "saved_scanned_document.pdf");
                    documentUri = Uri.fromFile(destFile);
                }

                if (documentUri == null) {
                    throw new IOException("Unable to create destination file URI");
                }

                try (OutputStream outputStream = contentResolver.openOutputStream(documentUri)) {
                    document.writeTo(outputStream);
                } finally {
                    document.close();
                }

                Log.d(TAG, "Document saved to: " + documentUri.toString());
            } catch (IOException e) {
                e.printStackTrace();
               Log.d(TAG,"Error saving PDF: " + e.getMessage());
            }
        } else {
            Log.d(TAG,"No Files to save");
        }
    }
    private byte[] generatePdfByteArray(List<Uri> imageUris) {
        try {
            PdfDocument document = new PdfDocument();
            ContentResolver contentResolver = getContentResolver();

            for (Uri imageUri : imageUris) {
                // Handle potential null URIs
                if (imageUri == null) {
                    continue; // Skip to next image
                }

                InputStream inputStream = contentResolver.openInputStream(imageUri);
                if (inputStream == null) {
                    throw new IOException("Unable to open input stream from URI: " + imageUri);
                }

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                // Scale the image down to fit within the maximum dimensions while maintaining the aspect ratio
                if (width > height) {
                    float ratio = (float) width / height;
                    if (width > MAX_WIDTH_PDF) {
                        width = MAX_WIDTH_PDF;
                        height = (int) (width / ratio);
                    }
                    if (height > MAX_HEIGHT_PDF) {
                        height = MAX_HEIGHT_PDF;
                        width = (int) (height * ratio);
                    }
                } else {
                    float ratio = (float) height / width;
                    if (height > MAX_HEIGHT_PDF) {
                        height = MAX_HEIGHT_PDF;
                        width = (int) (height / ratio);
                    }
                    if (width > MAX_WIDTH_PDF) {
                        width = MAX_WIDTH_PDF;
                        height = (int) (width * ratio);
                    }
                }


                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);


                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, imageUris.indexOf(imageUri) + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                page.getCanvas().drawBitmap(scaledBitmap, 0, 0, null);
                document.finishPage(page);
                inputStream.close();
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.writeTo(byteArrayOutputStream);
            document.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    private void sharePdfFile(List<Uri> imageUris) {
        byte[] pdfByteArray = generatePdfByteArray(imageUris);
        if (pdfByteArray != null) {
            try {
                File cacheDir = new File(getCacheDir(), "pdfs");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File pdfFile = new File(cacheDir, "shared_scanned_document.pdf");
                try (OutputStream outputStream = new FileOutputStream(pdfFile)) {
                    outputStream.write(pdfByteArray);
                }

                Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share PDF using"));

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Error sharing PDF: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Error generating PDF for sharing");
        }
    }
     void saveFile(String mimeType, String fileName, Uri sourceUri) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

         if (sourceUri != null) {
             try {
                 ContentResolver contentResolver = getContentResolver();
                 InputStream inputStream = contentResolver.openInputStream(sourceUri);

                 if (inputStream == null) {
                     throw new IOException("Unable to open input stream from URI");
                 }

                 Uri documentUri = null;
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                     ContentValues contentValues = new ContentValues();
                     contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                     contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType); // e.g., "image/jpeg"
                     contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                     documentUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                 } else {
                     File picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                     if (!picturesFolder.exists()) {
                         picturesFolder.mkdirs();
                     }
                     File destFile = new File(picturesFolder, fileName);
                     documentUri = Uri.fromFile(destFile);
                 }

                 if (documentUri == null) {
                     throw new IOException("Unable to create destination file URI");
                 }

                 try (OutputStream outputStream = contentResolver.openOutputStream(documentUri)) {
                     byte[] buffer = new byte[1024];
                     int bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                         outputStream.write(buffer, 0, bytesRead);
                     }
                 } finally {
                     inputStream.close();
                 }

                 Log.d("Tag","File saved to: " + documentUri.getPath());
     } catch (IOException e) {
                e.printStackTrace();
                Log.d("tag","Error saving file: " + e.getMessage());
            }
        } else {
            Log.d("Tag","No file to save");
        }
    }

    private void saveDocFile(List<Uri> imageUris) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        if (!imageUris.isEmpty()) {
            try {
                XWPFDocument document = new XWPFDocument();

                ContentResolver contentResolver = getContentResolver();
                for (Uri imageUri : imageUris) {
                    // Handle potential null URIs
                    if (imageUri == null) {
                        continue; // Skip to next image
                    }

                    contentResolver = getContentResolver();
                    InputStream inputStream = contentResolver.openInputStream(imageUri);
                    if (inputStream == null) {
                        throw new IOException("Unable to open input stream from URI: " + imageUri);
                    }
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inputStream, null, options);
                    int originalWidth = options.outWidth;
                    int originalHeight = options.outHeight;
                    inputStream.close(); // Close the input stream and reopen it later

                    // Scale the image down to fit within the maximum dimensions while maintaining the aspect ratio
                    int width = originalWidth;
                    int height = originalHeight;
                    if (originalWidth > originalHeight) {
                        float ratio = (float) originalWidth / originalHeight;
                        if (originalWidth > MAX_WIDTH_DOC) {
                            width = MAX_WIDTH_DOC;
                            height = (int) (width / ratio);
                        }
                        if (height > MAX_HEIGHT_DOC) {
                            height = MAX_HEIGHT_DOC;
                            width = (int) (height * ratio);
                        }
                    } else {
                        float ratio = (float) originalHeight / originalWidth;
                        if (originalHeight > MAX_HEIGHT_DOC) {
                            height = MAX_HEIGHT_DOC;
                            width = (int) (height / ratio);
                        }
                        if (width > MAX_WIDTH_DOC) {
                            width = MAX_WIDTH_DOC;
                            height = (int) (width * ratio);
                        }
                    }
                    inputStream = contentResolver.openInputStream(imageUri);



                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.addCarriageReturn();

                    int pictureType = XWPFDocument.PICTURE_TYPE_JPEG; // Adjust according to your image type
                    String fileName = "scanned_image_" + imageUris.indexOf(imageUri) + ".jpg"; // Generate unique filename based on index

                    File tempFile = new File(getCacheDir(), fileName);
                    OutputStream out = new FileOutputStream(tempFile);
                    IOUtils.copy(inputStream, out);
                    out.close();
                    inputStream.close();

                    try (InputStream imageStream = new FileInputStream(tempFile)) {


                        document.createParagraph().createRun().addPicture(imageStream, pictureType, fileName, Units.toEMU(width), Units.toEMU(height));
                    } catch (InvalidFormatException e) {
                        throw new RuntimeException(e);
                    } finally {
                        tempFile.delete(); // Delete the temporary file (optional)
                    }
                }

                // Save the document to storage
                Uri documentUri = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "saved_scanned_document.docx");
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

                    documentUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                } else {
                    File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    if (!documentsFolder.exists()) {
                        documentsFolder.mkdirs();
                    }
                    File destFile = new File(documentsFolder, "saved_scanned_document.docx");
                    documentUri = Uri.fromFile(destFile);
                }

                if (documentUri == null) {
                    throw new IOException("Unable to create destination file URI");
                }

                try (OutputStream outputStream = contentResolver.openOutputStream(documentUri)) {
                    document.write(outputStream);
                } finally {
                    document.close();
                }

                Log.d("Tag","DOCX saved to: " + documentUri.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("Tag","Error saving DOCX: " + e.getMessage());
            }
        } else {
           Log.d(TAG,"No Files to save");
        }
    }

    private byte[] generateDocxByteArray(List<Uri> imageUris) {
        try {
            XWPFDocument document = new XWPFDocument();
            ContentResolver contentResolver = getContentResolver();

            for (Uri imageUri : imageUris) {
                // Handle potential null URIs
                if (imageUri == null) {
                    continue; // Skip to next image
                }

                InputStream inputStream = contentResolver.openInputStream(imageUri);
                if (inputStream == null) {
                    throw new IOException("Unable to open input stream from URI: " + imageUri);
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                int originalWidth = options.outWidth;
                int originalHeight = options.outHeight;
                inputStream.close(); // Close the input stream and reopen it later

                int width = originalWidth;
                int height = originalHeight;
                if (originalWidth > originalHeight) {
                    float ratio = (float) originalWidth / originalHeight;
                    if (originalWidth > MAX_WIDTH_DOC) {
                        width = MAX_WIDTH_DOC;
                        height = (int) (width / ratio);
                    }
                    if (height > MAX_HEIGHT_DOC) {
                        height = MAX_HEIGHT_DOC;
                        width = (int) (height * ratio);
                    }
                } else {
                    float ratio = (float) originalHeight / originalWidth;
                    if (originalHeight > MAX_HEIGHT_DOC) {
                        height = MAX_HEIGHT_DOC;
                        width = (int) (height / ratio);
                    }
                    if (width > MAX_WIDTH_DOC) {
                        width = MAX_WIDTH_DOC;
                        height = (int) (width * ratio);
                    }
                }
                inputStream = contentResolver.openInputStream(imageUri);

                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.addCarriageReturn();

                int pictureType = XWPFDocument.PICTURE_TYPE_JPEG; // Adjust according to your image type
                String fileName = "scanned_image_" + imageUris.indexOf(imageUri) + ".jpg"; // Generate unique filename based on index

                File tempFile = new File(getCacheDir(), fileName);
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    IOUtils.copy(inputStream, out);
                }
                inputStream.close();

                try (InputStream imageStream = new FileInputStream(tempFile)) {
                    document.createParagraph().createRun().addPicture(imageStream, pictureType, fileName, Units.toEMU(width), Units.toEMU(height));
                } catch (InvalidFormatException e) {
                    throw new RuntimeException(e);
                } finally {
                    tempFile.delete(); // Delete the temporary file (optional)
                }
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.write(byteArrayOutputStream);
            document.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error generating DOCX: " + e.getMessage());
            return null;
        }
    }

    private void shareDocFile(List<Uri> imageUris) {
        byte[] docxByteArray = generateDocxByteArray(imageUris);
        if (docxByteArray != null) {
            try {
                File cacheDir = new File(getCacheDir(), "docs");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File docxFile = new File(cacheDir, "shared_scanned_document.docx");
                try (OutputStream outputStream = new FileOutputStream(docxFile)) {
                    outputStream.write(docxByteArray);
                }

                Uri docxUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", docxFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                shareIntent.putExtra(Intent.EXTRA_STREAM, docxUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share DOCX using"));

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Error sharing DOCX: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Error generating DOCX for sharing");
        }
    }

    private void clearCache(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        clearCache(this);
    }
}
