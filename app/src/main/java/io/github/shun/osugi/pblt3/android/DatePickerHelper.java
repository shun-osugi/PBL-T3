package io.github.shun.osugi.pblt3.android;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;

public class DatePickerHelper {

    public static void showRollerDatePicker(Context context, EditText targetEditText) {
        // 現在の日付を取得
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // DatePickerをレイアウトに動的に追加
        DatePicker datePicker = new DatePicker(context);
        datePicker.setCalendarViewShown(false); // カレンダー表示を無効化
        datePicker.setSpinnersShown(true);      // スピナー表示を有効化
        datePicker.init(year, month, day, null);


        // DatePickerをDialogにセット
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(datePicker);
        builder.setTitle("日付を選択");
        builder.setPositiveButton("OK", (dialog, which) -> {
            // 選択された日付を取得
            int selectedYear = datePicker.getYear();
            int selectedMonth = datePicker.getMonth();
            int selectedDay = datePicker.getDayOfMonth();

            // フォーマットしてEditTextに設定
            String date = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
            targetEditText.setText(date);
        });
        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
