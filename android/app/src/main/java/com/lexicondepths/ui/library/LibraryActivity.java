package com.lexicondepths.ui.library;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivityPlaceholderBinding;

/** Placeholder shell for Phase 3. */
public class LibraryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPlaceholderBinding binding = ActivityPlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.titleText.setText(R.string.library_title);
    }
}
