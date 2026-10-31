package br.com.fmlab.redmi;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pequeno banco de favoritos offline (somente dentro deste aparelho). */
public final class StationStore {
    public static final int MIN_TENTHS = 761;  // 76,1 MHz: eFM no Brasil
    public static final int MAX_TENTHS = 1079; // 107,9 MHz
    private static final String KEY = "favorites_json_v2";
    private final SharedPreferences prefs;

    public StationStore(Context context) {
        prefs = context.getSharedPreferences("fm_livre_stations", Context.MODE_PRIVATE);
    }

    public static final class Station {
        public final int frequencyTenths;
        public final String name;
        Station(int tenths, String name) { this.frequencyTenths = tenths; this.name = name; }
    }

    public static boolean isValid(int tenths) {
        return tenths >= MIN_TENTHS && tenths <= MAX_TENTHS;
    }

    public static String frequency(int tenths) {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", tenths / 10.0);
    }

    public List<Station> list() {
        List<Station> stations = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                int f = item.optInt("freq", -1);
                if (!isValid(f)) continue;
                String name = item.optString("name", "Emissora");
                stations.add(new Station(f, name));
            }
        } catch (JSONException ignored) {
            // Dados locais inválidos não derrubam o aplicativo.
        }
        Collections.sort(stations, Comparator.comparingInt(s -> s.frequencyTenths));
        return stations;
    }

    public void put(int frequencyTenths, String name) {
        if (!isValid(frequencyTenths)) return;
        List<Station> all = list();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).frequencyTenths == frequencyTenths) {
                all.remove(i);
                break;
            }
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() > 40) trimmed = trimmed.substring(0, 40);
        if (trimmed.isEmpty()) trimmed = "Estação " + frequency(frequencyTenths);
        all.add(new Station(frequencyTenths, trimmed));
        save(all);
    }

    public void delete(int frequencyTenths) {
        List<Station> all = list();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).frequencyTenths == frequencyTenths) all.remove(i);
        }
        save(all);
    }

    public Station at(int tenths) {
        for (Station station : list()) if (station.frequencyTenths == tenths) return station;
        return null;
    }

    private void save(List<Station> all) {
        JSONArray array = new JSONArray();
        for (Station station : all) {
            JSONObject item = new JSONObject();
            try {
                item.put("freq", station.frequencyTenths);
                item.put("name", station.name);
                array.put(item);
            } catch (JSONException ignored) { }
        }
        prefs.edit().putString(KEY, array.toString()).apply();
    }
}
