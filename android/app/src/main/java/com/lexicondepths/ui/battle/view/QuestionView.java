package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.widget.FrameLayout;

import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionType;

/**
 * Eleven question types collapse onto four subclasses (McqView, TextInputView, WordleGridView,
 * OrderingView) instead of one view per type — see phase-2.md P2-7. bind() is final so every
 * subclass gets answer timing for free, starting the moment the question is shown rather than
 * on the first keystroke; subclasses only implement onBind() and call submit() with the Answer
 * once the player responds.
 *
 * Deviation from the design doc's illustrative Listener.onAnswered(Answer, long): elapsedMillis
 * already lives on Answer itself (see Answer.java), so passing it a second time would just be
 * redundant — the listener here takes the Answer alone.
 */
public abstract class QuestionView extends FrameLayout {

    public interface Listener {
        void onAnswered(Answer answer);
    }

    private Listener listener;
    private long bindStartMillis;

    protected QuestionView(Context context) {
        super(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public final void bind(Question question) {
        bindStartMillis = System.currentTimeMillis();
        onBind(question);
    }

    protected abstract void onBind(Question question);

    protected long elapsedMillis() {
        return System.currentTimeMillis() - bindStartMillis;
    }

    protected void submit(Answer answer) {
        if (listener != null) {
            listener.onAnswered(answer);
        }
    }

    /** The eleven-types-onto-four-views mapping from phase-2.md's table, in one place. */
    public static QuestionView create(Context context, QuestionType type) {
        switch (type) {
            case DEFINITION_TO_WORD:
            case WORD_TO_DEFINITION:
            case SYNONYM_ANTONYM:
            case COLLOCATION:
            case WORD_FORM:
                return new McqView(context);
            case CLOZE:
            case LISTENING_SPELLING:
            case ANAGRAM:
                return new TextInputView(context);
            case WORDLE:
                return new WordleGridView(context);
            case SENTENCE_SCRAMBLE:
            case AFFIX_HARVEST:
                return new OrderingView(context);
            default:
                throw new IllegalArgumentException("No view family for " + type);
        }
    }
}
