# Contexto para o Codex

## O que é este projeto

`AmazefitCamera` é um projeto experimental que permite disparar a câmera de um telefone Android usando um Amazfit Active 2 Square. A solução tem dois módulos que precisam permanecer compatíveis:

- `android-camera-remote/`: app Android em Kotlin, com CameraX, preview da câmera e servidor HTTP local.
- `zepp-camera-remote/`: miniapp Zepp OS, interface do relógio e Side Service que encaminha comandos ao telefone.

Leia o [`README.md`](README.md) antes de fazer mudanças de maior escopo. Os READMEs dos módulos contêm instruções específicas de uso.

## Fluxo e contrato importante

```text
Relógio → miniapp Zepp OS → Side Service → POST http://127.0.0.1:8765/command → app Android → CameraX
```

- O comando canônico é `TAKE_PHOTO`.
- O corpo HTTP é JSON e pode conter `delaySeconds`; o valor válido é limitado entre 0 e 30 segundos.
- O servidor Android escuta somente em `127.0.0.1:8765` e só fica ativo com a Activity em primeiro plano.
- A captura é salva pelo Android em `Pictures/AmazefitRemote`.
- Ao alterar o protocolo, atualize em conjunto `zepp-camera-remote/shared/protocol.js`, o Side Service e `LocalCommandServer.kt`.

## Diretrizes de implementação

- Preserve a separação entre o app Android e o miniapp Zepp OS; não mova código entre os módulos sem necessidade.
- No Android, mantenha Kotlin, AndroidX/CameraX, namespace `br.com.projetos.cameraremote`, `minSdk 26`, `compileSdk 35` e Java/Kotlin 17.
- No Zepp OS, use a dependência oficial já declarada em `package.json` e mantenha constantes de protocolo no diretório `shared/`.
- Strings visíveis ao usuário Android devem ficar em `android-camera-remote/app/src/main/res/values/strings.xml`.
- Preserve o encaminhamento por loopback, salvo se a tarefa pedir explicitamente outra arquitetura. Ele é experimental por limitações que podem existir no HyperOS/Zepp.
- Não versionar `node_modules/`, `.gradle/`, `**/build/`, `local.properties`, `.idea/` ou `.DS_Store`. O pacote `.zab` existente em `zepp-camera-remote/dist/` é um artefato publicado intencionalmente e deve ser preservado, a menos que a tarefa peça sua remoção.

## Validação

- Para o Android, abra `android-camera-remote/` no Android Studio ou use `./gradlew` nesse diretório para fazer sync/build no dispositivo ou emulador.
- Para o miniapp, em `zepp-camera-remote/`, execute `npm install` e depois `zeus dev` conforme a documentação do Zepp OS.
- Para qualquer alteração, execute `git diff --check` e verifique `git status --short` antes de concluir.
- Não alegue que um build ou teste passou sem executar o comando correspondente; se uma ferramenta não estiver instalada, registre a limitação.
