package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.lexicondepths.db.CefrLevel;

@Entity
public class Realm {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    @NonNull
    public String topic;

    @NonNull
    public CefrLevel cefrMin;

    @NonNull
    public CefrLevel cefrMax;

    public boolean generated = false;
    public long createdAt;
}
