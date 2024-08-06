package com.example.documentsscanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
 LinearLayout scanNewButton,scanIdButton;
    private static HomeActivity instance;
    DraftAdapter adapter;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        instance = this;

        scanNewButton = findViewById(R.id.scanNewButton);
        scanIdButton = findViewById(R.id.scanIdButton);

        recyclerView = findViewById(R.id.recycler_recent_documents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

         scanNewButton.setOnClickListener(v->{
         Intent intent = new Intent(this,MainActivity.class);
         startActivity(intent);
         });
         scanIdButton.setOnClickListener(v->{
         Intent intent = new Intent(this,IdCardActivity.class);
         startActivity(intent);
         });
        refreshAdapter();

    }
    public static HomeActivity getInstance() {
        if (instance == null) {
            Log.d("HomeActivity", "getInstance: ");
        }
        return instance;
    }
    public void refreshAdapter(){
        File appDirectory = getFilesDir(); // No need for "Drafts" folder
        File[] folders = appDirectory.listFiles();
        List<SavedDraft> folderList = new ArrayList<>();

        if (folders != null) {
            for (File folder : folders) {
                if (folder.isDirectory()) {
                    folderList.add(new SavedDraft(folder.getName()));
                }
            }
        }
        adapter = new DraftAdapter(folderList, folderName -> {
            File folderDir = new File(appDirectory, folderName);
            File[] files = folderDir.listFiles();
            List<Uri> imageUris = new ArrayList<>();

            if (files != null) {
                Log.d("Files", "Size: " + files.length);

                for (File file : files) {
                    Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                    imageUris.add(uri);

                }
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("name", folderName);
                MainActivity.images1 = imageUris;
                startActivity(intent);
            }

        });
        recyclerView.setAdapter(adapter);

    }
}