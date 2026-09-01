package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scaffolding shared by the meaning (P2-3) and form/usage (P2-4) generators: same-band/topic
 * distractor pools, shuffled MCQ option lists, binary MCQ scoring, and finding the exact word
 * form that appears in a word's example sentence so a blank lands on a real grammatical slot
 * instead of an invented template.
 */
final class QuestionSupport {

    static final int OPTION_COUNT = 4;

    private QuestionSupport() {
    }

    /** Other words sharing word's CEFR band and topic — never the word itself. */
    static List<Word> sameBandTopic(Word word, List<Word> pool) {
        List<Word> matches = new ArrayList<>();
        for (Word candidate : pool) {
            if (candidate.id != word.id && candidate.cefr == word.cefr && candidate.topic.equals(word.topic)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    static List<String> headwords(List<Word> words) {
        List<String> result = new ArrayList<>();
        for (Word w : words) {
            result.add(w.headword);
        }
        return result;
    }

    static List<String> definitions(List<Word> words) {
        List<String> result = new ArrayList<>();
        for (Word w : words) {
            result.add(w.definition);
        }
        return result;
    }

    static List<String> collocations(List<Word> words) {
        List<String> result = new ArrayList<>();
        for (Word w : words) {
            if (w.collocations != null) {
                result.addAll(w.collocations);
            }
        }
        return result;
    }

    /** Builds a shuffled option list: correctText plus up to (OPTION_COUNT - 1) distinct distractors. */
    static McqOptions buildOptions(String correctText, List<String> distractorCandidates, Random rng) {
        List<String> deduped = new ArrayList<>(new LinkedHashSet<>(distractorCandidates));
        Collections.shuffle(deduped, rng);

        List<String> options = new ArrayList<>();
        options.add(correctText);
        for (String candidate : deduped) {
            if (options.size() >= OPTION_COUNT) {
                break;
            }
            if (!candidate.equals(correctText)) {
                options.add(candidate);
            }
        }
        Collections.shuffle(options, rng);
        return new McqOptions(options, options.indexOf(correctText));
    }

    static QuestionResult scoreMcq(Question question, Answer answer) {
        double ratio = answer.selectedOptionIndex == question.correctOptionIndex ? 1.0 : 0.0;
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }

    /**
     * Finds the one place in word.example where either the headword or one of its forms
     * appears as a whole word. Longest candidate is tried first so a specific inflected form
     * wins over a shorter one that happens to also match. Returns null — reject rather than
     * guess — when no candidate matches, or when the matching candidate appears more than
     * once (an ambiguous blank: which occurrence is "the" answer?).
     */
    static ExampleBlank findExampleBlank(Word word) {
        List<String> candidates = new ArrayList<>();
        candidates.add(word.headword);
        if (word.forms != null) {
            candidates.addAll(word.forms);
        }
        candidates.sort((a, b) -> b.length() - a.length());

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            Matcher matcher = Pattern
                    .compile("\\b" + Pattern.quote(candidate) + "\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(word.example);
            int start = -1;
            int end = -1;
            int count = 0;
            while (matcher.find()) {
                count++;
                if (count == 1) {
                    start = matcher.start();
                    end = matcher.end();
                }
            }
            if (count == 1) {
                return new ExampleBlank(candidate, start, end);
            }
            if (count > 1) {
                return null;
            }
        }
        return null;
    }

    static String blank(String example, ExampleBlank exampleBlank) {
        return example.substring(0, exampleBlank.start) + "____" + example.substring(exampleBlank.end);
    }

    static final class McqOptions {
        final List<String> options;
        final int correctIndex;

        McqOptions(List<String> options, int correctIndex) {
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    static final class ExampleBlank {
        final String candidate;
        final int start;
        final int end;

        ExampleBlank(String candidate, int start, int end) {
            this.candidate = candidate;
            this.start = start;
            this.end = end;
        }
    }
}
