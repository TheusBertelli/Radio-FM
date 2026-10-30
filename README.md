# FM Lab — Teste de rádio FM offline (Redmi Note 13 4G)

Aplicativo Android nativo para **diagnosticar indícios de suporte ao rádio FM e testar a recepção real no próprio aparelho**, sem root e sem conexão à internet durante o uso. Projeto em Java, API 24+ (Android 7.0 ou superior), sem bibliotecas de interface externas.

> **Importante:** o FM Lab **não sintoniza estações dentro do próprio aplicativo**, pois a interface de rádio FM do Android é reservada a aplicativos privilegiados/sistema. O app encontra possíveis evidências, tenta abrir o rádio nativo Xiaomi e registra o teste que você efetivamente fez. Ausência de evidências NÃO significa que não exista receptor FM.

## O que funciona

- Detecta pacotes conhecidos de rádio FM, especialmente o `com.miui.fm` da Xiaomi, verificando se podem ser abertos.
- Consulta `AudioDeviceInfo.TYPE_FM` e `TYPE_FM_TUNER`, recursos FM declarados, fones com fio e a visibilidade de `/dev/radio*`.
- Mostra o resultado com explicação e nunca afirma ausência de chip apenas por um teste negativo.
- Abre um aplicativo de rádio instalado, quando o sistema permite.
- Explica o teste manual: **fone com fio + Wi-Fi desligado + dados móveis desligados + emissora real**.
- Salva o resultado prático confirmado pelo usuário no dispositivo.
- Copia ou compartilha um relatório técnico completo.
- Funciona offline. Não possui permissão `INTERNET`, não solicita root nem coleta dados pessoais.

## Instalar no Redmi (maneira mais fácil: GitHub Actions)

1. Crie um repositório no GitHub ou use um repositório novo na sua organização.
2. Envie o **conteúdo desta pasta** para a branch `main` (inclua a pasta `.github`).
3. Entre em **Actions** → **FM Lab - Gerar APK Android** → **Run workflow**. Um push na `main` também inicia a build.
4. Ao concluir com sucesso, abra a execução e baixe o artefato **FM-Lab-Redmi-APK**.
5. Extraia o arquivo ZIP baixado no GitHub. Dentro estará o **`app-debug.apk`**.
6. Copie para o Redmi, instale (autorize a instalação de aplicativos dessa origem, se solicitado) e abra **FM Lab**.

O APK `debug` é **assinado automaticamente para testes** pelo sistema Android Gradle Plugin; não é um APK de produção/Play Store.

## Compilar localmente (Windows / Android Studio)

Requisitos: Android Studio com Android SDK 35, JDK 17 e acesso à internet **somente para baixar as dependências de compilação**.

1. Extraia o projeto e abra a pasta `FM-Lab-Redmi` no Android Studio.
2. Se o Android Studio pedir o Gradle Wrapper JAR, execute uma vez no PowerShell, na raiz do projeto:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\prepare-gradle.ps1
   ```

   O script baixa o wrapper oficial Gradle 8.11.1 e verifica o SHA-256. Depois sincronize o projeto no Android Studio.

3. Para compilar no terminal (com `java` e o Android SDK configurados):

   ```powershell
   .\gradlew.bat :app:assembleDebug
   ```

4. APK criado em `app\build\outputs\apk\debug\app-debug.apk`.

No Linux/macOS: `chmod +x gradlew && ./gradlew :app:assembleDebug`.

## Como interpretar

| Evidência | O que significa |
| --- | --- |
| `com.miui.fm` encontrado | Existe um aplicativo conhecido de rádio FM. Pode estar desativado ou depender do chipset/firmware. |
| `TYPE_FM_TUNER` exposto | O sistema anuncia uma rota de áudio FM, mas pode continuar sem dar acesso ao sintonizador. |
| `/dev/radio0` não aparece | O sistema pode esconder dispositivos, inclusive por SELinux. Não é prova de ausência. |
| App nativo realmente reproduz com Wi-Fi/dados desligados | **Confirma recepção offline naquele aparelho, conforme o teste prático.** |
| Nada encontrado | Resultado inconclusivo: fabricante pode não expor o recurso a apps comuns. |

## Privacidade e limites

- Este aplicativo não acessa internet e não tem `android.permission.INTERNET`.
- O botão **Compartilhar** permite abrir outro app escolhido pelo usuário; esse outro app pode usar internet, se você decidir enviar o relatório.
- Não acessa permissões ocultas, não altera o rádio do firmware, não requer desbloqueio de bootloader ou root.
- O Android não disponibiliza uma API pública universal para sintonizar rádio FM de smartphones. `ACCESS_FM_RADIO` é uma permissão privilegiada do sistema.
- Os resultados ainda precisam de confirmação física no Redmi Note 13 4G.

## Referências técnicas

- Xiaomi — [REDMI Note 13 FAQ](https://www.mi.com/my/support/faq/details/KA-91513/)
- Android Open Source — [permissão privilegiada ACCESS_FM_RADIO](https://android.googlesource.com/platform/frameworks/base/+/main/core/res/AndroidManifest.xml)
- Android — [AudioDeviceInfo](https://developer.android.com/reference/android/media/AudioDeviceInfo)
- Android — [visibilidade de pacotes](https://developer.android.com/training/package-visibility/declaring)

## Organização

```text
FM-Lab-Redmi/
├── .github/workflows/android-apk.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/br/com/fmlab/redmi/
│       │   ├── FmInspector.java
│       │   └── MainActivity.java
│       └── res/
│           ├── drawable/ic_launcher.xml
│           └── values/strings.xml
├── gradle/wrapper/gradle-wrapper.properties
├── scripts/prepare-gradle.ps1
├── gradlew / gradlew.bat
├── build.gradle
├── settings.gradle
└── README.md
```
