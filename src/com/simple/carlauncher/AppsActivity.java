package com.simple.carlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppsActivity extends Activity {

    private final List<ResolveInfo> apps = new ArrayList<>();
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), 0);
        root.setBackground(Ui.background(this));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setColorFilter(Color.WHITE);
        back.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0x1FFFFFFF);
        back.setBackground(circle);
        MainActivity.pressable(back);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView title = new TextView(this);
        title.setText("Aplicaciones");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setPadding(dp(18), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        // Theme switch: Auto (dark with car lights / at night) -> Light -> Dark.
        TextView theme = new TextView(this);
        theme.setTextSize(16);
        theme.setTextColor(Color.WHITE);
        theme.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable tbg = new GradientDrawable();
        tbg.setColor(0x33FFFFFF);
        tbg.setCornerRadius(dp(22));
        theme.setBackground(tbg);
        theme.setText(themeLabel());
        MainActivity.pressable(theme);
        theme.setOnClickListener(v -> {
            String m = Ui.themeMode(this);
            Ui.setThemeMode(this, Ui.THEME_AUTO.equals(m) ? Ui.THEME_LIGHT
                    : Ui.THEME_LIGHT.equals(m) ? Ui.THEME_DARK : Ui.THEME_AUTO);
            recreate();
        });
        header.addView(theme);

        TextView setup = new TextView(this);
        setup.setText("Configurar");
        setup.setTextSize(16);
        setup.setTextColor(Color.WHITE);
        setup.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(0x33FFFFFF);
        sbg.setCornerRadius(dp(22));
        setup.setBackground(sbg);
        MainActivity.pressable(setup);
        setup.setOnClickListener(v -> startActivity(new android.content.Intent(this, SetupActivity.class)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.leftMargin = dp(8);
        header.addView(setup, slp);
        root.addView(header);

        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(dp(12));
        grid.setHorizontalSpacing(dp(12));
        grid.setPadding(0, dp(16), 0, dp(16));
        grid.setClipToPadding(false);
        grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adapter = new BaseAdapter() {
            public int getCount() { return apps.size(); }
            public Object getItem(int p) { return apps.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int p, View convert, ViewGroup parent) {
                return buildItem(apps.get(p), convert);
            }
        };
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, pos, id) -> {
            ResolveInfo ri = apps.get(pos);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        });
        grid.setOnItemLongClickListener((parent, view, pos, id) -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + apps.get(pos).activityInfo.packageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        });
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        apps.clear();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            if (!ri.activityInfo.packageName.equals(getPackageName())) apps.add(ri);
        }
        Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(
                a.loadLabel(pm).toString(), b.loadLabel(pm).toString()));
        adapter.notifyDataSetChanged();
    }

    private View buildItem(ResolveInfo ri, View convert) {
        LinearLayout item;
        ImageView icon;
        TextView label;
        if (convert == null) {
            item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(dp(6), dp(12), dp(6), dp(12));
            GradientDrawable tile = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF3B4149, 0xFF20252B});
            tile.setCornerRadius(dp(16));
            tile.setStroke(dp(1), 0x35FFFFFF);
            item.setBackground(tile);
            icon = new ImageView(this);
            item.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));
            label = new TextView(this);
            label.setTextSize(13);
            label.setTextColor(0xE6FFFFFF);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setPadding(0, dp(8), 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(-1, -2));
        } else {
            item = (LinearLayout) convert;
            icon = (ImageView) item.getChildAt(0);
            label = (TextView) item.getChildAt(1);
        }
        PackageManager pm = getPackageManager();
        icon.setImageDrawable(ri.loadIcon(pm));
        label.setText(ri.loadLabel(pm));
        return item;
    }

    private String themeLabel() {
        String m = Ui.themeMode(this);
        return Ui.THEME_DARK.equals(m) ? "Tema: Oscuro" : Ui.THEME_LIGHT.equals(m) ? "Tema: Claro" : "Tema: Automático";
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
