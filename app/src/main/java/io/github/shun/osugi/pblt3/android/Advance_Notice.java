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

        // 「時限を選択」ボタンのクリックリスナー
        binding.timeButton.setOnClickListener(this::showPeriodSelectionDialog);

        // 「日付を選択」ボタンのクリックリスナー
        binding.button1.setOnClickListener(this::showDatePickerDialog);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // 日付を表示する
        selectedDate = String.format(Locale.US, "%d/%d/%d", year, monthOfYear + 1, dayOfMonth);
        binding.textView.setText(selectedDate);
    }

    // 時限選択ダイアログの表示
    public void showPeriodSelectionDialog(View v) {
        new AlertDialog.Builder(this)
                .setTitle("時限を選択")
                .setMultiChoiceItems(classArray, selectedClasses, (dialog, which, isChecked) -> {
                    // 選択状態を保存
                    selectedClasses[which] = isChecked;

                    // 選択された時限リストを更新
                    if (isChecked) {
                        selectedClassList.add(classArray[which]);
                    } else {
                        selectedClassList.remove(classArray[which]);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    // 選択された時限をテキストビューに表示
                    binding.textView2.setText(String.join(", ", selectedClassList));
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    // DatePickerダイアログを表示するメソッド
    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePick();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    // Firestore にデータを登録する
    public void registerSupplementaryClass(View v) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 入力チェック
        if (selectedDate.isEmpty() || selectedClassList.isEmpty()) {
            Toast.makeText(this, "日付と時限を選択してください", Toast.LENGTH_SHORT).show();
            return;
        }

        // 登録確認ダイアログの表示
        showConfirmationDialog();

//        // Firestore に保存
//        for (String period : selectedClassList) {
//            String documentPath = String.format("timetable/yourUserId/%s/%s", selectedDate, period); // TODO: UserIdを適切に設定
//            Map<String, Object> data = new HashMap<>();
//            data.put("補講情報", true); // 必要に応じて拡張
//
//            db.document(documentPath)
//                    .set(data, SetOptions.merge())
//                    .addOnSuccessListener(aVoid -> {
//                        Toast.makeText(this, "登録成功: " + period, Toast.LENGTH_SHORT).show();
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(this, "登録失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    });
//        }
//
//        // 登録後に次の画面へ遷移
//        navigateToTimetableActivity();
    }

    // ダイアログの表示
//    private void showConfirmationDialog(String date, String period) {
//        new AlertDialog.Builder(this)
//                .setTitle("登録完了")
//                .setMessage(date + " " + period + " に補講情報を登録しました")
//                .setPositiveButton("OK", null)
//                .show();
//    }

    // 確認ダイアログを表示
    private void showConfirmationDialog() {
        // 選択された時限を文字列に整形
        String selectedPeriods = String.join(", ", selectedClassList);

        // 確認メッセージ
        String message = String.format(Locale.US, "%s   %s に登録します。よろしいですか？", selectedDate, selectedPeriods);

        // ダイアログ表示
        new AlertDialog.Builder(this)
                .setTitle("登録確認")
                .setMessage(message)
                .setPositiveButton("はい", (dialog, which) -> {
                    // 「はい」が押された場合にデータをFirestoreに登録
                    saveToFirestore();
                })
                .setNegativeButton("いいえ", null) // 何もしない
                .show();
    }

    // Firestoreにデータを保存
    private void saveToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (String period : selectedClassList) {
            String documentPath = String.format("timetable/yourUserId/%s/%s", selectedDate, period); // TODO: UserIdを適切に設定
            Map<String, Object> data = new HashMap<>();
            data.put("補講情報", true); // 必要に応じて拡張

            db.document(documentPath)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "登録成功: " + period, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "登録失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // 登録完了後に次の画面に遷移
        navigateToTimetableActivity();
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