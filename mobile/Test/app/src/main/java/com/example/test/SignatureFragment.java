package com.example.test;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SignatureFragment extends Fragment {

    private SignatureView signatureView;
    private Button btnSave, btnClear;

    private static final int STORAGE_PERMISSION_CODE = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signature, container, false);

        signatureView = view.findViewById(R.id.signatureView);
        btnSave = view.findViewById(R.id.btnSaveSignature);
        btnClear = view.findViewById(R.id.btnClearSignature);

        // Set up button listeners
        btnSave.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
            } else {
                saveSignatureLocally();
            }
        });

        btnClear.setOnClickListener(v -> signatureView.clearSignature());

        return view;
    }

    private void saveSignatureLocally() {
        if (!signatureView.hasSignature()) {
            Toast.makeText(getContext(), "No signature to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap signatureBitmap = signatureView.getSignatureBitmap();

        File directory = new File(Environment.getExternalStorageDirectory() + "/Signatures");
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }

        String fileName = "signature_" + System.currentTimeMillis() + ".png";
        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            Toast.makeText(getContext(), "Signature saved at: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("SIGNATURE_SAVE", "Error saving signature: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to save signature", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(getContext(), "Storage permission is required to save the signature.", Toast.LENGTH_LONG).show();
        }

        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveSignatureLocally();
            } else {
                Toast.makeText(getContext(), "Permission denied. Unable to save signature.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
