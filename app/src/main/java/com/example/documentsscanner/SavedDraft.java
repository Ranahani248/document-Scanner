package com.example.documentsscanner;

public class SavedDraft {
    private String folderName;

    public SavedDraft(String folderName) {
        this.folderName = folderName;
    }

    public String getDraftName() {
        return folderName;
    }
}
