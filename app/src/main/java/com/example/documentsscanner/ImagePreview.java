package com.example.documentsscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.canhub.cropper.CropImageView;
import com.namangarg.androiddocumentscannerandfilter.DocumentFilter;

public class ImagePreview extends AppCompatActivity {
    Uri uri;
    SeekBar contrastSeekbar, brightnessSeekbar;
    ImageView imageView ,backButton, saveButton,normalImage, greyScaleImage, blackAndWhiteImage, lightenImage, magicImage, noShadowImage;
    ImageView cropButton;
    CropImageView cropImageView;
    Bitmap bitmap;
    boolean changeImage = false;
    Bitmap currentFilter;
    static boolean save = false;
    DocumentFilter documentFilter = new DocumentFilter();
    Button cancelCrop, saveCrop;
    static Bitmap currentBitmap, cropBitmap;
    Bitmap cropped;
    static int position = -1;
    float contrastProgress = 1.0f;
    float saturationProgress = 1.0f;
    float brightnessProgress = 0.0f;

    ConstraintLayout croplayout, normal, greyScale, blackAndWhite, lighten,magic, noShadow,selectedFilter;
    LinearLayout seekbarLayout;
    private static ImagePreview instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        instance = this;

        position = getIntent().getIntExtra("position", -1);
        uri = getIntent().getParcelableExtra("uri");
        changeImage = getIntent().getBooleanExtra("changeImage", false);
        imageView = findViewById(R.id.image);
        cropImageView = findViewById(R.id.cropImageView);
        cancelCrop = findViewById(R.id.cancelCrop);
        croplayout = findViewById(R.id.cropimageLayout);
        saveCrop = findViewById(R.id.saveCrop);
        cropButton = findViewById(R.id.cropButton);

