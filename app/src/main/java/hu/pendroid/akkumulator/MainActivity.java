package hu.pendroid.akkumulator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.text.InputFilter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialAutoCompleteTextView actvActivity;

    // A tevékenység választó (legördülő menü, + gomb, szerkesztés, törlés, mentés) külön osztályban van
    private ActivityPicker activityPicker;

    // A felvett tevékenységek (név -> időtartam + fogyasztás)
    Map<String, ActivityItem> activity = new HashMap<>();

    // A terv sorai a képernyőn (név -> sor), hogy ugyanazt a tevékenységet felül tudjuk írni
    private final Map<String, View> planRows = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // A kinézet világos témára készült, sötét módban a szövegek eltűnnének a fehér kártyákon.
        // Ennek az első sorban kell lennie, a super.onCreate előtt.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

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

        // Tevékenység választó: a legördülő menü sorait, a + gombot és a mentést az ActivityPicker intézi
        TextInputLayout tilActivity = findViewById(R.id.tilActivity);
        activityPicker = new ActivityPicker(this, actvActivity, tilActivity, findViewById(R.id.btnNewActivity));

        // ☰ opens the sidebar
        findViewById(R.id.btnMenu).setOnClickListener(
                v -> drawerLayout.openDrawer(GravityCompat.START));

        Button btnCalculate = findViewById(R.id.btnCalculate);
        Button btnAddActivity = findViewById(R.id.btnAddActivity);
        EditText etCurrentBattery = findViewById(R.id.etCurrentBattery);
        EditText etTargetReserve = findViewById(R.id.etTargetReserve);
        EditText etDuration = findViewById(R.id.etDuration);
        EditText etActivityHours = findViewById(R.id.etActivityHours);

        LinearLayout llActivityList = findViewById(R.id.llActivityList);
        TextView tvEmptyList = findViewById(R.id.tvEmptyList);

        // Szűrő: meggátolja, hogy 100-nál nagyobb számot írhassanak be a %-os mezőkbe.
        // Minden leütésnél lefut, még mielőtt a szöveg megváltozna: összerakja, milyen lenne
        // a mező tartalma a leütés után, és ha ez 100 fölé menne, üres szöveget ad vissza
        // (vagyis a leütés nem történik meg). A null azt jelenti: engedd át.
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

        // A tartalék nem lehet több a jelenlegi töltöttségnél. Gépelés közben nem ellenőrizzük
        // (lehet, hogy a tartalékot írják be előbb), hanem amikor kilépnek valamelyik mezőből.
        etTargetReserve.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) reserveFitsBattery(etCurrentBattery, etTargetReserve);
        });
        etCurrentBattery.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) reserveFitsBattery(etCurrentBattery, etTargetReserve);
        });

        btnAddActivity.setOnClickListener(v -> {
            String activityName = actvActivity.getText().toString().trim();
            String hoursStr = etActivityHours.getText().toString().trim();

            // A fogyasztást a kiválasztott tevékenységből vesszük, ezért annak szerepelnie kell a listában
            Double catalogRate = activityPicker.getRate(activityName);
            if (catalogRate == null) {
                actvActivity.setError("Válassz ki egy tevékenységet!");
                return;
            }
            if (hoursStr.isEmpty()) {
                etActivityHours.setError("Add meg az időtartamot!");
                return;
            }

            try {
                double hours = Double.parseDouble(hoursStr);
                double rate = catalogRate;

                if (hours <= 0) {
                    etActivityHours.setError("Az időtartamnak 0-nál nagyobbnak kell lennie!");
                    return;
                }

                // A teljes időtartam kell, hogy tudjuk, belefér-e a tevékenység.
                // Üres mezőnél előbb ezt kérjük be, mert nélküle nem tudunk ellenőrizni.
                String durationText = etDuration.getText().toString().trim();
                if (durationText.isEmpty()) {
                    etDuration.setError("Először add meg, mennyi ideig kell kitartania!");
                    return;
                }
                double duration = Double.parseDouble(durationText);

                // Eddig hozzáadott tevékenységek hossza (ha felülírjuk a meglévőt, levonjuk a régit)
                double currentTotalHours = getTotalHours() - (activity.containsKey(activityName) ? activity.get(activityName).duration : 0);

                if (currentTotalHours + hours > duration) {
                    double availableHours = Math.max(0, duration - currentTotalHours);
                    etActivityHours.setError(String.format("A tevékenységek hossza túlnyúlik a megadott időtartamon! (max. %.1f óra, szabad: %.1f óra)", duration, availableHours));
                    return;
                }

                // Ha ez a tevékenység már szerepel a tervben, a régi sort töröljük, az új felülírja
                View oldRow = planRows.get(activityName);
                if (oldRow != null) {
                    llActivityList.removeView(oldRow);
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
                    planRows.remove(activityName);
                    // 2. Eltávolítjuk a nézetet a LinearLayout-ból
                    llActivityList.removeView(itemView);
                    // 3. Ha ez volt az utolsó sor, visszajön a "nincs tevékenység" felirat
                    if(llActivityList.getChildCount() == 0){
                        tvEmptyList.setVisibility(View.VISIBLE);
                    }
                });

                // ui sáv kiirása
                llActivityList.addView(itemView);
                planRows.put(activityName, itemView);
                // Az első sor felvétele után eltűnik a "nincs tevékenység" felirat
                if(llActivityList.getChildCount() < 2){
                    tvEmptyList.setVisibility(View.GONE);
                }

                // lenullázzuk az inputokat
                etActivityHours.setText("");
                activityPicker.clearSelection();
            } catch (NumberFormatException e) {
                // pl. ha csak egy pont van a mezőben
                etActivityHours.setError("Ez nem érvényes szám!");
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

                    // Az időtartam: egyszer olvassuk be, itt ellenőrizzük, és végig ezt használjuk
                    double duration = Double.parseDouble(durationStr);
                    if (duration <= 0) {
                        etDuration.setError("Az időtartamnak 0-nál nagyobbnak kell lennie!");
                        return;
                    }

                    if (currentBattery > 100) {
                        etCurrentBattery.setError("A jelenlegi töltöttség nem lehet 100%-nál több!");
                        return;
                    }
                    if (targetReserve > 100) {
                        etTargetReserve.setError("A megőrzendő tartalék nem lehet 100%-nál több!");
                        return;
                    }

                    // Töltéssel nem számolunk, ezért a tartalék nem lehet több a mostani töltöttségnél
                    if (!reserveFitsBattery(etCurrentBattery, etTargetReserve)) {
                        return;
                    }

                    // Passzív (készenléti) fogyasztási ráta (%/óra) - pl. 1.2%/óra
                    double passiveRate = 1.2;

                    // Az aktív órák összege, és a tevékenységek fogyasztása (óra * %/óra)
                    double totalHours = getTotalHours();
                    double totalConsumption = 0;

                    for (ActivityItem item : activity.values()) {
                        totalConsumption += item.duration * item.consumption;
                    }

                    // Passzív fogyasztás órái (az aktív időn kívül)
                    double passiveHours = Math.max(0, duration - totalHours);
                    double totalPassiveConsumption = passiveHours * passiveRate;

                    // Ennyi marad a végén (az int levágja a tizedeseket)
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
                    // Maximális használati idő: a költhető % (akku - tartalék) osztva az óránkénti átlagfogyasztással
                    if (tvMaxTime != null) {
                        double totalDrain = totalConsumption + totalPassiveConsumption;
                        if (totalDrain > 0) {
                            tvMaxTime.setText(String.format("%.1f óra", (currentBattery - targetReserve) / (totalDrain / duration)));
                        } else {
                            // Ha nincs fogyasztás, nincs korlát (0-val osztani nem lehet)
                            tvMaxTime.setText("–");
                        }
                    }

                    if (remainingBattery >= targetReserve) {
                        if (tvVerdict != null) {
                            tvVerdict.setText("✅ A terv tartható a kívánt tartalékkal!");
                        }
                        if (tvSuggestions != null) {
                            tvSuggestions.setText("Minden rendben, az akkumulátor töltöttsége meghaladja a kívánt tartalékot (" + targetReserve + "%).");
                        }
                    } else {
                        if (tvVerdict != null) {
                            tvVerdict.setText("⚠️ A terv nem tartható a kívánt tartalékkal!");
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

    // A hozzáadott tevékenységek összes időtartama (órában)
    private double getTotalHours() {
        double totalHours = 0;
        for (ActivityItem item : activity.values()) {
            totalHours += item.duration;
        }
        return totalHours;
    }

    // Igaz, ha a tartalék nem több a jelenlegi töltöttségnél. Különben hibát ír a tartalék mezőre.
    // Ha valamelyik mező még üres, nincs mit összehasonlítani, ezért igazat adunk vissza.
    private boolean reserveFitsBattery(EditText etCurrent, EditText etReserve) {
        String current = etCurrent.getText().toString().trim();
        String reserve = etReserve.getText().toString().trim();

        if (current.isEmpty() || reserve.isEmpty()) {
            return true;
        }
        if (Integer.parseInt(reserve) > Integer.parseInt(current)) {
            etReserve.setError("A tartalék nem lehet több, mint a jelenlegi töltöttség!");
            return false;
        }
        return true;
    }
}