package br.com.fmlab.redmi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import java.io.File;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diagnóstico de rádio FM com APIs públicas. NÃO comanda sintonizadores privados.
 * Os pacotes OEM são possibilidades de descoberta, não APIs garantidas.
 */
public final class FmInspector {
    private FmInspector() {}

    private static final String[] KNOWN_PACKAGES = {
        // Samsung, Xiaomi/Redmi/POCO, Motorola e variantes Qualcomm/MediaTek.
        "com.sec.android.app.fm", "com.samsung.android.app.fmradio",
        "com.miui.fm", "com.miui.fmradio", "com.miui.fmservice",
        "com.motorola.fmplayer", "com.motorola.fmradio", "com.motorola.radio",
        "com.android.fmradio", "com.qualcomm.qti.fm", "com.caf.fmradio",
        "com.mediatek.fmradio", "com.oppo.fmradio", "com.transsion.fmradio"
    };

    public static final class RadioApp {
        public final String packageName, displayName, launcherActivity;
        public final boolean canLaunch, enabled, systemApp, knownOem;
        RadioApp(String pkg, String label, String activity, boolean launch,
                 boolean enabled, boolean systemApp, boolean knownOem) {
            this.packageName = pkg;
            this.displayName = label;
            this.launcherActivity = activity;
            this.canLaunch = launch;
            this.enabled = enabled;
            this.systemApp = systemApp;
            this.knownOem = knownOem;
        }
    }

    public static final class Scan {
        public String device, build, android, manufacturer, timestamp;
        public boolean fmAudioRoute, wiredHeadset, fmFeature, radioNodeVisible, radioNodeReadable;
        public final List<RadioApp> apps = new ArrayList<>();
        public final List<String> fmAudioTypes = new ArrayList<>();
        public final List<String> audioAccessories = new ArrayList<>();
        public final List<String> usbDevices = new ArrayList<>();
        public final List<String> radioNodes = new ArrayList<>();
        public final List<String> fmFeatures = new ArrayList<>();
        public final List<String> exportedComponents = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
        public boolean hasNativeApp() {
            for (RadioApp app : apps) if (app.enabled && app.canLaunch && app.knownOem) return true;
            return false;
        }
        public boolean hasAnyEvidence() {
            return !apps.isEmpty() || fmAudioRoute || fmFeature || radioNodeVisible;
        }
    }

    public static Scan inspect(Context context) {
        Scan s = new Scan();
        s.device = Build.MANUFACTURER + " " + Build.MODEL;
        s.manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        s.build = Build.DEVICE + " / " + Build.PRODUCT;
        s.android = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        s.timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        PackageManager pm = context.getPackageManager();
        Map<String, RadioApp> found = new HashMap<>();

        // 1. Apps de rádio de fabricantes conhecidos, limitados pelas <queries>.
        for (String pkg : KNOWN_PACKAGES) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                boolean sys = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                String label = String.valueOf(pm.getApplicationLabel(info));
                RadioApp app = new RadioApp(pkg, label, null, launch != null,
                        info.enabled, sys, true);
                found.put(pkg, app);
                readExportedComponents(pm, s, pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Invisibilidade não prova ausência de chip ou aplicativo.
            } catch (RuntimeException ex) {
                s.notes.add("Consulta de " + pkg + ": " + ex.getClass().getSimpleName());
            }
        }

