package hu.pendroid.akkumulator;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialAutoCompleteTextView actvActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        actvActivity = findViewById(R.id.actvActivity);

        // padding
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Dropdown
        String[] activities = {"🎬 Videó", "🎮 Játék", "🎵 Zene", "🗺️ Navigáció"};
        actvActivity.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, activities));

        // ☰ opens the sidebar
        findViewById(R.id.btnMenu).setOnClickListener(
                v -> drawerLayout.openDrawer(GravityCompat.START));

        Button btnCalculate = findViewById(R.id.btnCalculate);
        Button btnAddActivity = findViewById(R.id.btnAddActivity);
        EditText etCurrentBattery = findViewById(R.id.etCurrentBattery);
        EditText etTargetReserve = findViewById(R.id.etTargetReserve);
        EditText etDuration = findViewById(R.id.etDuration);
        EditText etActivityHours = findViewById(R.id.etActivityHours);
        EditText etActivityRate = findViewById(R.id.etActivityRate);

        Map<String, ActivityItem> activity = new HashMap<>();

        btnAddActivity.setOnClickListener(v -> {
            String hoursStr = etActivityHours.getText().toString().trim();
            String rateStr = etActivityRate.getText().toString().trim();

            if (!hoursStr.isEmpty() && !rateStr.isEmpty()) {
                try {
                    double hours = Double.parseDouble(hoursStr);
                    double rate = Double.parseDouble(rateStr);

                    activity.put(actvActivity.getText().toString(), new ActivityItem(hours, rate));
                } catch (NumberFormatException e) {
                    // error message
                }
            }
        });

        btnCalculate.setOnClickListener(v -> {

        });
    }
}