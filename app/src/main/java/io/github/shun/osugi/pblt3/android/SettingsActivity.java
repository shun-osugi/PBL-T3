package io.github.shun.osugi.pblt3.android;

import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "FirestoreExample";
    private FirebaseFirestore db;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Firebaseの初期化
        FirebaseApp.initializeApp(this);

        // Firestoreのインスタンスを取得
        db = FirebaseFirestore.getInstance();

        // ヘッダーのタイトルを動的に変更
        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("設定");

        // フッターのクリックイベントを設定
        FooterUtils.setupFooter(this);

        userID = "sample（ユーザーID）";

        displayUI();

    }

    private void displayUI() {
        db.collection("user").document(userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    // データ取得
                    int classes = ((Long) data.getOrDefault("最大授業数", 5L)).intValue();
                    int days = ((Long) data.getOrDefault("表示する曜日", 5L)).intValue();
                    boolean absentNotificationEnabled = snapshot.contains("一日欠席通知設定") ? snapshot.getBoolean("一日欠席通知設定") : false;
                    String absentNotificationTime = snapshot.contains("一日欠席通知時間") ? snapshot.getString("一日欠席通知時間") : "08:00";
                    Map<String, Object> startTimeMap = (Map<String, Object>) snapshot.get("開始時刻");
                    String[] startTimes = new String[7];
                    if (startTimeMap != null) {
                        int index = 0;
                        for (int i = 1; i <= startTimeMap.size(); i++) {
                            String key = i + "限";
                            if (startTimeMap.containsKey(key)) {
                                startTimes[index] = (String) startTimeMap.get(key);
                                index++;
                            }
                        }
                    }

                    settingClasses(classes);
                    settingDays(days);
                    settingStartTime(startTimes);
                    settingAbsence(absentNotificationEnabled, absentNotificationTime);

                }
            }
        });
    }

    // 最大授業数を設定するラジオボタンの設定
    private void settingClasses(int classes) {
        RadioButton radio5 = findViewById(R.id.radio_5);
        RadioButton radio6 = findViewById(R.id.radio_6);
        RadioButton radio7 = findViewById(R.id.radio_7);
        EditText start_time6 = findViewById(R.id.start_time_6);
        EditText start_time7 = findViewById(R.id.start_time_7);

        if (classes == 5) {
            radio5.setChecked(true);
            start_time6.setEnabled(false);
            start_time7.setEnabled(false);
        } else if (classes == 6) {
            radio6.setChecked(true);
            start_time6.setEnabled(true);
            start_time7.setEnabled(false);
        } else if (classes == 7) {
            radio7.setChecked(true);
            start_time6.setEnabled(true);
            start_time7.setEnabled(true);
        }

        radio5.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                db.collection("user").document(userID)
                        .update("最大授業数", 5);
                start_time6.setEnabled(false);
                start_time7.setEnabled(false);
            }
        });
        radio6.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                db.collection("user").document(userID)
                        .update("最大授業数", 6);
                start_time6.setEnabled(true);
                start_time7.setEnabled(false);
            }
        });
        radio7.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                db.collection("user").document(userID)
                        .update("最大授業数", 7);
                start_time6.setEnabled(true);
                start_time7.setEnabled(true);
            }
        });
    }

    // 表示する曜日を設定するスピナーの設定
    private void settingDays(int days) {
        Spinner spinner = findViewById(R.id.spinnerDayDisplay);
        String[] daysList = {"平日のみ", "平日＋土", "平日＋土日"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                daysList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int selectedDays, long id) {
                db.collection("user").document(userID)
                        .update("表示する曜日", selectedDays + 5);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 選択解除時の処理
            }
        });
    }

    // 時刻入力
    private void showTimePickerDialog(final EditText editText, String fieldName) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%2d:%02d", selectedHour, selectedMinute);
                    editText.setText(time);
                    db.collection("user").document(userID)
                            .update(fieldName, time);
                }, hour, minute, true); // 'true' for 24-hour format

        timePickerDialog.show();
    }

    // 開始時刻の設定
    private void settingStartTime(String[] startTimes){
        int[] timeFieldIds = {
                R.id.start_time_1, R.id.start_time_2, R.id.start_time_3,
                R.id.start_time_4, R.id.start_time_5, R.id.start_time_6, R.id.start_time_7
        };
        for (int i = 0; i < timeFieldIds.length; i++) {
            EditText timeField = findViewById(timeFieldIds[i]);
            timeField.setText(startTimes[i]);
            int finalI = i;
            timeField.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showTimePickerDialog(timeField, String.format("開始時刻.%d限", finalI + 1));
                    return true;
                }
                return false;
            });
        }
    }

    // 一日欠席通知に関する設定
    private void settingAbsence(boolean ANE, String ANT) {
        Switch notificationSwitch = findViewById(R.id.switch_absence);
        EditText absence_time = findViewById(R.id.absence_time);
        notificationSwitch.setChecked(ANE);
        absence_time.setEnabled(ANE);
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("user").document(userID)
                    .update("一日欠席通知設定", isChecked);
            absence_time.setEnabled(isChecked);
        });

        absence_time.setText(ANT);
        absence_time.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                showTimePickerDialog(absence_time, "一日欠席通知時間");
                return true;
            }
            return false;
        });
    }
}