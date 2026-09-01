package com.lexicondepths.db;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles three shapes: List<String> <-> CSV, enum <-> String, and long epoch
 * millis for dates (no converter needed there — dates are plain longs).
 * No child tables for list fields: they are never queried by element.
 */
public class Converters {

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }

    @TypeConverter
    public static List<String> toStringList(String csv) {
        if (csv == null || csv.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    @TypeConverter
    public static String fromCefrLevel(CefrLevel level) {
        return level == null ? null : level.name();
    }

    @TypeConverter
    public static CefrLevel toCefrLevel(String value) {
        return value == null ? null : CefrLevel.valueOf(value);
    }

    @TypeConverter
    public static String fromNodeType(NodeType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static NodeType toNodeType(String value) {
        return value == null ? null : NodeType.valueOf(value);
    }

    @TypeConverter
    public static String fromRunStatus(RunStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static RunStatus toRunStatus(String value) {
        return value == null ? null : RunStatus.valueOf(value);
    }
}
