package com.example.test;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoriqueAdapter extends RecyclerView.Adapter<HistoriqueAdapter.ViewHolder> {

    private final List<HistoriqueItem> historiqueList;

    public HistoriqueAdapter(List<HistoriqueItem> historiqueList) {
        this.historiqueList = historiqueList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historique, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoriqueItem item = historiqueList.get(position);
        holder.txtTerminee.setText("Terminé : " + item.getTerminee());
        holder.txtDate.setText(item.getDatePlanification());
        holder.txtCommentaire.setText(item.getCommentaire());
    }

    @Override
    public int getItemCount() {
        return historiqueList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTerminee, txtDate, txtCommentaire;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTerminee = itemView.findViewById(R.id.txtTerminee);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtCommentaire = itemView.findViewById(R.id.txtCommentaire);
        }
    }
}
