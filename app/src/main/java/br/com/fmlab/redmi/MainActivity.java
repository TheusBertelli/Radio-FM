package br.com.fmlab.redmi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Aplicativo FM offline sem root. O seletor NÃO sintoniza o hardware:
 * a sintonia ainda é feita no aplicativo do fabricante, quando disponível.
 */
public final class MainActivity extends Activity {
    private int bg, surface, raised, border, primary, secondary, accent, orange;
    private boolean light;
    private SharedPreferences prefs;
    private StationStore stationStore;
    private FmInspector.Scan scan;
    private int selected = 999;
    private int tab = 0;
    private LinearLayout content;
    private ScrollView scroll;
    private TextView frequencyView, stationHint, savedState;
    private SeekBar frequencySeek;
    private FrequencyMeter meter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("fm_livre_ui", MODE_PRIVATE);
        stationStore = new StationStore(this);
        light = prefs.getBoolean("light", false);
        selected = prefs.getInt("frequency", 999);
        if (!StationStore.isValid(selected)) selected = 999;
        scan = FmInspector.inspect(this);
        if (state != null) {
            tab = state.getInt("tab", 0);
            selected = state.getInt("selected", selected);
        }
        style();
        render();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("tab", tab);
        state.putInt("selected", selected);
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        // Reconsulta conexões de fone e pacotes após voltar do rádio nativo.
        if (prefs != null) scan = FmInspector.inspect(this);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void style() {
        bg = light ? 0xFFF7F8F5 : 0xFF080D13;
        surface = light ? 0xFFFFFFFF : 0xFF141D28;
        raised = light ? 0xFFEAF0EA : 0xFF202E38;
        border = light ? 0xFFD7E1DB : 0xFF32434C;
        primary = light ? 0xFF15261E : 0xFFF2F7F2;
        secondary = light ? 0xFF536B60 : 0xFFADBEBA;
        accent = light ? 0xFF17663C : 0xFFA4FF9B;
        orange = light ? 0xFF855E00 : 0xFFF7D37F;
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(light ?
                (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) : 0);
        if (Build.VERSION.SDK_INT >= 35) getWindow().setDecorFitsSystemWindows(false);
    }

