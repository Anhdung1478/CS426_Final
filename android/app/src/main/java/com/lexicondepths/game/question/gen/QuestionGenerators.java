package com.lexicondepths.game.question.gen;

import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionType;

import java.util.EnumMap;
import java.util.Map;

/** One instance per QuestionType — the lookup Encounter (P2-8) and the battle screen (P2-11) share. */
public final class QuestionGenerators {

    private static final Map<QuestionType, QuestionGenerator> ALL = build();

    private QuestionGenerators() {
    }

    public static QuestionGenerator forType(QuestionType type) {
        return ALL.get(type);
    }

    private static Map<QuestionType, QuestionGenerator> build() {
        Map<QuestionType, QuestionGenerator> map = new EnumMap<>(QuestionType.class);
        register(map, new DefinitionToWordGenerator());
        register(map, new WordToDefinitionGenerator());
        register(map, new SynonymAntonymGenerator());
        register(map, new WordFormGenerator());
        register(map, new ClozeGenerator());
        register(map, new CollocationGenerator());
        register(map, new AnagramGenerator());
        register(map, new SentenceScrambleGenerator());
        register(map, new WordleGenerator());
        // Datamuse in front, offline behind: a network failure degrades to the Phase 2 behaviour.
        register(map, new AffixHarvestGenerator(
                new DatamuseAffixKeySource(new OfflineAffixKeySource())));
        register(map, new ListeningSpellingGenerator());
        register(map, new RegisterFormalityGenerator());
        return map;
    }

    private static void register(Map<QuestionType, QuestionGenerator> map, QuestionGenerator generator) {
        map.put(generator.type(), generator);
    }
}
