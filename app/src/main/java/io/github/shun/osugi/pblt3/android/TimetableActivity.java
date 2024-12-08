package io.github.shun.osugi.pblt3.android;


import android.content.res.ColorStateList;
import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
//時間割に関するプログラム

public class TimetableActivity extends AppCompatActivity {
    private static final String TAG = "FirestoreExample";
    private FirebaseFirestore db;
    private String userID;
    private ProgressBar progressBar;
    private TableLayout tableLayout;
    final String[] daysOfWeek = {"月", "火", "水", "木", "金", "土", "日"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
        progressBar = findViewById(R.id.progress_bar);
        tableLayout = findViewById(R.id.table_layout);

        // Firebaseの初期化
        FirebaseApp.initializeApp(this);

        // Firestoreのインスタンスを取得
        db = FirebaseFirestore.getInstance();

        // ヘッダーのタイトルを動的に変更
        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("時間割");

        // フッターのクリックイベントを設定
        FooterUtils.setupFooter(this);

        userID = "sample（ユーザーID）";
        loadTimetable();
    }

    private void loadTimetable() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("user").document(userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    int days = ((Long) data.getOrDefault("表示する曜日", 5L)).intValue();
                    int classes = ((Long) data.getOrDefault("最大授業数", 5L)).intValue();

                    createTable(days, classes, data);
                } else {
                    db.collection("students").document(userID).set(Map.of(
                            "表示する曜日", 5,
                            "最大授業数", 5
                    ));
                    loadTimetable();
                }
            } else {
                // Handle error
                showError("Failed to load timetable.");
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void createTable(int days, int classes, Map<String, Object> data) {
        tableLayout.removeAllViews();
        String today = getToday();

        // 画面サイズを取得
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // セルの高さを計算
        int screenHeight = size.y;
        int marginHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
        int cellHeight = (screenHeight - marginHeight) / classes;

        // 固定幅レイアウト(1列目)
        TableRow.LayoutParams fixedParams = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.MATCH_PARENT,
                0.4f
        );
        // 曜日レイアウト
        TableRow.LayoutParams dayParams = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.MATCH_PARENT,
                1.0f
        );
        // 行レイアウト
        TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        // 授業レイアウト
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                cellHeight,
                1.0f
        );

        // 曜日行
        TableRow headerRow = new TableRow(this);
        TextView emptyHeader = createTextView("");
        emptyHeader.setLayoutParams(fixedParams);
        emptyHeader.setBackgroundResource(R.drawable.border0);
        headerRow.addView(emptyHeader);

        for (int i = 0; i < days; i++) {
            TextView header = createTextView(daysOfWeek[i]);
            header.setLayoutParams(dayParams);
            header.setGravity(android.view.Gravity.CENTER);
            header.setTextColor(daysOfWeek[i].equals(today) ? getColor(R.color.cyan) : getColor(R.color.black));
            header.setBackgroundResource(R.drawable.border0);
            headerRow.addView(header);
        }
        tableLayout.addView(headerRow);

        // Data rows
        for (int i = 0; i < classes; i++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(rowParams);

            // 固定幅の時限数セル
            TextView periodText = createTextView(String.valueOf(i + 1));
            periodText.setLayoutParams(fixedParams);
            periodText.setGravity(android.view.Gravity.CENTER);
            periodText.setBackgroundResource(R.drawable.border0);
            tableRow.addView(periodText);

            for (int j = 0; j < days; j++) {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setClipChildren(true);
                linearLayout.setClipToPadding(true);
                linearLayout.setLayoutParams(params);
                linearLayout.setTextAlignment(Button.TEXT_ALIGNMENT_CENTER);
                linearLayout.setBackgroundResource(R.drawable.border0);
                tableRow.addView(linearLayout);
                addClass(linearLayout, daysOfWeek[j], i+1);
            }
            tableLayout.addView(tableRow);
        }
    }

    private void addClass(LinearLayout linearLayout, String day, int period) {
        db.collection("timetable").document(userID).collection(day).document(String.valueOf(period)).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    // データ取得
                    String subjectName = snapshot.contains("教科名") ? snapshot.getString("教科名") : "不明";
                    String attendanceMethod = snapshot.contains("出席方法") ? snapshot.getString("出席方法") : "不明";
                    int notificationTime = ((Long) data.getOrDefault("通知時間", -10L)).intValue();
                    int lateTime = ((Long) data.getOrDefault("遅刻時間", 20L)).intValue();
                    Boolean danger = snapshot.contains("危険") ? snapshot.getBoolean("危険") : false;
                    Map<String, Object> scheduleMap = (Map<String, Object>) snapshot.get("授業日程");
                    if (scheduleMap != null) {
                        for (String key : scheduleMap.keySet()) {
                            Map<String, Object> lesson = (Map<String, Object>) scheduleMap.get(key);
                            String lessonDate = (String) lesson.get("授業日");
                            Boolean attendance = (Boolean) lesson.get("出欠");
                            Boolean late = (Boolean) lesson.get("遅刻");
                        }
                    }

                    // ボタンを追加
                    Button classButton = new Button(this);
                    classButton.setText(subjectName);
                    classButton.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    classButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.cyan)));

                    // 科目の詳細ダイアログを表示
                    classButton.setOnClickListener(showClassDetails -> {

                    });
                    linearLayout.addView(classButton);

                } else {

                    // ボタンを追加
                    Button emptyButton = new Button(this);
                    emptyButton.setText("未登録");
                    emptyButton.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));

                    // 科目の追加ダイアログを表示
                    emptyButton.setOnClickListener(addClassDetail -> {

                    });
                    linearLayout.addView(emptyButton);
                }
            } else {
                // Handle error
                showError("Failed to load timetable.");
            }
        });
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        return textView;
    }

    private String getToday() {
        SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.JAPANESE);
        return sdf.format(Calendar.getInstance().getTime());
    }

    private void showError(String message) {
        // Show error message to user
        TextView errorText = findViewById(R.id.error_text);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}