package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

/** Serves Definition→Word, Word→Definition, Synonym/Antonym, Collocation, Word form. */
public final class McqView extends QuestionView {

    private final TextView promptText;
    private final LinearLayout optionsContainer;

    public McqView(Context context) {
        super(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        promptText = new TextView(context);
        optionsContainer = new LinearLayout(context);
        optionsContainer.setOrientation(LinearLayout.VERTICAL);

        root.addView(promptText);
        root.addView(optionsContainer);
        addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onBind(Question question) {
        promptText.setText(question.prompt);
        optionsContainer.removeAllViews();
        for (int i = 0; i < question.options.size(); i++) {
            int index = i;
            Button option = new Button(getContext());
            option.setText(question.options.get(i));
            option.setOnClickListener(v -> submit(Answer.ofOption(index, elapsedMillis())));
            optionsContainer.addView(option);
        }
    }
}
