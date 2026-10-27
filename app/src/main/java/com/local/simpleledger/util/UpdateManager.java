package com.local.simpleledger.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.local.simpleledger.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks the public GitHub Releases feed, downloads the APK, and opens the installer. */
public final class UpdateManager {

    public static final String REPOSITORY = "wanvx/simple-ledger";
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
    private static final String USER_AGENT = "SimpleLedger-Android";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private UpdateManager() {
    }

    public interface CheckCallback {
        void onResult(UpdateInfo update, String error);
    }

    public interface DownloadCallback {
        void onProgress(int percent);

        void onComplete(File file);

        void onError(String message);
    }

    public enum InstallResult {
        STARTED,
        NEED_PERMISSION,
        NO_INSTALLER,
        ERROR
    }

    public static final class UpdateInfo {
        public final String version;
        public final String tag;
        public final String notes;
        public final String downloadUrl;
        public final String releaseUrl;
        public final long sizeBytes;
        public final boolean newer;

        UpdateInfo(String version, String tag, String notes, String downloadUrl,
                   String releaseUrl, long sizeBytes, boolean newer) {
            this.version = version;
            this.tag = tag;
            this.notes = notes;
            this.downloadUrl = downloadUrl;
            this.releaseUrl = releaseUrl;
            this.sizeBytes = sizeBytes;
            this.newer = newer;
        }
    }

    public static void checkForUpdate(CheckCallback callback) {
        IO.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = open(LATEST_RELEASE_URL);
                int code = connection.getResponseCode();
                InputStream input = code >= 200 && code < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                String body = readAll(input);
                if (code != 200) {
                    throw new IOException("GitHub HTTP " + code);
                }
                JSONObject release = new JSONObject(body);
                String tag = release.optString("tag_name", "").trim();
                String version = normalizeVersion(tag);
                if (version.isEmpty()) {
                    throw new IOException("Release tag is missing");
                }
                String downloadUrl = null;
                long size = 0;
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset == null) {
                            continue;
                        }
                        String name = asset.optString("name", "");
                        if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", null);
                            size = asset.optLong("size", 0);
                            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                                break;
                            }
                        }
                    }
                }
                if (downloadUrl == null || downloadUrl.isEmpty()) {
                    throw new IOException("No APK asset in the latest release");
                }
                String notes = release.optString("body", "").trim();
                if (notes.isEmpty()) {
                    notes = release.optString("name", "新版本");
                }
                if (notes.length() > 1800) {
                    notes = notes.substring(0, 1800) + "…";
                }
                boolean newer = compareVersions(version, BuildConfig.VERSION_NAME) > 0;
                UpdateInfo update = new UpdateInfo(version, tag, notes, downloadUrl,
                        release.optString("html_url", ""), size, newer);
                MAIN.post(() -> callback.onResult(update, null));
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "网络请求失败";
                }
                String finalMessage = message;
                MAIN.post(() -> callback.onResult(null, finalMessage));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void download(Context context, UpdateInfo info, DownloadCallback callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            HttpURLConnection connection = null;
            File part = null;
            try {
                File directory = new File(app.getCacheDir(), "updates");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IOException("无法创建更新目录");
                }
                String safeVersion = info.version.replaceAll("[^0-9A-Za-z._-]", "_");
                File target = new File(directory, "simple-ledger-" + safeVersion + ".apk");
                if (target.exists() && target.length() > 0
                        && (info.sizeBytes <= 0 || target.length() == info.sizeBytes)) {
                    File ready = target;
                    MAIN.post(() -> callback.onComplete(ready));
                    return;
                }
                part = new File(directory, target.getName() + ".part");
                if (part.exists() && !part.delete()) {
                    throw new IOException("无法清理旧下载文件");
                }

                connection = open(info.downloadUrl);
                int code = connection.getResponseCode();
                if (code != 200) {
                    throw new IOException("下载失败（HTTP " + code + "）");
                }
                long total = info.sizeBytes > 0
                        ? info.sizeBytes : connection.getContentLength();
                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream output = new FileOutputStream(part)) {
                    byte[] buffer = new byte[16 * 1024];
                    long downloaded = 0;
                    int read;
                    int lastPercent = -1;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        downloaded += read;
                        if (total > 0) {
                            int percent = (int) Math.min(100, downloaded * 100 / total);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                int value = percent;
                                MAIN.post(() -> callback.onProgress(value));
                            }
                        }
                    }
                    output.flush();
                }
                if (total > 0 && part.length() != total) {
                    throw new IOException("下载文件不完整");
                }
                if (target.exists() && !target.delete()) {
                    throw new IOException("无法覆盖旧安装包");
                }
                if (!part.renameTo(target)) {
                    throw new IOException("无法保存安装包");
                }
                File ready = target;
                MAIN.post(() -> callback.onComplete(ready));
            } catch (Exception e) {
                if (part != null) {
                    part.delete();
                }
                String message = e.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "下载失败";
                }
                String finalMessage = message;
                MAIN.post(() -> callback.onError(finalMessage));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static boolean needsInstallPermission(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !context.getPackageManager().canRequestPackageInstalls();
    }

    public static InstallResult install(Context context, File apk) {
        if (!apk.exists() || apk.length() <= 0) {
            return InstallResult.ERROR;
        }
        if (needsInstallPermission(context)) {
            return InstallResult.NEED_PERMISSION;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return InstallResult.STARTED;
        } catch (ActivityNotFoundException e) {
            return InstallResult.NO_INSTALLER;
        } catch (Exception e) {
            return InstallResult.ERROR;
        }
    }

    public static boolean openInstallPermissionSettings(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String normalizeVersion(String tag) {
        if (tag == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)*)").matcher(tag.trim());
        return matcher.find() ? matcher.group(1) : "";
    }

    public static int compareVersions(String left, String right) {
        String[] a = normalizeVersion(left).split("\\.");
        String[] b = normalizeVersion(right).split("\\.");
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            int av = i < a.length && !a[i].isEmpty() ? parseInt(a[i]) : 0;
            int bv = i < b.length && !b[i].isEmpty() ? parseInt(b[i]) : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        return connection;
    }

    private static String readAll(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
