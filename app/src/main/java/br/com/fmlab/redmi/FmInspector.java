package br.com.fmlab.redmi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Inspeciona apenas informações disponibilizadas ao aplicativo comum pelo Android.
 * Não usa reflection em API privada, root ou a permissão privilegiada ACCESS_FM_RADIO.
 */
public final class FmInspector {
    private FmInspector() {}

    private static final String[] KNOWN_PACKAGES = {
            "com.miui.fm", "com.miui.fmservice", "com.miui.fmradio",
            "com.android.fmradio", "com.qualcomm.qti.fm", "com.caf.fmradio",
            "com.mediatek.fmradio", "com.samsung.android.app.fmradio",
            "com.motorola.fmplayer", "com.oppo.fmradio"
    };

    public static final class RadioApp {
        public final String packageName;
        public final String displayName;
        public final boolean canLaunch;
        public final boolean enabled;
        public final boolean systemApp;
        RadioApp(String pkg, String label, boolean launch, boolean enabled, boolean systemApp) {
            this.packageName = pkg;
            this.displayName = label;
            this.canLaunch = launch;
            this.enabled = enabled;
            this.systemApp = systemApp;
        }
    }

    public static final class Scan {
        public String device;
        public String build;
        public String android;
        public String timestamp;
        public boolean fmAudioRoute;
        public boolean wiredHeadset;
        public boolean fmFeature;
        public boolean radioNodeVisible;
        public boolean radioNodeReadable;
        public final List<RadioApp> apps = new ArrayList<>();
        public final List<String> fmAudioTypes = new ArrayList<>();
        public final List<String> radioNodes = new ArrayList<>();
        public final List<String> fmFeatures = new ArrayList<>();
        public final List<String> exportedComponents = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();

        public boolean hasNativeApp() {
            for (RadioApp a : apps) {
                if (a.canLaunch && a.enabled) return true;
            }
            return false;
        }
        public boolean hasAnyEvidence() {
            return !apps.isEmpty() || fmAudioRoute || fmFeature || radioNodeVisible;
        }
    }

