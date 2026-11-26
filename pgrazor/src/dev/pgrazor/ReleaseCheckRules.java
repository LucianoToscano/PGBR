package dev.pgrazor;

/** Local decisions for the official PGBR release channel, independent of Android. */
final class ReleaseCheckRules {
    private ReleaseCheckRules() {}
    static final String REPOSITORY = "https://github.com/LucianoToscano/PGBR/releases";
    static final String API = "https://api.github.com/repos/LucianoToscano/PGBR/releases/latest";

    static int version(String tag) {
        if (tag == null || !tag.matches("v?0\\.00\\.[0-9]{1,6}-LT")) return -1;
        int first = tag.lastIndexOf('.') + 1;
        return Integer.parseInt(tag.substring(first, tag.length() - 3));
    }

    static boolean newer(String latest, String installed) {
        int current = version(installed), candidate = version(latest);
        return current >= 0 && candidate > current;
    }

    static String asset(String tag, boolean root) {
        if (version(tag) < 0 || !tag.startsWith("v")) return null;
        return "PGBR-" + tag + (root ? "-Root.apk" : ".apk");
    }

    static boolean trustedAsset(String tag, String name, String url) {
        return version(tag) >= 0 && tag.startsWith("v") && name != null &&
            (name.equals(asset(tag, false)) || name.equals(asset(tag, true)) || name.equals("SHA256SUMS.txt")) &&
            (REPOSITORY + "/download/" + tag + "/" + name).equals(url);
    }

    static String releasePage(String tag) {
        return version(tag) >= 0 && tag.startsWith("v") ? REPOSITORY + "/tag/" + tag : REPOSITORY;
    }
}
