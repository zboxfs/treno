package io.zbox.treno;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.zbox.zboxfs.DirEntry;

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
}
