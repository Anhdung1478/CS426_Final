package com.lexicondepths.ui.onboarding;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivityOnboardingBinding;
import com.lexicondepths.ui.widget.Typewriter;

/**
 * Four pages explaining the three unfamiliar mechanics that stack in this app: Wordle letter
 * feedback, roguelike node navigation, and a combat model where the monster has no health bar.
 * None of the three is guessable, which is why project-idea.md §11 calls a cold demo the
 * problem this solves.
 *
 * No ViewPager2 — not on §2's approved list, and four pages of static text do not need a
 * pager. One layout, a page index, and Next / Back / Skip swapping the text, with Typewriter
 * (P1-11) doing the reveal.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String STATE_PAGE = "page";

    /** Page 2's last clause is the project's headline design claim; a player who does not know
     * it plays scared, which is the opposite of what the whole permadeath boundary buys. */
    private static final int[] TITLES = {
            R.string.onboarding_1_title,
            R.string.onboarding_2_title,
            R.string.onboarding_3_title,
            R.string.onboarding_4_title,
    };
    private static final int[] BODIES = {
            R.string.onboarding_1_body,
            R.string.onboarding_2_body,
            R.string.onboarding_3_body,
            R.string.onboarding_4_body,
    };
    private static final String[] ART = {
            "  .-\"\"\"-.\n /  ___  \\\n |  |A|  |\n \\  '-'  /\n  '-...-'",
            "   [B]  [R]\n     \\  /\n   [T]  [B]\n     \\  /\n   [E]  !!!",
            "  ( o.o )\n   > ^ <\n  ??  ??  ??",
            "  [ P ][ L ][ A ][ N ][ T ]\n    ✓    ~    ✗    ✓    ~",
    };

    private ActivityOnboardingBinding binding;
    private int page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Restored rather than reset: rotating or surviving a process kill mid-carousel must
        // not throw the reader back to page 1.
        page = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_PAGE, 0);

        binding.backButton.setOnClickListener(v -> show(page - 1));
        binding.nextButton.setOnClickListener(v -> {
            if (page + 1 < TITLES.length) {
                show(page + 1);
            } else {
                finishOnboarding();
            }
        });
        // Skipping on page 1 is a decision, same as finishing — re-showing this on the next
        // launch would be the app arguing with the user.
        binding.skipButton.setOnClickListener(v -> finishOnboarding());

        show(page);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, page);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Typewriter.cancel(binding.titleText);
    }

    private void show(int index) {
        page = Math.max(0, Math.min(TITLES.length - 1, index));
        binding.pageIndicatorText.setText(getString(R.string.onboarding_page, page + 1, TITLES.length));
        Typewriter.start(binding.titleText, getString(TITLES[page]), 25);
        binding.artText.setText(ART[page]);
        binding.bodyText.setText(getString(BODIES[page]));

        binding.backButton.setVisibility(page == 0 ? View.INVISIBLE : View.VISIBLE);
        binding.nextButton.setText(page == TITLES.length - 1
                ? R.string.onboarding_start : R.string.onboarding_next);
        binding.skipButton.setVisibility(page == TITLES.length - 1 ? View.GONE : View.VISIBLE);
    }

    private void finishOnboarding() {
        App.get().prefs().setOnboardingSeen();
        finish();
    }
}
