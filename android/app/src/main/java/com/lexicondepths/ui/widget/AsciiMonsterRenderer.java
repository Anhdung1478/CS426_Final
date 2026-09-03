package com.lexicondepths.ui.widget;

import android.widget.TextView;

import com.lexicondepths.R;
import com.lexicondepths.content.Monster;

public final class AsciiMonsterRenderer implements MonsterRenderer {
    @Override
    public void render(Monster monster, TextView target) {
        target.setText(String.join("\n", monster.ascii));
        // Read aloud, box-drawing characters are pure noise. Naming the monster is the whole
        // of what the art conveys.
        target.setContentDescription(
                target.getContext().getString(R.string.cd_monster_art, monster.name));
    }
}
