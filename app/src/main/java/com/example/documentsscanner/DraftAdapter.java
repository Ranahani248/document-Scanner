package com.example.documentsscanner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.DraftViewHolder> {

    private final List<SavedDraft> folderList;
    private final OnDraftClickListener listener;

    public interface OnDraftClickListener {
        void onDraftClick(String folderName);
    }

    public DraftAdapter(List<SavedDraft> folderList, OnDraftClickListener listener) {
        this.folderList = folderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DraftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draft, parent, false);
        return new DraftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DraftViewHolder holder, int position) {
        SavedDraft folder = folderList.get(position);
        holder.bind(folder, listener);
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class DraftViewHolder extends RecyclerView.ViewHolder {
        private final TextView draftName;

        public DraftViewHolder(@NonNull View itemView) {
            super(itemView);
            draftName = itemView.findViewById(R.id.draftName);
        }

        public void bind(SavedDraft draft, OnDraftClickListener listener) {
            draftName.setText(draft.getDraftName());
            itemView.setOnClickListener(v -> listener.onDraftClick(draft.getDraftName()));
        }
    }
}