    public static Scan inspect(Context context) {
        Scan s = new Scan();
        s.device = Build.MANUFACTURER + " " + Build.MODEL;
        s.build = Build.DEVICE + " / " + Build.PRODUCT;
        s.android = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        s.timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        PackageManager pm = context.getPackageManager();

        for (String pkg : KNOWN_PACKAGES) {
            try {
                ApplicationInfo app = pm.getApplicationInfo(pkg, 0);
                Intent launcher = pm.getLaunchIntentForPackage(pkg);
                String label = pm.getApplicationLabel(app).toString();
                boolean sys = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                s.apps.add(new RadioApp(pkg, label, launcher != null, app.enabled, sys));
                // Levanta apenas metadados públicos de componentes; não os executa.
                try {
                    int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS;
                    PackageInfo details = pm.getPackageInfo(pkg, flags);
                    if (details.activities != null) for (ActivityInfo a : details.activities)
                        recordExported(s, pkg, a, "Activity", a.permission);
                    if (details.services != null) for (ServiceInfo service : details.services)
                        recordExported(s, pkg, service, "Service", service.permission);
                    if (details.receivers != null) for (ActivityInfo receiver : details.receivers)
                        recordExported(s, pkg, receiver, "Receiver", receiver.permission);
                } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
                    s.notes.add("Detalhes públicos de " + pkg + " não acessíveis.");
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // Um pacote desconhecido ou oculto não comprova ausência de hardware FM.
            } catch (RuntimeException e) {
                s.notes.add("Consulta de pacote " + pkg + ": " + e.getClass().getSimpleName());
            }
        }

        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                for (AudioDeviceInfo device : am.getDevices(AudioManager.GET_DEVICES_ALL)) {
                    int type = device.getType();
                    if (type == AudioDeviceInfo.TYPE_FM_TUNER || type == AudioDeviceInfo.TYPE_FM) {
                        s.fmAudioRoute = true;
                        s.fmAudioTypes.add((type == AudioDeviceInfo.TYPE_FM_TUNER ? "FM_TUNER" : "FM")
                                + " (" + (device.isSource() ? "entrada" : "saída") + ")");
                    }
                    if (type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                        s.wiredHeadset = true;
                    }
                }
            }
        } catch (RuntimeException e) {
            s.notes.add("Rotas de áudio: " + e.getClass().getSimpleName());
        }

        try {
            String[] features = {"android.hardware.fm", "android.hardware.fmradio", "android.hardware.radio.fm"};
            for (String feature : features) {
                if (pm.hasSystemFeature(feature)) {
                    s.fmFeature = true;
                    s.fmFeatures.add(feature);
                }
            }
        } catch (RuntimeException e) {
            s.notes.add("Recursos de hardware: " + e.getClass().getSimpleName());
        }

        // A ausência destes nós na visão de um app comum é inconclusiva.
        for (String path : new String[]{"/dev/radio0", "/dev/radio1", "/dev/fm", "/dev/fmradio"}) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    s.radioNodeVisible = true;
                    boolean readable = file.canRead();
                    s.radioNodeReadable |= readable;
                    s.radioNodes.add(path + (readable ? " (legível)" : " (acesso restrito)"));
                }
            } catch (SecurityException e) {
                s.notes.add("Acesso restrito: " + path);
            }
        }
        return s;
    }

    private static void recordExported(Scan s, String pkg, ComponentInfo info, String kind, String permission) {
        if (!info.exported || s.exportedComponents.size() >= 25) return;
        String name = info.name == null ? "?" : info.name;
        s.exportedComponents.add(kind + " " + pkg + "/" + name +
                (permission == null ? " (sem permissão declarada)" : " (exige " + permission + ")"));
    }

    public static boolean launchNativeRadio(Context ctx, Scan scan) {
        PackageManager pm = ctx.getPackageManager();
        for (RadioApp app : scan.apps) {
            if (!app.enabled) continue;
            Intent launch = pm.getLaunchIntentForPackage(app.packageName);
            if (launch == null) continue;
            try {
                ctx.startActivity(launch);
                return true;
            } catch (RuntimeException ignored) {
                // Pode ser bloqueado pelo fabricante. Tenta o próximo app conhecido.
            }
        }
        return false;
    }

    private static String join(List<String> strings) {
        StringBuilder b = new StringBuilder();
        for (String s : strings) {
            if (b.length() > 0) b.append(", ");
            b.append(s);
        }
        return b.toString();
    }

    public static String report(Scan s, String userResult) {
        StringBuilder b = new StringBuilder("FM LIVRE — RELATÓRIO OFFLINE\n");
        b.append("Data: ").append(s.timestamp).append('\n');
        b.append("Dispositivo: ").append(s.device).append('\n');
        b.append("Codinomes: ").append(s.build).append('\n');
        b.append("Android: ").append(s.android).append("\n\n");
        b.append("Aplicativos FM conhecidos:\n");
        if (s.apps.isEmpty()) b.append("- Nenhum visível (resultado inconclusivo)\n");
        for (RadioApp a : s.apps) {
            b.append("- ").append(a.displayName).append(" [").append(a.packageName).append("]")
                    .append(" | abrir: ").append(a.canLaunch ? "sim" : "não")
                    .append(" | habilitado: ").append(a.enabled ? "sim" : "não")
                    .append(" | sistema: ").append(a.systemApp ? "sim" : "não").append('\n');
        }
        b.append("Rotas de áudio FM: ")
                .append(s.fmAudioTypes.isEmpty() ? "não expostas" : join(s.fmAudioTypes)).append('\n');
        b.append("Fone com fio detectado: ").append(s.wiredHeadset ? "sim" : "não").append('\n');
        b.append("Recursos FM declarados: ")
                .append(s.fmFeatures.isEmpty() ? "nenhum" : join(s.fmFeatures)).append('\n');
        b.append("Interfaces /dev: ")
                .append(s.radioNodes.isEmpty() ? "não visíveis" : join(s.radioNodes)).append('\n');
        b.append("Componentes FM exportados: ").append(s.exportedComponents.size()).append('\n');
        for (String component : s.exportedComponents) b.append("- ").append(component).append('\n');
        b.append("Observação: componente exportado não prova comando de sintonia.\n");
        b.append("Teste prático informado pelo usuário: ").append(userResult).append('\n');
        if (!s.notes.isEmpty()) b.append("Observações técnicas: ").append(join(s.notes)).append('\n');
        b.append("\nINTERPRETAÇÃO: resultados negativos de consultas não provam ausência de chip FM. ")
                .append("Somente uma recepção real de emissora com internet desligada confirma funcionamento no aparelho. ")
                .append("Este aplicativo não sintoniza diretamente: Android restringe a API FM a apps privilegiados.\n");
        return b.toString();
    }
}
