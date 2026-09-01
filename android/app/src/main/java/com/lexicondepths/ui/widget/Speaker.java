package com.lexicondepths.ui.widget;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Thin wrapper around Android's TextToSpeech. Callers check isReady() before showing
 * a pronunciation button rather than crashing on missing voice data or init failure.
 */
public class Speaker {

    private final TextToSpeech tts;
    private volatile boolean ready;

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
        if (ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexicon_tts");
        }
    }

    public void shutdown() {
        tts.stop();
        tts.shutdown();
    }
}
