package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;

import java.util.ArrayList;
import java.util.List;

/** Test-only Word fixtures — shared by the generator tests in this package. */
final class TestWords {

    private TestWords() {
    }

    static Word word(long id, String headword, CefrLevel cefr, String topic, String definition, String example) {
        Word w = new Word();
        w.id = id;
        w.headword = headword;
        w.cefr = cefr;
        w.topic = topic;
        w.pos = "noun";
        w.definition = definition;
        w.example = example;
        w.synonyms = new ArrayList<>();
        w.antonyms = new ArrayList<>();
        w.collocations = new ArrayList<>();
        w.forms = new ArrayList<>();
        return w;
    }

    /** target plus fillerCount same-band/topic filler words, for distractor-pool tests. */
    static List<Word> poolAround(Word target, int fillerCount) {
        List<Word> pool = new ArrayList<>();
        pool.add(target);
        for (int i = 0; i < fillerCount; i++) {
            pool.add(word(1000L + i, "filler" + i, target.cefr, target.topic,
                    "filler definition " + i, "Filler example sentence " + i + "."));
        }
        return pool;
    }
}
