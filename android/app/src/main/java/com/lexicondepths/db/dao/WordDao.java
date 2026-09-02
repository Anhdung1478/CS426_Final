package com.lexicondepths.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;

import java.util.List;

@Dao
public interface WordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Word> words);

    /** Same insert, but -1 in place of any row an existing headword shadowed — see RealmImport. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAllReturningIds(List<Word> words);

    @Query("SELECT COUNT(*) FROM Word")
    int count();

    @Query("SELECT * FROM Word WHERE id = :id")
    Word getById(long id);

    @Query("SELECT * FROM Word WHERE headword = :headword LIMIT 1")
    Word getByHeadword(String headword);

    @Query("SELECT * FROM Word WHERE topic = :topic AND cefr = :cefr")
    List<Word> getByTopicAndCefr(String topic, CefrLevel cefr);

    /** A realm run's candidate pool: the whole topic, every band — Damage's below/at/above bands need the spread. */
    @Query("SELECT * FROM Word WHERE topic = :topic")
    List<Word> getByTopic(String topic);

    /** Echo Trial's candidate pool: every unlocked topic. */
    @Query("SELECT * FROM Word")
    List<Word> getAll();

    /** Synchronous due-id set for Encounter's due-word weighting — getDueWords() is LiveData-only. */
    @Query("SELECT w.id FROM Word w INNER JOIN WordProgress p ON w.id = p.wordId WHERE p.dueAt <= :now")
    List<Long> getDueWordIdsSync(long now);

    /** Due for review: has a WordProgress row whose dueAt has passed. */
    @Query("SELECT w.* FROM Word w INNER JOIN WordProgress p ON w.id = p.wordId " +
            "WHERE p.dueAt <= :now ORDER BY p.dueAt ASC")
    LiveData<List<Word>> getDueWords(long now);

    /** Never reviewed: no WordProgress row yet, filtered by CEFR and topic. */
    @Query("SELECT w.* FROM Word w LEFT JOIN WordProgress p ON w.id = p.wordId " +
            "WHERE p.wordId IS NULL AND w.cefr = :cefr AND w.topic = :topic")
    List<Word> getNewWords(CefrLevel cefr, String topic);

    /** Combined queue: due words first, then new words, capped at :limit. */
    @Query("SELECT w.* FROM Word w LEFT JOIN WordProgress p ON w.id = p.wordId " +
            "WHERE (p.dueAt IS NOT NULL AND p.dueAt <= :now) " +
            "   OR (p.wordId IS NULL AND w.cefr = :cefr) " +
            "ORDER BY CASE WHEN p.dueAt IS NULL THEN 1 ELSE 0 END, p.dueAt ASC " +
            "LIMIT :limit")
    List<Word> getQueue(long now, CefrLevel cefr, int limit);
}
