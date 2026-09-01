package com.lexicondepths.ui.widget;

import android.widget.TextView;

import com.lexicondepths.content.Monster;

/**
 * The escape valve from the design doc §8: if someone who can draw sprites turns up later,
 * a bitmap renderer swaps in behind this interface without touching anything else.
 * AsciiMonsterRenderer is the only implementation for now.
 */
public interface MonsterRenderer {
    void render(Monster monster, TextView target);
}
