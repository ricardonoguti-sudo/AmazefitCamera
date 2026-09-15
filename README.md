# Amazfit Camera Remote

Controle a câmera do Android pelo Amazfit Active 2 Square. O projeto é dividido em um app Android, que mantém a câmera aberta e recebe comandos locais, e um miniapp Zepp OS, que envia o comando a partir do relógio.

## Como funciona

```text
Amazfit Active 2 → miniapp Zepp OS → Side Service → HTTP local → app Android → CameraX → galeria
```

Ao tocar em **TIRAR FOTO** no relógio, o miniapp envia `TAKE_PHOTO`. O app Android escuta `127.0.0.1:8765/command`, captura a imagem e salva em `Pictures/AmazfitRemote`. O temporizador oferece captura imediata ou atrasos de 3, 5 e 10 segundos.

O relógio exibe **FOTO SALVA** e vibra somente após o CameraX confirmar que a imagem foi gravada. Falhas na captura são devolvidas ao relógio como erro.

## Estrutura

- [`android-camera-remote/`](android-camera-remote/) — aplicativo Android em Kotlin com CameraX.
- [`zepp-camera-remote/`](zepp-camera-remote/) — miniapp Zepp OS e Side Service.
- [`zepp-camera-remote/dist/`](zepp-camera-remote/dist/) — pacote `.zab` gerado do miniapp.

## Requisitos

- Android Studio atualizado e um dispositivo Android com Android 8.0 (API 26) ou superior.
- JDK 17, Android SDK 35 e acesso à internet para resolver dependências Gradle.
- Node.js/npm e Zeus CLI para desenvolver o miniapp Zepp OS.
- Amazfit Active 2 Square e o app Android Camera Remote instalados/abertos.

## App Android

1. Abra [`android-camera-remote/`](android-camera-remote/) no Android Studio.
2. Execute o app no telefone e conceda a permissão de câmera.
3. Mantenha a Activity da câmera em primeiro plano.
4. Toque no preview para ajustar foco, se o hardware oferecer esse recurso.

O app usa a maior resolução 4:3 disponível, qualidade máxima e JPEG em 95%. O servidor local só fica ativo enquanto a Activity está em primeiro plano.

## Miniapp Zepp OS

1. Instale o Zeus CLI conforme a [documentação do Zepp OS](https://docs.zepp.com/docs/v2/guides/quick-start/).
2. Entre em [`zepp-camera-remote/`](zepp-camera-remote/) e instale as dependências:

   ```bash
   npm install
   ```

3. Execute `zeus dev` e faça o preview pelo QR code no Zepp.
4. Com o app Android aberto, selecione o temporizador e toque em **TIRAR FOTO**.

O encaminhamento para `localhost` é experimental: algumas versões do HyperOS ou do Zepp podem bloquear o loopback. Nesse caso, será necessário usar um relay HTTPS local ou externo.

## Status

Este é um projeto experimental para uso pessoal. As versões e os artefatos gerados podem mudar durante o desenvolvimento.
