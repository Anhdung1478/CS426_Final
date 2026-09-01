package com.lexicondepths.ui.map;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivityPlaceholderBinding;

/** Placeholder shell for Phase 2. */
public class DungeonMapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPlaceholderBinding binding = ActivityPlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.titleText.setText(R.string.dungeon_map_title);
    }
}
