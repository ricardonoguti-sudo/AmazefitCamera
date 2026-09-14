# Android Camera Remote

App Android para o POCO X6 Pro 5G. Ele mantém uma câmera CameraX aberta e escuta comandos locais enviados pelo Side Service do Zepp OS.

## Uso

1. Abra o app e conceda permissão de câmera.
2. Escolha **USAR CÂMERA FRONTAL** se quiser trocar a lente; o botão alterna entre frontal e traseira.
3. Toque no preview para ajustar foco e deixe a tela de câmera aberta.
4. No Active 2 Square, escolha o temporizador e toque em **TIRAR FOTO**.
5. As fotos aparecem na galeria em `Pictures/AmazfitRemote`.

A captura usa a maior resolução 4:3 disponível, modo de qualidade máxima e JPEG em 95%. O foco por toque usa autofocus e medição de exposição quando o hardware oferece esses recursos; câmeras frontais com foco fixo continuam funcionando normalmente.
O temporizador aceita foto imediata ou atrasos de 3, 5 e 10 segundos, selecionados no relógio.

O servidor local escuta `127.0.0.1:8765/command` somente enquanto a Activity está em primeiro plano. Ele aceita `POST` cujo corpo contenha `TAKE_PHOTO` e responde `200 OK` após enfileirar a captura. Requisições inválidas retornam `400` ou `404`.

## Compilação

Abra este diretório no Android Studio atualizado e execute no POCO X6 Pro. O projeto usa Kotlin, AndroidX e CameraX.
