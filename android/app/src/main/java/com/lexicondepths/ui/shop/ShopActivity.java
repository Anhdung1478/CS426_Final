package com.lexicondepths.ui.shop;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.Relic;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivityShopBinding;
import com.lexicondepths.db.entity.Profile;

import java.util.List;

/**
 * The Marks sink. Until P4-8, SpoilsActivity wrote Marks and HubActivity displayed them and
 * nothing in the codebase read the field — "Marks are the permanent currency, spent at the Hub
 * on starting bonuses" (project-context.md §6) was simply false.
 *
 * Buying sets Profile.pendingRelicId; RunEngine.startRun turns it into the run's first RunRelic
 * and clears it. Nothing new happens in the damage path — all eight effects already exist as
 * switch branches from P2-9, which is the whole argument for this design over permanent stat
 * upgrades in the last phase of the project.
 *
 * Second reason the screen earns its place: it is where a player can read what the eight relics
 * do. Until now the only way to learn a relic was to take it blind at a reward screen.
 */
public class ShopActivity extends AppCompatActivity {

    private ActivityShopBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        reload();
    }

    private void reload() {
        App.get().io().execute(() -> {
            List<Relic> relics = RelicCatalog.load(getApplicationContext());
            Profile profile = App.get().db().profileDao().getProfileSync();
            int marks = profile == null ? 0 : profile.marks;
            String equipped = profile == null ? null : profile.pendingRelicId;
            runOnUiThread(() -> render(relics, marks, equipped));
        });
    }

    private void render(List<Relic> relics, int marks, String equippedId) {
        binding.marksText.setText(getString(R.string.shop_marks, marks));
        Relic equipped = findById(relics, equippedId);
        binding.equippedText.setText(equipped == null
                ? getString(R.string.shop_equipped_none)
                : getString(R.string.shop_equipped, equipped.name));

        binding.relicContainer.removeAllViews();
        for (Relic relic : relics) {
            addRelicRow(relic, marks, relic.id.equals(equippedId));
        }
    }

    private void addRelicRow(Relic relic, int marks, boolean isEquipped) {
        boolean affordable = marks >= relic.price;

        TextView description = new TextView(this);
        description.setTextAppearance(R.style.TextAppearance_Lexicon_Body);
        // Descriptions come from relics.json, never duplicated into strings.xml — one of them
        // would go stale and there would be no way to tell which.
        description.setText(getString(R.string.shop_relic_row, relic.name, relic.desc, relic.price));
        addToContainer(description);

        Button buy = new Button(this);
        buy.setSingleLine(false);
        if (isEquipped) {
            buy.setText(R.string.shop_buy_equipped);
            buy.setEnabled(false);
        } else if (affordable) {
            buy.setText(getString(R.string.shop_buy, relic.price));
            buy.setOnClickListener(v -> buy(relic));
        } else {
            // The reason is visible before the tap, not as a toast after it.
            buy.setText(getString(R.string.shop_buy_short, relic.price - marks));
            buy.setEnabled(false);
        }
        addToContainer(buy);
    }

    private void addToContainer(android.view.View view) {
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_sm);
        view.setLayoutParams(params);
        binding.relicContainer.addView(view);
    }

    private void buy(Relic relic) {
        App.get().io().execute(() -> {
            // One transaction in the DAO — deducting Marks and assigning the relic as two
            // Activity-side calls would let a crash between them take the Marks for nothing.
            boolean bought = App.get().db().profileDao().buyRelic(relic.id, relic.price);
            runOnUiThread(() -> {
                if (!bought) {
                    Toast.makeText(this, R.string.shop_buy_failed, Toast.LENGTH_SHORT).show();
                }
                reload();
            });
        });
    }

    private static Relic findById(List<Relic> relics, String id) {
        if (id == null) {
            return null;
        }
        for (Relic relic : relics) {
            if (relic.id.equals(id)) {
                return relic;
            }
        }
        return null;
    }
}
