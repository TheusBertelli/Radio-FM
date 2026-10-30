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
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


/** Interface 100% nativa, offline e sem bibliotecas externas. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 16, 22);
    private static final int SURFACE = Color.rgb(21, 30, 38);
    private static final int BORDER = Color.rgb(43, 60, 67);
    private static final int TEXT = Color.rgb(243, 248, 244);
    private static final int SECONDARY = Color.rgb(168, 182, 185);
    private static final int GREEN = Color.rgb(169, 255, 139);
    private static final int YELLOW = Color.rgb(253, 213, 119);
    private static final int RED = Color.rgb(255, 146, 140);

    private FmInspector.Scan scan;
    private SharedPreferences prefs;
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("fm_lab_user_tests", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 30) {
            // Inset explícito para Android 15+ (evita conteúdo atrás das barras).
            getWindow().setDecorFitsSystemWindows(false);
        }
        runScan();
    }

    private int dp(float x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, float radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setLineSpacing(dp(2), 1.05f);
        return t;
    }

    private LinearLayout column() {
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

    private void gap(LinearLayout parent, int size) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(size)));
    }

    private void add(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = column();
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        c.setBackground(shape(SURFACE, 22, BORDER));
        add(parent, c);
        return c;
    }

    private void section(String title, String subtitle) {
        TextView t = text(title, 19, TEXT, true);
        add(content, t);
        if (subtitle != null) {
            gap(content, 5);
            add(content, text(subtitle, 13, SECONDARY, false));
        }
        gap(content, 12);
    }

    private TextView pill(String label, int color) {
        TextView p = text(label, 11, color, true);
        p.setPadding(dp(10), dp(7), dp(10), dp(7));
        p.setBackground(shape(Color.rgb(34, 47, 46), 14, 0));
        return p;
    }

    private TextView button(String label, boolean filled, Runnable onClick) {
        TextView b = text(label, 15, filled ? BG : TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(16), dp(14), dp(16));
        b.setBackground(shape(filled ? GREEN : SURFACE, 16, filled ? 0 : BORDER));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(v -> onClick.run());
        b.setContentDescription(label);
        return b;
    }

    private void runScan() {
        scan = FmInspector.inspect(this);
        render();
    }

    private String userResult() { return prefs.getString("practical_result", "não realizado"); }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BG);
        scroll.setVerticalScrollBarEnabled(false);
        content = column();
        content.setPadding(dp(20), dp(28), dp(20), dp(34));
        scroll.addView(content);
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                content.setPadding(dp(20), bars.top + dp(28), dp(20), bars.bottom + dp(34));
                return insets;
            });
        }
        setContentView(scroll);

        LinearLayout header = row();
        TextView badge = pill("●  OFFLINE · SEM STREAMING", GREEN);
        header.addView(badge);
        add(content, header);
        gap(content, 20);
        add(content, text("FM Lab", 35, TEXT, true));
        gap(content, 7);
        add(content, text("Seu celular consegue receber rádio FM sem internet?", 15, SECONDARY, false));
        gap(content, 18);

        LinearLayout hero = card(content);
        hero.setBackground(shape(Color.rgb(24, 41, 43), 24, Color.rgb(53, 85, 77)));
        TunerVisual graphic = new TunerVisual(this);
        add(hero, graphic);
        graphic.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(142)));
        gap(hero, 9);
        TextView heroLabel = text("DIAGNÓSTICO DE RECEPTOR", 11, GREEN, true);
        heroLabel.setGravity(Gravity.CENTER);
        add(hero, heroLabel);
        gap(hero, 5);
        TextView heroDesc = text("Sem root • Sem internet • Sem dados pessoais", 12, SECONDARY, false);
        heroDesc.setGravity(Gravity.CENTER);
        add(hero, heroDesc);
        gap(content, 22);

        section("Resultado atual", "O diagnóstico não altera configurações do celular.");
        LinearLayout result = card(content);
        String test = userResult();
        String title;
        String details;
        int statusColor;
        if ("sim".equals(test)) {
            title = "Recepção FM confirmada";
            details = "Você informou que ouviu uma emissora com Wi-Fi e dados móveis desligados.";
            statusColor = GREEN;
        } else if ("não".equals(test)) {
            title = "Teste prático sem sucesso";
            details = "O teste não funcionou, mas isso não comprova ausência do receptor FM.";
            statusColor = RED;
        } else if (scan.hasAnyEvidence()) {
            title = "Indícios de suporte FM";
            details = "O Android mostrou sinais relacionados a rádio. Falta testar a recepção real.";
            statusColor = YELLOW;
        } else {
            title = "Resultado inconclusivo";
            details = "O sistema não expôs sinais conclusivos de FM. O hardware ainda pode existir.";
            statusColor = YELLOW;
        }
        add(result, text("●  " + title, 18, statusColor, true));
        gap(result, 10);
        add(result, text(details, 13, SECONDARY, false));
        gap(result, 14);
        add(result, text("" + scan.device + " · Android " + scan.android, 12, TEXT, false));
        gap(content, 14);
        add(content, button("↻   Executar diagnóstico novamente", false, this::runScan));
        gap(content, 24);

        section("Verificações do aparelho", "Um sinal positivo é um indício; um negativo pode ser uma restrição do Android.");
        StringBuilder appDetails = new StringBuilder();
        for (FmInspector.RadioApp a : scan.apps) {
            if (appDetails.length() > 0) appDetails.append("\n");
            appDetails.append(a.displayName).append(" (" ).append(a.packageName).append(")");
            if (!a.canLaunch) appDetails.append(" · sem atividade pública para abrir");
            if (!a.enabled) appDetails.append(" · desativado");
        }
        check("01", "Aplicativo FM nativo", scan.apps.isEmpty() ? "Não identificado" : "Encontrado",
                scan.apps.isEmpty() ? "Nenhum dos pacotes conhecidos ficou visível. Isso não descarta outros apps ou um chip FM." : appDetails.toString(),
                !scan.apps.isEmpty());
        gap(content, 10);
        check("02", "Rota de áudio FM", scan.fmAudioRoute ? "Exposta" : "Não exposta",
                scan.fmAudioRoute ? join(scan.fmAudioTypes) : "Nenhum dispositivo TYPE_FM ou TYPE_FM_TUNER foi exposto à API pública.",
                scan.fmAudioRoute);
        gap(content, 10);
        check("03", "Interface de rádio no sistema", scan.radioNodeVisible ? "Detectada" : "Não visível",
                scan.radioNodeVisible ? join(scan.radioNodes) : "Interfaces /dev/radio* podem estar ocultas por permissões e SELinux.",
                scan.radioNodeVisible);
        gap(content, 10);
        check("04", "Recursos FM anunciados", scan.fmFeature ? "Declarados" : "Não declarados",
                scan.fmFeature ? join(scan.fmFeatures) : "O fabricante pode não declarar um recurso FM público.",
                scan.fmFeature);
        gap(content, 10);
        check("05", "Fone com fio (antena)", scan.wiredHeadset ? "Detectado" : "Não detectado",
                scan.wiredHeadset ? "Um dispositivo de áudio com fio aparece conectado." : "Conecte um fone com fio para o teste prático; alguns aparelhos precisam dele como antena.",
                scan.wiredHeadset);
        gap(content, 24);

        section("Teste definitivo de recepção", "Faça o teste no seu próprio telefone, sem rede.");
        LinearLayout tutorial = card(content);
        guide(tutorial, "1", "Conecte um fone com fio", "Se possível, use a entrada P2 do Redmi.");
        gap(tutorial, 16);
        guide(tutorial, "2", "Desligue Wi-Fi e dados móveis", "Não é necessário ativar o modo avião.");
        gap(tutorial, 16);
        guide(tutorial, "3", "Abra o rádio do fabricante", "Escolha uma emissora local e confirme que existe áudio ao vivo.");
        gap(content, 12);
        add(content, button("◉   Abrir rádio FM instalado", true, this::openRadio));
        gap(content, 7);
        add(content, text("Se não abrir, procure manualmente por Rádio FM na lista de apps do aparelho.", 12, SECONDARY, false));
        gap(content, 18);

        add(content, text("Você conseguiu ouvir uma emissora sem internet?", 16, TEXT, true));
        gap(content, 10);
        LinearLayout confirm = row();
        TextView yes = button("Sim, consegui", false, () -> confirmTest("sim"));
        TextView no = button("Não consegui", false, () -> confirmTest("não"));
        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        half1.rightMargin = dp(6);
        confirm.addView(yes, half1);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        half2.leftMargin = dp(6);
        confirm.addView(no, half2);
        add(content, confirm);
        gap(content, 8);
        add(content, button("Limpar resultado manual", false, () -> confirmTest("não realizado")));
        gap(content, 24);

        section("Relatório técnico", "Salve ou compartilhe o diagnóstico para investigar compatibilidade.");
        add(content, button("▢   Copiar relatório", false, this::copyReport));
        gap(content, 10);
        add(content, button("↗   Compartilhar relatório", false, this::shareReport));
        gap(content, 17);
        add(content, text("O FM Lab nunca usa streaming, não precisa de root e não solicita acesso à internet. " +
                "O aplicativo apenas diagnostica e abre um rádio nativo disponível; não controla diretamente o chip FM.",
                12, SECONDARY, false));
        gap(content, 14);
        TextView footer = text("FM LAB  •  VERSÃO 1.0.0", 10, SECONDARY, true);
        footer.setGravity(Gravity.CENTER);
        add(content, footer);
    }

    private String join(java.util.List<String> items) {
        StringBuilder b = new StringBuilder();
        for (String s : items) {
            if (b.length() > 0) b.append(", ");
            b.append(s);
        }
        return b.toString();
    }

    private void guide(LinearLayout parent, String number, String title, String detail) {
        LinearLayout r = row();
        TextView circle = text(number, 14, BG, true);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(shape(GREEN, 30, 0));
        r.addView(circle, new LinearLayout.LayoutParams(dp(32), dp(32)));
        LinearLayout column = column();
        column.setPadding(dp(12), 0, 0, 0);
        add(column, text(title, 14, TEXT, true));
        gap(column, 3);
        add(column, text(detail, 12, SECONDARY, false));
        r.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        add(parent, r);
    }

    private void check(String index, String label, String state, String explanation, boolean positive) {
        LinearLayout c = card(content);
        LinearLayout heading = row();
        TextView idx = text(index, 12, GREEN, true);
        heading.addView(idx);
        TextView name = text(label, 14, TEXT, true);
        name.setPadding(dp(12), 0, 0, 0);
        heading.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView status = text(state, 11, positive ? GREEN : YELLOW, true);
        heading.addView(status);
        add(c, heading);
        gap(c, 9);
        add(c, text(explanation, 12, SECONDARY, false));
    }

    private void openRadio() {
        if (FmInspector.launchNativeRadio(this, scan)) return;
        new AlertDialog.Builder(this)
                .setTitle("Rádio nativo indisponível")
                .setMessage("Não foi possível abrir um aplicativo FM conhecido. Procure por 'Rádio FM' entre seus aplicativos. " +
                        "Se não houver, isso não prova que o chip FM está ausente: o fabricante pode ter ocultado ou bloqueado o recurso.")
                .setPositiveButton("Entendi", null)
                .show();
    }

    private void confirmTest(String result) {
        if ("sim".equals(result)) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmar recepção offline")
                    .setMessage("Você realmente ouviu uma estação transmitida pelo ar com Wi-Fi e dados móveis desligados (e não áudio gravado ou streaming)?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Sim, confirmei", (d, w) -> saveTest("sim"))
                    .show();
        } else saveTest(result);
    }

    private void saveTest(String result) {
        prefs.edit().putString("practical_result", result).apply();
        render();
        Toast.makeText(this, "Resultado registrado neste dispositivo", Toast.LENGTH_SHORT).show();
    }

    private void copyReport() {
        String report = FmInspector.report(scan, userResult());
        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText("Relatório FM Lab", report));
            Toast.makeText(this, "Relatório copiado", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReport() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico de Rádio FM — FM Lab");
        send.putExtra(Intent.EXTRA_TEXT, FmInspector.report(scan, userResult()));
        try {
            startActivity(Intent.createChooser(send, "Compartilhar diagnóstico"));
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Não há aplicativo compatível para compartilhar", Toast.LENGTH_LONG).show();
        }
    }

    /** Ilustração estática: não simula recepção nem medidor de intensidade. */
    private static final class TunerVisual extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        TunerVisual(Context context) { super(context); setContentDescription("Ilustração decorativa de rádio FM"); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h / 2f;
            float radius = Math.min(w * 0.24f, h * 0.46f);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f); p.setColor(0xFF457A6C);
            c.drawCircle(cx, cy, radius, p);
            p.setStrokeWidth(1.5f); p.setColor(0xFF2E5954);
            c.drawCircle(cx, cy, radius * 0.78f, p);
            for (int i = 0; i < 21; i++) {
                double rad = Math.PI + (Math.PI * i / 20.0);
                float x1 = cx + (float) Math.cos(rad) * radius * 1.16f;
                float y1 = cy + (float) Math.sin(rad) * radius * 1.16f;
                float x2 = cx + (float) Math.cos(rad) * radius * (i % 5 == 0 ? 1.36f : 1.25f);
                float y2 = cy + (float) Math.sin(rad) * radius * (i % 5 == 0 ? 1.36f : 1.25f);
                p.setColor(i == 14 ? 0xFFA9FF8B : 0xFF4C8D7A);
                p.setStrokeWidth(i % 5 == 0 ? 3f : 1.5f);
                c.drawLine(x1, y1, x2, y2, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFA9FF8B); p.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            p.setTextSize(radius * 0.68f); p.setTextAlign(Paint.Align.CENTER);
            c.drawText("FM", cx, cy + radius * 0.16f, p);
            p.setColor(0xFFB0C1BA); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            p.setTextSize(radius * 0.21f);
            c.drawText("87,5 — 108 MHz", cx, cy + radius * 0.49f, p);
        }
    }
}
