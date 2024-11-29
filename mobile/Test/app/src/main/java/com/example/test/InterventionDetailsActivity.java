package com.example.test;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class InterventionDetailsActivity extends AppCompatActivity {

    private int interventionId;
    private SQLiteDatabase db;

    // UI elements
    private TextView txtNumIntervention, txtNameIntervention, txtTimeIntervention, priority, txtDateIntervention;
    private Switch switchCompleted;

    // Tabs and ViewPager2
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intervention_details);

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

        // Set up tabs and view pager
        setupTabs();

        // Load intervention details
        loadInterventionDetails();

        // Back button listener
        findViewById(R.id.btnback).setOnClickListener(v -> finish());

        // Delete button listener
        findViewById(R.id.btndelete).setOnClickListener(v -> deleteIntervention());

        // Edit button listener
        findViewById(R.id.btnedit).setOnClickListener(v -> {
            Intent intent = new Intent(InterventionDetailsActivity.this, EditInterventionActivity.class);
            intent.putExtra("interventionId", interventionId);
            startActivity(intent);
        });


        // Sync button listener
        findViewById(R.id.btnload).setOnClickListener(v -> {
            SyncHelper.syncData(this, db, () -> {
                // Reload intervention details after successful sync
                loadInterventionDetails();
                Toast.makeText(this, "Sync completed and details updated", Toast.LENGTH_SHORT).show();
            });
        });

        // Switch listener for marking completion
        switchCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> updateCompletionStatus(isChecked));
    }

    private void initializeUI() {
        txtNumIntervention = findViewById(R.id.txtnumIntervention);
        txtNameIntervention = findViewById(R.id.txtNameIntervention);
        txtTimeIntervention = findViewById(R.id.txtTimeIntervention);
        priority = findViewById(R.id.priority);
        txtDateIntervention = findViewById(R.id.txtDateIntervention);
        switchCompleted = findViewById(R.id.switchcompleted);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupTabs() {
        TabAdapter tabAdapter = new TabAdapter(this, interventionId, db);
        viewPager.setAdapter(tabAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Details");
                    break;
                case 1:
                    tab.setText("Files");
                    break;
                case 2:
                    tab.setText("Signature");
                    break;
                default:
                    tab.setText("Details");
            }
        }).attach();
    }

    private void loadInterventionDetails() {
        String query = "SELECT i.titre, i.heuredebuteffect, i.heurefineffect, i.dateplanification, " +
                "i.terminee, p.nom AS priority " +
                "FROM interventions i " +
                "JOIN priorites p ON i.priorite_id = p.id " +
                "WHERE i.id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(interventionId)});

        if (cursor.moveToFirst()) {
            String titre = getSafeString(cursor, "titre", "Unknown Title");
            String heureDebutEffect = getSafeString(cursor, "heuredebuteffect", "Unknown Start Time");
            String heureFinEffect = getSafeString(cursor, "heurefineffect", "Unknown End Time");
            String datePlanification = getSafeString(cursor, "dateplanification", "Unknown Date");
            String priorityValue = getSafeString(cursor, "priority", "No Priority");
            boolean isCompleted = getSafeInt(cursor, "terminee", 0) == 1;

            txtNumIntervention.setText("Intervention Numéro : " + interventionId);
            txtNameIntervention.setText(titre);
            txtTimeIntervention.setText(heureDebutEffect + " - " + heureFinEffect);
            txtDateIntervention.setText(formatDateToDayMonth(datePlanification));
            priority.setText(priorityValue);
            switchCompleted.setChecked(isCompleted);
        } else {
            Toast.makeText(this, "Intervention not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        cursor.close();
    }

    private String formatDateToDayMonth(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return parsedDate != null ? outputFormat.format(parsedDate) : "Invalid Date";
        } catch (ParseException e) {
            Log.e("DATE_FORMAT_ERROR", "Error formatting date: " + e.getMessage());
            return "Invalid Date";
        }
    }

    private void updateCompletionStatus(boolean isCompleted) {
        ContentValues values = new ContentValues();
        values.put("terminee", isCompleted ? 1 : 0);

        int rowsUpdated = db.update("interventions", values, "id = ?", new String[]{String.valueOf(interventionId)});
        if (rowsUpdated > 0) {
            syncCompletionStatusToMySQL(isCompleted);
        } else {
            Toast.makeText(this, "Failed to update status locally", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncCompletionStatusToMySQL(boolean isCompleted) {
        String url = "http://192.168.1.20/updateInterventionStatus.php";

        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject payload = new JSONObject();
        try {
            payload.put("id", interventionId);
            payload.put("terminee", isCompleted ? 1 : 0);
        } catch (Exception e) {
            Log.e("SYNC_ERROR", "Failed to create JSON payload: " + e.getMessage());
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            Toast.makeText(this, "Status synced with MySQL", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to sync with MySQL: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SYNC_ERROR", "Response parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR", "Failed to connect to server: " + error.getMessage());
                    Toast.makeText(this, "Failed to sync with MySQL", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    private void deleteIntervention() {
        int rowsDeleted = db.delete("interventions", "id = ?", new String[]{String.valueOf(interventionId)});
        if (rowsDeleted > 0) {
            syncDeleteWithMySQL(interventionId);
        } else {
            Toast.makeText(this, "Failed to delete intervention locally", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncDeleteWithMySQL(int interventionId) {
        String url = "http://192.168.1.20/deleteIntervention.php";

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", interventionId);
        } catch (Exception e) {
            Log.e("DELETE_SYNC_ERROR", "Failed to create JSON payload: " + e.getMessage());
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            Toast.makeText(this, "Intervention deleted successfully from MySQL", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to delete from MySQL: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("DELETE_SYNC_ERROR", "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("DELETE_SYNC_ERROR", "Volley error: " + error.getMessage());
                    Toast.makeText(this, "Failed to sync deletion with MySQL", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }



    public SQLiteDatabase getDatabase() {
        return db;
    }

    public int getInterventionId() {
        return interventionId;
    }

    // Utility methods to safely retrieve values from the cursor
    private String getSafeString(Cursor cursor, String columnName, String defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : defaultValue;
    }

    private int getSafeInt(Cursor cursor, String columnName, int defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getInt(columnIndex) : defaultValue;
    }
}
