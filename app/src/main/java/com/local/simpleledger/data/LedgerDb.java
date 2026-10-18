package com.local.simpleledger.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.local.simpleledger.model.Category;
import com.local.simpleledger.model.SubcategoryTotal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dependency-free SQLite storage. WAL is enabled, every write is a real SQLite write, and
 * amounts are stored as integer cents so restarts, edits and re-installs all keep stable data.
 */
public final class LedgerDb extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "simple_ledger.db";
    private static final int DATABASE_VERSION = 2;

    private static volatile LedgerDb instance;

    public static LedgerDb get(Context context) {
        if (instance == null) {
            synchronized (LedgerDb.class) {
                if (instance == null) {
                    instance = new LedgerDb(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private LedgerDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date TEXT NOT NULL,"
                + "category TEXT NOT NULL,"
                + "subcategory TEXT NOT NULL,"
                + "custom_label TEXT NOT NULL DEFAULT '',"
                + "item TEXT NOT NULL,"
                + "amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),"
                + "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_entries_date ON entries(date)");
        db.execSQL("CREATE INDEX idx_entries_category_date ON entries(category, date)");
        db.execSQL("CREATE INDEX idx_entries_created ON entries(created_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN custom_label TEXT NOT NULL DEFAULT ''");
        }
    }

    public long addEntry(String date, Category category, String subcategory,
                         String customLabel, String item, long amountCents) {
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("category", category.key);
        values.put("subcategory", subcategory);
        values.put("custom_label", customLabel == null ? "" : customLabel);
        values.put("item", item);
        values.put("amount_cents", amountCents);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("entries", null, values);
    }

    public int updateEntry(long id, String date, Category category, String subcategory,
                           String customLabel, String item, long amountCents) {
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("category", category.key);
        values.put("subcategory", subcategory);
        values.put("custom_label", customLabel == null ? "" : customLabel);
        values.put("item", item);
        values.put("amount_cents", amountCents);
        return getWritableDatabase().update("entries", values, "_id = ?",
                new String[]{String.valueOf(id)});
    }

    public int deleteEntry(long id) {
        return getWritableDatabase().delete("entries", "_id = ?",
                new String[]{String.valueOf(id)});
    }

    public Entry entry(long id) {
        Entry result = null;
        try (Cursor cursor = getReadableDatabase().query("entries", Entry.COLUMNS,
                "_id = ?", new String[]{String.valueOf(id)}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                result = fromCursor(cursor);
            }
        }
        return result;
    }

    public List<Entry> entriesForDate(String date) {
        return query("date = ?", new String[]{date});
    }

    public List<Entry> entriesBetween(String start, String end) {
        return query("date >= ? AND date <= ?", new String[]{start, end});
    }

    private List<Entry> query(String selection, String[] args) {
        List<Entry> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("entries", Entry.COLUMNS,
                selection, args, null, null, "date DESC, created_at DESC, _id DESC")) {
            while (cursor.moveToNext()) {
                result.add(fromCursor(cursor));
            }
        }
        return result;
    }

    private static Entry fromCursor(Cursor cursor) {
        return new Entry(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getLong(6),
                cursor.getLong(7));
    }

    public long totalBetween(String start, String end) {
        return DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COALESCE(SUM(amount_cents), 0) FROM entries WHERE date >= ? AND date <= ?",
                new String[]{start, end});
    }

    public int countBetween(String start, String end) {
        return (int) DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM entries WHERE date >= ? AND date <= ?",
                new String[]{start, end});
    }

    public Map<String, Long> dailyTotalsForMonth(String monthKey) {
        Map<String, Long> result = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT date, SUM(amount_cents) FROM entries WHERE date LIKE ? "
                        + "GROUP BY date ORDER BY date ASC",
                new String[]{monthKey + "-%"})) {
            while (cursor.moveToNext()) {
                result.put(cursor.getString(0), cursor.getLong(1));
            }
        }
        return result;
    }

    /** All six categories are present, in catalog order, even when their amount is zero. */
    public Map<Category, Long> categoryTotals(String start, String end) {
        Map<Category, Long> result = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            result.put(category, 0L);
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category, SUM(amount_cents) FROM entries "
                        + "WHERE date >= ? AND date <= ? GROUP BY category",
                new String[]{start, end})) {
            while (cursor.moveToNext()) {
                Category category = Category.byKey(cursor.getString(0));
                result.put(category, cursor.getLong(1));
            }
        }
        return result;
    }

    /** Aggregated by the visible subcategory name, including names given to "其他". */
    public List<SubcategoryTotal> subcategoryTotals(String start, String end) {
        Map<String, Long> amounts = new LinkedHashMap<>();
        String labelExpression =
                "CASE WHEN TRIM(custom_label) = '' THEN subcategory ELSE custom_label END";
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category, " + labelExpression + " AS label, SUM(amount_cents) "
                        + "FROM entries WHERE date >= ? AND date <= ? "
                        + "GROUP BY category, " + labelExpression
                        + " ORDER BY category ASC, label ASC",
                new String[]{start, end})) {
            while (cursor.moveToNext()) {
                amounts.put(cursor.getString(0) + "\u0000" + cursor.getString(1),
                        cursor.getLong(2));
            }
        }
        List<SubcategoryTotal> result = new ArrayList<>();
        for (Category category : Category.values()) {
            Set<String> handled = new HashSet<>();
            for (String subcategory : category.subcategories()) {
                Long amount = amounts.get(category.key + "\u0000" + subcategory);
                if (amount != null && amount > 0) {
                    result.add(new SubcategoryTotal(category, subcategory, amount));
                    handled.add(subcategory);
                }
            }
            String prefix = category.key + "\u0000";
            for (Map.Entry<String, Long> entry : amounts.entrySet()) {
                if (!entry.getKey().startsWith(prefix) || entry.getValue() <= 0) {
                    continue;
                }
                String label = entry.getKey().substring(prefix.length());
                if (!handled.contains(label)) {
                    result.add(new SubcategoryTotal(category, label, entry.getValue()));
                    handled.add(label);
                }
            }
        }
        return result;
    }

    public long totalAll() {
        return DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COALESCE(SUM(amount_cents), 0) FROM entries", null);
    }

    public int countAll() {
        return (int) DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(*) FROM entries", null);
    }

    public int activeDays() {
        return (int) DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COUNT(DISTINCT date) FROM entries", null);
    }

    public long earliestDateMillis() {
        long value = DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COALESCE(MIN(created_at), 0) FROM entries", null);
        return value;
    }

    /** Clears every expense atomically. Financial preferences are reset separately. */
    public void clearAllEntries() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("entries", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
