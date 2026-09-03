package com.lexicondepths.game.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.content.Monster;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.QuestionType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class EncounterTest {

    private static Word word(long id, String headword, String definition) {
        Word w = new Word();
        w.id = id;
        w.headword = headword;
        w.cefr = CefrLevel.B1;
        w.topic = "emotions";
        w.pos = "noun";
        w.definition = definition;
        w.example = headword + " example sentence.";
        w.synonyms = new ArrayList<>();
        w.antonyms = new ArrayList<>();
        w.collocations = new ArrayList<>();
        w.forms = new ArrayList<>();
        return w;
    }

    private static Monster monster(String id, List<String> types, int slots) {
        Monster m = new Monster();
        m.id = id;
        m.name = id;
        m.questionTypes = types;
        m.slots = slots;
        m.resists = Collections.emptyList();
        m.ascii = Collections.emptyList();
        return m;
    }

    /** Every real monster from assets/monsters.json builds a full encounter from a rich pool. */
    @Test
    public void everySingleTypeMonsterFromContentBuildsAValidEncounter() {
        List<Word> pool = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            pool.add(word(i, "word" + i, "definition " + i));
        }
        String[][] realMonsters = {
                {"hydra", "AFFIX_HARVEST", "3"},
                {"void_eater", "CLOZE", "2"},
                {"mimic", "WORD_FORM", "2"},
                {"sphinx", "DEFINITION_TO_WORD", "2"},
                {"cipher", "WORDLE", "3"},
                {"twins", "SYNONYM_ANTONYM", "2"},
                {"echo", "LISTENING_SPELLING", "2"},
                {"chimera", "COLLOCATION", "3"},
        };
        for (String[] spec : realMonsters) {
            Monster m = monster(spec[0], Collections.singletonList(spec[1]), Integer.parseInt(spec[2]));
            // WORDLE/LISTENING_SPELLING/AFFIX_HARVEST/CLOZE/DEFINITION_TO_WORD/SYNONYM_ANTONYM
            // all canGenerate off a bare headword+definition; word_form/collocation need extra
            // fields, so give the pool what those two specifically require.
            List<Word> tailoredPool = tailoredPool(spec[1]);
            Encounter encounter = Encounter.build(m, tailoredPool, Collections.emptySet(), new Random(1));
            assertEquals("monster " + spec[0] + " should fill every declared slot",
                    Integer.parseInt(spec[2]), encounter.slots.size());
            for (Encounter.Slot slot : encounter.slots) {
                assertEquals(QuestionType.valueOf(spec[1]), slot.type);
            }
        }
    }

    private static List<Word> tailoredPool(String typeName) {
        List<Word> pool = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Word w = word(i, "word" + i, "definition " + i);
            if (typeName.equals("WORD_FORM")) {
                w.forms = Arrays.asList("word" + i + "ly");
                w.example = "This is word" + i + " in a sentence.";
            }
            if (typeName.equals("COLLOCATION")) {
                w.collocations = Arrays.asList("make word" + i, "do word" + i);
            }
            if (typeName.equals("CLOZE")) {
                w.cefr = CefrLevel.B1;
                w.example = "This is word" + i + " in a sentence.";
            }
            if (typeName.equals("SYNONYM_ANTONYM")) {
                w.synonyms = Arrays.asList("syn" + i);
            }
            if (typeName.equals("AFFIX_HARVEST")) {
                w.affixKey = "un-";
            }
            if (typeName.equals("REGISTER_FORMALITY")) {
                w.formalAlt = "formal" + i;
                w.example = "This is word" + i + " in a sentence.";
            }
            pool.add(w);
        }
        return pool;
    }

    /**
     * P4-10's first designed-against failure mode. Encounter.pickSlot drops a slot it cannot
     * fill and falls back only to the monster's *other* declared types, so a monster declaring
     * REGISTER_FORMALITY alone would build a zero-slot encounter in any topic whose pool has no
     * formalAlt words. The Courtier declares a second type precisely so that cannot happen.
     */
    @Test
    public void theCourtierNeverProducesAZeroSlotEncounter() {
        Monster courtier = monster("courtier",
                Arrays.asList("REGISTER_FORMALITY", "SYNONYM_ANTONYM"), 2);

        List<Word> withFormalAlts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Word w = word(i, "word" + i, "definition " + i);
            w.formalAlt = "formal" + i;
            w.synonyms = Arrays.asList("syn" + i);
            withFormalAlts.add(w);
        }
        Encounter rich = Encounter.build(courtier, withFormalAlts, Collections.emptySet(), new Random(1));
        assertEquals(2, rich.slots.size());
        assertEquals("the first slot must be the type the monster leads with",
                QuestionType.REGISTER_FORMALITY, rich.slots.get(0).type);

        // A topic that carries no formalAlt at all: every slot degrades to the second type,
        // and the encounter still has its full complement.
        List<Word> noFormalAlts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Word w = word(i, "word" + i, "definition " + i);
            w.synonyms = Arrays.asList("syn" + i);
            noFormalAlts.add(w);
        }
        Encounter degraded = Encounter.build(courtier, noFormalAlts, Collections.emptySet(), new Random(1));
        assertEquals("dropping a slot instead of falling back would be a shorter fight",
                2, degraded.slots.size());
        for (Encounter.Slot slot : degraded.slots) {
            assertEquals(QuestionType.SYNONYM_ANTONYM, slot.type);
        }
    }

    @Test
    public void bossSlotsGroupByTypeWithRemainderGoingToEarlierTypes() {
        List<Word> pool = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Word w = word(i, "word" + i, "definition " + i);
            w.synonyms = Arrays.asList("syn" + i);
            pool.add(w);
        }
        Monster boss = monster("boss", Arrays.asList("DEFINITION_TO_WORD", "SYNONYM_ANTONYM", "WORDLE"), 5);

        Encounter encounter = Encounter.build(boss, pool, Collections.emptySet(), new Random(1));

        assertEquals(5, encounter.slots.size());
        List<QuestionType> typesInOrder = new ArrayList<>();
        for (Encounter.Slot slot : encounter.slots) {
            typesInOrder.add(slot.type);
        }
        assertEquals(Arrays.asList(
                QuestionType.DEFINITION_TO_WORD, QuestionType.DEFINITION_TO_WORD,
                QuestionType.SYNONYM_ANTONYM, QuestionType.SYNONYM_ANTONYM,
                QuestionType.WORDLE), typesInOrder);
    }

    @Test
    public void aTypeWithNoEligibleWordFallsBackRatherThanCrashing() {
        // Pool has words with no `forms`, so WORD_FORM can never canGenerate. The monster's
        // only other declared type (DEFINITION_TO_WORD) always can, off the same pool.
        List<Word> pool = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pool.add(word(i, "word" + i, "definition " + i));
        }
        Monster monster = monster("starved", Arrays.asList("WORD_FORM", "DEFINITION_TO_WORD"), 3);

        Encounter encounter = Encounter.build(monster, pool, Collections.emptySet(), new Random(1));

        assertEquals(3, encounter.slots.size());
        for (Encounter.Slot slot : encounter.slots) {
            assertEquals(QuestionType.DEFINITION_TO_WORD, slot.type);
        }
    }

    @Test
    public void aSlotIsDroppedRatherThanCrashingWhenNoTypeCanBeGenerated() {
        List<Word> pool = new ArrayList<>();
        pool.add(word(1, "lonelyword", "the only word"));
        Monster monster = monster("desperate", Collections.singletonList("WORD_FORM"), 3);

        Encounter encounter = Encounter.build(monster, pool, Collections.emptySet(), new Random(1));

        assertTrue(encounter.slots.isEmpty());
    }

    @Test
    public void dueWordsAreChosenBeforeNonDueWords() {
        List<Word> pool = new ArrayList<>();
        pool.add(word(1, "notdue", "def"));
        Word due = word(2, "due", "def");
        pool.add(due);
        Set<Long> dueIds = new HashSet<>(Collections.singletonList(2L));
        Monster monster = monster("m", Collections.singletonList("DEFINITION_TO_WORD"), 1);

        Encounter encounter = Encounter.build(monster, pool, dueIds, new Random(1));

        assertEquals(1, encounter.slots.size());
        assertEquals(2L, encounter.slots.get(0).word.id);
    }

    @Test
    public void noWordIsUsedTwiceInTheSameEncounter() {
        List<Word> pool = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pool.add(word(i, "word" + i, "def" + i));
        }
        Monster monster = monster("m", Collections.singletonList("DEFINITION_TO_WORD"), 3);

        Encounter encounter = Encounter.build(monster, pool, Collections.emptySet(), new Random(1));

        Set<Long> ids = new HashSet<>();
        for (Encounter.Slot slot : encounter.slots) {
            assertTrue("word " + slot.word.id + " reused", ids.add(slot.word.id));
        }
    }
}
