package io.github.shun.osugi.pblt3.android;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AttendanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        // ヘッダーのタイトルを動的に変更
        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("出席確認");

        // フッターのクリックイベントを設定
        FooterUtils.setupFooter(this);
    }
}