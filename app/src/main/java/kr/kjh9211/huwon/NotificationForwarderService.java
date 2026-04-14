package kr.kjh9211.huwon;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

public class NotificationForwarderService extends NotificationListenerService {

    private static final String TAG = "NotificationForwarder";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (sbn == null) {
            return;
        }

        AppSettingsStore settingsStore = new AppSettingsStore(getApplicationContext());
        String endpoint = settingsStore.getServerUrl().trim();
        String token = settingsStore.getToken().trim();
        String targetPackage = settingsStore.getTargetPackage().trim();

        if (TextUtils.isEmpty(endpoint) || TextUtils.isEmpty(token)) {
            return;
        }

        String packageName = sbn.getPackageName();
        if (!shouldForward(packageName, targetPackage)) {
            return;
        }

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        String title = charSequenceToString(extras.getCharSequence(Notification.EXTRA_TITLE));
        String content = charSequenceToString(extras.getCharSequence(Notification.EXTRA_TEXT));
        if (TextUtils.isEmpty(content)) {
            content = charSequenceToString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        }

        String appName = resolveAppName(packageName);
        NotificationSender.sendAsync(
                endpoint,
                appName,
                content,
                title,
                token,
                sbn.getPostTime(),
                null
        );
    }

    private boolean shouldForward(String packageName, String targetPackage) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        return packageName.equals(targetPackage);
    }

    private String resolveAppName(String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            return label == null ? packageName : label.toString();
        } catch (Exception exception) {
            Log.w(TAG, "Unable to resolve app name for " + packageName, exception);
            return packageName;
        }
    }

    private String charSequenceToString(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
