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
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
//

public class AttendanceActivity extends AppCompatActivity {
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
        loadAttendanceTable("月", "1");
    }

    private void loadAttendanceTable(String date, String time) {
        progressBar.setVisibility(View.VISIBLE); //ロードのぐるぐる
        /* data曜日のtime時限目を取得 */
        db.collection("timetable").document(userID).collection(date).document(time).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) { //取得に成功
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    String subject = (String) data.getOrDefault("教科名", "5");

                    createTable(subject, data);
                } else {
                    db.collection("students").document(userID).set(Map.of(
                            "教科名", 5
                    ));
                    loadAttendanceTable(date, time);
                }
            } else {
                // Handle error
                showError("Failed to load timetable.");
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void createTable(String subject, Map<String, Object> data) {
        tableLayout.removeAllViews();

        // 出席数、欠席数、遅刻数をカウント
        Map<String, Object> schedule = (Map<String, Object>) data.get("授業日程");
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;

        if (schedule != null) {
            for (Map.Entry<String, Object> entry : schedule.entrySet()) {
                Map<String, Object> session = (Map<String, Object>) entry.getValue();
                if (session != null) {
                    boolean isPresent = (Boolean) session.getOrDefault("出欠", false);
                    boolean isLate = (Boolean) session.getOrDefault("遅刻", false);

                    if (isPresent) {
                        presentCount++;
                    } else if (isLate) {
                        lateCount++;
                    } else {
                        absentCount++;
                    }
                }
            }
        }

        // 科目名と出席数などを表示するヘッダー部分を作成
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(16, 16, 16, 16);

        TextView subjectText = createTextView(subject);
        subjectText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        subjectText.setGravity(Gravity.CENTER);

        TextView attendanceSummary = createTextView(
                "出席: " + presentCount + " | 欠席: " + absentCount + " | 遅刻: " + lateCount
        );
        attendanceSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        attendanceSummary.setGravity(Gravity.CENTER);

        headerLayout.addView(subjectText);
        headerLayout.addView(attendanceSummary);

        // ヘッダー部分をTableLayoutの上に追加
        tableLayout.addView(headerLayout);

        int rows = 3; // 行数を定義
        int columns = 5; // 列数を定義

        // 上部のヘッダー行を作成
        TableRow headerRow = new TableRow(this);

        if (schedule != null) {
            for (int i = 0; i < rows; i++) {
                TableRow tableRow = new TableRow(this);
                for (int j = 0; j < columns; j++) {
                    int currentCell = i * columns + j + 1; // 現在のマス番号

                    LinearLayout cellLayout = new LinearLayout(this);
                    cellLayout.setOrientation(LinearLayout.VERTICAL);
                    cellLayout.setGravity(Gravity.CENTER);

                    // セルのサイズを動的に設定
                    int cellSize = 150; // 任意の正方形サイズ
                    TableRow.LayoutParams cellParams = new TableRow.LayoutParams(cellSize, cellSize);
                    cellLayout.setLayoutParams(cellParams);

                    TextView numberText = createTextView(String.valueOf(currentCell));
                    numberText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    numberText.setGravity(Gravity.CENTER);

                    Map<String, Object> session = (Map<String, Object>) schedule.get("第" + currentCell + "授業日");
                    String displayText = "---";
                    if (session != null) {
                        boolean isPresent = (Boolean) session.getOrDefault("出欠", false);
                        boolean isLate = (Boolean) session.getOrDefault("遅刻", false);

                        if (isPresent) {
                            displayText = "出";
                        } else if (isLate) {
                            displayText = "遅";
                        } else {
                            displayText = "欠";
                        }
                    }

                    TextView cellText = createTextView(displayText);
                    cellText.setGravity(Gravity.CENTER);
                    cellText.setBackgroundResource(R.drawable.border0);

                    cellLayout.addView(numberText);
                    cellLayout.addView(cellText);

                    tableRow.addView(cellLayout);
                }
                tableLayout.addView(tableRow);
            }

        }
        else {
            showError("授業日程のデータがありません。");
        }
    }




    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        return textView;
    }

    private void showError(String message) {
        // Show error message to user
        TextView errorText = findViewById(R.id.error_text);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}