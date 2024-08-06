package com.example.documentsscanner;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class saveFiles {
    private static final int REQUEST_WRITE_STORAGE = 112 ;
    private static final int MAX_WIDTH_DOC = 300;
    private static final int MAX_WIDTH_PDF = 600;
    private static final int MAX_HEIGHT_PDF = 800;
    private static final int MAX_HEIGHT_DOC = 400;

    public static void saveDocFile(List<Uri> imageUris, Context context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Activity activity = (Activity) context;
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        if (!imageUris.isEmpty()) {
            try {
                XWPFDocument document = new XWPFDocument();

                ContentResolver contentResolver = context.getContentResolver();
                for (Uri imageUri : imageUris) {
                    // Handle potential null URIs
                    if (imageUri == null) {
                        continue; // Skip to next image
                    }

                    contentResolver = context.getContentResolver();
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

                    File tempFile = new File(context.getCacheDir(), fileName);
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
            Log.d("TAG","No Files to save");
        }
    }
    static void saveImageFile(String mimeType, String fileName, Uri sourceUri,Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Activity activity = (Activity) context;
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        if (sourceUri != null) {
            try {
                ContentResolver contentResolver = context.getContentResolver();
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
    static void savePdfFile(List<Uri> imageUris, Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Activity activity = (Activity) context;
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        if (!imageUris.isEmpty()) {
            try {
                PdfDocument document = new PdfDocument();
                ContentResolver contentResolver = context.getContentResolver();

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

                Log.d("TAG", "Document saved to: " + documentUri.toString());
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("TAG","Error saving PDF: " + e.getMessage());
            }
        } else {
            Log.d("TAG","No Files to save");
        }
    }


}
