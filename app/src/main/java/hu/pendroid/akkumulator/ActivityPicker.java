package hu.pendroid.akkumulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tevékenység választó: a legördülő menü, a hozzá tartozó + gomb, a szerkesztő és törlő ablakok,
 * és a tevékenységek mentése. Külön osztályban van, hogy a MainActivity-t alig kelljen módosítani.
 */
public class ActivityPicker {

    // Saját mentési fájl, hogy ne keveredjen más beállításokkal
    private static final String PREFS_NAME = "pendroid_activities";
    private static final String KEY_CATALOG = "activity_catalog";

    private final AppCompatActivity host;
    private final MaterialAutoCompleteTextView actv;
    private final TextInputLayout til;
    private final SharedPreferences prefs;
    private final ActivityAdapter adapter;

    // A választható tevékenységek (név -> fogyasztás %/óra). A LinkedHashMap megőrzi a sorrendet.
    private final LinkedHashMap<String, Double> catalog = new LinkedHashMap<>();

    // Szűrő a fogyasztás mezőre: nem engedi 100 fölé (a vesszőt pontnak veszi)
    private final InputFilter maxRateFilter = (source, start, end, dest, dstart, dend) -> {
        try {
            String input = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());
            if (input.isEmpty()) return null;
            if (Double.parseDouble(input.replace(',', '.')) <= 100) return null;
        } catch (NumberFormatException ignored) {}
        return "";
    };

    public ActivityPicker(AppCompatActivity host, MaterialAutoCompleteTextView actv,
                          TextInputLayout til, View btnNew) {
        this.host = host;
        this.actv = actv;
        this.til = til;
        this.prefs = host.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        loadCatalog();
        adapter = new ActivityAdapter();
        actv.setAdapter(adapter);

        // A lenyíló lista háttere: fehér és lekerekített, hogy illjen a kártyákhoz.
        // A sarok sugarát dp-ben adjuk meg, ezért szorozni kell a kijelző sűrűségével.
        GradientDrawable popupBackground = new GradientDrawable();
        popupBackground.setColor(Color.WHITE);
        popupBackground.setCornerRadius(16 * host.getResources().getDisplayMetrics().density);
        actv.setDropDownBackgroundDrawable(popupBackground);

        // Ha kiválasztanak egy tevékenységet, a mező alatt megjelenik a fogyasztása
        actv.setOnItemClickListener((parent, view, position, id) -> updateHelper());

        // + gomb: új tevékenység felvétele
        btnNew.setOnClickListener(v -> showActivityDialog(null));
    }

    // ------------------------------------------------------------------
    // Amit a MainActivity használ
    // ------------------------------------------------------------------

    // A tevékenység fogyasztása (%/óra), vagy null, ha nincs ilyen nevű tevékenység
    public Double getRate(String name) {
        return catalog.get(name);
    }

    // Kiüríti a választást (a false azt jelenti: ne szűrje a listát)
    public void clearSelection() {
        actv.setText("", false);
        updateHelper();
    }

    // ------------------------------------------------------------------
    // Mentés és betöltés
    // ------------------------------------------------------------------

    // Első indításnál (vagy sérült adatnál) a négy alap tevékenységgel indulunk
    private void loadCatalog() {
        catalog.clear();
        String json = prefs.getString(KEY_CATALOG, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    catalog.put(o.getString("name"), o.getDouble("rate"));
                }
                return;
            } catch (JSONException e) {
                catalog.clear();
            }
        }
        // Az alap fogyasztások becsült értékek (%/óra), a felhasználó átírhatja őket
        catalog.put("🎬 Videó", 12.0);
        catalog.put("🎮 Játék", 22.0);
        catalog.put("🎵 Zene", 6.0);
        catalog.put("🗺️ Navigáció", 16.0);
    }

    // A teljes listát JSON szövegként mentjük (egyetlen szöveg a SharedPreferences-ben)
    private void saveCatalog() {
        JSONArray arr = new JSONArray();
        try {
            for (Map.Entry<String, Double> e : catalog.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("name", e.getKey());
                o.put("rate", e.getValue());
                arr.put(o);
            }
        } catch (JSONException ignored) {}
        prefs.edit().putString(KEY_CATALOG, arr.toString()).apply();
    }

    // ------------------------------------------------------------------
    // Ablakok: új / szerkesztés / törlés
    // ------------------------------------------------------------------

    // A mező alatt kiírja a kiválasztott tevékenység fogyasztását (vagy eltünteti, ha nincs kiválasztva)
    private void updateHelper() {
        Double rate = catalog.get(actv.getText().toString().trim());
        if (rate == null) {
            til.setHelperText(null);
        } else {
            til.setHelperText("Fogyasztás: " + formatRate(rate) + " %/óra");
        }
    }

    // Új tevékenység (oldName == null) vagy meglévő szerkesztése. Hiba esetén az ablak nyitva marad.
    private void showActivityDialog(String oldName) {
        View dialogView = host.getLayoutInflater().inflate(R.layout.dialog_activity, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilActName);
        TextInputLayout tilRate = dialogView.findViewById(R.id.tilActRate);
        EditText etName = dialogView.findViewById(R.id.etActName);
        EditText etRate = dialogView.findViewById(R.id.etActRate);
        etRate.setFilters(new InputFilter[]{ maxRateFilter });

        boolean isEdit = oldName != null;
        if (isEdit) {
            etName.setText(oldName);
            etRate.setText(formatRate(catalog.get(oldName)));
        }

        AlertDialog alert = new MaterialAlertDialogBuilder(host)
                .setTitle(isEdit ? "Tevékenység szerkesztése" : "Új tevékenység")
                .setView(dialogView)
                // A "Mentés" gombot lent felülírjuk, mert alapból minden kattintásra bezárná az ablakot
                .setPositiveButton("Mentés", null)
                .setNegativeButton("Mégse", null)
                .show();
        tintDialogButtons(alert);

        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String rateText = etRate.getText().toString().trim();
            tilName.setError(null);
            tilRate.setError(null);

            if (newName.isEmpty()) {
                tilName.setError("Adj meg egy nevet!");
                return;
            }
            // Két azonos nevű tevékenység összekeveredne, de a saját nevét megtarthatja
            if (catalog.containsKey(newName) && !newName.equals(oldName)) {
                tilName.setError("Ilyen nevű tevékenység már van!");
                return;
            }

            double rate;
            try {
                rate = Double.parseDouble(rateText.replace(',', '.'));
            } catch (NumberFormatException e) {
                tilRate.setError("Adj meg egy számot!");
                return;
            }
            if (rate <= 0) {
                tilRate.setError("A fogyasztásnak 0-nál nagyobbnak kell lennie!");
                return;
            }

            saveActivity(oldName, newName, rate);
            alert.dismiss();
        });
    }

    // Beírja a változást a listába, elmenti, és frissíti a legördülő menüt
    private void saveActivity(String oldName, String newName, double rate) {
        if (oldName == null) {
            catalog.put(newName, rate);
            // Az újonnan felvett tevékenységet rögtön ki is választjuk
            actv.setText(newName, false);
        } else {
            // Szerkesztésnél a sorrend ne változzon, ezért újraépítjük a listát
            LinkedHashMap<String, Double> rebuilt = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : catalog.entrySet()) {
                if (e.getKey().equals(oldName)) {
                    rebuilt.put(newName, rate);
                } else {
                    rebuilt.put(e.getKey(), e.getValue());
                }
            }
            catalog.clear();
            catalog.putAll(rebuilt);
            // Ha pont ezt választották ki, a mezőben is az új név látszik
            if (actv.getText().toString().trim().equals(oldName)) {
                actv.setText(newName, false);
            }
        }
        saveCatalog();
        adapter.refresh();
        updateHelper();
    }

    // Kuka gomb: rákérdez, és csak az "Igen" után törli a tevékenységet a listából
    private void confirmDelete(String name) {
        AlertDialog alert = new MaterialAlertDialogBuilder(host)
                .setTitle("Törlés")
                .setMessage("Biztosan törlöd a(z) \"" + name + "\" tevékenységet a listából? A tervbe már felvett sorok megmaradnak.")
                .setPositiveButton("Törlés", (d, which) -> {
                    catalog.remove(name);
                    saveCatalog();
                    adapter.refresh();
                    // Ha pont ezt választották ki, kiürítjük a mezőt
                    if (actv.getText().toString().trim().equals(name)) {
                        actv.setText("", false);
                    }
                    updateHelper();
                })
                .setNegativeButton("Mégse", null)
                .show();
        tintDialogButtons(alert);
    }

    // ------------------------------------------------------------------
    // Apróságok
    // ------------------------------------------------------------------

    // 12.0 helyett 12-t írunk ki, 12.5-nél marad a tizedes
    private String formatRate(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(Math.round(value * 100) / 100.0);
    }

    // A felugró ablak gombjai alapból a téma színét kapják (nálunk lila), ezért zöldre színezzük őket
    private void tintDialogButtons(AlertDialog dialog) {
        int emerald = Color.parseColor("#059669");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(emerald);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(emerald);
    }

    // ------------------------------------------------------------------
    // A legördülő menü sorai: név, fogyasztás, ceruza és kuka gomb.
    // A Filterable csak azért kell, mert a legördülő mező ezt kéri; a szűrő itt mindig az összes elemet adja vissza.
    // ------------------------------------------------------------------
    private class ActivityAdapter extends BaseAdapter implements Filterable {

        private List<String> names = new ArrayList<>(catalog.keySet());

        // Ha változott a lista, ezzel kell újratölteni
        void refresh() {
            names = new ArrayList<>(catalog.keySet());
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return names.size();
        }

        @Override
        public String getItem(int position) {
            return names.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = host.getLayoutInflater().inflate(R.layout.item_dropdown_activity, parent, false);
            }
            String name = names.get(position);
            ((TextView) convertView.findViewById(R.id.tvActName)).setText(name);
            ((TextView) convertView.findViewById(R.id.tvActRate)).setText(formatRate(catalog.get(name)) + " %/óra");

            // A gombok megnyomásakor előbb becsukjuk a listát, aztán megnyitjuk a megfelelő ablakot
            convertView.findViewById(R.id.btnActEdit).setOnClickListener(v -> {
                actv.dismissDropDown();
                showActivityDialog(name);
            });
            convertView.findViewById(R.id.btnActDelete).setOnClickListener(v -> {
                actv.dismissDropDown();
                confirmDelete(name);
            });
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = names;
                    results.count = names.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}