        // 2. Descoberta limitada a atividades de tela inicial com nome compatível
        //    com Rádio FM. Não enumera serviços privados nem requer QUERY_ALL_PACKAGES.
        try {
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(launcher, 0)) {
                ActivityInfo activity = ri.activityInfo;
                if (activity == null || activity.packageName == null || !activity.exported) continue;
                String pkg = activity.packageName;
                String label = String.valueOf(ri.loadLabel(pm));
                if (!looksLikeRadio(label, pkg)) continue;
                boolean known = isKnown(pkg);
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    boolean sys = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                    RadioApp existing = found.get(pkg);
                    if (existing == null || !existing.canLaunch) {
                        found.put(pkg, new RadioApp(pkg, label, activity.name,
                                true, info.enabled && activity.enabled, sys, known));
                    }
                } catch (PackageManager.NameNotFoundException ignored) { }
            }
        } catch (RuntimeException ex) {
            s.notes.add("Descoberta de apps visíveis: " + ex.getClass().getSimpleName());
        }

        s.apps.addAll(found.values());
        s.apps.sort(Comparator
            .comparingInt((RadioApp app) -> appScore(s.manufacturer, app))
            .thenComparing(app -> app.displayName.toLowerCase(Locale.ROOT)));

        // 3. Rotas públicas de áudio: a presença delas não abre a API de sintonia.
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                for (AudioDeviceInfo d : audio.getDevices(AudioManager.GET_DEVICES_ALL)) {
                    int t = d.getType();
                    if (t == AudioDeviceInfo.TYPE_FM_TUNER || t == AudioDeviceInfo.TYPE_FM) {
                        s.fmAudioRoute = true;
                        s.fmAudioTypes.add((t == AudioDeviceInfo.TYPE_FM_TUNER ? "FM_TUNER" : "FM")
                                + (d.isSource() ? " [entrada]" : " [saída]"));
                    }
                    if (t == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || t == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                        s.wiredHeadset = true;
                        addOnce(s.audioAccessories, "Fone analógico com fio");
                    } else if (t == AudioDeviceInfo.TYPE_USB_HEADSET || t == AudioDeviceInfo.TYPE_USB_DEVICE) {
                        addOnce(s.audioAccessories, "Áudio USB");
                    } else if (t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                        addOnce(s.audioAccessories, "Saída Bluetooth");
                    }
                }
            }
        } catch (RuntimeException ex) {
            s.notes.add("Rotas de áudio: " + ex.getClass().getSimpleName());
        }

        // 4. Diagnóstico de USB host. USB detectado não implica SDR FM compatível.
        try {
            UsbManager usb = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usb != null) for (UsbDevice dev : usb.getDeviceList().values()) {
                s.usbDevices.add(String.format(Locale.ROOT,
                    "USB VID %04X / PID %04X", dev.getVendorId(), dev.getProductId()));
            }
        } catch (RuntimeException ex) {
            s.notes.add("Dispositivos USB: " + ex.getClass().getSimpleName());
        }

        // 5. Features não padronizadas declaradas por algumas ROMs.
        for (String feature : new String[]{"android.hardware.fm", "android.hardware.fmradio", "android.hardware.radio.fm"}) {
            try {
                if (pm.hasSystemFeature(feature)) { s.fmFeature = true; s.fmFeatures.add(feature); }
            } catch (RuntimeException ignored) { }
        }
        // 6. /dev: apenas verificar existência/legibilidade; NÃO abrir, escrever ou invocar ioctls.
        for (String path : new String[]{"/dev/radio0", "/dev/radio1", "/dev/fm", "/dev/fmradio"}) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    s.radioNodeVisible = true;
                    boolean read = f.canRead();
                    s.radioNodeReadable |= read;
                    s.radioNodes.add(path + (read ? " (visível/legível)" : " (acesso restrito)"));
                }
            } catch (SecurityException ex) {
                s.notes.add(path + " bloqueado pelo Android");
            }
        }
        return s;
    }

    private static void addOnce(List<String> items, String item) {
        if (!items.contains(item)) items.add(item);
    }

    private static int appScore(String manufacturer, RadioApp app) {
        // Menor número = candidato de lançamento mais provável.
        int score = app.knownOem ? 10 : 40;
        if (!app.canLaunch || !app.enabled) score += 100;
        if (manufacturer.contains("samsung") && app.packageName.startsWith("com.sec.android.app.fm")) score -= 9;
        if ((manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco"))
                && app.packageName.startsWith("com.miui.fm")) score -= 9;
        if (manufacturer.contains("motorola") && app.packageName.startsWith("com.motorola.fm")) score -= 9;
        return score;
    }

    private static boolean isKnown(String pkg) {
        for (String known : KNOWN_PACKAGES) if (known.equals(pkg)) return true;
        return false;
    }

    private static boolean looksLikeRadio(String label, String pkg) {
        String clean = Normalizer.normalize(label == null ? "" : label, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        String p = pkg.toLowerCase(Locale.ROOT);
        return clean.equals("radio") || clean.equals("fm") || clean.contains("radio fm")
            || clean.contains("fm radio") || clean.contains("radio tuner")
            || p.contains("fmradio") || p.endsWith(".fm") || p.endsWith(".fmplayer");
    }

    private static void readExportedComponents(PackageManager pm, Scan s, String pkg) {
        try {
            int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS;
            PackageInfo details = pm.getPackageInfo(pkg, flags);
            if (details.activities != null) for (ActivityInfo a : details.activities)
                recordExported(s, pkg, a, "Activity", a.permission);
            if (details.services != null) for (ServiceInfo x : details.services)
                recordExported(s, pkg, x, "Service", x.permission);
            if (details.receivers != null) for (ActivityInfo x : details.receivers)
                recordExported(s, pkg, x, "Receiver", x.permission);
        } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
            s.notes.add("Metadados públicos de " + pkg + " indisponíveis");
        }
    }

    private static void recordExported(Scan s, String pkg, ComponentInfo i, String type, String permission) {
        if (!i.exported || s.exportedComponents.size() >= 40) return;
        s.exportedComponents.add(type + " " + pkg + "/" + i.name +
            (permission == null ? " [nenhuma permissão declarada]" : " [exige " + permission + "]"));
    }

    /** Inicia somente uma atividade pública/lançável; não chama serviço de sintonia privado. */
    public static boolean launchRadioApp(Context context, RadioApp app) {
        if (app == null || !app.canLaunch || !app.enabled) return false;
        PackageManager pm = context.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(app.packageName);
        if (launch == null && app.launcherActivity != null) {
            launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(new ComponentName(app.packageName, app.launcherActivity));
        }
        if (launch == null) return false;
        if (!(context instanceof android.app.Activity)) launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(launch);
            return true;
        } catch (RuntimeException denied) {
            return false;
        }
    }

    public static boolean launchNativeRadio(Context ctx, Scan scan) {
        for (RadioApp a : scan.apps) if (a.knownOem && launchRadioApp(ctx, a)) return true;
        return false;
    }

    private static String join(List<String> values) {
        return values.isEmpty() ? "nenhum" : String.join(", ", values);
    }

    public static String report(Scan s, String userResult) {
        StringBuilder b = new StringBuilder("FM LIVRE UNIVERSAL — DIAGNÓSTICO OFFLINE\n");
        b.append("Data: ").append(s.timestamp).append('\n');
        b.append("Dispositivo: ").append(s.device).append('\n');
        b.append("Fabricante: ").append(s.manufacturer).append('\n');
        b.append("Codinomes: ").append(s.build).append('\n');
        b.append("Android: ").append(s.android).append("\n\n");
        b.append("Apps de rádio encontrados (visibilidade limitada pelo sistema):\n");
        if (s.apps.isEmpty()) b.append("- Nenhum detectado; resultado inconclusivo\n");
        for (RadioApp a : s.apps) b.append("- ").append(a.displayName).append(" [").append(a.packageName)
            .append("] / ").append(a.knownOem ? "OEM conhecido" : "descoberta por nome")
            .append(" / abrir: ").append(a.canLaunch ? "sim" : "não")
            .append(" / ativado: ").append(a.enabled ? "sim" : "não")
            .append(" / sistema: ").append(a.systemApp ? "sim" : "não").append('\n');
        b.append("Rotas FM: ").append(join(s.fmAudioTypes)).append('\n');
        b.append("Acessórios áudio: ").append(join(s.audioAccessories)).append('\n');
        b.append("Fone analógico: ").append(s.wiredHeadset ? "detectado" : "não detectado").append('\n');
        b.append("USB: ").append(join(s.usbDevices)).append('\n');
        b.append("Recursos FM de ROM: ").append(join(s.fmFeatures)).append('\n');
        b.append("Nós visíveis: ").append(join(s.radioNodes)).append('\n');
        b.append("Componentes OEM exportados (não implica comando): ")
            .append(s.exportedComponents.size()).append('\n');
        for (String c : s.exportedComponents) b.append("- ").append(c).append('\n');
        b.append("Teste de recepção reportado: ").append(userResult).append('\n');
        b.append("Notas: ").append(join(s.notes)).append("\n\n");
        b.append("Avisos: FM_TUNER/feature/nó/dev não confirmam controle do chip; "
            + "não existe API pública universal para sintonia FM. "
            + "Sem INTERNET, sem root e sem permissões privilegiadas.\n");
        return b.toString();
    }
}
