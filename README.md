# FM Livre 2.0 — Rádio FM offline para Redmi Note 13 4G

Aplicativo Android nativo **sem internet**, pensado para aproveitar a confirmação de recepção FM no Redmi Note 13 4G. Feito em **Java**, sem bibliotecas externas, com suporte a **Android 7.0+**.

## Estado real do desenvolvimento

**Funciona nesta versão:** interface de rádio, faixa brasileira de **76,1 a 107,9 MHz** (pré-seleção), ajustes em passos de 0,1 MHz, nomeação e armazenamento local de estações favoritas, modo claro/escuro, detecção de sinais FM, abertura do rádio do fabricante quando a ROM permite, diagnóstico e exportação de relatório.

**Ainda não funciona:** alterar a sintonia do receptor diretamente *dentro do FM Livre*, buscar estações automaticamente, comandar volume/reprodução do hardware FM ou capturar o áudio da rádio nativa. A frequência escolhida na tela do FM Livre é **somente uma pré-seleção salva localmente**. A estação real deve ser sintonizada dentro do aplicativo FM da Xiaomi.

O fato de o Redmi ter um receptor funcional **não concede automaticamente** permissões de rádio a um aplicativo instalado normalmente. O Android reserva `ACCESS_FM_RADIO` / `ACCESS_BROADCAST_RADIO` para apps de sistema/assinados. O projeto não contorna isso com root, serviços de acessibilidade ou permissão privilegiada falsa.

## Funcionalidades do FM Livre

- **Rádio:** dial e seletor de frequência (76,1–107,9 MHz), passos de 0,1 MHz, botão para abrir o rádio nativo.
- **Favoritos:** salvar/renomear/excluir estações, listagem local, manter a última frequência selecionada.
- **Aparência:** tema escuro e claro, layout responsivo, sem bibliotecas externas.
- **Diagnóstico:** nome do aparelho, versão Android, pacotes FM conhecidos, rotas de áudio FM, conexão de fone com fio, recursos do sistema e nós de dispositivo visíveis.
- **Teste real:** registra se o usuário ouviu uma emissora com Wi-Fi e dados móveis desligados; recupera esse registro da versão anterior do FM Lab.
- **Investigação avançada:** relatório técnico copiável/compartilhável e comandos ADB para verificar o firmware.
- **Privacidade:** não solicita INTERNET, root, localização ou acesso ao microfone. Os favoritos ficam no próprio aparelho. Compartilhamento só ocorre se você acionar o botão e selecionar outro app.

## Gerar o APK no GitHub Actions

1. Crie um repositório novo ou atualize o projeto anterior, enviando **o conteúdo** desta pasta para a branch `main`, inclusive `.github/workflows/android-apk.yml`.
2. Abra **Actions** → **FM Livre — APK Android** → **Run workflow**, ou faça um `push` para `main`.
3. Quando a compilação terminar, abra a execução → **Artifacts** → **FM-Livre-Android-APK**.
4. Extraia o ZIP do artefato e instale `app-debug.apk` no Redmi Note 13 4G.
5. Conecte um fone com fio (se necessário como antena), desligue Wi-Fi e dados móveis e use **Abrir rádio FM do celular**.

O fluxo compila com **JDK 17**, **Gradle 8.11.1**, **Android SDK 35** e `android-actions/setup-android@v4` (corrigindo o antigo erro do pacote `tools`). Gera APK **debug** assinado automaticamente para testes, não um binário de publicação comercial.

### Compilar localmente

Precisa ter Android Studio + Android SDK 35 e Java 17. Abra a pasta no Android Studio. Caso o wrapper ainda não tenha o JAR, execute no PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prepare-gradle.ps1
.\gradlew.bat :app:assembleDebug
```

O APK fica em `app\build\outputs\apk\debug\app-debug.apk`.

Alternativa com Gradle 8.11.1 instalado: `gradle --no-daemon :app:assembleDebug`.

## Como usar

1. Abra **FM Livre** e ajuste a **pré-seleção** da frequência.
2. Toque em **Salvar / editar frequência favorita** para atribuir o nome da emissora.
3. Toque em **Abrir rádio FM do celular** para abrir o rádio original. **Ajuste também a frequência nele**.
4. Confirme que está captando FM com internet desligada.
5. Na aba **Diagnóstico**, toque em **Copiar diagnóstico** para preparar a próxima etapa do projeto.

## Para descobrir se conseguiremos controlar o receptor de dentro do app

No Windows, habilite as opções de desenvolvedor e a depuração USB e use o ADB. As instruções estão em [docs/adb-investigacao.md](docs/adb-investigacao.md). Os resultados ajudarão a verificar se a versão HyperOS/MIUI do dispositivo expõe alguma atividade ou interface compatível para um app comum.

**Não é garantido que exista uma API pública:** mesmo quando o rádio FM nativo funciona, outro aplicativo pode não ter como controlar a sintonia sem privilégios de sistema. Não distribuiremos técnicas que exigem root como se fossem solução universal.

## Arquivos principais

```
app/src/main/java/br/com/fmlab/redmi/
  MainActivity.java        # Interface de rádio, favoritos, diagnóstico e relatório
  FmInspector.java        # Verificações públicas do Android e abertura do rádio OEM
  StationStore.java       # Favoritos locais em SharedPreferences/JSON
app/src/main/AndroidManifest.xml  # Sem permissão INTERNET
.github/workflows/android-apk.yml # Compila e disponibiliza APK para download
docs/adb-investigacao.md         # Próximo teste de permissões/integração
```

Mantido o **mesmo applicationId `br.com.fmlab.redmi`** da primeira versão para que o APK possa atualizar o FM Lab instalado sem perder a confirmação manual do teste. `versionCode` aumentado de 1 para 2. A atualização exige que o APK seja assinado com a **mesma chave** usada na instalação anterior; builds debug em runners diferentes normalmente têm chaves diferentes. Se a atualização falhar, desinstale o FM Lab antigo e instale o novo (isso apaga os dados locais do app).

## Referências técnicas

- [Xiaomi — Redmi Note 13, FAQ (suporte a FM)](https://www.mi.com/my/support/faq/details/KA-91513/)
- [Android — `AudioDeviceInfo.TYPE_FM_TUNER`](https://developer.android.com/reference/android/media/AudioDeviceInfo)
- [AOSP — `ACCESS_FM_RADIO` e `ACCESS_BROADCAST_RADIO` (permissões reservadas)](https://android.googlesource.com/platform/frameworks/base/+/main/core/res/AndroidManifest.xml)
- [android-actions/setup-android v4](https://github.com/android-actions/setup-android)

**Verificação:** os arquivos do projeto foram preparados e revisados neste ambiente, mas **o APK não foi compilado nem testado fisicamente aqui**. A compilação é realizada pelo Android Studio/GitHub Actions; a recepção FM depende do Redmi real.
