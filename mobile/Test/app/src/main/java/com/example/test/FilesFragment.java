package com.example.test;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class FilesFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private ArrayList<Bitmap> photoList;
    private PhotoAdapter photoAdapter;
    private SQLiteDatabase db;
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<Intent> selectPhotoLauncher;
    private int interventionId; // Store the intervention ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_files, container, false);

        db = requireActivity().openOrCreateDatabase("gem", Context.MODE_PRIVATE, null);

        // Get interventionId from arguments
        if (getArguments() != null) {
            interventionId = getArguments().getInt("intervention_id", -1);
        }

        // Initialize photo list and RecyclerView
        photoList = new ArrayList<>();
        RecyclerView rvPhotos = rootView.findViewById(R.id.rvPhotos);
        rvPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        photoAdapter = new PhotoAdapter(photoList);
        rvPhotos.setAdapter(photoAdapter);

        // Setup buttons
        rootView.findViewById(R.id.btnTakePhoto).setOnClickListener(v -> checkCameraPermission());
        rootView.findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> openGallery());

        // Register activity launchers
        setupActivityLaunchers();

        return rootView;
    }

    public static FilesFragment newInstance(int interventionId) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putInt("intervention_id", interventionId);
        fragment.setArguments(args);
        return fragment;
    }

    private void setupActivityLaunchers() {
        // Camera Intent Launcher
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        photoList.add(photo);
                        photoAdapter.notifyDataSetChanged();

                        // Save to database
                        saveImageToDatabase(photo, "Captured Photo");
                    }
                }
        );

        selectPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        try {
                            Bitmap photo = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImage);
                            photoList.add(photo);
                            photoAdapter.notifyDataSetChanged();

                            // Save to database
                            saveImageToDatabase(photo, "Gallery Photo");
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Failed to load photo", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            takePhotoLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent selectPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectPhotoIntent.setType("image/*");
        selectPhotoLauncher.launch(selectPhotoIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToDatabase(Bitmap bitmap, String imageName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            ContentValues values = new ContentValues();
            values.put("nom", imageName);
            values.put("img", imageBytes);
            values.put("dateCapture", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            values.put("intervention_id", interventionId); // Use interventionId from arguments
            values.put("valsync", 1); // Mark for syncing

            long result = db.insert("images", null, values);
            if (result != -1) {
                Toast.makeText(getContext(), "Image saved locally", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
