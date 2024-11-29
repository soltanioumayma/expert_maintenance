package com.example.test;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class SyncHelper {

    public static void syncData(Context context, SQLiteDatabase db, Runnable onComplete) {
        String url = "http://192.168.1.20/getAllData.php"; // Use your actual IP address

        RequestQueue queue = Volley.newRequestQueue(context);

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

                            Toast.makeText(context, "Data synced successfully!", Toast.LENGTH_SHORT).show();
                            if (onComplete != null) onComplete.run();
                        } else {
                            Toast.makeText(context, "Failed to sync data", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SYNC_ERROR", "Error syncing data: " + e.toString());
                        Toast.makeText(context, "Error syncing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR", error.toString());
                    Toast.makeText(context, "Error connecting to server", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    public static void syncUpdatedRecords(Context context, SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT * FROM interventions WHERE valsync = 1", null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex("id");
                int termineeIndex = cursor.getColumnIndex("terminee");
                int id = (idIndex != -1) ? cursor.getInt(idIndex) : -1; // Default to -1 if not found
                int terminee = (termineeIndex != -1) ? cursor.getInt(termineeIndex) : -1; // Default to -1 if not found
                JSONObject postData = new JSONObject();
                try {
                    postData.put("id", id);
                    postData.put("terminee", terminee);
                } catch (Exception e) {
                    Log.e("Sync Error", "Error preparing JSON payload for ID " + id + ": " + e.getMessage());
                }

                String url = "http://192.168.1.20/updateInterventionStatus.php"; // Use your actual IP address
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        postData,
                        response -> {
                            try {
                                if (response.getString("status").equals("success")) {
                                    // Reset valsync after successful sync
                                    ContentValues values = new ContentValues();
                                    values.put("valsync", 0);
                                    db.update("interventions", values, "id = ?", new String[]{String.valueOf(id)});
                                } else {
                                    Log.e("Sync Error", "Failed to sync ID " + id + ": " + response.getString("message"));
                                }
                            } catch (Exception e) {
                                Log.e("Sync Error", "Error parsing server response for ID " + id + ": " + e.getMessage());
                            }
                        },
                        error -> Log.e("Volley Error", "Error syncing ID " + id + ": " + error.toString())
                );

                RequestQueue queue = Volley.newRequestQueue(context);
                queue.add(request);
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    private static void insertTableData(SQLiteDatabase db, String tableName, JSONArray data) {
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.getJSONObject(i);

                ContentValues values = new ContentValues();
                Iterator<String> keys = row.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    values.put(key, row.optString(key, null)); // Use optString to handle missing keys
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
}
