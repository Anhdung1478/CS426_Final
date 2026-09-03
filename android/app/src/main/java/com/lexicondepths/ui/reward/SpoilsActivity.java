package com.lexicondepths.ui.reward;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivitySpoilsBinding;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Profile;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunRelic;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordEvent;
import com.lexicondepths.game.run.RunResult;
import com.lexicondepths.ui.hub.HubActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Phase 2 milestone screen. Reads every failed WordEvent for the run, resets those words'
 * dueAt to now (never touching ease/interval/reps/lapses — the permadeath boundary), writes
 * Marks/streak/bestFloor to Profile, and finally wipes the run-scoped tables. All of that runs
 * inside one Room transaction so a force-quit mid-processing can never leave Spoils half-applied
 * or a Run row leaked — winning and losing both route through this exact same cleanup.
 */
public class SpoilsActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_ID = "runId";

    private ActivitySpoilsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpoilsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        if (runId < 0) {
            finish();
            return;
        }

        binding.continueButton.setOnClickListener(v -> {
            startActivity(new Intent(this, HubActivity.class));
            finishAffinity();
        });

        process(runId);
    }

    private void process(long runId) {
        App.get().io().execute(() -> {
            AppDatabase db = App.get().db();
            Run run = db.runDao().getRun(runId);
            List<WordEvent> failed = db.wordEventDao().getFailedEventsForRun(runId);
            Set<String> relicIds = new HashSet<>();
            for (RunRelic relic : db.runDao().getRelicsForRun(runId)) {
                relicIds.add(relic.relicId);
            }
            RunResult result = RunResult.from(run, failed,
                    RelicCatalog.effectsFor(getApplicationContext(), relicIds));

            List<Word> missedWords = new ArrayList<>();
            for (Long wordId : result.failedWordIds) {
                Word word = db.wordDao().getById(wordId);
                if (word != null) {
                    missedWords.add(word);
                }
            }

            long now = System.currentTimeMillis();
            db.runInTransaction(() -> {
                for (Long wordId : result.failedWordIds) {
                    // ...OrCreate, not resetDueNow: a word first met in this battle has no
                    // WordProgress row, and a bare UPDATE would skip precisely the words the
                    // player just proved they do not know.
                    db.wordProgressDao().resetDueNowOrCreate(wordId, now);
                }

                Profile profile = db.profileDao().getProfileSync();
                if (profile == null) {
                    profile = new Profile();
                }
                profile.marks += result.marksEarned;
                profile.totalRuns += 1;
                profile.bestFloor = Math.max(profile.bestFloor, result.floorReached);
                // streak zeroes on a loss; runsWon must not, or "Depth Conqueror" would
                // re-lock itself the next time the player died. Same transaction as the rest.
                if (result.status == RunStatus.WON) {
                    profile.streak += 1;
                    profile.runsWon += 1;
                } else {
                    profile.streak = 0;
                }
                profile.lastActiveAt = now;
                db.profileDao().upsert(profile);

                db.runDao().clearRunState(runId);
            });

            runOnUiThread(() -> render(result, missedWords));
        });
    }

    private void render(RunResult result, List<Word> missedWords) {
        binding.resultText.setText(result.status == RunStatus.WON
                ? R.string.spoils_result_won : R.string.spoils_result_lost);
        binding.summaryText.setText(getString(R.string.spoils_summary, result.floorReached, result.marksEarned));

        if (missedWords.isEmpty()) {
            binding.missedWordsText.setText(R.string.spoils_missed_words_empty);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Word word : missedWords) {
            sb.append(word.headword).append(" — ").append(word.definition).append('\n');
        }
        binding.missedWordsText.setText(sb.toString().trim());
    }
}
