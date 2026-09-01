package com.lexicondepths.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.RunRelic;

import java.util.List;

@Dao
public interface RunDao {

    @Insert
    long insertRun(Run run);

    @Update
    void updateRun(Run run);

    @Query("SELECT * FROM Run WHERE status = 'ACTIVE' LIMIT 1")
    Run getActiveRun();

    @Query("SELECT * FROM Run WHERE id = :runId")
    Run getRun(long runId);

    @Insert
    void insertNodes(List<RunNode> nodes);

    @Update
    void updateNode(RunNode node);

    @Query("SELECT * FROM RunNode WHERE id = :nodeId")
    RunNode getNode(long nodeId);

    @Query("SELECT * FROM RunNode WHERE runId = :runId ORDER BY floor, step, slot")
    List<RunNode> getNodesForRun(long runId);

    @Insert
    void insertRelic(RunRelic relic);

    @Query("SELECT * FROM RunRelic WHERE runId = :runId")
    List<RunRelic> getRelicsForRun(long runId);

    /** Node + run commit together so a crash between the two can never leave floor/step ahead of the node it came from. */
    @Transaction
    default void commitNodeCompletion(RunNode node, Run run) {
        updateNode(node);
        updateRun(run);
    }

    /** Per-question commit: slot progress and HP land together, which is what makes mid-battle resume safe. */
    @Transaction
    default void commitSlotResult(RunNode node, Run run) {
        updateNode(node);
        updateRun(run);
    }

    /**
     * The permadeath boundary. Deletes only run-scoped state: Run, RunNode, RunRelic.
     * Must never reference WordProgress in any form — see PermadeathBoundaryTest.
     */
    @Transaction
    default void clearRunState(long runId) {
        deleteNodes(runId);
        deleteRelics(runId);
        deleteRun(runId);
    }

    @Query("DELETE FROM RunNode WHERE runId = :runId")
    void deleteNodes(long runId);

    @Query("DELETE FROM RunRelic WHERE runId = :runId")
    void deleteRelics(long runId);

    @Query("DELETE FROM Run WHERE id = :runId")
    void deleteRun(long runId);
}
