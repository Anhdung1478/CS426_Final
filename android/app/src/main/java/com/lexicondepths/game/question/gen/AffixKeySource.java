package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;

import java.util.List;
import java.util.Set;

/**
 * Where AffixHarvestGenerator gets its "valid words for this affix" answer key. Phase 2 answers
 * against the offline seed data (OfflineAffixKeySource); Phase 3 swaps in the Datamuse `sp=`
 * wildcard query behind this same interface, so that swap touches one class.
 */
interface AffixKeySource {
    Set<String> wordsFor(Word word, List<Word> pool);
}