        if (uri != null) {
            // Load the image into the ImageView and store the original bitmap
            Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .override(1024, 1024) // Set the maximum width and height
                    .into(new BitmapImageViewTarget(imageView) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                            super.onResourceReady(resource, transition);
                            bitmap = resource;
                            currentBitmap = resource;
                            currentFilter = bitmap;
                            cropBitmap = bitmap;
                            cropped = bitmap;
                        }
                    });

            // Load a downscaled image into the CropImageView
            cropImageView.setImageUriAsync(uri);
        }




        cropButton.setOnClickListener(v -> {
            croplayout.setVisibility(View.VISIBLE);
//            cropImageView.setGuidelines(CropImageView.Guidelines.ON);
            cropImageView.setImageBitmap(cropBitmap);

        });
        normalImage = findViewById(R.id.normal);
        greyScaleImage = findViewById(R.id.greyScale);
        blackAndWhiteImage = findViewById(R.id.blackAndWhite);
        lightenImage = findViewById(R.id.lighten);
        magicImage = findViewById(R.id.magic);
        noShadowImage = findViewById(R.id.noShadow);



        saveCrop.setOnClickListener(v -> {
             cropped = cropImageView.getCroppedImage();
            if (cropped != null) {
                applyFilter(1);
                imageView.setImageBitmap(cropped);
               currentBitmap = cropped;
                if(selectedFilter == greyScale){applyFilter(1);}
                if(selectedFilter == blackAndWhite){applyFilter(2);}
                if(selectedFilter == lighten){applyFilter(3);}
                if(selectedFilter == magic){applyFilter(4);}
                if(selectedFilter == noShadow){applyFilter(5);}
               croplayout.setVisibility(View.GONE);
            }
        });
        cancelCrop.setOnClickListener(v -> {
            croplayout.setVisibility(View.GONE);
        });
        croplayout.setOnClickListener(v -> {

        });

        seekbarLayout = findViewById(R.id.seekbarLayout);
        backButton  = findViewById(R.id.back);
        backButton.setOnClickListener(v ->{ onBackPressed(); save = false;});

        saveButton = findViewById(R.id.save);
        saveButton.setOnClickListener(v -> {
            save = true;

            if(changeImage) {
                MainActivity activity = MainActivity.getInstance();
                if (activity != null) {
                     activity.images.set(position, uri);
                      activity.adapter.notifyItemChanged(position);
                    finish();
                }
            }
            Log.d("ImagePreview", "currentFilter: " + currentFilter);
            onBackPressed();
        });


        ConstraintLayout constraintLayout = findViewById(R.id.main);
        constraintLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            // Apply insets to your ConstraintLayout
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            lp.topMargin = insets.getSystemWindowInsetTop();
            lp.bottomMargin = insets.getSystemWindowInsetBottom();
            v.setLayoutParams(lp);

            // Return the insets consumed to avoid system UI elements overlapping
            return insets.consumeSystemWindowInsets();
        });

        normal = findViewById(R.id.normalLayout);
        greyScale = findViewById(R.id.greyScaleLayout);
        blackAndWhite = findViewById(R.id.bwLayout);
        lighten = findViewById(R.id.lightenLayout);
        magic = findViewById(R.id.magicLayout);
        noShadow = findViewById(R.id.noShadowLayout);

        normal.setOnClickListener(onClickListener);
        greyScale.setOnClickListener(onClickListener);
        blackAndWhite.setOnClickListener(onClickListener);
        lighten.setOnClickListener(onClickListener);
        magic.setOnClickListener(onClickListener);
        noShadow.setOnClickListener(onClickListener);

        contrastSeekbar = findViewById(R.id.contrastSeekbar);
        contrastSeekbar.setMax(100);  // Range from 0 to 100
        contrastSeekbar.setProgress(50);  // Initial progress set to neutral state



        brightnessSeekbar = findViewById(R.id.brightnessSeekbar);
        brightnessSeekbar.setMax(200);  // Range from -100 to 100
        brightnessSeekbar.setProgress(100);  // Initial progress set to neutral state

        contrastSeekbar.setOnSeekBarChangeListener(seekBarChangeListener);
        brightnessSeekbar.setOnSeekBarChangeListener(seekBarChangeListener);
        applyImageOnFilters();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent resultIntent = new Intent();
                resultIntent.putExtra("position", position);
                setResult(RESULT_OK, resultIntent);

        finish();
    }
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == normal) {

                selectedFilter = normal;
                 applyFilter(0);

            } else if (v == greyScale) {

                selectedFilter = greyScale;
                applyFilter(1);
            } else if (v == blackAndWhite) {

                selectedFilter = blackAndWhite;
                applyFilter(2);
            } else if (v == lighten) {

                selectedFilter = lighten;
                applyFilter(3);
            }
            else if (v == magic) {
                if(selectedFilter == magic || selectedFilter == null) {
                    seekbarLayout.setVisibility(View.VISIBLE);
                }
                selectedFilter = magic;
                applyFilter(5);
            }
            else if (v == noShadow) {
                selectedFilter = noShadow;
                applyFilter(6);
            }
        }
    };

    private final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (currentFilter != null) {
                if (seekBar == contrastSeekbar) {
                    float contrast;
                    if (progress > 50) {
                        contrast = ((progress + 50) - 100) / 100f + 1;  // Increase contrast
                    } else {
                        contrast = (progress + 50) / 100f;  // Decrease contrast
                    }
                    contrastProgress = contrast;
                } else if (seekBar == brightnessSeekbar) {
                    float brightness = progress - 100;
                    brightnessProgress = brightness;
                }

                imageView.setImageBitmap(changeBitmapContrastSaturationBrightness(currentFilter, contrastProgress, saturationProgress, brightnessProgress));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }
    };

    private void applyFilter(int filterType) {
        if (cropBitmap != null) {
            ColorMatrix colorMatrix = new ColorMatrix();

            switch (filterType) {
                case 0: // Normal
                    currentFilter = cropped;
                    contrastProgress = 1.0f;
                    saturationProgress = 1.0f;
                    brightnessProgress = 0.0f;
                    break;
                case 1: // Grey Scale
                    colorMatrix.setSaturation(0);
                    currentFilter = applyColorMatrix(cropped, colorMatrix);
                    contrastProgress = 1.0f;
                    saturationProgress = 0.0f;
                    brightnessProgress = 0.0f;
                    break;
                case 2: // Black & White
                    ColorMatrix bwMatrix = new ColorMatrix(new float[]{
                            0.33f, 0.33f, 0.33f, 0, 0,
                            0.33f, 0.33f, 0.33f, 0, 0,
                            0.33f, 0.33f, 0.33f, 0, 0,
                            0, 0, 0, 1, 0
                    });
                    colorMatrix.postConcat(bwMatrix);
                    currentFilter = applyColorMatrix(cropped, colorMatrix);
                    contrastProgress = 1.0f;
                    saturationProgress = 0.0f;
                    brightnessProgress = 0.0f;
                    break;
                case 3: // Lighten
                    ColorMatrix lightenMatrix = new ColorMatrix(new float[]{
                            1, 0, 0, 0, 50,
                            0, 1, 0, 0, 50,
                            0, 0, 1, 0, 50,
                            0, 0, 0, 1, 0
                    });
                    colorMatrix.postConcat(lightenMatrix);
                    currentFilter = applyColorMatrix(cropped, colorMatrix);
                    contrastProgress = 1.0f;
                    saturationProgress = 1.0f;
                    brightnessProgress = 50.0f;
                    break;
                case 5: // Magic
                    documentFilter.getMagicFilter(cropped, bitmap -> {
                        contrastProgress = 1.2f;
                        saturationProgress = 1.0f;
                        brightnessProgress = 0.7f;
                        currentFilter = bitmap;
                        imageView.setImageBitmap(bitmap);
                        updateSeekBarValues();

                    });
                    break;
                case 6: // No Shadow
                    documentFilter.getShadowRemoval(cropped, bitmap -> {
                        contrastProgress = 1.0f;
                        saturationProgress = 1.0f;
                        brightnessProgress = 0.0f;
                        Log.d("ImagePreview", "currentFilter B: " + bitmap);
                        currentFilter = bitmap;
                        imageView.setImageBitmap(bitmap);
                        updateSeekBarValues();
                    });
                    break;
            }

            imageView.setImageBitmap(currentFilter);
            updateSeekBarValues();
        }
    }

    private void updateSeekBarValues() {
        contrastSeekbar.setProgress((int) (contrastProgress * 50)); // Assuming range is from 0 to 2 for contrast
        brightnessSeekbar.setProgress((int) (brightnessProgress + 100));
    }



    private Bitmap applyColorMatrix(Bitmap bmp, ColorMatrix colorMatrix) {
        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig() != null ? bmp.getConfig() : Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bmp, 0, 0, paint);
        currentBitmap = ret;
        return ret;
    }
    public void applyImageOnFilters(){
        Log.d("ImagePreview", "Received URI: " + uri);

                glideImage(normalImage, uri, bitmap -> {
                });

             glideImage(greyScaleImage, uri, bitmap -> {
                 if (bitmap != null) {
                     ColorMatrix greyScaleMatrix = new ColorMatrix();
                     greyScaleMatrix.setSaturation(0);
                     greyScaleImage.setImageBitmap(applyColorMatrix(bitmap, greyScaleMatrix));
                 } else {
                     Log.e("ImagePreview", "Failed to load bitmap from URI");
                 }
             });


                glideImage(blackAndWhiteImage, uri, bitmap -> {
                    if (bitmap != null) {
                        ColorMatrix bwMatrix = new ColorMatrix(new float[]{
                                0.33f, 0.33f, 0.33f, 0, 0,
                                0.33f, 0.33f, 0.33f, 0, 0,
                                0.33f, 0.33f, 0.33f, 0, 0,
                                0, 0, 0, 1, 0
                        });
                        blackAndWhiteImage.setImageBitmap(applyColorMatrix(bitmap, bwMatrix));
                    } else {
                        Log.e("ImagePreview", "Failed to load bitmap from URI");
                    }
                });

                glideImage(lightenImage, uri, bitmap -> {
                    if (bitmap != null) {
                        ColorMatrix lightenMatrix = new ColorMatrix(new float[]{
                                1, 0, 0, 0, 50,
                                0, 1, 0, 0, 50,
                                0, 0, 1, 0, 50,
                                0, 0, 0, 1, 0
                        });
                        lightenImage.setImageBitmap(applyColorMatrix(bitmap, lightenMatrix));
                    } else {
                        Log.e("ImagePreview", "Failed to load bitmap from URI");
                    }
                });


                    glideImage(magicImage, uri, bitmap -> {
                        if (bitmap != null) {
                            documentFilter.getMagicFilter(bitmap, bitmap1 -> {
                                magicImage.setImageBitmap(bitmap1);
                            });
                        } else {
                            Log.e("ImagePreview", "Failed to load bitmap from URI");
                        }
                    });


                    glideImage(noShadowImage, uri, bitmap -> {
                        if (bitmap != null) {
                            documentFilter.getShadowRemoval(bitmap, bitmap1 -> {
                                noShadowImage.setImageBitmap(bitmap1);
                            });
                        } else {
                            Log.e("ImagePreview", "Failed to load bitmap from URI");
                        }
                    });
    }
    private void glideImage(ImageView imageView, Uri imageUri, BitmapCallback callback) {
        if (imageUri != null) {
            Glide.with(this).asBitmap().load(imageUri).into(new BitmapImageViewTarget(imageView) {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                    super.onResourceReady(resource, transition);
                    // Call the callback with the loaded bitmap
                    if (callback != null) {
                        callback.onBitmapLoaded(resource);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onBitmapLoaded(null);
            }
        }
    }
    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
    }

    public Bitmap changeBitmapContrastSaturationBrightness(Bitmap bmp, float contrast, float saturation, float brightness) {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(saturation);

        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, 0,
                0, contrast, 0, 0, 0,
                0, 0, contrast, 0, 0,
                0, 0, 0, 1, 0
        });

        cm.postConcat(contrastMatrix);

        // Adjust brightness
        ColorMatrix brightnessMatrix = new ColorMatrix(new float[]{
                1, 0, 0, 0, brightness,
                0, 1, 0, 0, brightness,
                0, 0, 1, 0, brightness,
                0, 0, 0, 1, 0
        });

        cm.postConcat(brightnessMatrix);

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig() != null ? bmp.getConfig() : Bitmap.Config.ARGB_8888);

        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        currentBitmap = ret;
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    public static void finishImagePreview() {
        Log.d("TAG", "finishImagePreview: ");
        if (instance != null) {
            Log.d("TAG", "finishImagePreview: Done ");

            instance.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
