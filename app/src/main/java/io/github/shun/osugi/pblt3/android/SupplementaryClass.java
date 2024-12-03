package io.github.shun.osugi.pblt3.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.core.ActivityScope;

//import io.github.shun.osugi.pblt3.android.databinding.ActivitySupplementaryClassBinding;

public class SupplementaryClass extends AppCompatActivity {

    //private ActivitySupplementaryClassBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        //binding = ActivitySupplementaryClassBinding.inflate(getLayoutInflater());
        //setContentView(binding.getRoot());

        //setContentView(R.layout.activity_supplementary_class);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ドラムロール式DatePickerのクリックリスナーを設定
        //binding.datePickerActions.setOnClickListener(v -> {
        //   DatePickerHelper.showRollerDatePicker(this, binding.date);
       // });

    }
}