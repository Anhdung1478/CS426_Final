package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.lexicondepths.R;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.gen.WordleGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Serves Wordle. Feedback is never color-only: every tile carries a glyph too (✓ correct
 * position, ~ wrong position, ✗ absent) so grayscale display stays fully readable — see the
 * design doc's colorblind-safe callout.
 */
public final class WordleGridView extends QuestionView {

    private final TextView promptText;
    private final TextView historyText;
    private final EditText guessInput;

    private final List<String> guesses = new ArrayList<>();
    private String target;

    public WordleGridView(Context context) {
        super(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        promptText = new TextView(context);
        historyText = new TextView(context);
        guessInput = new EditText(context);

        Button submitButton = new Button(context);
        submitButton.setText(android.R.string.ok);
        submitButton.setOnClickListener(v -> submitGuess());

        root.addView(promptText);
        root.addView(historyText);
        root.addView(guessInput);
        root.addView(submitButton);
        addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onBind(Question question) {
        target = question.correctAnswer;
        guesses.clear();
        historyText.setText("");
        guessInput.setText("");
        guessInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(target.length())});
        promptText.setText(question.prompt + " (" + target.length() + " letters, "
                + WordleGenerator.MAX_GUESSES + " guesses)");
    }

    private void submitGuess() {
        String guess = guessInput.getText().toString().trim();
        if (guess.length() != target.length()) {
            return;
        }
        guesses.add(guess);
        appendFeedbackRow(guess);
        guessInput.setText("");

        boolean solved = guess.equalsIgnoreCase(target);
        if (solved || guesses.size() >= WordleGenerator.MAX_GUESSES) {
            submit(Answer.ofText(String.join(",", guesses), elapsedMillis()));
        }
    }

    private void appendFeedbackRow(String guess) {
        String upperGuess = guess.toUpperCase(Locale.ROOT);
        WordleGenerator.LetterState[] states = WordleGenerator.feedback(upperGuess, target.toUpperCase(Locale.ROOT));

        SpannableStringBuilder row = new SpannableStringBuilder();
        for (int i = 0; i < upperGuess.length(); i++) {
            String glyph;
            int colorRes;
            switch (states[i]) {
                case CORRECT:
                    glyph = "✓"; // check
                    colorRes = R.color.success;
                    break;
                case PRESENT:
                    glyph = "~";
                    colorRes = R.color.warn;
                    break;
                default:
                    glyph = "✗"; // cross
                    colorRes = R.color.fg_dim;
                    break;
            }
            String tile = upperGuess.charAt(i) + glyph + " ";
            int start = row.length();
            row.append(tile);
            row.setSpan(new BackgroundColorSpan(ContextCompat.getColor(getContext(), colorRes)),
                    start, start + tile.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        row.append("\n");
        historyText.append(row);
    }
}
