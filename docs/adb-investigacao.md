# Investigação FM da Xiaomi por ADB — sem root

Esta investigação coleta apenas informações que o Android permite consultar. Não altera o firmware, não desbloqueia o bootloader e não garante acesso ao hardware FM por um aplicativo comum.

## Preparação (Windows)

1. Ative **Opções de desenvolvedor**: Configurações → Sobre o telefone → toque repetidamente na versão do sistema/MIUI, até ativar o modo desenvolvedor.
2. Ative **Depuração USB** nas Opções de desenvolvedor.
3. Instale o **Android Platform Tools** do Google: https://developer.android.com/tools/releases/platform-tools
4. Conecte o Redmi por USB e permita a depuração no telefone.
5. Abra PowerShell na pasta que contém `adb.exe`.

## Consultas

```powershell
.\adb.exe devices
.\adb.exe shell getprop ro.product.model
.\adb.exe shell getprop ro.build.version.release
.\adb.exe shell getprop ro.build.version.incremental
.\adb.exe shell pm list packages | findstr /I "fm radio miui"
.\adb.exe shell dumpsys package com.miui.fm
.\adb.exe shell dumpsys media.audio_policy
```

Se o FM nativo usar outro pacote, substitua `com.miui.fm` no comando `dumpsys package` pelo nome encontrado na lista. Se o `dumpsys` não retornar informações, o app pode estar ausente, oculto ou restrito.

## O que procurar

- Nome do pacote FM e atividades exportadas (`exported=true`) que outros aplicativos possam abrir.
- Permissões declaradas e requeridas nos serviços/receivers FM. Componentes com permissões `signature` ou `privileged` não são utilizáveis por apps comuns.
- Rota de áudio `FM`/`FM_TUNER`, quando exposta.

Não suponha que um serviço exportado permita alterar frequência: só podemos implementar um adaptador quando houver uma interface documentada ou um contrato de comunicação confirmado e permitido.

## Privacidade

`dumpsys package` pode conter informações sobre configuração de aplicativos. Antes de compartilhar, revise e apague tokens, números de série, identificadores de dispositivo e informações particulares que porventura apareçam. Não envie relatórios completos de `getprop` ou `logcat` sem revisão.
