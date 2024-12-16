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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.view.Gravity;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity {
    private static final String TAG = "FirestoreExample";
    private FirebaseFirestore db;
    private String userID;
    private ProgressBar progressBar;
    private TableLayout tableLayout;
    private ListView classListView;
    private TextView attendanceTable;
    final String[] daysOfWeek = {"月", "火", "水", "木", "金", "土", "日"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        progressBar = findViewById(R.id.progress_bar);
        tableLayout = findViewById(R.id.table_layout);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("出席確認");

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
                    createTimeTable(days, classes, data);
                } else {
                    db.collection("users").document(userID).set(Map.of(
                            "表示する曜日", 5,
                            "最大授業数", 5
                    ));
                    loadTimetable();
                }
            } else {
                showError("Failed to load timetable.");
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void createTimeTable(int days, int classes, Map<String, Object> data) {
        tableLayout.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);

        // 曜日順に並べ替えるための配列
        List<String> correctOrder = Arrays.asList("月", "火", "水", "木", "金");

        // Firestoreの各曜日のデータを取得後、並べ替えを行って表示
        for (String day : correctOrder) {
            db.collection("timetable").document(userID)
                    .collection(day) // 曜日ごとのコレクション
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 曜日名のTextViewを追加
                            TableRow dayRow = new TableRow(this);
                            TextView dayTextView = new TextView(this);
                            dayTextView.setText(day);  // 曜日を表示
                            dayTextView.setPadding(16, 8, 16, 8);
                            dayTextView.setTextSize(16);
                            dayTextView.setBackgroundColor(getResources().getColor(R.color.gray));  // 曜日行の背景色
                            dayRow.addView(dayTextView);
                            tableLayout.addView(dayRow);

                            // 各授業ごとに1行に表示
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String classTime = document.getId(); // 授業時間 (1, 2...)
                                String subjectName = document.getString("教科名");

                                // 授業情報を表示する行
                                TableRow classRow = new TableRow(this);
                                TextView classTextView = new TextView(this);
                                classTextView.setText("時限: " + classTime + " 科目名: " + subjectName);
                                classTextView.setPadding(16, 8, 16, 8);
                                classTextView.setTextSize(16);
                                classTextView.setBackgroundResource(R.drawable.table_row_border);  // 枠線を追加

                                // ここでタップリスナーを設定
                                classTextView.setOnClickListener(v -> loadAttendanceTable(day, classTime));

                                classRow.addView(classTextView);
                                tableLayout.addView(classRow);

                                // 各行の区切り線を追加
                                View separator = new View(this);
                                separator.setLayoutParams(new TableLayout.LayoutParams(
                                        TableLayout.LayoutParams.MATCH_PARENT,
                                        2
                                ));
                                separator.setBackgroundColor(getResources().getColor(R.color.black)); // 区切り線の色
                                tableLayout.addView(separator);
                            }
                        } else {
                            showError("データの読み込みに失敗しました: " + day);
                        }
                        progressBar.setVisibility(View.GONE);
                    });
        }
    }




    private void loadAttendanceTable(String date, String time) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("timetable").document(userID).collection(date).document(time).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    String subject = (String) data.getOrDefault("教科名", "5");
                    createAttendanceTable(subject, data);
                } else {
                    db.collection("students").document(userID).set(Map.of(
                            "教科名", 5
                    ));
                    loadAttendanceTable(date, time);
                }
            } else {
                showError("Failed to load timetable.");
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void createAttendanceTable(String subject, Map<String, Object> data) {
        tableLayout.removeAllViews();

        Map<String, Object> schedule = (Map<String, Object>) data.get("授業日程");
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;

        if (schedule != null) {
            for (Map.Entry<String, Object> entry : schedule.entrySet()) {
                Map<String, Object> session = (Map<String, Object>) entry.getValue();
                if (session != null) {
                    Boolean isPresent = (Boolean) session.get("出欠");
                    Boolean isLate = (Boolean) session.get("遅刻");

                    // 出欠がnullの場合も考慮して判定
                    if (isPresent != null && isPresent) {
                        presentCount++;
                    } else if (isLate != null && isLate) {
                        lateCount++;
                    } else {
                        absentCount++; // 出欠や遅刻が未登録またはfalseの場合
                    }
                }
            }
        }

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
        tableLayout.addView(headerLayout);

        int rows = 3;
        int columns = 5;

        if (schedule != null) {
            for (int i = 0; i < rows; i++) {
                TableRow tableRow = new TableRow(this);
                for (int j = 0; j < columns; j++) {
                    int currentCell = i * columns + j + 1;
                    LinearLayout cellLayout = new LinearLayout(this);
                    cellLayout.setOrientation(LinearLayout.VERTICAL);
                    cellLayout.setGravity(Gravity.CENTER);

                    int cellSize = 150;
                    TableRow.LayoutParams cellParams = new TableRow.LayoutParams(cellSize, cellSize);
                    cellLayout.setLayoutParams(cellParams);

                    TextView numberText = createTextView(String.valueOf(currentCell));
                    numberText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    numberText.setGravity(Gravity.CENTER);

                    Map<String, Object> session = (Map<String, Object>) schedule.get("第" + currentCell + "授業日");
                    String displayText = "---";
                    if (session != null) {
                        Boolean isPresent = (Boolean) session.get("出欠");
                        Boolean isLate = (Boolean) session.get("遅刻");
                        if (isPresent != null && isPresent) {
                            displayText = "出";
                        } else if (isLate != null && isLate) {
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
        } else {
            showError("授業日程のデータがありません。");
        }

        // 「元の時間割に戻る」ボタンを追加
        Button backButton = new Button(this);
        backButton.setText("戻る");
        backButton.setOnClickListener(v -> loadTimetable());  // loadTimetable() を呼び出して元の時間割に戻る
        tableLayout.addView(backButton);
    }


    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        return textView;
    }

    private void showError(String message) {
        TextView errorText = findViewById(R.id.error_text);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}
