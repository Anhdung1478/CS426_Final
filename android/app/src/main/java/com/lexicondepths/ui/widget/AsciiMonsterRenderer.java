package com.lexicondepths.ui.widget;

import android.widget.TextView;

import com.lexicondepths.content.Monster;

public final class AsciiMonsterRenderer implements MonsterRenderer {
    @Override
    public void render(Monster monster, TextView target) {
        target.setText(String.join("\n", monster.ascii));
    }
}
