package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lexicondepths.R;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Serves Sentence scramble and Affix harvest — both build up an ordered list of tokens over
 * several taps/entries before one final submit. Sentence scramble: tap the scrambled option
 * chips (Question.options) back into order, auto-submitting once every chip is used. Affix
 * harvest: type a word and add it to the collected list, repeating until Done.
 */
public final class OrderingView extends QuestionView {

    private final TextView promptText;
    private final LinearLayout chipContainer;
    private final TextView collectedText;
    private final EditText freeInput;
    private final Button addButton;
    private final Button doneButton;

    private final List<String> collected = new ArrayList<>();
    private final List<String> remainingChips = new ArrayList<>();
    private boolean chipMode;

    public OrderingView(Context context) {
        super(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        promptText = new TextView(context);
        collectedText = new TextView(context);
        chipContainer = new LinearLayout(context);
        chipContainer.setOrientation(LinearLayout.HORIZONTAL);

        freeInput = new EditText(context);
        addButton = new Button(context);
        addButton.setText(R.string.add_word_label);
        addButton.setOnClickListener(v -> addTypedWord());

        doneButton = new Button(context);
        doneButton.setText(android.R.string.ok);
        doneButton.setOnClickListener(v -> finish());

        root.addView(promptText);
        root.addView(collectedText);
        root.addView(chipContainer);
        root.addView(freeInput);
        root.addView(addButton);
        root.addView(doneButton);
        addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onBind(Question question) {
        promptText.setText(question.prompt);
        collected.clear();
        collectedText.setText("");
        chipContainer.removeAllViews();

        chipMode = question.type == QuestionType.SENTENCE_SCRAMBLE;
        freeInput.setVisibility(chipMode ? GONE : VISIBLE);
        addButton.setVisibility(chipMode ? GONE : VISIBLE);
        doneButton.setVisibility(chipMode ? GONE : VISIBLE);
        chipContainer.setVisibility(chipMode ? VISIBLE : GONE);

        if (chipMode) {
            remainingChips.clear();
            remainingChips.addAll(question.options);
            renderChips();
        } else {
            freeInput.setText("");
        }
    }

    private void renderChips() {
        chipContainer.removeAllViews();
        for (String chip : remainingChips) {
            Button chipButton = new Button(getContext());
            chipButton.setText(chip);
            chipButton.setOnClickListener(v -> pickChip(chip));
            chipContainer.addView(chipButton);
        }
    }

    private void pickChip(String chip) {
        remainingChips.remove(chip);
        collected.add(chip);
        collectedText.setText(String.join(" ", collected));
        renderChips();
        if (remainingChips.isEmpty()) {
            submit(Answer.ofText(String.join(" ", collected), elapsedMillis()));
        }
    }

    private void addTypedWord() {
        String word = freeInput.getText().toString().trim();
        if (!word.isEmpty()) {
            collected.add(word);
            collectedText.setText(String.join(", ", collected));
            freeInput.setText("");
        }
    }

    private void finish() {
        submit(Answer.ofText(String.join(",", collected), elapsedMillis()));
    }
}
