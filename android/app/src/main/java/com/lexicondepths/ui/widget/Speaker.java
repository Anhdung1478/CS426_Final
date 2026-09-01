package com.lexicondepths.ui.widget;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Thin wrapper around Android's TextToSpeech. Callers check isReady() before showing
 * a pronunciation button rather than crashing on missing voice data or init failure.
 *
 * Plays are capped at MAX_PLAYS_PER_QUESTION and reset via resetPlays() — callers that bind a
 * new question/card must call resetPlays() themselves, or the cap applies across their whole
 * session instead of per question.
 */
public class Speaker {

    private static final int MAX_PLAYS_PER_QUESTION = 3;

    private final TextToSpeech tts;
    private volatile boolean ready;
    private int playsUsed;

    public Speaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this::onInit);
    }

    private void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    public boolean isReady() {
        return ready;
    }

    public void speak(String text) {
        if (ready && playsUsed < MAX_PLAYS_PER_QUESTION) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexicon_tts");
            playsUsed++;
        }
    }

    public void resetPlays() {
        playsUsed = 0;
    }

    public int playsRemaining() {
        return Math.max(0, MAX_PLAYS_PER_QUESTION - playsUsed);
    }

    public void setSpeechRate(float rate) {
        tts.setSpeechRate(rate);
    }

    public void shutdown() {
        tts.stop();
        tts.shutdown();
    }
}
