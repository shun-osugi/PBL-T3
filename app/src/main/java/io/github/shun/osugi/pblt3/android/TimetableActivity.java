package io.github.shun.osugi.pblt3.android;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
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

    @SuppressLint("ResourceAsColor")
    private void addClass(LinearLayout linearLayout, String day, int period) {
        db.collection("timetable").document(userID).collection(day).document(String.valueOf(period)).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    // データ取得
                    String subjectName = snapshot.contains("教科名") ? snapshot.getString("教科名") : "未設定";
                    String attendanceMethod1 = snapshot.contains("出席方法1") ? snapshot.getString("出席方法1") : "未設定";
                    String attendanceMethod2 = snapshot.contains("出席方法2") ? snapshot.getString("出席方法2") : "未設定";
                    String attendanceMethod3 = snapshot.contains("出席方法3") ? snapshot.getString("出席方法3") : "未設定";
                    Integer notificationTime1 = ((Long) data.getOrDefault("通知時間1", -10L)).intValue();
                    Integer notificationTime2 = ((Long) data.getOrDefault("通知時間2", -10L)).intValue();
                    Integer notificationTime3 = ((Long) data.getOrDefault("通知時間3", -10L)).intValue();
                    Integer lateTime = ((Long) data.getOrDefault("遅刻時間", 20L)).intValue();
                    Boolean danger = snapshot.contains("危険") ? snapshot.getBoolean("危険") : false;
                    Map<String, Object> scheduleMap = (Map<String, Object>) snapshot.get("授業日程");

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

                        // カスタムレイアウトのビューを読み込む
                        LayoutInflater inflater = LayoutInflater.from(this);
                        View dialogView = inflater.inflate(R.layout.edit_dialog, null);
                        dialogView.setBackgroundColor(danger == false ? ContextCompat.getColor(this, R.color.cyan) : ContextCompat.getColor(this, R.color.red));

                        // 各UI要素を取得
                        TextView title = dialogView.findViewById(R.id.title);
                        title.setText(day + "曜" + period + "限");

                        EditText subjectNameEdit = dialogView.findViewById(R.id.subjectName);
                        Button deleteButton = dialogView.findViewById(R.id.optionButton);
                        EditText startDate = dialogView.findViewById(R.id.startDate);
                        Button detailButton = dialogView.findViewById(R.id.detailButton);
                        CheckBox check1 = dialogView.findViewById(R.id.check1);
                        CheckBox check2 = dialogView.findViewById(R.id.check2);
                        CheckBox check3 = dialogView.findViewById(R.id.check3);
                        EditText attendanceMethodEdit1 = dialogView.findViewById(R.id.attendanceMethod1);
                        EditText attendanceMethodEdit2 = dialogView.findViewById(R.id.attendanceMethod2);
                        EditText attendanceMethodEdit3 = dialogView.findViewById(R.id.attendanceMethod3);
                        EditText notificationTimeEdit1 = dialogView.findViewById(R.id.notificationTime1);
                        EditText notificationTimeEdit2 = dialogView.findViewById(R.id.notificationTime2);
                        EditText notificationTimeEdit3 = dialogView.findViewById(R.id.notificationTime3);
                        TextView lateTimeEdit = dialogView.findViewById(R.id.lateTime);
                        Button saveButton = dialogView.findViewById(R.id.saveButton);

                        subjectNameEdit.setText(subjectName);
                        attendanceMethodEdit1.setText(attendanceMethod1);
                        attendanceMethodEdit2.setText(attendanceMethod2);
                        attendanceMethodEdit3.setText(attendanceMethod3);
                        notificationTimeEdit1.setText(notificationTime1.toString());
                        notificationTimeEdit2.setText(notificationTime2.toString());
                        notificationTimeEdit3.setText(notificationTime3.toString());
                        lateTimeEdit.setText(lateTime.toString());

                        // 開始日入力の設定
                        Calendar calendar = Calendar.getInstance(); // 今日の日付を取得
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        String todayDate = dateFormat.format(calendar.getTime());

                        if (scheduleMap != null && scheduleMap.containsKey("第1回")) {
                            Map<String, Object> firstLesson = (Map<String, Object>) scheduleMap.get("第1回");
                            String firstLessonDate = (String) firstLesson.get("授業日");
                            if (firstLessonDate != null && !firstLessonDate.isEmpty()) {startDate.setText(firstLessonDate);}
                        } else {startDate.setText(todayDate);}

                        startDate.setText(todayDate);
                        startDate.setOnClickListener(v -> {
                            // DatePickerDialogを表示
                            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                                    (view, year, month, dayOfMonth) -> {
                                        // 日付が選択されたときの処理
                                        String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                                        startDate.setText(selectedDate); // 選択した日付をセット
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                            );

                            datePickerDialog.show();
                        });

                        // チェックボックスの設定
                        check1.setChecked(true);
                        check1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            attendanceMethodEdit1.setEnabled(isChecked);
                            notificationTimeEdit1.setEnabled(isChecked);
                            if (!isChecked) {
                                attendanceMethodEdit1.setText(null);
                                notificationTimeEdit1.setText(null);
                            }
                        });

                        check2.setChecked(true);
                        check2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            attendanceMethodEdit2.setEnabled(isChecked);
                            notificationTimeEdit2.setEnabled(isChecked);
                            if (!isChecked) {
                                attendanceMethodEdit2.setText(null);
                                notificationTimeEdit2.setText(null);
                            }
                        });

                        check3.setChecked(true);
                        check3.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            attendanceMethodEdit3.setEnabled(isChecked);
                            notificationTimeEdit3.setEnabled(isChecked);
                            if (!isChecked) {
                                attendanceMethodEdit3.setText(null);
                                notificationTimeEdit3.setText(null);
                            }
                        });

                        attendanceMethodEdit1.setEnabled(check1.isChecked());
                        notificationTimeEdit1.setEnabled(check1.isChecked());

                        attendanceMethodEdit2.setEnabled(check2.isChecked());
                        notificationTimeEdit2.setEnabled(check2.isChecked());

                        attendanceMethodEdit3.setEnabled(check3.isChecked());
                        notificationTimeEdit3.setEnabled(check3.isChecked());

                        // ダイアログを作成
                        AlertDialog dialog = new AlertDialog.Builder(this)
                                .setView(dialogView)
                                .setCancelable(true)
                                .create();

                        saveButton.setOnClickListener(v -> {
                            String newSubjectName = subjectNameEdit.getText().toString().trim();
                            String newStartDate = startDate.getText().toString().trim();
                            Map<String, Map<String, String>> startDateMap = mapDates(newStartDate);

                            // 出席方法1、2、3のチェック
                            String newAttendanceMethod1 = attendanceMethodEdit1.getText().toString().trim();
                            String newAttendanceMethod2 = attendanceMethodEdit2.getText().toString().trim();
                            String newAttendanceMethod3 = attendanceMethodEdit3.getText().toString().trim();

                            // 通知時間のパース
                            Integer newNotificationTime1 = parseIntegerOrNull(notificationTimeEdit1.getText().toString().trim());
                            Integer newNotificationTime2 = parseIntegerOrNull(notificationTimeEdit2.getText().toString().trim());
                            Integer newNotificationTime3 = parseIntegerOrNull(notificationTimeEdit3.getText().toString().trim());

                            // 遅刻時間のパース
                            Integer newLateTime = parseIntegerOrNull(lateTimeEdit.getText().toString().trim());

                            // classコレクションに保存
                            String classDocumentId = db.collection("class").document().getId();
                            Map<String, Object> classData = new HashMap<>();
                            classData.put("教科名", newSubjectName);
                            classData.put("授業開始日", newStartDate);
                            classData.put("出席方法1", newAttendanceMethod1.isEmpty() ? null : newAttendanceMethod1);
                            classData.put("出席方法2", newAttendanceMethod2.isEmpty() ? null : newAttendanceMethod2);
                            classData.put("出席方法3", newAttendanceMethod3.isEmpty() ? null : newAttendanceMethod3);
                            classData.put("通知時間1", newNotificationTime1);
                            classData.put("通知時間2", newNotificationTime2);
                            classData.put("通知時間3", newNotificationTime3);
                            classData.put("遅刻時間", newLateTime);

                            db.collection("class").document(classDocumentId)
                                    .collection(day)
                                    .document(String.valueOf(period))
                                    .set(classData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {loadTimetable();})
                                    .addOnFailureListener(e -> {showError("Failed to save class details: " + e.getMessage());});

                            // timetableコレクションに保存
                            Map<String, Object> timetableData = new HashMap<>();
                            timetableData.put("教科名", newSubjectName);
                            timetableData.put("授業日程", startDateMap);
                            timetableData.put("出席方法1", newAttendanceMethod1.isEmpty() ? null : newAttendanceMethod1);
                            timetableData.put("出席方法2", newAttendanceMethod2.isEmpty() ? null : newAttendanceMethod2);
                            timetableData.put("出席方法3", newAttendanceMethod3.isEmpty() ? null : newAttendanceMethod3);
                            timetableData.put("通知時間1", newNotificationTime1);
                            timetableData.put("通知時間2", newNotificationTime2);
                            timetableData.put("通知時間3", newNotificationTime3);
                            timetableData.put("遅刻時間", newLateTime);
                            timetableData.put("危険" , false);

                            db.collection("timetable").document(userID)
                                    .collection(day)
                                    .document(String.valueOf(period))
                                    .set(timetableData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {loadTimetable();})
                                    .addOnFailureListener(e -> {showError("Failed to save class details: " + e.getMessage());});
                            dialog.dismiss();
                            loadTimetable();
                        });

                        // 時間割から削除
                        deleteButton.setText("削除");
                        deleteButton.setOnClickListener(delete -> {
                            db.collection("timetable")
                                    .document(userID)
                                    .collection(day)          // 曜日
                                    .document(String.valueOf(period)) // 期間
                                    .delete();
                            dialog.dismiss();
                            loadTimetable();
                        });
                        dialog.show();
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

                        // カスタムレイアウトのビューを読み込む
                        LayoutInflater inflater = LayoutInflater.from(this);
                        View dialogView = inflater.inflate(R.layout.edit_dialog, null);
                        dialogView.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

                        // 各UI要素を取得
                        TextView title = dialogView.findViewById(R.id.title);
                        title.setText(day + "曜" + period + "限");

                        EditText subjectNameEdit = dialogView.findViewById(R.id.subjectName);
                        Button registeredClassesButton = dialogView.findViewById(R.id.optionButton);
                        EditText startDate = dialogView.findViewById(R.id.startDate);
                        Button detailButton = dialogView.findViewById(R.id.detailButton);
                        CheckBox check1 = dialogView.findViewById(R.id.check1);
                        CheckBox check2 = dialogView.findViewById(R.id.check2);
                        CheckBox check3 = dialogView.findViewById(R.id.check3);
                        EditText attendanceMethodEdit1 = dialogView.findViewById(R.id.attendanceMethod1);
                        EditText attendanceMethodEdit2 = dialogView.findViewById(R.id.attendanceMethod2);
                        EditText attendanceMethodEdit3 = dialogView.findViewById(R.id.attendanceMethod3);
                        EditText notificationTimeEdit1 = dialogView.findViewById(R.id.notificationTime1);
                        EditText notificationTimeEdit2 = dialogView.findViewById(R.id.notificationTime2);
                        EditText notificationTimeEdit3 = dialogView.findViewById(R.id.notificationTime3);
                        TextView lateTimeEdit = dialogView.findViewById(R.id.lateTime);
                        Button saveButton = dialogView.findViewById(R.id.saveButton);

                        // 開始日入力の設定
                        Calendar calendar = Calendar.getInstance(); // 今日の日付を取得
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        String todayDate = dateFormat.format(calendar.getTime());
                        startDate.setText(todayDate);
                        startDate.setOnClickListener(v -> {
                            // DatePickerDialogを表示
                            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                                    (view, year, month, dayOfMonth) -> {
                                        // 日付が選択されたときの処理
                                        String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                                        startDate.setText(selectedDate); // 選択した日付をセット
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                            );

                            datePickerDialog.show();
                        });

                        // みんなの登録ダイアログを表示
                        registeredClassesButton.setText("みんなの登録");

                        // 詳細設定を非表示
                        detailButton.setVisibility(View.GONE);

                        // チェックボックスの設定
                        check1.setChecked(true);
                        check1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            attendanceMethodEdit1.setEnabled(isChecked);
                            notificationTimeEdit1.setEnabled(isChecked);
                            if (!isChecked) {
                                attendanceMethodEdit1.setText(null);
                                notificationTimeEdit1.setText(null);
                            }
                        });

                        check2.setChecked(true);
                        check2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            attendanceMethodEdit2.setEnabled(isChecked);
                            notificationTimeEdit2.setEnabled(isChecked);
                            if (!isChecked) {
                                attendanceMethodEdit2.setText(null);
                                notificationTimeEdit2.setText(null);
                            }
                        });

                        check3.setChecked(true);
                        check3.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            attendanceMethodEdit3.setEnabled(isChecked);
                            notificationTimeEdit3.setEnabled(isChecked);
                            if (!isChecked) {
                                attendanceMethodEdit3.setText(null);
                                notificationTimeEdit3.setText(null);
                            }
                        });

                        attendanceMethodEdit1.setEnabled(check1.isChecked());
                        notificationTimeEdit1.setEnabled(check1.isChecked());

                        attendanceMethodEdit2.setEnabled(check2.isChecked());
                        notificationTimeEdit2.setEnabled(check2.isChecked());

                        attendanceMethodEdit3.setEnabled(check3.isChecked());
                        notificationTimeEdit3.setEnabled(check3.isChecked());

                        // ダイアログを作成
                        AlertDialog dialog = new AlertDialog.Builder(this)
                                .setView(dialogView)
                                .setCancelable(true)
                                .create();

                        saveButton.setOnClickListener(v -> {
                            String newSubjectName = subjectNameEdit.getText().toString().trim();
                            String newStartDate = startDate.getText().toString().trim();
                            Map<String, Map<String, String>> startDateMap = mapDates(newStartDate);

                            // 出席方法1、2、3のチェック
                            String newAttendanceMethod1 = attendanceMethodEdit1.getText().toString().trim();
                            String newAttendanceMethod2 = attendanceMethodEdit2.getText().toString().trim();
                            String newAttendanceMethod3 = attendanceMethodEdit3.getText().toString().trim();

                            // 通知時間のパース
                            Integer newNotificationTime1 = parseIntegerOrNull(notificationTimeEdit1.getText().toString().trim());
                            Integer newNotificationTime2 = parseIntegerOrNull(notificationTimeEdit2.getText().toString().trim());
                            Integer newNotificationTime3 = parseIntegerOrNull(notificationTimeEdit3.getText().toString().trim());

                            // 遅刻時間のパース
                            Integer newLateTime = parseIntegerOrNull(lateTimeEdit.getText().toString().trim());

                            // classコレクションに保存
                            String classDocumentId = db.collection("class").document().getId();
                            Map<String, Object> classData = new HashMap<>();
                            classData.put("教科名", newSubjectName);
                            classData.put("授業開始日", newStartDate);
                            classData.put("出席方法1", newAttendanceMethod1.isEmpty() ? null : newAttendanceMethod1);
                            classData.put("出席方法2", newAttendanceMethod2.isEmpty() ? null : newAttendanceMethod2);
                            classData.put("出席方法3", newAttendanceMethod3.isEmpty() ? null : newAttendanceMethod3);
                            classData.put("通知時間1", newNotificationTime1);
                            classData.put("通知時間2", newNotificationTime2);
                            classData.put("通知時間3", newNotificationTime3);
                            classData.put("遅刻時間", newLateTime);

                            db.collection("class").document(classDocumentId)
                                    .collection(day)
                                    .document(String.valueOf(period))
                                    .set(classData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {loadTimetable();})
                                    .addOnFailureListener(e -> {showError("Failed to save class details: " + e.getMessage());});

                            // timetableコレクションに保存
                            Map<String, Object> timetableData = new HashMap<>();
                            timetableData.put("教科名", newSubjectName);
                            timetableData.put("授業日程", startDateMap);
                            timetableData.put("出席方法1", newAttendanceMethod1.isEmpty() ? null : newAttendanceMethod1);
                            timetableData.put("出席方法2", newAttendanceMethod2.isEmpty() ? null : newAttendanceMethod2);
                            timetableData.put("出席方法3", newAttendanceMethod3.isEmpty() ? null : newAttendanceMethod3);
                            timetableData.put("通知時間1", newNotificationTime1);
                            timetableData.put("通知時間2", newNotificationTime2);
                            timetableData.put("通知時間3", newNotificationTime3);
                            timetableData.put("遅刻時間", newLateTime);

                            db.collection("timetable").document(userID)
                                    .collection(day)
                                    .document(String.valueOf(period))
                                    .set(timetableData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {loadTimetable();})
                                    .addOnFailureListener(e -> {showError("Failed to save class details: " + e.getMessage());});
                            dialog.dismiss();
                            loadTimetable();
                        });
                        dialog.show();
                    });
                    linearLayout.addView(emptyButton);
                }
            } else {
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

    private Map<String, Map<String, String>> mapDates(String startDate) {
        Map<String, Map<String, String>> dateMap = new HashMap<>();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(startDate)); // 入力された日付を設定

            for (int i = 1; i <= 15; i++) {
                String date = dateFormat.format(calendar.getTime());
                Map<String, String> eachdateMap = new HashMap<>();
                eachdateMap.put("授業日", date);
                dateMap.put("第" + i + "回", eachdateMap);
                calendar.add(Calendar.DAY_OF_MONTH, 7);
            }

        } catch (ParseException e) {
            showError("Invalid start date format: " + e.getMessage());
        }

        return dateMap;
    }

    private Integer parseIntegerOrNull(String input) {
        try {
            return input.isEmpty() ? null : Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}