package com.lexicondepths.ui.widget;

import android.animation.ObjectAnimator;
import android.view.View;

/** Screen shake on damage: an ObjectAnimator on translationX. Cancellable via the view's tag, like Typewriter. */
public final class Shake {

    private static final long DURATION_MS = 400;

    private Shake() {
    }

    public static void run(View target) {
        cancel(target);
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                target, "translationX", 0f, -24f, 24f, -18f, 18f, -10f, 10f, 0f);
        animator.setDuration(DURATION_MS);
        target.setTag(animator);
        animator.start();
    }

    public static void cancel(View target) {
        Object tag = target.getTag();
        if (tag instanceof ObjectAnimator) {
            ((ObjectAnimator) tag).cancel();
        }
    }
}
