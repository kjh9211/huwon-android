package kr.kjh9211.huwon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final LinkedHashMap<String, TextInputEditText> inputMap = new LinkedHashMap<>();
    private final ActivityResultLauncher<Intent> appPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }

                String packageName = result.getData().getStringExtra(AppPickerActivity.EXTRA_SELECTED_PACKAGE);
                if (!TextUtils.isEmpty(packageName)) {
                    setInputValue("targetPackage", packageName);
                }
            });

    private AppSettingsStore settingsStore;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsStore = new AppSettingsStore(this);
        statusTextView = findViewById(R.id.statusTextView);

        createDynamicInputs();
        createDynamicButtons();
        populateSavedValues();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void createDynamicInputs() {
        LinearLayout formContainer = findViewById(R.id.formContainer);
        formContainer.removeAllViews();

        addInputField(
                formContainer,
                "serverUrl",
                "서버 주소",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
                false
        );
        addInputField(
                formContainer,
                "token",
                "토큰",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                false
        );
        addInputField(
                formContainer,
                "targetPackage",
                "대상 앱",
                InputType.TYPE_CLASS_TEXT,
                true
        );
    }

    private void createDynamicButtons() {
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        buttonContainer.removeAllViews();

        Map<String, Runnable> buttonActions = new LinkedHashMap<>();
        buttonActions.put("저장", this::saveSettings);
        buttonActions.put("앱스 화면 열기", this::openAppPickerScreen);
        buttonActions.put("알림 접근 권한 열기", this::openNotificationAccessSettings);

        for (Map.Entry<String, Runnable> entry : buttonActions.entrySet()) {
            MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(12);
            button.setLayoutParams(params);
            button.setText(entry.getKey());
            button.setOnClickListener(v -> entry.getValue().run());
            buttonContainer.addView(button);
        }
    }

    private void addInputField(
            LinearLayout parent,
            String key,
            String label,
            int inputType,
            boolean readOnly
    ) {
        TextInputLayout textInputLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputStyle);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.topMargin = dp(12);
        textInputLayout.setLayoutParams(layoutParams);
        textInputLayout.setHint(label);

        TextInputEditText editText = new TextInputEditText(this, null, com.google.android.material.R.attr.editTextStyle);
        editText.setInputType(inputType);
        editText.setTextColor(getColor(R.color.text_primary));
        editText.setHintTextColor(getColor(R.color.text_secondary));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        editText.setBackground(null);
        editText.setMinHeight(dp(56));
        if ("serverUrl".equals(key) || "token".equals(key)) {
            editText.setPaddingRelative(dp(32), editText.getPaddingTop(), dp(20), editText.getPaddingBottom());
        }

        if (readOnly) {
            editText.setFocusable(false);
            editText.setClickable(true);
            editText.setCursorVisible(false);
            editText.setOnClickListener(v -> openAppPickerScreen());
            textInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            textInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.mtrl_ic_arrow_drop_down);
            textInputLayout.setEndIconOnClickListener(v -> openAppPickerScreen());
        }

        textInputLayout.addView(editText);
        parent.addView(textInputLayout);
        inputMap.put(key, editText);
    }

    private void populateSavedValues() {
        setInputValue("serverUrl", settingsStore.getServerUrl());
        setInputValue("token", settingsStore.getToken());
        setInputValue("targetPackage", settingsStore.getTargetPackage());
    }

    private boolean saveSettings() {
        String serverUrl = getInputValue("serverUrl");
        String token = getInputValue("token");
        String targetPackage = getInputValue("targetPackage");

        if (TextUtils.isEmpty(serverUrl)) {
            toast("서버 주소를 입력해주세요.");
            return false;
        }

        if (TextUtils.isEmpty(token)) {
            toast("토큰을 입력해주세요.");
            return false;
        }

        if (TextUtils.isEmpty(targetPackage)) {
            toast("대상 앱을 선택해주세요.");
            return false;
        }

        settingsStore.save(serverUrl, token, targetPackage);
        toast("설정을 저장했습니다.");
        refreshStatus();
        return true;
    }

    private void openAppPickerScreen() {
        Intent intent = new Intent(this, AppPickerActivity.class);
        intent.putExtra(AppPickerActivity.EXTRA_SELECTED_PACKAGE, getInputValue("targetPackage"));
        appPickerLauncher.launch(intent);
    }

    private void openNotificationAccessSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void refreshStatus() {
        String status = isNotificationAccessEnabled(this)
                ? getString(R.string.listener_enabled)
                : getString(R.string.listener_disabled);
        statusTextView.setText(getString(R.string.status_prefix, status));
    }

    private boolean isNotificationAccessEnabled(Context context) {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );

        if (TextUtils.isEmpty(enabledListeners)) {
            return false;
        }

        String serviceName = new ComponentName(context, NotificationForwarderService.class).flattenToString();
        return enabledListeners.contains(serviceName);
    }

    private String getInputValue(String key) {
        TextInputEditText editText = inputMap.get(key);
        return editText == null || editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

    private void setInputValue(String key, String value) {
        TextInputEditText editText = inputMap.get(key);
        if (editText != null) {
            editText.setText(value);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }
}
