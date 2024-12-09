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
        progressBar.setVisibility(View.VISIBLE);
        db.collection("timetable").document(userID).collection(date).document(time).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
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

        // テーブルの行数と列数
        int rows = 3;
        int columns = 5;

        // 上部のヘッダー行
        TableRow headerRow = new TableRow(this);
        for (int i = 1; i <= columns; i++) {
            TextView headerCell = createTextView(String.valueOf(i));
            headerCell.setGravity(Gravity.CENTER);
            headerCell.setBackgroundResource(R.drawable.border0);
            headerRow.addView(headerCell);
        }
        tableLayout.addView(headerRow);

        // 出席データの表示
        for (int i = 0; i < rows; i++) {
            TableRow tableRow = new TableRow(this);
            for (int j = 0; j < columns; j++) {
                int currentCell = i * columns + j + 1; // 現在のマス番号
                String status = (String) data.getOrDefault(String.valueOf(currentCell), "---");

                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setBackgroundResource(R.drawable.border0);
                tableRow.addView(cell);
            }
            tableLayout.addView(tableRow);
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