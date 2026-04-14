package kr.kjh9211.huwon;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettingsStore {

    private static final String PREFS_NAME = "notification_forwarder_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_TARGET_PACKAGE = "target_package";

    private final SharedPreferences preferences;

    public AppSettingsStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void save(String serverUrl, String token, String targetPackage) {
        preferences.edit()
                .putString(KEY_SERVER_URL, serverUrl)
                .putString(KEY_TOKEN, token)
                .putString(KEY_TARGET_PACKAGE, targetPackage)
                .apply();
    }

    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, "");
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, "supertoken");
    }

    public String getTargetPackage() {
        return preferences.getString(KEY_TARGET_PACKAGE, "");
    }
}
