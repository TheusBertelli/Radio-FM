# Estratégia de compatibilidade OEM — FM Livre Universal

A API pública do Android permite **diagnosticar** dispositivos `AudioDeviceInfo.TYPE_FM_TUNER` e `TYPE_FM`, não iniciar, ligar ou sintonizar o hardware FM.

- Samsung: o app de rádio pode usar `com.sec.android.app.fm` em determinados modelos; outros podem não ter rádio ou não oferecer activity pública.
- Xiaomi/Redmi/POCO: ROMs MIUI/HyperOS variam os pacotes e bloqueios do rádio, mesmo quando o receptor é funcional.
- Motorola: disponibilidade de rádio e nome do pacote dependem de modelo e ROM; consulta inclui candidatos conhecidos sem presumir que existam.
- Outros: atividades de tela inicial com nome de rádio são detectadas sem `QUERY_ALL_PACKAGES`, mas podem ser players por internet.

## APIs e evidências pesquisadas

1. `PackageManager`: pacotes/atividades iniciáveis e exportadas — não permite comandar serviço privado do OEM.
2. `AudioManager`/`AudioDeviceInfo`: rotas FM, fones e áudio USB/Bluetooth — não prova chip acessível nem sintonia.
3. `UsbManager`: periféricos conectados, IDs VID/PID — não equivale a driver USB SDR.
4. `PackageManager.hasSystemFeature`: nomes FM não padronizados — indício fraco e ausências inconclusivas.
5. Arquivos `/dev/radio*`: só existência e `canRead()` — não permite comandos nem evita SELinux.

Nenhum dos sinais é suficiente isoladamente para habilitar controles Play/Pause/Scan FM reais. Um adaptador OEM exige API pública/documentada ou parceria com o fabricante, e testes por versão de firmware.

## Testes de regressão no aparelho

- Iniciar sem `INTERNET`, negar permissões indisponíveis (o app não as solicita).
- Abrir com fone desconectado/conectado e com rádio nativo ausente, desativado e presente.
- Dispositivo sem rádio físico: nenhuma estação fictícia toca.
- Android 11, 12, 13, 14, 15, 16: testar descobrimento `<queries>`, tema claro/escuro e rotação.
- OEM Samsung, Motorola, Xiaomi/Redmi/POCO: confirmar pacote real, se activity é exportada e launch ocorre.
- Sem receptor acessível: relatório claro, sem crash ou falsa confirmação.
- Com receptor nativo: confirmar áudio com Wi-Fi e dados móveis desligados.
