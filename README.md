# FM Livre Universal 3.0 — Android 11+

Aplicativo Android **nativo Java**, sem dependências de terceiros no APK, projetado para diagnosticar a disponibilidade de **rádio FM offline** em celulares Samsung, Motorola, Xiaomi/Redmi/POCO e ROMs de outros fabricantes. Evolução do FM Livre 2.0, com seletor local de frequências, favoritos, diagnóstico e abertura do rádio do fabricante. Projeto-fonte pronto para compilar no Android Studio ou GitHub Actions.

> **Limite real:** Android não oferece uma API pública de sintonia FM universal para apps comuns. `ACCESS_FM_RADIO` e `ACCESS_BROADCAST_RADIO` são permissões de sistema/assinatura privilegiada. Um chip FM instalado não concede acesso automático à sintonia para outros aplicativos. Este projeto **não promete reproduzir FM dentro de sua própria janela**. Onde houver um aplicativo de rádio nativo, ele é aberto para que o usuário sintonize e ouça por ele. Não há root, reflection em APIs ocultas, violação de SELinux ou comandos falsos para mudar a estação.

## Novidades da versão 3.0

- **Android 11 (API 30) até versões recentes:** `minSdk 30`, `targetSdk 35`, interface redimensionável, modo claro/escuro, rolagem e layout de sistema seguro.
- **Samsung, Motorola e Xiaomi:** consulta a pacotes de rádio conhecidos e ordenação de candidatos conforme o fabricante. Inclui o pacote Samsung `com.sec.android.app.fm`, pacotes Xiaomi/MIUI e possíveis nomes de pacotes Motorola; o nome do pacote por si só **não garante compatibilidade**.
- **Descoberta de rádio visível no dispositivo:** procura atividades de tela inicial com rótulos como "Rádio", "FM" ou "Rádio FM" e guarda apenas candidatos relacionados. O `AndroidManifest.xml` declara `<queries>` direcionadas e evita `QUERY_ALL_PACKAGES`.
- **Escolher o rádio a abrir:** lista de aplicativos encontrados, lembrança do último escolhido e tratamento de impossibilidade de abertura; apps não OEM são identificados como **não verificados** e podem ser rádio por internet.
- **Diagnóstico ampliado:** fabricante, Android, pacotes OEM, componentes exportados, rotas públicas de áudio `TYPE_FM` / `TYPE_FM_TUNER`, fones analógicos, áudio USB, Bluetooth, dispositivos USB conectados (VID/PID) e presença de `/dev/radio*` sem abrir os nós.
- **Offline e privacidade:** o app **não possui permissão `INTERNET`**, nem microfone, localização ou coleta de dados. Relatórios são compartilhados somente por iniciativa do usuário. Favoritos e preferência OEM ficam locais.
- **Estações favoritas:** pré-seleção 76,1–107,9 MHz em passos de 0,1 MHz, renomear, salvar e excluir. Atenção: pré-seleção local **não comanda o sintonizador real**. O rádio OEM precisa ser sintonizado separadamente.
- **USB:** relatório de dispositivos USB que o sistema conseguir enumerar. Isso **não fornece suporte a dongles SDR**; um SDR requer hardware, driver e implementação próprios, ainda inexistentes.

## Como gerar APK automaticamente

No GitHub, envie os arquivos desta pasta para um repositório. O workflow `.github/workflows/android-apk.yml` roda em push na `main` ou manualmente na aba **Actions**. Ele instala JDK 17, Android SDK 35, Gradle 8.11.1 e executa `gradle :app:assembleDebug`. Ao terminar, disponibiliza o artefato **FM-Livre-Universal-Android11-APK**, contendo `app-debug.apk`.

**Não utiliza Expo, Node.js nem EXPO_TOKEN**: é um projeto Android nativo, separado do Synapse. Uma build `debug` no GitHub Actions é para testes. A assinatura pode mudar entre executores; caso seja diferente da versão instalada, será necessário desinstalar o APK anterior para instalar o novo (isso remove dados locais). Para distribuir atualizações sem perder dados, configure uma assinatura de release estável.

## Rodar no Android Studio (opcional)

Abra a pasta que contém `settings.gradle`. O projeto precisa de Android SDK 35 e JDK 17. Se faltar o `gradle/wrapper/gradle-wrapper.jar`, o arquivo `gradlew.bat` tenta baixá-lo e validar seu SHA-256; também é possível usar Gradle 8.11.1 instalado. Execute:

```powershell
.\gradlew.bat :app:assembleDebug
```

O APK ficará em `app/build/outputs/apk/debug/app-debug.apk`.

## Testar em Samsung, Motorola e Xiaomi

1. Instale o APK. Abra **FM Livre Universal** e acesse **Diagnóstico**.
2. Conecte fone analógico se o rádio do aparelho exigir antena. Algumas portas USB-C não servem como antena FM; depende do hardware do aparelho.
3. Toque em **Escolher e abrir rádio FM disponível** e selecione um app do fabricante. Se não houver nenhum, o app informa o problema em vez de simular áudio.
4. Dentro do aplicativo do fabricante, sintonize uma estação. Desative dados móveis e Wi-Fi para confirmar que a recepção é realmente FM offline.
5. Use **Copiar diagnóstico** e revise seus dados antes de compartilhá-los. Com evidência de APIs públicas OEM documentadas, podem ser criados adaptadores para modelos específicos, sem afirmar que sejam universais.

## Compatibilidade — o que significa

| Recurso | Android 11+ | Observação |
|---|---|---|
| Instalação e interface do FM Livre | Projetado para API 30+ | Compilação e testes físicos pendentes |
| Favoritos e pré-seleção offline | Implementado | Não controla o chip FM |
| Abrir app FM nativo | Melhor esforço | Depende de app instalado, exportado e acessível na ROM |
| Diagnosticar rotas `TYPE_FM_TUNER` | Implementado | A ausência é inconclusiva |
| Verificar fone e dispositivos USB | Implementado | Dispositivos USB não são sintonizadores suportados |
| Sintonia, busca e áudio FM dentro do FM Livre | **Não disponível** | Requer API específica autorizada ou integração de sistema |

### Referências técnicas

- [Android 11: visibilidade de pacotes `<queries>`](https://developer.android.com/training/package-visibility/declaring)
- [Android: `AudioDeviceInfo` e tipos FM](https://developer.android.com/reference/android/media/AudioDeviceInfo)
- [Permissões de sistema de Broadcast Radio no AOSP](https://android.googlesource.com/platform/frameworks/base/+/main/core/res/AndroidManifest.xml)
- [API Android para USB Host](https://developer.android.com/develop/connectivity/usb/host)

**Estado de validação:** o código foi revisado e o Manifest XML verificado, porém não há Android SDK neste ambiente para compilar a versão 3.0 nem aparelhos físicos disponíveis para confirmar funcionamento. Primeiro use o GitHub Actions, depois testes em Samsung/Motorola/Xiaomi.
