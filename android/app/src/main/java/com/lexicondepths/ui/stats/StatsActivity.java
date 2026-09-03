package com.lexicondepths.ui.stats;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.StatsLoader;
import com.lexicondepths.databinding.ActivityStatsBinding;
import com.lexicondepths.game.question.QuestionType;
import com.lexicondepths.game.stats.Achievements;
import com.lexicondepths.game.stats.StatsSnapshot;
import com.lexicondepths.game.stats.TypeAccuracy;
import com.lexicondepths.game.stats.WeakWord;

/**
 * The Phase 4 milestone. Every answer since P2-11 has been recorded and never read back; this
 * is the first screen that shows the player what they have actually learned.
 *
 * One StatsSnapshot, built once on App.io(), feeds all five sections — the same object the
 * achievements and the review reminder read, so nothing on screen can disagree with anything
 * off it.
 *
 * No chart library: a proportion bar is a LinearLayout whose children carry layout_weight,
 * which is how HpBar's segments already work. §2's approved dependency list has no room for
 * one and this needs about fifteen lines.
 */
public class StatsActivity extends AppCompatActivity {

    private ActivityStatsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        App.get().io().execute(() -> {
            StatsSnapshot snapshot = StatsLoader.load(App.get().db(), System.currentTimeMillis());
            runOnUiThread(() -> render(snapshot));
        });
    }

    private void render(StatsSnapshot snapshot) {
        renderMastery(snapshot);
        renderTypeAccuracy(snapshot);
        renderWeakWords(snapshot);
        renderRunRecord(snapshot);
        renderAchievements(snapshot);
    }

    private void renderMastery(StatsSnapshot snapshot) {
        binding.masteryCountsText.setText(getString(R.string.stats_mastery_counts,
                snapshot.newWords(), snapshot.learning, snapshot.mastered, snapshot.dueNow));

        binding.masteryBar.removeAllViews();
        addBarSegment(snapshot.newWords(), R.color.fg_dim);
        addBarSegment(snapshot.learning, R.color.warn);
        addBarSegment(snapshot.mastered, R.color.success);
        // An entirely empty bank would leave a zero-weight bar with nothing in it, which reads
        // as a rendering bug rather than as "no words yet".
        if (snapshot.totalWords == 0) {
            addBarSegment(1, R.color.surface);
        }
        binding.masteryBar.setContentDescription(binding.masteryCountsText.getText());
    }

    private void addBarSegment(int weight, int colorRes) {
        if (weight <= 0) {
            return;
        }
        View segment = new View(this);
        segment.setBackgroundColor(ContextCompat.getColor(this, colorRes));
        segment.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight));
        segment.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        binding.masteryBar.addView(segment);
    }

    /** Worst-first — the ORDER BY is in the query. Best-first would be a trophy case. */
    private void renderTypeAccuracy(StatsSnapshot snapshot) {
        binding.typeAccuracyContainer.removeAllViews();
        if (!snapshot.hasAnswerHistory() || snapshot.typeAccuracy.isEmpty()) {
            addLine(binding.typeAccuracyContainer, getString(R.string.stats_empty_no_answers),
                    R.style.TextAppearance_Lexicon_Dim);
            return;
        }
        for (TypeAccuracy row : snapshot.typeAccuracy) {
            addLine(binding.typeAccuracyContainer, getString(R.string.stats_type_accuracy_row,
                    getString(labelResForType(row.questionType)), row.percent(), row.attempts),
                    R.style.TextAppearance_Lexicon_Body);
        }
    }

    private void renderWeakWords(StatsSnapshot snapshot) {
        binding.weakWordsContainer.removeAllViews();
        if (snapshot.weakWords.isEmpty()) {
            addLine(binding.weakWordsContainer,
                    getString(snapshot.hasAnswerHistory()
                            ? R.string.stats_empty_not_enough_attempts
                            : R.string.stats_empty_no_answers),
                    R.style.TextAppearance_Lexicon_Dim);
            return;
        }
        for (WeakWord word : snapshot.weakWords) {
            addLine(binding.weakWordsContainer, getString(R.string.stats_weak_word_row,
                    word.headword, word.percent(), word.attempts),
                    R.style.TextAppearance_Lexicon_Body);
        }
    }

    private void renderRunRecord(StatsSnapshot snapshot) {
        binding.runRecordText.setText(getString(R.string.stats_run_record,
                snapshot.streak, snapshot.bestFloor, snapshot.runsWon, snapshot.totalRuns,
                winRatePercent(snapshot), snapshot.marks));
    }

    private static int winRatePercent(StatsSnapshot snapshot) {
        return snapshot.totalRuns == 0 ? 0 : Math.round(100f * snapshot.runsWon / snapshot.totalRuns);
    }

    /**
     * Locked and unlocked differ by glyph and text weight, never by color alone (§7), and a
     * locked row states its own unlock condition — a hidden requirement is not a goal.
     */
    private void renderAchievements(StatsSnapshot snapshot) {
        binding.achievementsContainer.removeAllViews();
        for (Achievements achievement : Achievements.values()) {
            boolean unlocked = achievement.isUnlockedBy(snapshot);
            String name = getString(nameRes(achievement));
            String line = unlocked
                    ? getString(R.string.stats_achievement_unlocked, name)
                    : getString(R.string.stats_achievement_locked, name, getString(requirementRes(achievement)));
            TextView view = addLine(binding.achievementsContainer, line,
                    unlocked ? R.style.TextAppearance_Lexicon_Body : R.style.TextAppearance_Lexicon_Dim);
            view.setTypeface(view.getTypeface(), unlocked ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
    }

    private TextView addLine(LinearLayout container, String text, int styleRes) {
        TextView view = new TextView(this);
        view.setTextAppearance(styleRes);
        view.setText(text);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_xs);
        view.setLayoutParams(params);
        container.addView(view);
        return view;
    }

    /**
     * Question-type labels are player-facing, so they live in strings.xml rather than being
     * the enum name with the underscores swapped out.
     */
    @StringRes
    private static int labelResForType(String questionType) {
        QuestionType type;
        try {
            type = QuestionType.valueOf(questionType);
        } catch (IllegalArgumentException unknown) {
            return R.string.qtype_unknown;
        }
        switch (type) {
            case DEFINITION_TO_WORD: return R.string.qtype_definition_to_word;
            case WORD_TO_DEFINITION: return R.string.qtype_word_to_definition;
            case SYNONYM_ANTONYM: return R.string.qtype_synonym_antonym;
            case WORD_FORM: return R.string.qtype_word_form;
            case CLOZE: return R.string.qtype_cloze;
            case COLLOCATION: return R.string.qtype_collocation;
            case ANAGRAM: return R.string.qtype_anagram;
            case SENTENCE_SCRAMBLE: return R.string.qtype_sentence_scramble;
            case WORDLE: return R.string.qtype_wordle;
            case AFFIX_HARVEST: return R.string.qtype_affix_harvest;
            case LISTENING_SPELLING: return R.string.qtype_listening_spelling;
            case REGISTER_FORMALITY: return R.string.qtype_register_formality;
            default: return R.string.qtype_unknown;
        }
    }

    @StringRes
    private static int nameRes(Achievements achievement) {
        switch (achievement) {
            case FIRST_DESCENT: return R.string.ach_first_descent;
            case DELVER: return R.string.ach_delver;
            case DEPTH_CONQUEROR: return R.string.ach_depth_conqueror;
            case WORD_HOARD: return R.string.ach_word_hoard;
            case LEXICOGRAPHER: return R.string.ach_lexicographer;
            case UNBROKEN: return R.string.ach_unbroken;
            case REALM_FORGER: return R.string.ach_realm_forger;
            case DILIGENT: return R.string.ach_diligent;
            default: return R.string.ach_silver_tongue;
        }
    }

    @StringRes
    private static int requirementRes(Achievements achievement) {
        switch (achievement) {
            case FIRST_DESCENT: return R.string.ach_first_descent_req;
            case DELVER: return R.string.ach_delver_req;
            case DEPTH_CONQUEROR: return R.string.ach_depth_conqueror_req;
            case WORD_HOARD: return R.string.ach_word_hoard_req;
            case LEXICOGRAPHER: return R.string.ach_lexicographer_req;
            case UNBROKEN: return R.string.ach_unbroken_req;
            case REALM_FORGER: return R.string.ach_realm_forger_req;
            case DILIGENT: return R.string.ach_diligent_req;
            default: return R.string.ach_silver_tongue_req;
        }
    }
}
