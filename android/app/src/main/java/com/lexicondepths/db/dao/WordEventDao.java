package com.lexicondepths.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lexicondepths.db.entity.WordEvent;
import com.lexicondepths.game.stats.TypeAccuracy;
import com.lexicondepths.game.stats.WeakWord;

import java.util.List;

@Dao
public interface WordEventDao {

    @Insert
    void insert(WordEvent event);

    @Query("SELECT * FROM WordEvent WHERE runId = :runId")
    List<WordEvent> getEventsForRun(long runId);

    @Query("SELECT * FROM WordEvent WHERE runId = :runId AND ratio < 1.0")
    List<WordEvent> getFailedEventsForRun(long runId);

    // ---- P4-1 stats aggregates. WordEvent has recorded every answer since P2-11 and nothing
    // has ever read it back except Spoils; these three queries are what cash that in.

    @Query("SELECT COUNT(*) FROM WordEvent")
    int countAnswers();

    /** Worst-first is the point: best-first is a trophy case, worst-first is a study plan. */
    @Query("SELECT questionType AS questionType, AVG(ratio) AS avgRatio, COUNT(*) AS attempts "
            + "FROM WordEvent GROUP BY questionType ORDER BY avgRatio ASC")
    List<TypeAccuracy> getTypeAccuracy();

    /**
     * minAttempts keeps a single unlucky answer off the top of the list — one miss is noise,
     * not a weak word.
     */
    @Query("SELECT e.wordId AS wordId, w.headword AS headword, "
            + "AVG(e.ratio) AS avgRatio, COUNT(*) AS attempts "
            + "FROM WordEvent e INNER JOIN Word w ON w.id = e.wordId "
            + "GROUP BY e.wordId HAVING COUNT(*) >= :minAttempts "
            + "ORDER BY avgRatio ASC, attempts DESC LIMIT :limit")
    List<WeakWord> getWeakWords(int minAttempts, int limit);
}
