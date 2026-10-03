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

import android.text.InputFilter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.time.LocalTime;
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

        // Szűrő: meggátolja, hogy 100-nál nagyobb számot írhassanak be a %-os mezőkbe
        InputFilter max100Filter = (source, start, end, dest, dstart, dend) -> {
            try {
                String input = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());
                if (input.isEmpty()) return null;
                double val = Double.parseDouble(input);
                if (val <= 100) return null;
            } catch (NumberFormatException ignored) {}
            return "";
        };

        etCurrentBattery.setFilters(new InputFilter[]{ max100Filter });
        etTargetReserve.setFilters(new InputFilter[]{ max100Filter });
        etActivityRate.setFilters(new InputFilter[]{ max100Filter });

        btnAddActivity.setOnClickListener(v -> {
            String hoursStr = etActivityHours.getText().toString().trim();
            String rateStr = etActivityRate.getText().toString().trim();
            String activityName = actvActivity.getText().toString().trim();

            if (activityName.isEmpty()) {
                actvActivity.setError("Válassz ki egy tevékenységet!");
                return;
            }

            if (!hoursStr.isEmpty() && !rateStr.isEmpty()) {
                try {
                    double hours = Double.parseDouble(hoursStr);
                    double rate = Double.parseDouble(rateStr);

                    if (hours <= 0) {
                        etActivityHours.setError("Az időtartamnak 0-nál nagyobbnak kell lennie!");
                        return;
                    }

                    if (rate > 100) {
                        etActivityRate.setError("A fogyasztás nem lehet 100%-nál több!");
                        return;
                    }

                    double maxAllowedHours = getMaxAllowedHours(etDuration.getText().toString().trim());

                    // Eddig hozzáadott tevékenységek hossza (ha felülírjuk a meglévőt, levonjuk a régit)
                    double currentTotalHours = getTotalHours() - (activity.containsKey(activityName) ? activity.get(activityName).duration : 0);

                    if (currentTotalHours + hours > maxAllowedHours) {
                        double availableHours = Math.max(0, maxAllowedHours - currentTotalHours);
                        etActivityHours.setError(String.format("A tevékenységek hossza túlnyúlik a nap végén! (max. %.1f óra, szabad: %.1f óra)", maxAllowedHours, availableHours));
                        return;
                    }

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
                    });

                    // ui sáv kiirása
                    llActivityList.addView(itemView);

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

                    if (currentBattery > 100) {
                        etCurrentBattery.setError("A jelenlegi töltöttség nem lehet 100%-nál több!");
                        return;
                    }
                    if (targetReserve > 100) {
                        etTargetReserve.setError("A megőrzendő tartalék nem lehet 100%-nál több!");
                        return;
                    }

                    double hoursUntilMidnight = getHoursUntilMidnight();
                    double effectiveDuration = getMaxAllowedHours(durationStr);

                    // Passzív (készenléti) fogyasztási ráta (%/óra) - pl. 1.2%/óra
                    double passiveRate = 1.2;

                    double totalHours = getTotalHours();
                    double totalConsumption = 0;

                    for (ActivityItem item : activity.values()) {
                        totalConsumption += item.duration * item.consumption;
                    }

                    // Passzív fogyasztás órái (az aktív időn kívül)
                    double passiveHours = Math.max(0, effectiveDuration - totalHours);
                    double totalPassiveConsumption = passiveHours * passiveRate;

                    int remainingBattery = (int) (currentBattery - (totalConsumption + totalPassiveConsumption));

                    TextView tvResultValue = findViewById(R.id.tvResultValue);
                    TextView tvVerdict = findViewById(R.id.tvVerdict);
                    TextView tvSuggestions = findViewById(R.id.tvSuggestions);
                    TextView tvTotalDrain = findViewById(R.id.tvTotalDrain);
                    TextView tvMaxTime = findViewById(R.id.tvMaxTime);

                    if (tvResultValue != null) {
                        tvResultValue.setText(remainingBattery + "%");
                    }
                    if (tvTotalDrain != null) {
                        tvTotalDrain.setText(String.format("%.1f%%", totalConsumption + totalPassiveConsumption));
                    }
                    if (tvMaxTime != null) {
                        tvMaxTime.setText(String.format("%.1f óra", hoursUntilMidnight));
                    }

                    if (remainingBattery >= targetReserve) {
                        if (tvVerdict != null) {
                            tvVerdict.setText("✅ Az akkumulátor kitart éjfélig!");
                        }
                        if (tvSuggestions != null) {
                            tvSuggestions.setText("Minden rendben, az akkumulátor töltöttsége meghaladja a kívánt tartalékot (" + targetReserve + "%).");
                        }
                    } else {
                        if (tvVerdict != null) {
                            tvVerdict.setText("⚠️ Az akkumulátor le fog merülni éjfél előtt!");
                        }

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
                        } else {
                            suggestion.append("• Töltsd fel a telefont 100%-ra (de önmagában a 100% sem lesz elég, a használatot is csökkentened kell!).\n");
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

    private double getTotalHours() {
        double totalHours = 0;
        for (ActivityItem item : activity.values()) {
            totalHours += item.duration;
        }
        return totalHours;
    }

    private double getHoursUntilMidnight() {
        LocalTime now = LocalTime.now();
        double currentHourDecimal = now.getHour() + (now.getMinute() / 60.0);
        return 24.0 - currentHourDecimal;
    }

    private double getMaxAllowedHours(String durationStr) {
        double hoursUntilMidnight = getHoursUntilMidnight();
        if (!durationStr.isEmpty()) {
            try {
                double duration = Double.parseDouble(durationStr);
                if (duration > 0) {
                    return duration;
                }
            } catch (NumberFormatException ignored) {}
        }
        return hoursUntilMidnight;
    }
}