import { createWidget, widget, align, prop } from '@zos/ui'
import { BasePage } from '@zeppos/zml/base-page'
import { commandPayload, TAKE_PHOTO, TIMER_OPTIONS } from '../shared/protocol'

const BUTTON_LABEL = 'TIRAR FOTO'
const REQUEST_TIMEOUT = 8000

Page(BasePage({
  state: {
    button: null,
    timerButtons: [],
    statusWidget: null,
    resetTimer: null,
    requestInFlight: false,
    destroyed: false,
    selectedDelay: 0,
  },

  build() {
    this.state.destroyed = false
    createWidget(widget.TEXT, {
      x: 20,
      y: 42,
      w: 350,
      h: 50,
      text: 'CÂMERA REMOTA',
      text_size: 28,
      align_h: align.CENTER_H,
    })

    createWidget(widget.TEXT, {
      x: 30,
      y: 92,
      w: 330,
      h: 34,
      text: 'Active 2 Square  •  controle remoto',
      text_size: 17,
      color: 0x8f9bb3,
      align_h: align.CENTER_H,
    })

    this.state.statusWidget = createWidget(widget.TEXT, {
      x: 30,
      y: 122,
      w: 330,
      h: 28,
      text: 'PRONTO PARA CAPTURAR',
      text_size: 16,
      color: 0x34e073,
      align_h: align.CENTER_H,
    })

    const setButtonText = (text) => {
      if (this.state.button) this.state.button.setProperty(prop.TEXT, text)
    }

    const setStatus = (text, color = 0x8f9bb3) => {
      if (this.state.statusWidget) {
        this.state.statusWidget.setProperty(prop.MORE, {
          x: 30,
          y: 122,
          w: 330,
          h: 28,
          text,
          text_size: 16,
          color,
          align_h: align.CENTER_H,
        })
      }
    }

    const captureLabel = () => this.state.selectedDelay > 0
      ? `FOTO EM ${this.state.selectedDelay}s`
      : BUTTON_LABEL

    const finishRequest = (text, delay) => {
      if (this.state.destroyed) return
      setButtonText(text)
      setStatus(text, text === 'FOTO ENVIADA' ? 0x34e073 : 0xff6b6b)
      if (this.state.resetTimer) clearTimeout(this.state.resetTimer)
      this.state.resetTimer = setTimeout(() => {
        setButtonText(captureLabel())
        setStatus(
          this.state.selectedDelay > 0
            ? `TEMPORIZADOR: ${this.state.selectedDelay}s`
            : 'PRONTO PARA CAPTURAR',
          0x8f9bb3,
        )
        this.state.requestInFlight = false
        this.state.resetTimer = null
      }, delay)
    }

    const refreshTimerButtons = () => {
      this.state.timerButtons.forEach((timerButton, index) => {
        const delay = TIMER_OPTIONS[index]
        const selected = delay === this.state.selectedDelay
        timerButton.setProperty(prop.MORE, {
          x: 25 + index * 90,
          y: 310,
          w: 78,
          h: 58,
          text: delay === 0 ? 'AGORA' : `${delay}s`,
          text_size: 18,
          radius: 16,
          color: 0xffffff,
          normal_color: selected ? 0x2457d6 : 0x334155,
          press_color: selected ? 0x15368c : 0x1e293b,
        })
      })
    }

    const selectTimer = (delay) => {
      if (this.state.requestInFlight) return
      this.state.selectedDelay = delay
      refreshTimerButtons()
      setButtonText(captureLabel())
      setStatus(delay > 0 ? `TEMPORIZADOR: ${delay}s` : 'FOTO IMEDIATA')
    }

    const sendPhoto = () => {
      if (this.state.requestInFlight) return

      this.state.requestInFlight = true
      setButtonText('ENVIANDO…')
      setStatus(
        this.state.selectedDelay > 0
          ? `AGENDANDO FOTO EM ${this.state.selectedDelay}s`
          : 'ENVIANDO COMANDO',
        0xffd166,
      )
      this.request({
        method: TAKE_PHOTO,
        params: commandPayload(this.state.selectedDelay),
      }, { timeout: REQUEST_TIMEOUT }).then((result) => {
        if (!result || result.result !== true) {
          throw new Error('O telefone recusou o comando')
        }
        finishRequest('FOTO ENVIADA', 1200)
      }).catch(() => finishRequest('ERRO', 1800))
    }

    this.state.button = createWidget(widget.BUTTON, {
      x: 40,
      y: 150,
      w: 310,
      h: 120,
      text: BUTTON_LABEL,
      text_size: 28,
      radius: 24,
      normal_color: 0x2457d6,
      press_color: 0x15368c,
      click_func: sendPhoto,
    })

    createWidget(widget.TEXT, {
      x: 30,
      y: 280,
      w: 330,
      h: 25,
      text: 'TEMPORIZADOR',
      text_size: 16,
      color: 0x8f9bb3,
      align_h: align.CENTER_H,
    })

    TIMER_OPTIONS.forEach((delay) => {
      this.state.timerButtons.push(createWidget(widget.BUTTON, {
        x: 25 + this.state.timerButtons.length * 90,
        y: 310,
        w: 78,
        h: 58,
        text: delay === 0 ? 'AGORA' : `${delay}s`,
        text_size: 18,
        radius: 16,
        color: 0xffffff,
        normal_color: delay === this.state.selectedDelay ? 0x2457d6 : 0x334155,
        press_color: delay === this.state.selectedDelay ? 0x15368c : 0x1e293b,
        click_func: () => selectTimer(delay),
      }))
    })

    createWidget(widget.TEXT, {
      x: 30,
      y: 390,
      w: 330,
      h: 40,
      text: 'Mantenha o app aberto no telefone',
      text_size: 16,
      color: 0x8f9bb3,
      align_h: align.CENTER_H,
    })
  },

  onDestroy() {
    this.state.destroyed = true
    if (this.state.resetTimer) {
      clearTimeout(this.state.resetTimer)
      this.state.resetTimer = null
    }
    this.state.button = null
    this.state.timerButtons = []
    this.state.statusWidget = null
    this.state.requestInFlight = false
  },
}))
