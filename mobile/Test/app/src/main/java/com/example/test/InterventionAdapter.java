package com.example.test;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.List;

public class InterventionAdapter extends ArrayAdapter<Intervention> {

    private SQLiteDatabase db;

    public InterventionAdapter(@NonNull Context context, @NonNull List<Intervention> interventions, SQLiteDatabase database) {
        super(context, 0, interventions);
        this.db = database;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_intervention, parent, false);
        }

        // Get the current intervention
        Intervention intervention = getItem(position);

        // Populate the views
        TextView itemTitle = convertView.findViewById(R.id.itemTitle);
        TextView itemName = convertView.findViewById(R.id.itemName);
        TextView itemAdress = convertView.findViewById(R.id.itemAdress);
        TextView itemTime = convertView.findViewById(R.id.itemTime);
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);

        itemTitle.setText(intervention.getTitle());
        itemName.setText(intervention.getClientName());
        itemAdress.setText(intervention.getClientAddress());
        String startTime = intervention.getStartTime() != null ? intervention.getStartTime() : "";
        String endTime = intervention.getEndTime() != null ? intervention.getEndTime() : "";

        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            itemTime.setText(startTime + " - " + endTime);
        } else if (!startTime.isEmpty()) {
            itemTime.setText(startTime); // Only start time is available
        } else if (!endTime.isEmpty()) {
            itemTime.setText(endTime); // Only end time is available
        } else {
            itemTime.setText("No time specified"); // Default message
        }
        checkBox.setChecked(intervention.isCompleted());

        // Add listener to handle checkbox state change
        checkBox.setOnClickListener(v -> {
            boolean isChecked = checkBox.isChecked();

            new AlertDialog.Builder(getContext())
                    .setTitle(isChecked ? "Confirmer l'intervention" : "Décocher")
                    .setMessage(isChecked ? "Marquer cette intervention comme terminée ?" : "Annuler la complétion ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        updateInterventionStatus(intervention.getId(), isChecked);
                        intervention.setCompleted(isChecked);
                        notifyDataSetChanged();
                    })
                    .setNegativeButton("Annuler", (dialog, which) -> checkBox.setChecked(!isChecked))
                    .show();
        });

        return convertView;
    }

    private void updateInterventionStatus(int interventionId, boolean isCompleted) {
        // Update SQLite database
        ContentValues values = new ContentValues();
        values.put("terminee", isCompleted ? 1 : 0);
        values.put("valsync", 1);
        db.update("interventions", values, "id = ?", new String[]{String.valueOf(interventionId)});

        // Prepare JSON payload for syncing with MySQL
        JSONObject payload = new JSONObject();
        try {
            payload.put("id", interventionId);
            payload.put("terminee", isCompleted ? 1 : 0);

            // Send request to MySQL server
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, "http://10.0.2.2/updateInterventionStatus.php", payload,
                    response -> {
                        try {
                            if (response.getString("status").equals("success")) {
                                // Reset valsync after successful sync
                                ContentValues syncReset = new ContentValues();
                                syncReset.put("valsync", 0);
                                db.update("interventions", syncReset, "id = ?", new String[]{String.valueOf(interventionId)});
                            } else {
                                Log.e("SYNC_ERROR", "Échec de la synchronisation");
                            }
                        } catch (Exception e) {
                            Log.e("SYNC_ERROR", "Erreur de parsing: " + e.getMessage());
                        }
                    },
                    error -> Log.e("SYNC_ERROR", "Erreur de synchronisation: " + error.toString()));
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
        } catch (Exception e) {
            Log.e("SYNC_ERROR", "Erreur de construction de payload: " + e.getMessage());
        }
    }
}
