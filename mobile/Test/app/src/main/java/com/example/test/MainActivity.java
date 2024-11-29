package com.example.test;
import com.example.test.R;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private ListView lsvIntervention;
    private TextView txtDate;
    private ImageButton btnPreviousDay, btnNextDay, btnSync;
    private ArrayList<Intervention> interventionList;
    private int dayOffset = 0;
    private SQLiteDatabase db;
    private DrawerLayout drawerLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interventions);

        // Initialize Drawer Layout and Navigation View
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_interventions) {
                // Navigate to the Interventions Activity
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            } else if (itemId == R.id.menu_assign) {
                // Navigate to Assign Interventions Activity
                Toast.makeText(MainActivity.this, "Interventions à assigner clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.menu_messages) {
                // Navigate to Messages Activity
                Toast.makeText(MainActivity.this, "Messages clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.menu_client) {
                // Navigate to Client List Activity
                Toast.makeText(MainActivity.this, "Client clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.menu_addresses) {
                // Navigate to Addresses Activity
                Toast.makeText(MainActivity.this, "Adresses clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.menu_settings) {
                // Navigate to Settings Activity
                Toast.makeText(MainActivity.this, "Paramètres clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.menu_about) {
                // Show About information
                Toast.makeText(MainActivity.this, "À propos clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.menu_logout) {
                // Handle logout
                Toast.makeText(MainActivity.this, "Déconnexion clicked", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawers();
            return true;


        });


        // Open or create the database
        db = openOrCreateDatabase("gem", MODE_PRIVATE, null);

        // Initialize tables if not already created
        initializeDatabaseTables();
        db.execSQL("PRAGMA foreign_keys = ON;");

        // Initialize views
        lsvIntervention = findViewById(R.id.lsvIntervention);
        txtDate = findViewById(R.id.txtDate);
        btnPreviousDay = findViewById(R.id.btnpreviousday);
        btnNextDay = findViewById(R.id.btnnextday);
        btnSync = findViewById(R.id.btnSync);

        // Initialize data list
        interventionList = new ArrayList<>();

        // Date navigation listeners
        btnPreviousDay.setOnClickListener(v -> {
            dayOffset--;
            updateDateAndLoadInterventions();
        });

        btnNextDay.setOnClickListener(v -> {
            dayOffset++;
            updateDateAndLoadInterventions();
        });

        // Sync button listener
        btnSync.setOnClickListener(v -> syncData());

        // Load interventions for the current date
        updateDateAndLoadInterventions();
    }


    private void updateDateAndLoadInterventions() {
        String currentDate = getCurrentDateWithOffset(dayOffset);
        txtDate.setText(currentDate);
        loadInterventionsFromSQLite(currentDate);
    }

    private String getCurrentDateWithOffset(int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(calendar.getTime());
    }

    private void syncData() {
        String url = "http://192.168.1.20/getAllData.php"; // Change to your server's address

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            JSONObject tables = response.getJSONObject("tables");

                            db.beginTransaction();
                            try {
                                db.execSQL("DELETE FROM clients");
                                db.execSQL("DELETE FROM contrats");
                                db.execSQL("DELETE FROM employes");
                                db.execSQL("DELETE FROM employes_interventions");
                                db.execSQL("DELETE FROM interventions");
                                db.execSQL("DELETE FROM priorites");
                                db.execSQL("DELETE FROM sites");
                                db.execSQL("DELETE FROM taches");

                                // Insert new data
                                insertTableData(db, "clients", tables.getJSONArray("clients"));
                                insertTableData(db, "sites", tables.getJSONArray("sites"));
                                insertTableData(db, "interventions", tables.getJSONArray("interventions"));
                                insertTableData(db, "contrats", tables.getJSONArray("contrats"));
                                insertTableData(db, "employes_interventions", tables.getJSONArray("employes_interventions"));
                                insertTableData(db, "employes", tables.getJSONArray("employes"));
                                insertTableData(db, "priorites", tables.getJSONArray("priorites"));
                                insertTableData(db, "taches", tables.getJSONArray("taches"));
                                db.setTransactionSuccessful();
                            } finally {
                                db.endTransaction();
                            }
                            syncImages();
                            Toast.makeText(this, "Data synced successfully!", Toast.LENGTH_SHORT).show();
                            updateDateAndLoadInterventions();
                        } else {
                            Toast.makeText(this, "Failed to sync data", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SYNC_ERROR", "Error syncing data: " + e.toString());
                        Toast.makeText(this, "Error syncing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR", error.toString());
                    Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }


    private void insertTableData(SQLiteDatabase db, String tableName, JSONArray data) {
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.getJSONObject(i);

                ContentValues values = new ContentValues();
                Iterator<String> keys = row.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    values.put(key, row.optString(key, null)); // Use optString to handle missing keys
                }

                // For interventions, derive client_id from site_id
                if ("interventions".equals(tableName)) {
                    int siteId = row.optInt("site_id", -1);
                    if (siteId != -1) {
                        Cursor cursor = db.rawQuery("SELECT client_id FROM sites WHERE id = ?", new String[]{String.valueOf(siteId)});
                        if (cursor.moveToFirst()) {
                            @SuppressLint("Range") int clientId = cursor.getInt(cursor.getColumnIndex("client_id"));
                            values.put("client_id", clientId);
                        }
                        cursor.close();
                    }
                }

                long result = db.insert(tableName, null, values);
                if (result == -1) {
                    Log.e("DB_INSERT_ERROR", "Failed to insert into " + tableName + ": " + values.toString());
                }
            }
        } catch (Exception e) {
            Log.e("DB_INSERT_ERROR", "Error inserting data into " + tableName + ": " + e.toString());
        }
    }


    private void loadInterventionsFromSQLite(String date) {
        interventionList.clear();

        String query = "SELECT i.id, i.titre, i.heuredebutplan, i.heurefinplan, i.terminee, i.dateplanification, " +
                "c.nom AS client_name, c.adresse AS client_address " +
                "FROM interventions i " +
                "JOIN sites s ON i.site_id = s.id " +
                "JOIN clients c ON s.client_id = c.id " +
                "WHERE i.dateplanification = ?";

        Cursor cursor = db.rawQuery(query, new String[]{date});

        while (cursor.moveToNext()) {
            int idIndex = cursor.getColumnIndex("id");
            int titleIndex = cursor.getColumnIndex("titre");
            int clientNameIndex = cursor.getColumnIndex("client_name");
            int clientAddressIndex = cursor.getColumnIndex("client_address");
            int startTimeIndex = cursor.getColumnIndex("heuredebutplan");
            int endTimeIndex = cursor.getColumnIndex("heurefinplan");
            int completedIndex = cursor.getColumnIndex("terminee");

            // Retrieve values only if the index is valid
            int id = (idIndex != -1) ? cursor.getInt(idIndex) : -1; // Default to -1 if not found
            String title = (titleIndex != -1) ? cursor.getString(titleIndex) : "Unknown Title";
            String clientName = (clientNameIndex != -1) ? cursor.getString(clientNameIndex) : "Unknown Client";
            String clientAddress = (clientAddressIndex != -1) ? cursor.getString(clientAddressIndex) : "Unknown Address";
            String startTime = (startTimeIndex != -1) ? cursor.getString(startTimeIndex) : "Unknown Start Time";
            String endTime = (endTimeIndex != -1) ? cursor.getString(endTimeIndex) : "Unknown End Time";
            boolean completed = (completedIndex != -1) && cursor.getInt(completedIndex) == 1;

            // Add to intervention list
            interventionList.add(new Intervention(
                    id, // Include id here
                    title,
                    clientName,
                    clientAddress,
                    startTime,
                    endTime,
                    completed
            ));
        }
        lsvIntervention.setOnItemClickListener((parent, view, position, id1) -> {
            // Get the selected intervention
            Intervention selectedIntervention = interventionList.get(position);

            // Create intent to launch InterventionDetailsActivity
            Intent intent = new Intent(MainActivity.this, InterventionDetailsActivity.class);
            intent.putExtra("interventionId", selectedIntervention.getId());
            startActivity(intent);
        });







        InterventionAdapter adapter = new InterventionAdapter(this, interventionList, db);
        lsvIntervention.setAdapter(adapter);
    }


    private void initializeDatabaseTables() {

        db.execSQL("CREATE TABLE IF NOT EXISTS clients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "adresse TEXT NOT NULL, " +
                "tel TEXT NOT NULL, " +
                "fax TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "contact TEXT NOT NULL, " +
                "telcontact TEXT NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS contrats (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "datedebut DATE NOT NULL, " +
                "datefin DATE NOT NULL, " +
                "redevence REAL NOT NULL, " +
                "client_id INTEGER NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS employes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "login TEXT NOT NULL, " +
                "pwd TEXT NOT NULL, " +
                "prenom TEXT NOT NULL, " +
                "nom TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "actif BOOLEAN NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS employes_interventions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employe_id INTEGER NOT NULL, " +
                "intervention_id INTEGER NOT NULL);");

        db.execSQL("CREATE TABLE IF NOT EXISTS images (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "img BLOB NOT NULL, " +
                "dateCapture DATE NOT NULL, " +
                "intervention_id INTEGER NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS interventions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "titre TEXT NOT NULL, " +
                "datedebut DATE NOT NULL, " +
                "datefin DATE NOT NULL, " +
                "heuredebutplan TIME NOT NULL, " +
                "heurefinplan TIME NOT NULL, " +
                "commentaires TEXT NOT NULL, " +
                "dateplanification DATE NOT NULL, " +
                "heuredebuteffect TIME, " +
                "heurefineffect TIME, " +
                "terminee BOOLEAN NOT NULL, " +
                "dateterminaison DATE, " +
                "validee BOOLEAN NOT NULL, " +
                "datevalidation DATE, " +
                "priorite_id INTEGER NOT NULL, " +
                "site_id INTEGER NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS priorites (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS sites (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "longitude REAL NOT NULL, " +
                "latitude REAL NOT NULL, " +
                "adresse TEXT NOT NULL, " +
                "rue TEXT NOT NULL, " +
                "codepostal INTEGER NOT NULL, " +
                "ville TEXT NOT NULL, " +
                "contact TEXT NOT NULL, " +
                "telcontact TEXT NOT NULL, " +
                "client_id INTEGER NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE IF NOT EXISTS taches (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "refernce TEXT NOT NULL, " +
                "nom TEXT NOT NULL, " +
                "duree REAL NOT NULL, " +
                "prixheure REAL NOT NULL, " +
                "dateaction DATE NOT NULL, " +
                "intervention_id INTEGER NOT NULL, " +
                "valsync INTEGER DEFAULT 0);");
    }
    private void syncImages() {
        String url = "http://192.168.1.20/getImagesData.php"; // Replace with your server's IP or domain

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            JSONArray imagesArray = response.getJSONArray("data");

                            db.beginTransaction();
                            try {
                                db.execSQL("DELETE FROM images"); // Clear existing images

                                for (int i = 0; i < imagesArray.length(); i++) {
                                    JSONObject imageObj = imagesArray.getJSONObject(i);

                                    ContentValues values = new ContentValues();
                                    values.put("id", imageObj.getInt("id"));
                                    values.put("nom", imageObj.getString("nom"));
                                    values.put("img", imageObj.getString("img")); // Save as a string for now
                                    values.put("dateCapture", imageObj.getString("dateCapture"));
                                    values.put("intervention_id", imageObj.getInt("intervention_id"));
                                    values.put("valsync", imageObj.getInt("valsync"));

                                    db.insert("images", null, values);
                                }

                                db.setTransactionSuccessful();
                            } finally {
                                db.endTransaction();
                            }

                            Toast.makeText(this, "Images synced successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to sync images", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SYNC_ERROR", "Error syncing images: " + e.getMessage());
                    }
                },
                error -> Log.e("VOLLEY_ERROR", "Error connecting to server: " + error.toString())
        );

        queue.add(request);
    }


}