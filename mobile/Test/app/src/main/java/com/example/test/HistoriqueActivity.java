package com.example.test;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoriqueActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private int siteId;
    private RecyclerView recyclerView;
    private HistoriqueAdapter adapter;
    private List<HistoriqueItem> historiqueList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historique);

        // Initialize database and get site_id
        db = openOrCreateDatabase("gem", MODE_PRIVATE, null);
        siteId = getIntent().getIntExtra("site_id", -1);

        if (siteId == -1) {
            Toast.makeText(this, "Invalid site ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize RecyclerView and Back Button
        recyclerView = findViewById(R.id.recyclerViewHistorique);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historiqueList = new ArrayList<>();
        adapter = new HistoriqueAdapter(historiqueList);
        recyclerView.setAdapter(adapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Sync button listener
        findViewById(R.id.btnSync).setOnClickListener(v -> {
            SyncHelper.syncData(this, db, this::loadHistorique);
        });

        // Load data into the table
        loadHistorique();
    }

    private void loadHistorique() {
        historiqueList.clear(); // Clear the list to avoid duplicates
        String query = "SELECT terminee, dateplanification, commentaires FROM interventions WHERE site_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(siteId)});

        if (cursor.moveToFirst()) {
            do {
                String terminee = cursor.getInt(cursor.getColumnIndexOrThrow("terminee")) == 1 ? "Oui" : "Non";
                String datePlanification = cursor.getString(cursor.getColumnIndexOrThrow("dateplanification"));
                String commentaire = cursor.getString(cursor.getColumnIndexOrThrow("commentaires"));

                historiqueList.add(new HistoriqueItem(terminee, datePlanification, commentaire));
            } while (cursor.moveToNext());
        } else {
            Toast.makeText(this, "No historical data found", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        adapter.notifyDataSetChanged();
    }
}
