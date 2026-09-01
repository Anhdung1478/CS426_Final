package com.lexicondepths.db.entity;

import androidx.room.Entity;

@Entity(primaryKeys = {"realmId", "wordId"})
public class RealmWord {
    public long realmId;
    public long wordId;
}
