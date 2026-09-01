package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Every seed word sharing the target word's affixKey, itself included. */
final class OfflineAffixKeySource implements AffixKeySource {

    @Override
    public Set<String> wordsFor(Word word, List<Word> pool) {
        Set<String> key = new TreeSet<>();
        key.add(word.headword.toLowerCase(Locale.ROOT));
        for (Word candidate : pool) {
            if (word.affixKey.equals(candidate.affixKey)) {
                key.add(candidate.headword.toLowerCase(Locale.ROOT));
            }
        }
        return key;
    }
}