    private GradientDrawable round(int color, int radius, int outline) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (outline != 0) d.setStroke(dp(1), outline);
        return d;
    }

    private TextView text(String body, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(body);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setLineSpacing(dp(2), 1.04f);
        return v;
    }

    private LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void add(LinearLayout p, View v) {
        p.addView(v, new LinearLayout.LayoutParams(-1, -2));
    }

    private void gap(LinearLayout p, int h) {
        p.addView(new View(this), new LinearLayout.LayoutParams(1, dp(h)));
    }

    private LinearLayout card() {
        LinearLayout l = col();
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(round(surface, 22, border));
        add(content, l);
        return l;
    }

    private TextView button(String title, boolean solid, Runnable action) {
        TextView b = text(title, 14, solid ? (light ? Color.WHITE : 0xFF07120B) : primary, true);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(48));
        b.setPadding(dp(14), dp(13), dp(14), dp(13));
        b.setBackground(round(solid ? accent : raised, 15, solid ? 0 : border));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(v -> action.run());
        b.setContentDescription(title);
        return b;
    }

    private void heading(String title, String description) {
        add(content, text(title, 20, primary, true));
        if (description != null) {
            gap(content, 5);
            add(content, text(description, 13, secondary, false));
        }
        gap(content, 13);
    }

    private TextView tag(String label) {
        TextView v = text(label, 11, accent, true);
        v.setPadding(dp(11), dp(7), dp(11), dp(7));
        v.setBackground(round(raised, 100, border));
        return v;
    }

    private void render() { render(false); }

    private void render(boolean preserveScroll) {
        int scrollY = scroll == null || !preserveScroll ? 0 : scroll.getScrollY();
        style();
        LinearLayout root = col();
        root.setBackgroundColor(bg);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        content = col();
        content.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        setContentView(root);

        LinearLayout header = row();
        TextView title = text("◉  FM Livre", 27, primary, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView theme = button(light ? "☾" : "☀", false, () -> {
            light = !light;
            prefs.edit().putBoolean("light", light).apply();
            render(true);
        });
        header.addView(theme, new LinearLayout.LayoutParams(dp(53), dp(48)));
        add(content, header);
        gap(content, 8);
        add(content, tag("●  SEM INTERNET  ·  SEM STREAMING"));
        gap(content, 23);

        if (tab == 0) renderRadio(); else renderDiagnostics();
        gap(content, 24);
        TextView bottomNote = text("FM LIVRE  •  VERSÃO 2.0  •  EXPERIMENTAL", 10, secondary, true);
        bottomNote.setGravity(Gravity.CENTER);
        add(content, bottomNote);

        LinearLayout nav = row();
        nav.setBackgroundColor(surface);
        nav.setPadding(dp(12), dp(8), dp(12), dp(8));
        TextView radioTab = button("◉  Rádio", tab == 0, () -> { tab = 0; render(); });
        TextView diagTab = button("▤  Diagnóstico", tab == 1, () -> {
            tab = 1;
            scan = FmInspector.inspect(this);
            render();
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -2, 1);
        a.rightMargin = dp(6);
        nav.addView(radioTab, a);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, -2, 1);
        b.leftMargin = dp(6);
        nav.addView(diagTab, b);
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        if (preserveScroll) scroll.post(() -> scroll.scrollTo(0, scrollY));
    }

    private void renderRadio() {
        add(content, text("Sua rádio, sem gastar dados.", 16, secondary, false));
        gap(content, 18);

        LinearLayout dial = card();
        dial.setBackground(round(light ? 0xFFE5F5EB : 0xFF162B28, 25, light ? 0xFFB7DBBE : 0xFF295249));
        TextView hint = text("PRÉ-SELEÇÃO DE FREQUÊNCIA", 11, accent, true);
        hint.setGravity(Gravity.CENTER);
        add(dial, hint);
        gap(dial, 7);
        frequencyView = text(StationStore.frequency(selected) + " MHz", 47, primary, true);
        frequencyView.setGravity(Gravity.CENTER);
        add(dial, frequencyView);
        stationHint = text("Pronto para salvar como favorita", 12, secondary, false);
        stationHint.setGravity(Gravity.CENTER);
        add(dial, stationHint);
        gap(dial, 12);
        meter = new FrequencyMeter(this);
        meter.frequency = selected;
        meter.markerColor = accent;
        meter.mutedColor = secondary;
        dial.addView(meter, new LinearLayout.LayoutParams(-1, dp(76)));
        gap(dial, 6);

        frequencySeek = new SeekBar(this);
        frequencySeek.setMax(StationStore.MAX_TENTHS - StationStore.MIN_TENTHS);
        frequencySeek.setProgress(selected - StationStore.MIN_TENTHS);
        frequencySeek.setContentDescription("Pré-seleção da frequência FM em megahertz");
        if (Build.VERSION.SDK_INT >= 21) {
            frequencySeek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
            frequencySeek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        }
        frequencySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
                if (fromUser) updateFrequency(StationStore.MIN_TENTHS + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seek) { }
            @Override public void onStopTrackingTouch(SeekBar seek) { saveFrequency(); }
        });
        add(dial, frequencySeek);
        LinearLayout controls = row();
        TextView minus = button("− 0,1", false, () -> step(-1));
        TextView plus = button("+ 0,1", false, () -> step(1));
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
        left.rightMargin = dp(7);
        controls.addView(minus, left);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
        right.leftMargin = dp(7);
        controls.addView(plus, right);
        add(dial, controls);
        gap(dial, 13);
        TextView caution = text("A frequência acima é uma anotação local. Ela NÃO altera o sintonizador da Xiaomi.",
                12, light ? 0xFF62653B : 0xFFF2DEA2, false);
        caution.setGravity(Gravity.CENTER);
        add(dial, caution);
        refreshStationPreview();
        gap(content, 13);

        add(content, button("◉  Abrir rádio FM do celular", true, this::launchNative));
        gap(content, 8);
        add(content, text("O áudio FM real continua sendo controlado pelo aplicativo original do aparelho. " +
                "Selecione nele a frequência desejada.", 12, secondary, false));
        gap(content, 13);
        add(content, button("☆  Salvar / editar frequência favorita", false, this::editFavorite));
        gap(content, 25);

        heading("Estações favoritas", "Salvas neste celular. Toque para selecionar; remova ou renomeie quando quiser.");
        List<StationStore.Station> stations = stationStore.list();
        if (stations.isEmpty()) {
            LinearLayout empty = card();
            add(empty, text("Nenhuma estação salva", 16, primary, true));
            gap(empty, 5);
            add(empty, text("Escolha uma frequência acima, dê um nome e toque em Salvar.", 13, secondary, false));
        } else {
            for (StationStore.Station station : stations) {
                LinearLayout c = card();
                LinearLayout line = row();
                LinearLayout labels = col();
                add(labels, text(station.name, 15, primary, true));
                gap(labels, 4);
                add(labels, text(StationStore.frequency(station.frequencyTenths) + " MHz  ·  favorito offline", 12, secondary, false));
                line.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
                TextView choose = button("Selecionar", false, () -> {
                    updateFrequency(station.frequencyTenths);
                    saveFrequency();
                    Toast.makeText(this, "Frequência pré-selecionada; sintonize no rádio original", Toast.LENGTH_LONG).show();
                });
                line.addView(choose, new LinearLayout.LayoutParams(-2, -2));
                add(c, line);
                gap(c, 10);
                LinearLayout actions = row();
                TextView rename = text("Renomear", 13, accent, true);
                rename.setPadding(0, dp(6), dp(20), dp(6));
                rename.setOnClickListener(v -> editFavoriteFor(station.frequencyTenths));
                actions.addView(rename);
                TextView delete = text("Excluir", 13, secondary, true);
                delete.setPadding(dp(10), dp(6), dp(6), dp(6));
                delete.setOnClickListener(v -> confirmDelete(station));
                actions.addView(delete);
                add(c, actions);
                gap(content, 10);
            }
        }
        gap(content, 25);
        heading("Como escutar sem internet", null);
        LinearLayout instructions = card();
        add(instructions, text("1  Conecte um fone com fio para servir de antena, se necessário.", 13, primary, false));
        gap(instructions, 10);
        add(instructions, text("2  Desligue Wi-Fi e dados móveis.", 13, primary, false));
        gap(instructions, 10);
        add(instructions, text("3  Abra o rádio FM original e sintonize a emissora desejada.", 13, primary, false));
        gap(instructions, 10);
        add(instructions, text("4  Para investigar o controle direto, consulte a aba Diagnóstico.", 13, primary, false));
    }

    private void refreshStationPreview() {
        if (stationHint == null) return;
        StationStore.Station s = stationStore.at(selected);
        stationHint.setText(s == null ? "Frequência ainda não salva" : "☆ " + s.name + " (favorita)");
    }

    private void updateFrequency(int freq) {
        selected = Math.max(StationStore.MIN_TENTHS, Math.min(StationStore.MAX_TENTHS, freq));
        if (frequencyView != null) frequencyView.setText(StationStore.frequency(selected) + " MHz");
        if (frequencySeek != null && frequencySeek.getProgress() != selected - StationStore.MIN_TENTHS) {
            frequencySeek.setProgress(selected - StationStore.MIN_TENTHS);
        }
        if (meter != null) { meter.frequency = selected; meter.invalidate(); }
        refreshStationPreview();
    }

    private void step(int delta) { updateFrequency(selected + delta); saveFrequency(); }

    private void saveFrequency() { prefs.edit().putInt("frequency", selected).apply(); }

    private void editFavorite() { editFavoriteFor(selected); }

    private void editFavoriteFor(int frequency) {
        StationStore.Station existing = stationStore.at(frequency);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(existing == null ? "" : existing.name);
        input.setHint("Nome da emissora");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSelectAllOnFocus(true);
        LinearLayout wrapper = col();
        wrapper.setPadding(dp(22), dp(4), dp(22), 0);
        add(wrapper, input);
        new AlertDialog.Builder(this)
                .setTitle("Salvar " + StationStore.frequency(frequency) + " MHz")
                .setMessage("O favorito não controla automaticamente a frequência do rádio nativo.")
                .setView(wrapper)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    stationStore.put(frequency, input.getText().toString());
                    render(true);
                    Toast.makeText(this, "Favorito salvo apenas neste aparelho", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void confirmDelete(StationStore.Station station) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir favorita?")
                .setMessage(station.name + " · " + StationStore.frequency(station.frequencyTenths) + " MHz")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (d, w) -> {
                    stationStore.delete(station.frequencyTenths);
                    render(true);
                }).show();
    }

    private void launchNative() {
        scan = FmInspector.inspect(this);
        if (FmInspector.launchNativeRadio(this, scan)) return;
        new AlertDialog.Builder(this)
                .setTitle("Rádio nativo não acessível")
                .setMessage("O Android não permitiu abrir um dos aplicativos de FM conhecidos. " +
                        "Procure o app Rádio FM da Xiaomi manualmente. Isso não significa que o receptor esteja ausente.")
                .setPositiveButton("Entendi", null).show();
    }

    private void diagnosticRow(LinearLayout parent, String name, String detail, boolean found) {
        TextView title = text((found ? "●  " : "○  ") + name, 15, found ? accent : primary, true);
        add(parent, title);
        gap(parent, 5);
        add(parent, text(detail, 12, secondary, false));
        gap(parent, 15);
    }

    private void renderDiagnostics() {
        heading("Diagnóstico do receptor", "Verificações públicas do Android. A ausência de sinais não prova ausência do rádio.");
        LinearLayout device = card();
        add(device, text(scan.device, 19, primary, true));
        gap(device, 8);
        add(device, text("Android " + scan.android + "\n" + scan.build, 12, secondary, false));
        gap(content, 12);
        add(content, button("↻  Atualizar informações do aparelho", false, () -> {
            scan = FmInspector.inspect(this);
            render(true);
        }));
        gap(content, 18);
        heading("Acesso ao hardware", null);
        LinearLayout info = card();
        diagnosticRow(info, "Aplicativos de rádio", scan.apps.isEmpty()
                ? "Nenhum dos nomes pesquisados ficou visível para o aplicativo."
                : appNames(), !scan.apps.isEmpty());
        diagnosticRow(info, "Rotas de áudio FM", scan.fmAudioRoute
                ? join(scan.fmAudioTypes) : "TYPE_FM / TYPE_FM_TUNER não expostos.", scan.fmAudioRoute);
        diagnosticRow(info, "Fone com fio", scan.wiredHeadset
                ? "Conectado" : "Não detectado pela API de áudio.", scan.wiredHeadset);
        diagnosticRow(info, "Interfaces /dev", scan.radioNodeVisible
                ? join(scan.radioNodes) : "Não visíveis. Podem estar protegidas por SELinux.", scan.radioNodeVisible);
        diagnosticRow(info, "Declaração FM do sistema", scan.fmFeature
                ? join(scan.fmFeatures) : "Nenhuma das declarações verificadas foi encontrada.", scan.fmFeature);
        diagnosticRow(info, "Componentes FM exportados", scan.exportedComponents.isEmpty()
                ? "Não identificados ou não acessíveis. Isso não elimina a possibilidade de interfaces internas."
                : scan.exportedComponents.size() + " componente(s) identificado(s). Copie o relatório para consultar nomes e permissões. A exportação NÃO garante sintonia.",
                !scan.exportedComponents.isEmpty());
        gap(content, 16);
        heading("Teste de recepção real", "Registre a confirmação manual para manter o resultado entre sessões.");
        LinearLayout verify = card();
        String test = getPreviousTest();
        add(verify, text("sim".equals(test) ? "● Recepção offline confirmada por você" :
                "não".equals(test) ? "○ Ainda sem recepção confirmada" :
                        "Teste prático ainda não registrado", 14, primary, true));
        gap(verify, 10);
        add(verify, text("Este resultado é informado por você e não prova que um app comum tenha permissão para sintonizar FM.",
                12, secondary, false));
        gap(verify, 12);
        add(verify, button("Confirmar que ouvi rádio offline", true, this::confirmReception));
        gap(verify, 8);
        add(verify, button("Não consegui ouvir / limpar confirmação", false, () -> {
            getTestPrefs().edit().putString("practical_result", "não").apply();
            render(true);
        }));
        gap(content, 19);
        heading("Próximo passo técnico", null);
        LinearLayout next = card();
        add(next, text("Precisamos descobrir se a Xiaomi oferece alguma interface de sintonia acessível ao nosso aplicativo.",
                14, primary, false));
        gap(next, 8);
        add(next, text("As permissões ACCESS_FM_RADIO e ACCESS_BROADCAST_RADIO são reservadas a apps privilegiados. " +
                "Por enquanto não há comando público confirmado para alterar a frequência da sua ROM.",
                12, secondary, false));
        gap(next, 12);
        add(next, button("▢  Copiar diagnóstico", false, this::copyReport));
        gap(next, 8);
        add(next, button("↗  Compartilhar diagnóstico", false, this::shareReport));
        gap(next, 8);
        add(next, button("▢  Copiar comandos ADB para PC", false, this::copyAdb));
        gap(content, 20);
        add(content, text("O FM Livre não possui permissão de internet, não usa root e não faz streaming. " +
                "O diagnóstico não contorna as proteções do Android.", 12, secondary, false));
    }

    private String getPreviousTest() { return getTestPrefs().getString("practical_result", "não realizado"); }
    private SharedPreferences getTestPrefs() { return getSharedPreferences("fm_lab_user_tests", MODE_PRIVATE); }

    private void confirmReception() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar recepção real")
                .setMessage("Você ouviu uma estação FM ao vivo, com Wi-Fi e dados móveis desligados, pelo receptor do aparelho?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sim, ouvi", (d, w) -> {
                    getTestPrefs().edit().putString("practical_result", "sim").apply();
                    render(true);
                }).show();
    }

    private String appNames() {
        StringBuilder b = new StringBuilder();
        for (FmInspector.RadioApp a : scan.apps) {
            if (b.length() > 0) b.append("\n");
            b.append(a.displayName).append(" (").append(a.packageName).append(")")
                    .append(a.canLaunch ? " · pode abrir" : " · sem entrada pública")
                    .append(a.enabled ? "" : " · desativado");
        }
        return b.toString();
    }

    private String join(List<String> list) {
        StringBuilder b = new StringBuilder();
        for (String value : list) {
            if (b.length() != 0) b.append(", ");
            b.append(value);
        }
        return b.toString();
    }

    private String makeReport() {
        StringBuilder b = new StringBuilder(FmInspector.report(scan, getPreviousTest()));
        b.append("\nFM LIVRE — FAVORITAS LOCAIS\n");
        b.append("Frequência pré-selecionada: ").append(StationStore.frequency(selected)).append(" MHz\n");
        b.append("Controle direto disponível: não comprovado\n");
        for (StationStore.Station s : stationStore.list()) {
            b.append("- ").append(s.name).append(": ")
                    .append(StationStore.frequency(s.frequencyTenths)).append(" MHz\n");
        }
        return b.toString();
    }

    private void copyString(String label, String value) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) return;
        manager.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, "Copiado para a área de transferência", Toast.LENGTH_SHORT).show();
    }

    private void copyReport() { copyString("Relatório FM Livre", makeReport()); }

    private void shareReport() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Relatório FM Livre");
        i.putExtra(Intent.EXTRA_TEXT, makeReport());
        try { startActivity(Intent.createChooser(i, "Compartilhar relatório")); }
        catch (RuntimeException ex) { Toast.makeText(this, "Nenhum aplicativo para compartilhar", Toast.LENGTH_LONG).show(); }
    }

    private void copyAdb() {
        String commands = "adb devices\n" +
                "adb shell getprop ro.product.model\n" +
                "adb shell getprop ro.build.version.release\n" +
                "adb shell pm list packages | grep -iE 'fm|radio|miui'\n" +
                "adb shell dumpsys package com.miui.fm\n" +
                "adb shell dumpsys media.audio_policy\n";
        copyString("Comandos ADB Linux/macOS (no Windows use findstr)", commands);
        new AlertDialog.Builder(this)
                .setTitle("Comandos copiados")
                .setMessage("Execute num computador com ADB e depuração USB autorizada. " +
                        "No PowerShell, troque o trecho 'grep -iE ...' por 'findstr /I fm radio miui'. " +
                        "Os comandos apenas consultam informações; não sintonizam FM.")
                .setPositiveButton("Entendi", null).show();
    }

    /** Escala ilustrativa da frequência selecionada; NÃO indica sinal de RF ou áudio. */
    private static final class FrequencyMeter extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int frequency = 999;
        int markerColor = 0xFFA4FF9B;
        int mutedColor = 0xFFADBEBA;
        FrequencyMeter(Context context) {
            super(context);
            setContentDescription("Escala ilustrativa da frequência pré-selecionada");
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            float centerX = w / 2f;
            p.setColor(mutedColor); p.setStrokeWidth(1.6f); p.setAlpha(160);
            for (int i = -13; i <= 13; i++) {
                int value = frequency + i;
                if (!StationStore.isValid(value)) continue;
                float x = centerX + i * (w / 29f);
                float height = i % 5 == 0 ? h * 0.50f : h * 0.28f;
                c.drawLine(x, h * 0.24f, x, h * 0.24f + height, p);
            }
            p.setAlpha(255);
            p.setColor(markerColor);
            p.setStrokeWidth(4.5f);
            c.drawLine(centerX, 2, centerX, h * 0.79f, p);
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            p.setTextSize(h * 0.17f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText("76,1 — 107,9 MHz", centerX, h * 0.96f, p);
        }
    }
}
