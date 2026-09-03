package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

/** Serves Definition→Word, Word→Definition, Synonym/Antonym, Collocation, Word form, Register/formality. */
public final class McqView extends QuestionView {

    private static final float MIN_TOUCH_TARGET_DP = 48f;

    private final TextView promptText;
    private final LinearLayout optionsContainer;

    public McqView(Context context) {
        super(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        promptText = new TextView(context);
        promptText.setFocusable(true); // TalkBack should land on the question before the options
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
            option.setSingleLine(false); // a long definition must not be clipped to one line
            // 48dp, the platform minimum. A Button added in code does not inherit the layout
            // minimums the XML widgets get.
            option.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    MIN_TOUCH_TARGET_DP, getResources().getDisplayMetrics()));
            option.setOnClickListener(v -> submit(Answer.ofOption(index, elapsedMillis())));
            optionsContainer.addView(option);
        }
    }
}
