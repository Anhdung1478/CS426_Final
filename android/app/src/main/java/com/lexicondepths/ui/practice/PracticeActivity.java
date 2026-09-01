package com.lexicondepths.ui.practice;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivityPracticeBinding;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordProgress;
import com.lexicondepths.game.srs.ReviewGrade;
import com.lexicondepths.game.srs.Sm2;
import com.lexicondepths.ui.widget.Speaker;

import java.util.List;

/**
 * The Phase 1 milestone: exercises the whole learning engine (WordDao queue + Sm2 + WordProgressDao)
 * with no HP, no permadeath, no timer — the same tables battles will use in Phase 2.
 */
public class PracticeActivity extends AppCompatActivity {

    private static final int QUEUE_LIMIT = 20;

    private ActivityPracticeBinding binding;
    private Speaker speaker;
    private List<Word> queue;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPracticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        speaker = new Speaker(this);

        binding.showAnswerButton.setOnClickListener(v -> showAnswer());
        binding.speakButton.setOnClickListener(v -> speaker.speak(currentWord().headword));
        binding.againButton.setOnClickListener(v -> rate(ReviewGrade.AGAIN));
        binding.hardButton.setOnClickListener(v -> rate(ReviewGrade.HARD));
        binding.goodButton.setOnClickListener(v -> rate(ReviewGrade.GOOD));
        binding.easyButton.setOnClickListener(v -> rate(ReviewGrade.EASY));

        loadQueue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speaker.shutdown();
    }

    private Word currentWord() {
        return queue.get(position);
    }

    private void loadQueue() {
        CefrLevel level = App.get().prefs().cefrLevel();
        long now = System.currentTimeMillis();
        App.get().io().execute(() -> {
            List<Word> result = App.get().db().wordDao().getQueue(now, level, QUEUE_LIMIT);
            runOnUiThread(() -> {
                queue = result;
                position = 0;
                showCard();
            });
        });
    }

    private void showCard() {
        if (queue == null || queue.isEmpty()) {
            binding.cardGroup.setVisibility(View.GONE);
            binding.emptyText.setText(R.string.practice_empty);
            binding.emptyText.setVisibility(View.VISIBLE);
            return;
        }
        if (position >= queue.size()) {
            binding.cardGroup.setVisibility(View.GONE);
            binding.emptyText.setText(R.string.practice_done);
            binding.emptyText.setVisibility(View.VISIBLE);
            return;
        }

        binding.emptyText.setVisibility(View.GONE);
        binding.cardGroup.setVisibility(View.VISIBLE);
        speaker.resetPlays();

        Word word = currentWord();
        binding.progressText.setText(getString(R.string.practice_progress, position + 1, queue.size()));
        binding.wordText.setText(word.headword);
        binding.definitionText.setText(word.definition);
        binding.exampleText.setText(word.example);

        boolean showGloss = word.viGloss != null && (word.cefr == CefrLevel.A1 || word.cefr == CefrLevel.A2);
        binding.glossText.setVisibility(showGloss ? View.VISIBLE : View.GONE);
        if (showGloss) {
            binding.glossText.setText(word.viGloss);
        }

        hideAnswer();
    }

    private void hideAnswer() {
        binding.answerGroup.setVisibility(View.GONE);
        binding.ratingRow.setVisibility(View.GONE);
        binding.showAnswerButton.setVisibility(View.VISIBLE);
    }

    private void showAnswer() {
        binding.answerGroup.setVisibility(View.VISIBLE);
        binding.ratingRow.setVisibility(View.VISIBLE);
        binding.showAnswerButton.setVisibility(View.GONE);
        binding.speakButton.setVisibility(speaker.isReady() ? View.VISIBLE : View.GONE);
    }

    private void rate(ReviewGrade grade) {
        Word word = currentWord();
        long now = System.currentTimeMillis();
        App.get().io().execute(() -> {
            WordProgress current = App.get().db().wordProgressDao().getByWordId(word.id);
            if (current == null) {
                current = new WordProgress();
                current.wordId = word.id;
            }
            WordProgress next = Sm2.apply(current, grade, now);
            App.get().db().wordProgressDao().upsert(next);
        });
        position++;
        showCard();
    }
}
