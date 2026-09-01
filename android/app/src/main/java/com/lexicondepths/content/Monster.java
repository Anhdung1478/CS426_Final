package com.lexicondepths.content;

import java.util.List;

/** Static content, not a Room entity — a monster's shape never changes at runtime. */
public class Monster {
    public String id;
    public String name;
    public List<String> questionTypes;
    public int slots;
    public List<String> resists;
    public List<String> ascii;
}
