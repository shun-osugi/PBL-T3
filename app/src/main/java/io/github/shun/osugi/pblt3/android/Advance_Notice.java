package io.github.shun.osugi.pblt3.android;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.shun.osugi.pblt3.android.databinding.ActivityAdvanceNoticeBinding;

public class Advance_Notice extends AppCompatActivity implements DatePickerDialog.OnDateSetListener  {

    private ActivityAdvanceNoticeBinding binding;

    private final String[] classArray = {"1限目", "2限目", "3限目", "4限目", "5限目", "6限目", "7限目"};
    private final boolean[] selectedClasses = new boolean[classArray.length]; // 選択状態を保持する配列
    private final ArrayList<String> selectedClassList = new ArrayList<>(); // 選択された時限を保存

    private String selectedDate = ""; // 選択された日付を保持

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAdvanceNoticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 日付・時限選択の初期化
        binding.textView.setText(""); // 日付の初期化
        binding.textView2.setText(""); // 時限の初期化

        // 「登録」ボタンをクリック時に Firestore へデータ保存
        binding.addButton.setOnClickListener(this::registerSupplementaryClass);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // 日付を表示する
        String dateString = String.format(Locale.US, "%d/%d/%d", year, monthOfYear + 1, dayOfMonth);
        binding.textView.setText(dateString);
    }

    // DatePickerダイアログを表示するメソッド
    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePick();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    // Firestore にデータを登録する
    public void registerSupplementaryClass(View v) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Firestore パス構造の各要素
        String userId = "yourUserId"; // TODO: ユーザーIDは Firebase Authentication などで取得
        String selectedDate = binding.textView.getText().toString(); // 選択された日付
        String selectedPeriodText = binding.textView2.getText().toString(); // 選択された時限（テキスト）

        // 入力データチェック
        if (selectedDate.isEmpty() || selectedPeriodText.isEmpty()) {
            Toast.makeText(this, "日付と時限を選択してください", Toast.LENGTH_SHORT).show();
            return;
        }

        String dayOfWeek = "yourDayOfWeek"; // TODO: 曜日データを適切に設定（現在固定値）

        // 選択された時限をループ処理
        String[] selectedPeriods = selectedPeriodText.split(" ");
        for (String period : selectedPeriods) {
            // Firestore ドキュメントパス: timetable/{userId}/{曜日}/{時限}
            String documentPath = String.format("timetable/%s/%s/%s", userId, dayOfWeek, period);

            // Firestore に保存するデータ
            Map<String, Object> supplementaryClass = new HashMap<>();
            supplementaryClass.put("授業日", selectedDate);
            supplementaryClass.put("出欠（1~3）", false);
            supplementaryClass.put("遅刻", false);

            Map<String, Object> lectureSchedule = new HashMap<>();
            lectureSchedule.put("第16回", supplementaryClass);

            // Firestore に保存処理
            db.document(documentPath)
                    .set(lectureSchedule, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        // 成功時の処理
                        showConfirmationDialog(selectedDate, period);
                        navigateToTimetableActivity();
                    })
                    .addOnFailureListener(e -> {
                        // エラー処理
                        Toast.makeText(this, "登録に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ダイアログの表示
    private void showConfirmationDialog(String date, String period) {
        new AlertDialog.Builder(this)
                .setTitle("登録完了")
                .setMessage(date + " " + period + " に補講情報を登録しました")
                .setPositiveButton("OK", null)
                .show();
    }

    // TimetableActivity への遷移
    private void navigateToTimetableActivity() {
        Intent intent = new Intent(this, TimetableActivity.class);
        startActivity(intent);
    }

    public void onCancelButtonClick(View view) {
        // キャンセルボタンが押された場合の処理
        Intent intent = new Intent(this, TimetableActivity.class);
        startActivity(intent);
        finish(); // 現在の画面を閉じる
    }

}