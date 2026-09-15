import { BaseSideService } from '@zeppos/zml/base-side'
import { TAKE_PHOTO } from '../shared/protocol'

const ANDROID_ENDPOINT = 'http://127.0.0.1:8765/command'
const REQUEST_TIMEOUT = 70000

AppSideService(BaseSideService({
  async onRequest(req, res) {
    if (!req || req.method !== TAKE_PHOTO || !req.params || req.params.name !== TAKE_PHOTO) {
      res('Comando desconhecido')
      return
    }

    try {
      // O BaseSideService expõe fetch(). httpRequest() existe na página do relógio,
      // mas não é um método disponível no serviço que roda dentro do Zepp App.
      const result = await this.fetch({
        url: ANDROID_ENDPOINT,
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: 'camera-command',
          name: TAKE_PHOTO,
          delaySeconds: Math.max(0, Math.min(30, Number(req.params.delaySeconds) || 0)),
          createdAt: Date.now(),
        }),
        timeout: REQUEST_TIMEOUT,
      })
      const status = Number(result && result.status)
      if (!Number.isInteger(status) || status < 200 || status >= 300) {
        res(`Android respondeu HTTP ${status || 'desconhecido'}`)
        return
      }
      res(null, { result: true, status })
    } catch (error) {
      res(error && error.message ? error.message : 'Não foi possível contactar o telefone')
    }
  },
}))
