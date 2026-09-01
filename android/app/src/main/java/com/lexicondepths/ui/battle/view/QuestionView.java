package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.widget.FrameLayout;

import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

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
}
