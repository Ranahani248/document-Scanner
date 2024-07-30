package com.example.documentsscanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.datatransport.backend.cct.BuildConfig;

import org.apache.xmlbeans.impl.xb.xsdschema.Public;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL;

import kotlin.jvm.internal.Lambda;

public class ScannedRecycleAdapter extends RecyclerView.Adapter<ScannedRecycleAdapter.ViewHolder> {
    List<Uri> list;
    List<Uri> selectedList = new ArrayList<>();
    boolean selection = false;
    Context context;
     static final int IMAGE_PREVIEW_REQUEST_CODE = 20;

    ScannedRecycleAdapter(List<Uri> list, Context context) {
        this.list = list;
        this.context = context;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.scannedpreviewholder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ScannedRecycleAdapter.ViewHolder holder, int position) {
        int i = position;
        MainActivity activity = (MainActivity) context;
        holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.unselect));
        holder.itemView.setOnClickListener(v -> {
            if(!selection) {
                Intent intent = new Intent(context, ImagePreview.class);
                intent.putExtra("uri", list.get(i));
                intent.putExtra("position", i);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ((Activity) context).startActivityForResult(intent, 20);
            }
            else{
                if (selectedList.contains(list.get(i))) {
                    selectedList.remove(list.get(i));
                    holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.unselect));
                    if(selectedList.isEmpty()){
                        selection = false;
                        activity.selectedImagesLayout.setVisibility(View.GONE);
                        activity.selectedNavigation.setVisibility(View.GONE);

                    }
                } else {
                    selectedList.add(list.get(i));
                    holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.selection));

                }
                activity.selectedImagesText.setText(selectedList.size() + " image(s) selected");
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            selection = true;
            activity.selectedImagesLayout.setVisibility(View.VISIBLE);
            activity.selectedNavigation.setVisibility(View.VISIBLE);


            if (selectedList.contains(list.get(i))) {
                selectedList.remove(list.get(i));
                holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.unselect));
                if(selectedList.isEmpty()){
                    selection = false;
                    activity.selectedImagesLayout.setVisibility(View.GONE);
                    activity.selectedNavigation.setVisibility(View.GONE);

                }
            } else {
                selectedList.add(list.get(i));
                holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.selection));

            }
            activity.selectedImagesText.setText(selectedList.size() + " image(s) selected");

            return true;
        });

//        holder.delete.setOnClickListener(v -> {
//            list.remove(i);
//            notifyItemRemoved(i);
//            notifyItemRangeChanged(i, list.size());
//
//        });
//        holder.change.setOnClickListener(v -> {
//            activity.editPosition = i;
//            activity.editImage = true;
//           activity.onScanButtonClicked(holder.change);
//        });
//        holder.save.setOnClickListener(v -> {
//            activity.saveFile("image/jpeg", "saved_scanned_image " + System.currentTimeMillis() + ".jpg" , list.get(i));
//        });
        Uri uri = list.get(i);
        if (uri != null) {
            Glide.with(holder.image.getContext()).load(uri).into(holder.image);
            Log.d("ScannedRecycleAdapter", "onBindViewHolder: Loaded image from URI: " + uri.toString());
        } else {
            Log.e("ScannedRecycleAdapter", "onBindViewHolder: URI is null at position " + i);
        }

    }
    public void updateImage(int position, Bitmap bitmap) {
        if (position >= 0 && position < list.size()) {
            // Convert bitmap to URI
            Uri uri = getImageUri(context, bitmap);
            list.set(position, uri);
            notifyItemChanged(position);
        }
    }
    public void unselect() {
        for (Uri uri : selectedList) {
            int position = list.indexOf(uri);
            if (position != -1) {
                notifyItemChanged(position);
            }
        }
        selectedList.clear();
        selection = false;
    }
    private Uri getImageUri(Context context, Bitmap bitmap) {
        // Create a temporary file to store the bitmap
        File file = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
    }


    @Override
    public int getItemCount() {
        return list.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.scanned_page);



        }
    }

}
