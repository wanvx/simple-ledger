package com.local.simpleledger.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.local.simpleledger.BuildConfig;
import com.local.simpleledger.model.Category;
import com.local.simpleledger.model.SubcategoryTotal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds shareable PNG / CSV statements at subcategory granularity only. */
public final class Exporter {

    private static final int WIDTH = 1080;
    private static final int MARGIN = 82;
    private static final int TEXT = 0xFF171B17;
    private static final int SECONDARY = 0xFF666F64;
    private static final int TERTIARY = 0xFF98A093;
    private static final int ACCENT = 0xFF2F7D5A;
    private static final int DIVIDER = 0xFFE2E7DD;
    private static final int BACKGROUND = 0xFFFFFFFF;

    private Exporter() {
    }

    public static File exportImage(Context context, DateRange range,
                                   List<SubcategoryTotal> all, long total) throws Exception {
        File directory = exportDirectory(context);
        File file = new File(directory, "简单记账_账单_" + range.filePart() + "_"
                + DateUtils.fileStamp() + ".png");
        Map<Category, List<SubcategoryTotal>> grouped = group(all);
        int height = measureHeight(grouped);

        Bitmap bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(BACKGROUND);

        Paint accentBar = fill(ACCENT);
        canvas.drawRect(0, 0, WIDTH, 14, accentBar);

        Paint titlePaint = textPaint(42, true, TEXT);
        canvas.drawText("简单记账 · 账单", MARGIN, 112, titlePaint);

        Paint rangePaint = textPaint(24, false, SECONDARY);
        canvas.drawText(DateRange.rangeLabel(range.start, range.end),
                MARGIN, 158, rangePaint);

        Paint totalLabel = textPaint(20, false, SECONDARY);
        canvas.drawText("合计支出", MARGIN, 212, totalLabel);
        Paint totalPaint = textPaint(44, true, ACCENT);
        canvas.drawText(Money.yuan(total), MARGIN, 268, totalPaint);

        Paint dividerPaint = fill(DIVIDER);
        canvas.drawRect(MARGIN, 300, WIDTH - MARGIN, 302, dividerPaint);

        int y = 342;
        if (grouped.isEmpty()) {
            Paint emptyPaint = textPaint(24, false, SECONDARY);
            canvas.drawText("这段时间没有支出", MARGIN, y + 10, emptyPaint);
            y += 50;
        } else {
            for (Map.Entry<Category, List<SubcategoryTotal>> entry : grouped.entrySet()) {
                Category category = entry.getKey();
                List<SubcategoryTotal> rows = entry.getValue();
                long categoryTotal = 0;
                for (SubcategoryTotal row : rows) {
                    categoryTotal += row.amountCents;
                }
                Paint dot = fill(categoryColor(category));
                canvas.drawCircle(MARGIN + 7, y + 7, 7, dot);
                Paint categoryPaint = textPaint(27, true, TEXT);
                canvas.drawText(category.title, MARGIN + 26, y + 15, categoryPaint);
                Paint categoryAmount = textPaint(24, true, TEXT);
                String categoryText = Money.yuan(categoryTotal);
                canvas.drawText(categoryText,
                        WIDTH - MARGIN - categoryAmount.measureText(categoryText),
                        y + 15, categoryAmount);
                y += 42;
                for (SubcategoryTotal row : rows) {
                    Paint namePaint = textPaint(22, false, SECONDARY);
                    canvas.drawText(row.subcategory, MARGIN + 34, y + 15, namePaint);
                    Paint amountPaint = textPaint(22, true, TEXT);
                    String value = Money.yuan(row.amountCents);
                    canvas.drawText(value,
                            WIDTH - MARGIN - amountPaint.measureText(value),
                            y + 15, amountPaint);
                    y += 39;
                }
                y += 26;
            }
        }

        Paint footerPaint = textPaint(17, false, TERTIARY);
        canvas.drawText("简单记账", MARGIN, height - 46, footerPaint);
        String stamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",
                java.util.Locale.CHINA).format(new java.util.Date());
        canvas.drawText(stamp,
                WIDTH - MARGIN - footerPaint.measureText(stamp),
                height - 46, footerPaint);

        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally {
            bitmap.recycle();
        }
        return file;
    }

    public static File exportCsv(Context context, DateRange range,
                                 List<SubcategoryTotal> all, long total) throws Exception {
        File directory = exportDirectory(context);
        File file = new File(directory, "简单记账_账单_" + range.filePart() + "_"
                + DateUtils.fileStamp() + ".csv");
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            writer.write("简单记账账单\r\n");
            writer.write("统计区间," + csv(DateRange.rangeLabel(range.start, range.end)) + "\r\n\r\n");
            writer.write("大类,小类,金额（元）\r\n");
            for (SubcategoryTotal row : all) {
                writer.write(csv(row.category.title) + "," + csv(row.subcategory) + ","
                        + Money.plain(row.amountCents) + "\r\n");
            }
            writer.write("合计,," + Money.plain(total) + "\r\n");
        }
        return file;
    }

    public static void share(Context context, File file, String mime, String title) {
        Uri uri = FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri(title, uri));
        Intent chooser = Intent.createChooser(intent, "导出账单");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    private static File exportDirectory(Context context) {
        File directory = new File(context.getCacheDir(), "exports");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File[] existing = directory.listFiles();
        if (existing != null) {
            for (File file : existing) {
                if (file.isFile() && System.currentTimeMillis() - file.lastModified()
                        > 3L * 24L * 60L * 60L * 1000L) {
                    // Best-effort cleanup of old exports.
                    file.delete();
                }
            }
        }
        return directory;
    }

    private static Map<Category, List<SubcategoryTotal>> group(List<SubcategoryTotal> all) {
        Map<Category, List<SubcategoryTotal>> result = new LinkedHashMap<>();
        for (SubcategoryTotal row : all) {
            List<SubcategoryTotal> rows = result.get(row.category);
            if (rows == null) {
                rows = new ArrayList<>();
                result.put(row.category, rows);
            }
            rows.add(row);
        }
        return result;
    }

    private static int measureHeight(Map<Category, List<SubcategoryTotal>> grouped) {
        int height = 400;
        if (grouped.isEmpty()) {
            height += 50;
        } else {
            for (List<SubcategoryTotal> rows : grouped.values()) {
                height += 42 + 26;
                height += rows.size() * 39;
            }
        }
        return Math.max(600, height + 100);
    }

    private static Paint textPaint(int size, boolean bold, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return paint;
    }

    private static Paint fill(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    private static int categoryColor(Category category) {
        switch (category) {
            case FOOD:
                return 0xFFD9853B;
            case TRANSPORT:
                return 0xFF4B78B8;
            case DAILY:
                return 0xFF3E8C83;
            case FUN:
                return 0xFF8466A8;
            case STUDY:
                return 0xFFB08A3E;
            case OTHER:
            default:
                return 0xFF7F857C;
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
