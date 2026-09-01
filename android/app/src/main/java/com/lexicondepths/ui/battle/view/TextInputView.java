package com.lexicondepths.ui.battle.view;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lexicondepths.R;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionType;
import com.lexicondepths.ui.widget.Speaker;

/**
 * Serves Cloze, Listening→Spelling, Anagram — free-text entry. For Listening→Spelling only, a
 * Play button drives TTS via the optional Speaker (set by the host screen); a TTS init failure,
 * missing voice, or muted device just leaves the button disabled — the player still types and
 * submits normally, so the question degrades to skippable rather than crashing or soft-locking.
 */
public final class TextInputView extends QuestionView {

    private final TextView promptText;
    private final Button playButton;
    private final EditText input;
    private Speaker speaker;
    private String spokenText;

    public TextInputView(Context context) {
        super(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        promptText = new TextView(context);
        playButton = new Button(context);
        playButton.setVisibility(GONE);
        playButton.setOnClickListener(v -> playIfPossible());

        input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        Button submitButton = new Button(context);
        submitButton.setText(android.R.string.ok);
        submitButton.setOnClickListener(v -> submit(Answer.ofText(input.getText().toString(), elapsedMillis())));

        root.addView(promptText);
        root.addView(playButton);
        root.addView(input);
        root.addView(submitButton);
        addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Nullable — a screen with no TTS available simply never calls this. */
    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
    }

    @Override
    protected void onBind(Question question) {
        promptText.setText(question.prompt);
        input.setText("");

        boolean listening = question.type == QuestionType.LISTENING_SPELLING;
        if (!listening) {
            playButton.setVisibility(GONE);
            return;
        }

        spokenText = question.correctAnswer;
        playButton.setVisibility(VISIBLE);
        boolean ttsUsable = speaker != null && speaker.isReady();
        playButton.setEnabled(ttsUsable);
        playButton.setText(ttsUsable ? R.string.tts_play : R.string.tts_unavailable);
        if (ttsUsable) {
            speaker.resetPlays();
            playIfPossible();
        }
    }

    private void playIfPossible() {
        if (speaker == null || !speaker.isReady() || spokenText == null) {
            return;
        }
        speaker.speak(spokenText);
        int remaining = speaker.playsRemaining();
        playButton.setEnabled(remaining > 0);
        playButton.setText(remaining > 0
                ? getContext().getString(R.string.tts_play_again, remaining)
                : getContext().getString(R.string.tts_no_plays_left));
    }
}
