package io.github.shun.osugi.pblt3.android;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.widget.TextView;
//時間割に関するプログラム

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FirestoreExample";
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);


        // Firebaseの初期化
        FirebaseApp.initializeApp(this);

        // Firestoreのインスタンスを取得
        db = FirebaseFirestore.getInstance();

        // Firestoreにデータを追加するメソッドを呼び出し
        addData();

        // ヘッダーのタイトルを動的に変更
        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("時間割");

        // フッターのクリックイベントを設定
        FooterUtils.setupFooter(this);
    }
    private void addData() {
        // Firestoreに保存するデータ
        Map<String, Object> data = new HashMap<>();
        data.put("sampleField", "Hello, Firestore!");

        // データをFirestoreのコレクションに追加
        db.collection("sampleCollection").add(data);
    }
}