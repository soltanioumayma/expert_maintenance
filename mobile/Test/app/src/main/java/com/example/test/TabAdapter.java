package com.example.test;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabAdapter extends FragmentStateAdapter {

    private final int interventionId;
    private final SQLiteDatabase db;

    public TabAdapter(@NonNull FragmentActivity fragmentActivity, int interventionId, SQLiteDatabase db) {
        super(fragmentActivity);
        this.interventionId = interventionId;
        this.db = db;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DetailsFragment(); // Pass details if needed
            case 1:
                return FilesFragment.newInstance(interventionId); // Pass the interventionId
            case 2:
                return new SignatureFragment(); // Placeholder for signature
            default:
                throw new IllegalStateException("Unexpected position: " + position);
        }
    }


    @Override
    public int getItemCount() {
        return 3;
    }
}
