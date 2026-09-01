package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.lexicondepths.db.CefrLevel;

import java.util.List;

@Entity(indices = {@Index(value = "headword", unique = true), @Index("cefr"), @Index("topic")})
public class Word {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String headword;

    @NonNull
    public CefrLevel cefr;

    @NonNull
    public String topic;

    @NonNull
    public String pos;

    @NonNull
    public String definition;

    @NonNull
    public String example;

    @Nullable
    public String viGloss;

    @NonNull
    public List<String> synonyms;

    @NonNull
    public List<String> antonyms;

    @NonNull
    public List<String> collocations;

    @NonNull
    public List<String> forms;

    @Nullable
    public String affixKey;
}
