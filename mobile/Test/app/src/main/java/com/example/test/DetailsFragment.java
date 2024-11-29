package com.example.test;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DetailsFragment extends Fragment {

    private SQLiteDatabase db;
    private int interventionId;
    private int siteId; // Added to pass to HistoriqueActivity

    // UI elements
    private TextView txtDatePlanifie, txtHeurePlanifie, txtTempsTravailler, txtCommentairePlanifie, txtActionPlanifie;
    private TextView txtDateEffectue, txtHeureEffectue, txtCommentaireEffectuer, txtTermineEffectue, txtActionEffectue;
    private TextView txtNomComplaitClient, txtClientAddress, txtTelClient, txtEmailClient, txtfaxClient;
    private TextView txtContactClient, txtContactTelClient, TxtAdressClient, txtPlusInfoClient;
    private Button btnEnvoyerEmailClient, btnAdressClient, btnHistoriqueClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Access SQLiteDatabase and interventionId from parent activity
        if (getActivity() instanceof InterventionDetailsActivity) {
            db = ((InterventionDetailsActivity) getActivity()).getDatabase();
            interventionId = ((InterventionDetailsActivity) getActivity()).getInterventionId();
        }

        // Handle the case where interventionId or db is invalid
        if (db == null || interventionId == -1) {
            Toast.makeText(getContext(), "Invalid intervention ID or database access error", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_details, container, false);

        // Initialize UI elements
        initializeUI(rootView);

        // Load intervention details if the ID is valid
        if (interventionId != -1) {
            loadDetails();
        } else {
            displayErrorMessage();
        }

        return rootView;
    }

    private void initializeUI(View rootView) {
        // Planifié section
        txtDatePlanifie = rootView.findViewById(R.id.txtDatePlanifie);
        txtHeurePlanifie = rootView.findViewById(R.id.txtHeurePlanifie);
        txtTempsTravailler = rootView.findViewById(R.id.txtTempsTravailler);
        txtCommentairePlanifie = rootView.findViewById(R.id.txtCommentairePlanifie);
        txtActionPlanifie = rootView.findViewById(R.id.txtActionPlanifie);

        // Effectué section
        txtDateEffectue = rootView.findViewById(R.id.txtDateEffectue);
        txtHeureEffectue = rootView.findViewById(R.id.txtHeureEffectue);
        txtCommentaireEffectuer = rootView.findViewById(R.id.txtCommentaireEffectuer);
        txtTermineEffectue = rootView.findViewById(R.id.txtTermineEffectue);
        txtActionEffectue = rootView.findViewById(R.id.txtActionEffectue);

        // Client section
        txtNomComplaitClient = rootView.findViewById(R.id.txtNomComplaitClient);
        txtClientAddress = rootView.findViewById(R.id.adressClient);
        txtTelClient = rootView.findViewById(R.id.telClient);
        txtEmailClient = rootView.findViewById(R.id.EmailClient);
        txtfaxClient = rootView.findViewById(R.id.txtfaxClient);
        txtContactClient = rootView.findViewById(R.id.contactClient);
        txtContactTelClient = rootView.findViewById(R.id.contactTelClient);

        // Address and more info sections
        TxtAdressClient = rootView.findViewById(R.id.TxtAdressClient);
        txtPlusInfoClient = rootView.findViewById(R.id.txtPlusInfoClient);

        // Buttons
        btnEnvoyerEmailClient = rootView.findViewById(R.id.btnEnvoyerEmailClient);
        btnAdressClient = rootView.findViewById(R.id.btnAdressClient);
        btnHistoriqueClient = rootView.findViewById(R.id.btnHistoriqueClient);

        // Historique button click listener
        btnHistoriqueClient.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HistoriqueActivity.class);
            intent.putExtra("site_id", siteId); // Pass the site_id to HistoriqueActivity
            startActivity(intent);
        });
        btnAdressClient.setOnClickListener(this::onClick);

    }

    private void loadDetails() {
        if (db == null) {
            Toast.makeText(getContext(), "Database is not accessible", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = "SELECT i.dateplanification, i.heuredebutplan, i.heurefinplan, i.commentaires, " +
                "i.datedebut, i.datefin, i.dateterminaison, i.datevalidation, i.heurefineffect, " +
                "i.heuredebuteffect, i.terminee, s.id AS site_id, c.nom AS client_name, c.adresse AS client_address, " +
                "c.tel AS client_tel, c.email AS client_email, c.fax AS client_fax, " +
                "c.contact AS client_contact, c.telcontact AS client_contact_tel, " +
                "s.adresse AS site_address, s.rue AS site_rue, s.codepostal AS site_codepostal, s.ville AS site_ville " +
                "FROM interventions i " +
                "JOIN sites s ON i.site_id = s.id " +
                "JOIN clients c ON s.client_id = c.id " +
                "WHERE i.id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(interventionId)});

        if (cursor.moveToFirst()) {
            // Save siteId for HistoriqueActivity
            siteId = cursor.getInt(cursor.getColumnIndexOrThrow("site_id"));

            // Planifié fields
            txtDatePlanifie.setText(formatDates(cursor, "datedebut", "datefin"));
            txtHeurePlanifie.setText(formatTimes(cursor, "heuredebutplan", "heurefinplan"));
            txtTempsTravailler.setText(calculateWorkingHours(cursor, "heuredebuteffect", "heurefineffect"));
            txtCommentairePlanifie.setText(getSafeString(cursor, "commentaires"));
            txtActionPlanifie.setText("Action : Planifié"); // Static text for now

            // Effectué fields
            txtDateEffectue.setText(formatDates(cursor, "dateterminaison", "datevalidation"));
            txtHeureEffectue.setText(getSafeString(cursor, "heurefineffect"));
            txtCommentaireEffectuer.setText(getSafeString(cursor, "commentaires"));
            txtTermineEffectue.setText("Terminé : " + (getSafeInt(cursor, "terminee") == 1 ? "Oui" : "Non"));
            txtActionEffectue.setText("Action : Effectué"); // Static text for now

            // Address fields
            TxtAdressClient.setText(formatAddress(cursor, "site_address", "site_rue", "site_codepostal", "site_ville"));

            // Client fields
            txtNomComplaitClient.setText(getSafeString(cursor, "client_name"));
            txtClientAddress.setText(getSafeString(cursor, "client_address"));
            txtTelClient.setText(getSafeString(cursor, "client_tel"));
            txtEmailClient.setText(getSafeString(cursor, "client_email"));
            txtfaxClient.setText(getSafeString(cursor, "client_fax"));
            txtContactClient.setText(getSafeString(cursor, "client_contact"));
            txtContactTelClient.setText(getSafeString(cursor, "client_contact_tel"));
        } else {
            // Display an error if no details are found
            Toast.makeText(getContext(), "No details found for this intervention", Toast.LENGTH_SHORT).show();
            displayErrorMessage();
        }

        cursor.close();
    }

    private void displayErrorMessage() {
        txtDatePlanifie.setText("N/A");
        txtHeurePlanifie.setText("N/A");
        txtTempsTravailler.setText("N/A");
        txtCommentairePlanifie.setText("N/A");
        txtActionPlanifie.setText("N/A");

        txtDateEffectue.setText("N/A");
        txtHeureEffectue.setText("N/A");
        txtCommentaireEffectuer.setText("N/A");
        txtTermineEffectue.setText("Terminé : N/A");
        txtActionEffectue.setText("N/A");

        TxtAdressClient.setText("N/A");

        txtNomComplaitClient.setText("N/A");
        txtClientAddress.setText("N/A");
        txtTelClient.setText("N/A");
        txtEmailClient.setText("N/A");
        txtfaxClient.setText("N/A");
        txtContactClient.setText("N/A");
        txtContactTelClient.setText("N/A");

        Toast.makeText(getContext(), "Unable to load details. Invalid intervention ID.", Toast.LENGTH_LONG).show();
    }

    private String getSafeString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "N/A";
    }

    private int getSafeInt(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getInt(columnIndex) : 0;
    }

    private String formatDates(Cursor cursor, String startColumn, String endColumn) {
        return getSafeString(cursor, startColumn) + " - " + getSafeString(cursor, endColumn);
    }

    private String formatTimes(Cursor cursor, String startColumn, String endColumn) {
        return getSafeString(cursor, startColumn) + " - " + getSafeString(cursor, endColumn);
    }

    private String calculateWorkingHours(Cursor cursor, String startColumn, String endColumn) {
        String startTime = getSafeString(cursor, startColumn);
        String endTime = getSafeString(cursor, endColumn);

        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");
            int startHours = Integer.parseInt(startParts[0]);
            int startMinutes = Integer.parseInt(startParts[1]);
            int endHours = Integer.parseInt(endParts[0]);
            int endMinutes = Integer.parseInt(endParts[1]);

            int totalStartMinutes = startHours * 60 + startMinutes;
            int totalEndMinutes = endHours * 60 + endMinutes;

            int diffMinutes = totalEndMinutes - totalStartMinutes;

            return (diffMinutes / 60) + " heures " + (diffMinutes % 60) + " minutes";
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String formatAddress(Cursor cursor, String addressColumn, String streetColumn, String postalCodeColumn, String cityColumn) {
        String address = getSafeString(cursor, addressColumn);
        String street = getSafeString(cursor, streetColumn);
        String postalCode = getSafeString(cursor, postalCodeColumn);
        String city = getSafeString(cursor, cityColumn);

        return address + ", " + street + ", " + postalCode + ", " + city;
    }

    private void onClick(View v) {
        launchMap(interventionId);

    }
    private void launchMap(int interventionId) {
        String query = "SELECT s.longitude, s.latitude " +
                "FROM sites s " +
                "JOIN interventions i ON s.id = i.site_id " +
                "WHERE i.id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(interventionId)});

        if (cursor.moveToFirst()) {
            int longitudeIndex = cursor.getColumnIndex("longitude");
            int latitudeIndex = cursor.getColumnIndex("latitude");

            if (longitudeIndex != -1 && latitudeIndex != -1) {
                double longitude = cursor.getDouble(longitudeIndex);
                double latitude = cursor.getDouble(latitudeIndex);

                // Pass the coordinates to the map activity
                Intent intent = new Intent(getContext(), OSMMapActivity.class);
                intent.putExtra("longitude", longitude);
                intent.putExtra("latitude", latitude);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Longitude or Latitude column not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Coordinates not found for this intervention", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }


}
