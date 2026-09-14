# Zepp Camera Remote

Miniapp para o Amazfit Active 2 Square (Zepp OS / API compatível 4.0).

## Como funciona

- Toque no botão no relógio.
- O miniapp envia o comando `TAKE_PHOTO` ao Side Service.
- O Side Service encaminha um POST para o app Android em `http://127.0.0.1:8765/command`.
- O seletor **TEMPORIZADOR** oferece diretamente foto imediata e atrasos de 3, 5 ou 10 segundos.
- Toques repetidos são ignorados durante o envio, e o botão só confirma sucesso quando o Android responde com HTTP 2xx.

## Teste

1. Instale o Zeus CLI conforme a documentação do Zepp OS.
2. Execute `zeus dev` neste diretório.
3. Ative o modo desenvolvedor no Zepp e faça o preview por QR code.
4. Mantenha o app Android Camera Remote aberto no POCO X6 Pro.

O encaminhamento para localhost é experimental porque o Side Service executa dentro do Zepp App. Se o HyperOS ou a versão do Zepp bloquear o loopback, a alternativa será usar um relay HTTPS local/externo.

Referências: [Quick Start do Zepp OS](https://docs.zepp.com/docs/v2/guides/quick-start/) e [Bluetooth Communication](https://docs.zepp.com/docs/guides/best-practice/bluetooth-communication/).
