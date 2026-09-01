package com.lexicondepths.ui.widget;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

/**
 * Reveals text one character at a time. Static helper — no custom View needed.
 * The Handler lives on the target's tag so cancel() can find it without a caller-held token.
 */
public final class Typewriter {

    private Typewriter() {
    }

    public static void start(TextView target, String text, long charDelayMs) {
        cancel(target);
        Handler handler = new Handler(Looper.getMainLooper());
        target.setTag(handler);
        postChar(handler, target, text, 0, charDelayMs);
    }

    public static void cancel(TextView target) {
        Object tag = target.getTag();
        if (tag instanceof Handler) {
            ((Handler) tag).removeCallbacksAndMessages(null);
        }
    }

    private static void postChar(Handler handler, TextView target, String text, int index, long delay) {
        target.setText(text.subSequence(0, index));
        if (index < text.length()) {
            handler.postDelayed(() -> postChar(handler, target, text, index + 1, delay), delay);
        }
    }
}
