package com.lexicondepths.content;

/** Static content, not a Room entity. effect is a key a Phase 2 switch branches on. */
public class Relic {
    public String id;
    public String name;
    public String desc;
    public String effect;

    /**
     * Marks cost at the shop (P4-8). In relics.json rather than a constant in code: a run earns
     * roughly 100-160 Marks, so this is the number most likely to need tuning during a demo,
     * and tuning it should not need a rebuild.
     */
    public int price;
}
