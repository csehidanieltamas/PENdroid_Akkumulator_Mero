package hu.pendroid.akkumulator;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    Map<String, ActivityItem> activity = new HashMap<>();

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

        LinearLayout llActivityList = findViewById(R.id.llActivityList);
        TextView tvEmptyList = findViewById(R.id.tvEmptyList);

        btnAddActivity.setOnClickListener(v -> {
            String hoursStr = etActivityHours.getText().toString().trim();
            String rateStr = etActivityRate.getText().toString().trim();

            if (!hoursStr.isEmpty() && !rateStr.isEmpty()) {
                try {
                    double hours = Double.parseDouble(hoursStr);
                    double rate = Double.parseDouble(rateStr);
                    String activityName = actvActivity.getText().toString().trim();

                    activity.put(activityName, new ActivityItem(hours, rate));
                    // ui sáv megszerzése
                    View itemView = getLayoutInflater().inflate(R.layout.item_tevekenyseg, llActivityList, false);

                    // Megkeresed a sornak a vezérlőit a felfújt itemView-on belül
                    TextView tvInfo = itemView.findViewById(R.id.tvInfo);
                    ImageButton btnDelete = itemView.findViewById(R.id.btnDelete);

                    // Beállítod a kiírandó szöveget
                    tvInfo.setText(activityName + " - " + hours + " óra (" + rate + "%/óra)");

                    // törlés gomb működése:
                    btnDelete.setOnClickListener(vDelete -> {
                        // 1. Kitöröljük a Java listából
                        activity.remove(activityName);
                        // 2. Eltávolítjuk a nézetet a LinearLayout-ból
                        llActivityList.removeView(itemView);
                        if(llActivityList.getChildCount() == 0){
                            tvEmptyList.setVisibility(View.VISIBLE);
                        }
                    });

                    // ui sáv kiirása
                    llActivityList.addView(itemView);
                    if(llActivityList.getChildCount() < 2){
                        tvEmptyList.setVisibility(View.GONE);
                    }

                    // lenullázzuk az inputokat
                    etActivityHours.setText("");
                    etActivityRate.setText("");
                    actvActivity.setText("");
                } catch (NumberFormatException e) {
                    // error message
                }
            }
        });

        btnCalculate.setOnClickListener(v -> {
            String currentBatteryStr = etCurrentBattery.getText().toString().trim();
            String targetReserveStr = etTargetReserve.getText().toString().trim();
            String durationStr = etDuration.getText().toString().trim();

            if (!currentBatteryStr.isEmpty() && !targetReserveStr.isEmpty() && !durationStr.isEmpty()) {
                try {
                    int currentBattery = Integer.parseInt(currentBatteryStr);
                    int targetReserve = Integer.parseInt(targetReserveStr);
                    double duration = Double.parseDouble(durationStr);

                    double passiveConsumption = 100 / duration;
                    double totalHours = 0;
                    double totalConsumption = 0;

                    for (ActivityItem item : activity.values()) {
                        totalHours += item.duration;
                        totalConsumption += item.duration * item.consumption;
                    }
                    int remainingBattery = (int) (currentBattery - (totalConsumption + (passiveConsumption * totalHours)));
                    if (remainingBattery >= targetReserve) {
                        // success message
                    } else {
                        // 1. Kiszámoljuk, hány % hiányzik a kívánt tartalékhoz képest
                        double deficit = targetReserve - remainingBattery; // pl. ha 15% maradt, de 20% a cél, akkor deficit = 5%

                        // 2. Átlagos aktív fogyasztás óránként (%/óra)
                        double avgActiveRate = totalHours > 0 ? (totalConsumption / totalHours) : 0;

                        // 3. Megkeressük a legnagyobb fogyasztású tevékenységet
                        String worstActivityName = "";
                        double worstActivityRate = 0;

                        for (Map.Entry<String, ActivityItem> entry : activity.entrySet()) {
                            if (entry.getValue().consumption > worstActivityRate) {
                                worstActivityRate = entry.getValue().consumption;
                                worstActivityName = entry.getKey();
                            }
                        }

                        // 4. Összeállítjuk a szöveges javaslatot
                        StringBuilder suggestion = new StringBuilder();
                        suggestion.append("⚠️ Nem éred el a kívánt ").append(targetReserve).append("%-os tartalékot!\n");
                        suggestion.append("Hiányzol: ").append(String.format("%.1f", deficit)).append("%\n\n");
                        suggestion.append("Javaslatok a cél eléréséhez:\n");

                        // A) Töltési javaslat:
                        int requiredBattery = (int) Math.ceil(currentBattery + deficit);

                        if (requiredBattery <= 100) {
                            suggestion.append("• Töltsd fel a telefont legalább ").append(requiredBattery).append("%-ra.\n");
                        }

                        // B) Általános időcsökkentési javaslat:
                        if (avgActiveRate > 0) {
                            double hoursToReduce = deficit / avgActiveRate;
                            suggestion.append(String.format("• Csökkentsd az aktív használatot kb. %.1f órával.\n", hoursToReduce));
                        }

                        // C) Konkrét tevékenység csökkentése (ha van ilyen):
                        if (!worstActivityName.isEmpty() && worstActivityRate > 0) {
                            double hoursFromWorst = deficit / worstActivityRate;
                            suggestion.append(String.format("• Vagy csökkentsd a(z) '%s' használatát %.1f órával.", worstActivityName, hoursFromWorst));
                        }

                        // 5. Kiírjuk az eredményt a felületre
                        TextView tvSuggestions = findViewById(R.id.tvSuggestions);
                        if (tvSuggestions != null) {
                            tvSuggestions.setText(suggestion.toString());
                        }
                    }
                } catch (NumberFormatException e){
                    // error message
                }
            }
        });
    }
}