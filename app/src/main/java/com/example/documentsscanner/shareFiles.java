package com.example.documentsscanner;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

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

public class shareFiles {
    private static final int REQUEST_WRITE_STORAGE = 112 ;
    private static final int MAX_WIDTH_DOC = 300;
    private static final int MAX_WIDTH_PDF = 600;
    private static final int MAX_HEIGHT_PDF = 800;
    private static final int MAX_HEIGHT_DOC = 400;

    private static byte[] generatePdfByteArray(List<Uri> imageUris, Context context) {

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

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.writeTo(byteArrayOutputStream);
            document.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("TAG", "Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    static void sharePdfFile(List<Uri> imageUris, Context context) {
        byte[] pdfByteArray = generatePdfByteArray(imageUris,context);
        if (pdfByteArray != null) {
            try {
                File cacheDir = new File(context.getCacheDir(), "pdfs");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File pdfFile = new File(cacheDir, "shared_scanned_document.pdf");
                try (OutputStream outputStream = new FileOutputStream(pdfFile)) {
                    outputStream.write(pdfByteArray);
                }

                Uri pdfUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(shareIntent, "Share PDF using"));

            } catch (IOException e) {
                e.printStackTrace();
                Log.d("TAG", "Error sharing PDF: " + e.getMessage());
            }
        } else {
            Log.d("TAG", "Error generating PDF for sharing");
        }
    }

    private static byte[] generateDocxByteArray(List<Uri> imageUris, Context context) {
        try {
            XWPFDocument document = new XWPFDocument();
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

                File tempFile = new File(context.getCacheDir(), fileName);
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
            Log.d("TAG", "Error generating DOCX: " + e.getMessage());
            return null;
        }
    }

    static void shareDocFile(List<Uri> imageUris, Context context) {
        byte[] docxByteArray = generateDocxByteArray(imageUris,context);
        if (docxByteArray != null) {
            try {
                File cacheDir = new File(context.getCacheDir(), "docs");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File docxFile = new File(cacheDir, "shared_scanned_document.docx");
                try (OutputStream outputStream = new FileOutputStream(docxFile)) {
                    outputStream.write(docxByteArray);
                }

                Uri docxUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", docxFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                shareIntent.putExtra(Intent.EXTRA_STREAM, docxUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(shareIntent, "Share DOCX using"));

            } catch (IOException e) {
                e.printStackTrace();
                Log.d("TAG", "Error sharing DOCX: " + e.getMessage());
            }
        } else {
            Log.d("TAG", "Error generating DOCX for sharing");
        }
    }

     static void shareImages(ArrayList<Uri> imageUris, Context context) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        shareIntent.setType("image/jpeg");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share images to..."));
    }

}
