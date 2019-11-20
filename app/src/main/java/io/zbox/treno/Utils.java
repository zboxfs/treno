package io.zbox.treno;

import android.util.Log;
import android.webkit.MimeTypeMap;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.zbox.zboxfs.DirEntry;
import io.zbox.zboxfs.Path;
import io.zbox.zboxfs.ZboxException;

class Utils {
    static String prettySize(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    static String prettyTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d H:mm", Locale.US);
        return sdf.format(time * 1000);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    static String formatTabStr(String head, String value) {
        int length = 35;
        return head + String.format("%1$" + (length - head.length()) + "s", value) + "\n";
    }

    static int fileIcon(DirEntry dent) {
        if (dent.metadata.isDir()) return R.drawable.ic_folder_black_24dp;

        switch (dent.path.extension().toLowerCase()) {
            case "avi":
            case "mpg":
            case "mp4":
                return R.drawable.ic_play_circle_filled_black_24dp;
            case "jpg":
            case "png":
                return R.drawable.ic_photo_black_24dp;
            default:
                return R.drawable.ic_insert_drive_file_black_24dp;
        }
    }

    static String detectMimeType(String pathStr) {
        String ext = null;
        try {
            ext = new Path(pathStr).extension();
        } catch (ZboxException ignore) {}
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static SecureRandom random = new SecureRandom();

    static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            sb.append(rndChar);
        }

        return sb.toString();
    }
}
