package com.lexicondepths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.ui.battle.BattleActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exit-checklist item 10, the half that was automated-only at Phase 4 close: REGISTER_FORMALITY
 * had been unit-tested end to end but had never been seen *rendered*, because monster placement
 * is random and no scripted walkthrough happened to roll The Courtier.
 *
 * This drives the real BattleActivity against the real seeded database with a hand-inserted run
 * whose node is The Courtier, so the type actually reaches the screen. The Courtier declares
 * REGISTER_FORMALITY then SYNONYM_ANTONYM over 2 slots, and Encounter.distribute gives one slot
 * to each in declaration order — so slot 0 is the register question whenever the pool holds a
 * formalAlt word, which the seed guarantees (24 of them). No seed search is needed.
 */
@RunWith(AndroidJUnit4.class)
public class CourtierRenderTest {

    @Test
    public void theCourtiersRegisterQuestionRendersFourSameRegisterOptions() throws Exception {
        AppDatabase db = App.get().db();
        List<Word> formal = awaitSeededFormalAltWords(db);
        assertFalse("seed must carry formalAlt words for this type to be reachable", formal.isEmpty());

        Run run = new Run();
        run.realmId = null; // Echo Trial — the pool is the whole word bank
        run.hp = 100;
        run.floor = 1;
        run.step = 1;
        run.marks = 0;
        run.seed = 12345L;
        run.status = RunStatus.ACTIVE;
        run.startedAt = System.currentTimeMillis();
        long runId = db.runDao().insertRun(run);

        RunNode node = new RunNode();
        node.runId = runId;
        node.floor = 1;
        node.step = 1;
        node.slot = 0;
        node.type = NodeType.BATTLE;
        node.monsterId = "courtier";
        db.runDao().insertNodes(Collections.singletonList(node));
        long nodeId = db.runDao().getNodesForRun(runId).get(0).id;

        try {
            android.content.Intent intent = new android.content.Intent(
                    androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                    BattleActivity.class);
            intent.putExtra(BattleActivity.EXTRA_RUN_ID, runId);
            intent.putExtra(BattleActivity.EXTRA_NODE_ID, nodeId);

            try (ActivityScenario<BattleActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    ViewGroup container = activity.findViewById(R.id.questionContainer);
                    List<Button> options = new ArrayList<>();
                    List<TextView> texts = new ArrayList<>();
                    collect(container, options, texts);

                    assertEquals("a register question renders exactly four options", 4, options.size());

                    // The prompt is the generator's, and it is on screen rather than empty.
                    boolean promptShown = false;
                    for (TextView text : texts) {
                        if (text.getText().toString().startsWith("Rewrite for a formal register:")) {
                            promptShown = true;
                        }
                    }
                    assertTrue("the register prompt is rendered", promptShown);

                    // Four distinct, non-empty, visible options — the render failure this test
                    // exists to rule out is a blank or collapsed button, not a wrong answer set,
                    // which RegisterFormalityGeneratorTest already covers.
                    List<String> labels = new ArrayList<>();
                    for (Button option : options) {
                        String label = option.getText().toString().trim();
                        assertFalse("no option renders blank", label.isEmpty());
                        assertEquals("every option is visible", View.VISIBLE, option.getVisibility());
                        assertTrue("no option is collapsed to zero height", option.getHeight() > 0);
                        labels.add(label);
                    }
                    assertEquals("options are distinct", 4, new java.util.HashSet<>(labels).size());

                    // The monster reached the screen too, with the contentDescription P4-12 added
                    // (TalkBack reads raw box-drawing ASCII as noise).
                    TextView name = activity.findViewById(R.id.monsterNameText);
                    assertEquals("The Courtier", name.getText().toString());
                });
            }
        } finally {
            db.runDao().clearRunState(runId);
        }
    }

    /** App seeds on a background thread at startup; the pool is only complete once it lands. */
    private static List<Word> awaitSeededFormalAltWords(AppDatabase db) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            List<Word> formal = new ArrayList<>();
            for (Word word : db.wordDao().getAll()) {
                if (word.formalAlt != null && !word.formalAlt.trim().isEmpty()) {
                    formal.add(word);
                }
            }
            if (!formal.isEmpty()) {
                return formal;
            }
            Thread.sleep(100);
        }
        return Collections.emptyList();
    }

    private static void collect(View view, List<Button> options, List<TextView> texts) {
        if (view instanceof Button) {
            options.add((Button) view);
            return;
        }
        if (view instanceof TextView) {
            texts.add((TextView) view);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i), options, texts);
            }
        }
    }
}
