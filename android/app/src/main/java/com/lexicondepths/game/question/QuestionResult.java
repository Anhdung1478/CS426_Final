package com.lexicondepths.game.question;

/**
 * ratio is clamped to [0.0, 1.0] here, in the constructor, so no generator can hand the
 * damage formula (game/combat/Damage.java) a value outside that range.
 */
public final class QuestionResult {

    public final long wordId;
    public final double ratio;
    public final long elapsedMillis;
    public final String correctAnswer;

    public QuestionResult(long wordId, double ratio, long elapsedMillis, String correctAnswer) {
        this.wordId = wordId;
        this.ratio = Math.max(0.0, Math.min(1.0, ratio));
        this.elapsedMillis = elapsedMillis;
        this.correctAnswer = correctAnswer;
    }
}
