package io.github.shun.osugi.pblt3.android;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import io.github.shun.osugi.pblt3.android.databinding.ActivityAdvanceNoticeBinding;

public class Advance_Notice extends AppCompatActivity implements DatePickerDialog.OnDateSetListener  {

    private ActivityAdvanceNoticeBinding binding;

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

        // DatePickerが選択されたときに表示するTextView
        binding.textView.setText(""); // 初期化
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
}