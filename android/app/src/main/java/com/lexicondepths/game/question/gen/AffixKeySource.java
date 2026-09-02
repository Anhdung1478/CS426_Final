package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;

import java.util.List;
import java.util.Set;

/**
 * Where AffixHarvestGenerator gets its "valid words for this affix" answer key. Phase 2 answered
 * against the offline seed data alone; Phase 3 keeps that as the target and layers the Datamuse
 * `sp=` wildcard query on top as the accepted set — see DatamuseAffixKeySource.
 */
public interface AffixKeySource {

    /** The target set. Its size is how many words the player needs, so damage balance rides on it. */
    Set<String> wordsFor(Word word, List<Word> pool);

    /**
     * What counts as a hit. Defaults to the target — only DatamuseAffixKeySource widens it, and
     * see that class for why widening the target instead would silently break balance.
     */
    default Set<String> acceptedFor(Word word, List<Word> pool) {
        return wordsFor(word, pool);
    }
}
