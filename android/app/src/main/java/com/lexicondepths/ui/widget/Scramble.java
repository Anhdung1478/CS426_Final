package com.lexicondepths.ui.widget;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.Random;

/** Random glyphs settling left-to-right into the target string. Static helper, same cancel pattern as Typewriter. */
public final class Scramble {

    private static final String GLYPHS = "!<>-_\\/[]{}=+*^?#$%&";
    private static final Random RANDOM = new Random();

    private Scramble() {
    }

    public static void start(TextView target, String finalText, long stepDelayMs) {
        cancel(target);
        Handler handler = new Handler(Looper.getMainLooper());
        target.setTag(handler);
        step(handler, target, finalText, 0, stepDelayMs);
    }

    public static void cancel(TextView target) {
        Object tag = target.getTag();
        if (tag instanceof Handler) {
            ((Handler) tag).removeCallbacksAndMessages(null);
        }
    }

    private static void step(Handler handler, TextView target, String finalText, int resolved, long delay) {
        StringBuilder sb = new StringBuilder(finalText.length());
        sb.append(finalText, 0, resolved);
        for (int i = resolved; i < finalText.length(); i++) {
            char c = finalText.charAt(i);
            sb.append(c == ' ' ? ' ' : GLYPHS.charAt(RANDOM.nextInt(GLYPHS.length())));
        }
        target.setText(sb);
        if (resolved < finalText.length()) {
            handler.postDelayed(() -> step(handler, target, finalText, resolved + 1, delay), delay);
        }
    }
}
