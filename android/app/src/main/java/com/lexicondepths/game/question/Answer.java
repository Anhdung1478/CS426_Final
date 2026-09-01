package com.lexicondepths.game.question;

/**
 * The player's response to a Question. Exactly one of selectedOptionIndex/text applies,
 * matching whichever the question's type expects. elapsedMillis is captured by the input
 * view starting at bind(), not the first keystroke — see ui/battle/view/QuestionView (P2-7).
 */
public final class Answer {

    public final int selectedOptionIndex; // -1 when the question isn't MCQ
    public final String text;             // null when the question is MCQ
    public final long elapsedMillis;

    public static Answer ofOption(int selectedOptionIndex, long elapsedMillis) {
        return new Answer(selectedOptionIndex, null, elapsedMillis);
    }

    public static Answer ofText(String text, long elapsedMillis) {
        return new Answer(-1, text, elapsedMillis);
    }

    private Answer(int selectedOptionIndex, String text, long elapsedMillis) {
        this.selectedOptionIndex = selectedOptionIndex;
        this.text = text;
        this.elapsedMillis = elapsedMillis;
    }
}
