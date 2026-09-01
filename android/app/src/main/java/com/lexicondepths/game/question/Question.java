package com.lexicondepths.game.question;

import java.util.Collections;
import java.util.List;

/**
 * One generated question. options is empty for free-text types (Cloze, Listening→Spelling,
 * Anagram, Wordle, Affix harvest); MCQ types fill it and set correctOptionIndex.
 * correctAnswer is always set — it's what free-text scoring compares against and what the
 * recap screen shows regardless of question shape.
 */
public final class Question {

    public final QuestionType type;
    public final long wordId;
    public final String prompt;
    public final List<String> options;
    public final int correctOptionIndex; // -1 when options is empty
    public final String correctAnswer;

    public Question(QuestionType type, long wordId, String prompt,
                     List<String> options, int correctOptionIndex, String correctAnswer) {
        this.type = type;
        this.wordId = wordId;
        this.prompt = prompt;
        this.options = options == null ? Collections.emptyList() : options;
        this.correctOptionIndex = correctOptionIndex;
        this.correctAnswer = correctAnswer;
    }
}
