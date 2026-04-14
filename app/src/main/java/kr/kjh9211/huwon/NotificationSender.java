package kr.kjh9211.huwon;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationSender {

    public interface Callback {
        void onComplete(boolean success, String message);
    }

    private static final String TAG = "NotificationSender";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void sendAsync(
            String endpoint,
            String appName,
            String content,
            String title,
            String token,
            long timestamp,
            Callback callback
    ) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject payload = new JSONObject();
                payload.put("appname", safe(appName));
                payload.put("content", safe(content));
                payload.put("title", safe(title));
                payload.put("time", formatTime(timestamp));
                payload.put("token", safe(token));

                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);

                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body);
                }

                int code = connection.getResponseCode();
                boolean success = code >= 200 && code < 300;
                String message = success ? "전송 성공 (" + code + ")" : "전송 실패 (" + code + ")";
                if (callback != null) {
                    callback.onComplete(success, message);
                }
            } catch (Exception exception) {
                Log.e(TAG, "Failed to send payload", exception);
                if (callback != null) {
                    callback.onComplete(false, "전송 오류: " + exception.getMessage());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String formatTime(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(timestamp));
    }
}
