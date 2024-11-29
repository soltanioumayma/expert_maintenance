package com.example.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EditInterventionActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private int interventionId;

    // UI elements
    private EditText edtTitle, edtStartTime, edtEndTime, edtDate;
    private Spinner spinnerPriority;
    private Button btnSave, btnCancel;

    private List<String> priorityList;
    private int selectedPriorityId = -1; // ID of the selected priority

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_intervention);

        // Initialize database
        db = openOrCreateDatabase("gem", MODE_PRIVATE, null);

        // Get intervention ID from Intent
        interventionId = getIntent().getIntExtra("interventionId", -1);

        if (interventionId == -1) {
            Toast.makeText(this, "Invalid intervention ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        initializeUI();

        // Load priorities into the Spinner
        loadPriorities();

        // Load intervention details into fields
        loadInterventionDetails();

        // Set up button listeners
        btnSave.setOnClickListener(v -> showConfirmationDialog());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initializeUI() {
        edtTitle = findViewById(R.id.edtTitle);
        edtStartTime = findViewById(R.id.edtStartTime);
        edtEndTime = findViewById(R.id.edtEndTime);
        edtDate = findViewById(R.id.edtDate);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        priorityList = new ArrayList<>();
    }

    private void loadPriorities() {
        Cursor cursor = db.rawQuery("SELECT id, nom FROM priorites", null);
        if (cursor.moveToFirst()) {
            do {
                priorityList.add(getSafeString(cursor, "nom", "Unknown Priority"));
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorityList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);
    }

    private void loadInterventionDetails() {
        String query = "SELECT titre, heuredebuteffect, heurefineffect, dateplanification, priorite_id " +
                "FROM interventions WHERE id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(interventionId)});

        if (cursor.moveToFirst()) {
            edtTitle.setText(getSafeString(cursor, "titre", ""));
            edtStartTime.setText(getSafeString(cursor, "heuredebuteffect", ""));
            edtEndTime.setText(getSafeString(cursor, "heurefineffect", ""));
            edtDate.setText(getSafeString(cursor, "dateplanification", ""));

            int priorityId = getSafeInt(cursor, "priorite_id", -1);
            if (priorityId > 0 && priorityId <= priorityList.size()) {
                spinnerPriority.setSelection(priorityId - 1); // Adjusting the selection to match the list
            }
            selectedPriorityId = priorityId;
        }
        cursor.close();
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer les modifications")
                .setMessage("Êtes-vous sûr de vouloir enregistrer les modifications ?")
                .setPositiveButton("Oui", (dialog, which) -> saveChanges())
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveChanges() {
        String title = edtTitle.getText().toString().trim();
        String startTime = edtStartTime.getText().toString().trim();
        String endTime = edtEndTime.getText().toString().trim();
        String date = edtDate.getText().toString().trim();
        String selectedPriority = (String) spinnerPriority.getSelectedItem();

        if (title.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || date.isEmpty() || selectedPriority.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = db.rawQuery("SELECT id FROM priorites WHERE nom = ?", new String[]{selectedPriority});
        if (cursor.moveToFirst()) {
            selectedPriorityId = getSafeInt(cursor, "id", -1);
        }
        cursor.close();

        if (selectedPriorityId == -1) {
            Toast.makeText(this, "Priorité invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("titre", title);
        values.put("heuredebuteffect", startTime);
        values.put("heurefineffect", endTime);
        values.put("dateplanification", date);
        values.put("priorite_id", selectedPriorityId);
        values.put("valsync", 1);

        int rowsUpdated = db.update("interventions", values, "id = ?", new String[]{String.valueOf(interventionId)});
        if (rowsUpdated > 0) {
            syncEditedInterventionWithMySQL();
            finish();
            Toast.makeText(this, "Intervention mise à jour avec succès", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Échec de la mise à jour de l'intervention", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncEditedInterventionWithMySQL() {
        Cursor cursor = db.rawQuery("SELECT * FROM interventions WHERE id = ?", new String[]{String.valueOf(interventionId)});
        if (cursor.moveToFirst()) {
            JSONObject payload = new JSONObject();
            try {
                payload.put("id", interventionId);
                payload.put("titre", getSafeString(cursor, "titre", ""));
                payload.put("heuredebuteffect", getSafeString(cursor, "heuredebuteffect", ""));
                payload.put("heurefineffect", getSafeString(cursor, "heurefineffect", ""));
                payload.put("dateplanification", getSafeString(cursor, "dateplanification", ""));
                payload.put("priorite_id", getSafeInt(cursor, "priorite_id", 0));

                RequestQueue queue = Volley.newRequestQueue(this);
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, "http://10.0.2.2/updateEditedIntervention.php", payload,
                        response -> {
                            try {
                                if (response.getString("status").equals("success")) {
                                    ContentValues values = new ContentValues();
                                    values.put("valsync", 0);
                                    db.update("interventions", values, "id = ?", new String[]{String.valueOf(interventionId)});
                                    Toast.makeText(this, "Synchronisation réussie avec MySQL", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Échec de la synchronisation", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e("SYNC_ERROR", "Erreur de réponse: " + e.getMessage());
                            }
                        },
                        error -> Log.e("SYNC_ERROR", "Échec de la synchronisation: " + error.toString())
                );
                queue.add(request);
            } catch (Exception e) {
                Log.e("SYNC_ERROR", "Erreur lors de la construction des données de synchronisation: " + e.getMessage());
            }
        }
        cursor.close();
    }


    private String getSafeString(Cursor cursor, String columnName, String defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : defaultValue;
    }

    private int getSafeInt(Cursor cursor, String columnName, int defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getInt(columnIndex) : defaultValue;
    }
}
