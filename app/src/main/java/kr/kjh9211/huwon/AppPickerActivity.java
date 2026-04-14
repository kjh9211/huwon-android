package kr.kjh9211.huwon;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AppPickerActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PACKAGE = "selected_package";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        MaterialButton backButton = findViewById(R.id.backButton);
        GridView appGridView = findViewById(R.id.appGridView);

        List<AppOption> appOptions = loadInstalledApps();
        String selectedPackage = getIntent().getStringExtra(EXTRA_SELECTED_PACKAGE);

        appGridView.setAdapter(new AppGridAdapter(appOptions, selectedPackage));
        appGridView.setOnItemClickListener((parent, view, position, id) -> {
            AppOption selected = appOptions.get(position);
            Intent result = new Intent();
            result.putExtra(EXTRA_SELECTED_PACKAGE, selected.packageName);
            setResult(RESULT_OK, result);
            finish();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private List<AppOption> loadInstalledApps() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> installedApps;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            installedApps = packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0));
        } else {
            installedApps = packageManager.getInstalledApplications(0);
        }
        List<AppOption> appOptions = new ArrayList<>();

        for (ApplicationInfo applicationInfo : installedApps) {
            String packageName = applicationInfo.packageName;
            if (TextUtils.isEmpty(packageName)) {
                continue;
            }

            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            Drawable icon = packageManager.getApplicationIcon(applicationInfo);
            appOptions.add(new AppOption(
                    label == null ? packageName : label.toString(),
                    packageName,
                    icon
            ));
        }

        Collator collator = Collator.getInstance(Locale.KOREA);
        Collections.sort(appOptions, (left, right) -> collator.compare(left.label, right.label));
        return appOptions;
    }

    private static class AppOption {
        final String label;
        final String packageName;
        final Drawable icon;

        AppOption(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private class AppGridAdapter extends BaseAdapter {

        private final List<AppOption> items;
        private final String selectedPackage;

        AppGridAdapter(List<AppOption> items, String selectedPackage) {
            this.items = items;
            this.selectedPackage = selectedPackage;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_app_grid, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppOption item = items.get(position);
            holder.iconView.setImageDrawable(item.icon);
            holder.labelView.setText(item.label);
            holder.packageView.setText(item.packageName);
            convertView.setSelected(item.packageName.equals(selectedPackage));
            convertView.setActivated(item.packageName.equals(selectedPackage));
            return convertView;
        }
    }

    private static class ViewHolder {
        final ImageView iconView;
        final TextView labelView;
        final TextView packageView;

        ViewHolder(View itemView) {
            iconView = itemView.findViewById(R.id.appIconView);
            labelView = itemView.findViewById(R.id.appLabelView);
            packageView = itemView.findViewById(R.id.appPackageView);
        }
    }
}
