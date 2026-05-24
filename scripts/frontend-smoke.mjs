import {spawn} from 'node:child_process'
import crypto from 'node:crypto'
import {existsSync, mkdtempSync, rmSync} from 'node:fs'
import net from 'node:net'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

const baseUrl = process.env.FRONTEND_BASE_URL || 'http://localhost:5173'
const debugPort = Number(process.env.FRONTEND_SMOKE_DEBUG_PORT || 9222)
const skipLogin = process.env.FRONTEND_SMOKE_SKIP_LOGIN === '1'
const username = process.env.FRONTEND_SMOKE_USERNAME || 'admin'
const password = process.env.FRONTEND_SMOKE_PASSWORD || 'Admin@123456'
const expectLoginRedirect = process.env.FRONTEND_SMOKE_EXPECT_LOGIN_REDIRECT === '1'
const routesToCheck = process.env.FRONTEND_SMOKE_ROUTES
  ? process.env.FRONTEND_SMOKE_ROUTES.split(',').map((item) => item.trim()).filter(Boolean)
  : [
      '/dashboard',
      '/system/users',
      '/system/roles',
      '/system/depts',
      '/system/menus',
      '/ai/chat',
      '/ai/models',
      '/ai/rag/knowledge',
      '/ai/ask/overview',
      '/ai/agents',
      '/ai/workflows',
    ]

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function findBrowser() {
  const candidates = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  ]
  return candidates.find((candidate) => existsSync(candidate))
}

async function waitForJson(url, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url)
      if (response.ok) {
        return response.json()
      }
    } catch (error) {
      // Browser not ready yet.
    }
    await sleep(200)
  }
  throw new Error(`Timed out waiting for ${url}`)
}

class TinyWebSocket {
  constructor(wsUrl) {
    this.url = new URL(wsUrl)
    this.socket = null
    this.readyState = 'connecting'
    this.handshakeBuffer = Buffer.alloc(0)
    this.frameBuffer = Buffer.alloc(0)
    this.fragmentBuffer = Buffer.alloc(0)
    this.openHandlers = []
    this.messageHandlers = []
    this.errorHandlers = []
    this.closeHandlers = []
    this.openPromise = new Promise((resolve, reject) => {
      this._resolveOpen = resolve
      this._rejectOpen = reject
    })
    this.connect()
  }

  connect() {
    const port = Number(this.url.port || 80)
    this.socket = net.createConnection({
      host: this.url.hostname,
      port,
    })

    this.socket.once('connect', () => {
      const key = crypto.randomBytes(16).toString('base64')
      const request = [
        `GET ${this.url.pathname}${this.url.search} HTTP/1.1`,
        `Host: ${this.url.hostname}:${port}`,
        'Upgrade: websocket',
        'Connection: Upgrade',
        `Sec-WebSocket-Key: ${key}`,
        'Sec-WebSocket-Version: 13',
        '\r\n',
      ].join('\r\n')
      this.socket.write(request)
    })

    this.socket.on('data', (chunk) => {
      if (this.readyState === 'connecting') {
        this.handshakeBuffer = Buffer.concat([this.handshakeBuffer, chunk])
        const boundary = this.handshakeBuffer.indexOf('\r\n\r\n')
        if (boundary === -1) {
          return
        }
        const header = this.handshakeBuffer.subarray(0, boundary).toString('utf8')
        if (!header.startsWith('HTTP/1.1 101')) {
          this.emitError(new Error(`WebSocket handshake failed: ${header.split('\r\n')[0]}`))
          return
        }
        this.readyState = 'open'
        this._resolveOpen()
        this.openHandlers.forEach((handler) => handler())
        const remaining = this.handshakeBuffer.subarray(boundary + 4)
        this.handshakeBuffer = Buffer.alloc(0)
        if (remaining.length) {
          this.consumeFrames(remaining)
        }
        return
      }
      this.consumeFrames(chunk)
    })

    this.socket.on('error', (error) => {
      if (this.readyState === 'connecting') {
        this._rejectOpen(error)
      }
      this.emitError(error)
    })

    this.socket.on('close', () => {
      this.readyState = 'closed'
      this.closeHandlers.forEach((handler) => handler())
    })
  }

  consumeFrames(chunk) {
    this.frameBuffer = Buffer.concat([this.frameBuffer, chunk])
    while (true) {
      if (this.frameBuffer.length < 2) {
        return
      }

      const firstByte = this.frameBuffer[0]
      const secondByte = this.frameBuffer[1]
      const fin = Boolean(firstByte & 0x80)
      const opcode = firstByte & 0x0f
      const masked = Boolean(secondByte & 0x80)
      let payloadLength = secondByte & 0x7f
      let offset = 2

      if (payloadLength === 126) {
        if (this.frameBuffer.length < offset + 2) {
          return
        }
        payloadLength = this.frameBuffer.readUInt16BE(offset)
        offset += 2
      } else if (payloadLength === 127) {
        if (this.frameBuffer.length < offset + 8) {
          return
        }
        const high = this.frameBuffer.readUInt32BE(offset)
        const low = this.frameBuffer.readUInt32BE(offset + 4)
        payloadLength = high * 2 ** 32 + low
        offset += 8
      }

      const maskOffset = masked ? 4 : 0
      if (this.frameBuffer.length < offset + maskOffset + payloadLength) {
        return
      }

      let payload = this.frameBuffer.subarray(offset + maskOffset, offset + maskOffset + payloadLength)
      if (masked) {
        const mask = this.frameBuffer.subarray(offset, offset + 4)
        payload = Buffer.from(payload)
        for (let i = 0; i < payload.length; i += 1) {
          payload[i] ^= mask[i % 4]
        }
      }

      this.frameBuffer = this.frameBuffer.subarray(offset + maskOffset + payloadLength)
      this.handleFrame(opcode, fin, payload)
    }
  }

  handleFrame(opcode, fin, payload) {
    if (opcode === 0x8) {
      this.readyState = 'closed'
      this.socket?.end()
      return
    }

    if (opcode === 0x9) {
      this.sendFrame(0xA, payload)
      return
    }

    if (opcode === 0x1 || opcode === 0x0) {
      this.fragmentBuffer = Buffer.concat([this.fragmentBuffer, payload])
      if (!fin) {
        return
      }
      const message = this.fragmentBuffer.toString('utf8')
      this.fragmentBuffer = Buffer.alloc(0)
      this.messageHandlers.forEach((handler) => handler({data: message}))
    }
  }

  async send(data) {
    await this.openPromise
    this.sendFrame(0x1, Buffer.from(data, 'utf8'))
  }

  sendFrame(opcode, payload) {
    const payloadLength = payload.length
    const mask = crypto.randomBytes(4)
    let header

    if (payloadLength < 126) {
      header = Buffer.alloc(2)
      header[1] = 0x80 | payloadLength
    } else if (payloadLength < 65536) {
      header = Buffer.alloc(4)
      header[1] = 0x80 | 126
      header.writeUInt16BE(payloadLength, 2)
    } else {
      header = Buffer.alloc(10)
      header[1] = 0x80 | 127
      header.writeUInt32BE(Math.floor(payloadLength / 2 ** 32), 2)
      header.writeUInt32BE(payloadLength >>> 0, 6)
    }

    header[0] = 0x80 | opcode
    const maskedPayload = Buffer.from(payload)
    for (let i = 0; i < maskedPayload.length; i += 1) {
      maskedPayload[i] ^= mask[i % 4]
    }
    this.socket?.write(Buffer.concat([header, mask, maskedPayload]))
  }

  addEventListener(type, handler) {
    if (type === 'open') {
      this.openHandlers.push(handler)
      return
    }
    if (type === 'message') {
      this.messageHandlers.push(handler)
      return
    }
    if (type === 'error') {
      this.errorHandlers.push(handler)
      return
    }
    if (type === 'close') {
      this.closeHandlers.push(handler)
    }
  }

  emitError(error) {
    this.errorHandlers.forEach((handler) => handler(error))
  }

  close() {
    if (this.readyState === 'closed') {
      return
    }
    this.sendFrame(0x8, Buffer.alloc(0))
    this.socket?.end()
    this.readyState = 'closed'
  }
}

class CdpClient {
  constructor(wsUrl) {
    this.ws = new TinyWebSocket(wsUrl)
    this.nextId = 1
    this.pending = new Map()
    this.eventHandlers = new Map()
    this.openPromise = new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve)
      this.ws.addEventListener('error', reject)
    })
    this.ws.addEventListener('message', (event) => {
      const payload = JSON.parse(event.data.toString())
      if (payload.id) {
        const pending = this.pending.get(payload.id)
        if (!pending) {
          return
        }
        this.pending.delete(payload.id)
        if (payload.error) {
          pending.reject(new Error(payload.error.message))
          return
        }
        pending.resolve(payload.result)
        return
      }
      const handlers = this.eventHandlers.get(payload.method) || []
      handlers.forEach((handler) => handler(payload.params || {}))
    })
  }

  async ready() {
    await this.openPromise
  }

  async send(method, params = {}) {
    await this.ready()
    const id = this.nextId++
    const payload = JSON.stringify({id, method, params})
    return new Promise((resolve, reject) => {
      this.pending.set(id, {resolve, reject})
      this.ws.send(payload)
    })
  }

  on(method, handler) {
    const handlers = this.eventHandlers.get(method) || []
    handlers.push(handler)
    this.eventHandlers.set(method, handlers)
  }

  once(method) {
    return new Promise((resolve) => {
      const handler = (params) => {
        const handlers = this.eventHandlers.get(method) || []
        this.eventHandlers.set(
          method,
          handlers.filter((item) => item !== handler),
        )
        resolve(params)
      }
      this.on(method, handler)
    })
  }

  close() {
    this.ws.close()
  }
}

async function evaluate(page, expression) {
  const result = await page.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
  })
  return result.result?.value
}

async function waitForCondition(page, expression, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const value = await evaluate(page, expression)
    if (value) {
      return value
    }
    await sleep(200)
  }
  throw new Error(`Condition timed out: ${expression}`)
}

async function navigate(page, url) {
  const loadEvent = page.once('Page.loadEventFired')
  await page.send('Page.navigate', {url})
  await loadEvent
}

async function installErrorHooks(page) {
  await evaluate(
    page,
    `
      (() => {
        if (window.__techenSmokeInstalled) {
          return true
        }
        window.__techenSmokeInstalled = true
        window.__techenSmokeErrors = []
        window.addEventListener('error', (event) => {
          window.__techenSmokeErrors.push({
            type: 'error',
            message: event.message || 'Unknown error'
          })
        })
        window.addEventListener('unhandledrejection', (event) => {
          const reason = event.reason
          window.__techenSmokeErrors.push({
            type: 'unhandledrejection',
            message: typeof reason === 'string' ? reason : JSON.stringify(reason)
          })
        })
        return true
      })()
    `,
  )
}

async function performLogin(page) {
  await navigate(page, `${baseUrl}/login`)
  await waitForCondition(page, `!!document.querySelector('.login-card')`)
  await installErrorHooks(page)
  await evaluate(
    page,
    `
      (() => {
        const inputs = Array.from(document.querySelectorAll('.login-form input'))
        if (inputs.length < 2) {
          throw new Error('Login inputs not found')
        }
        const [usernameInput, passwordInput] = inputs
        const setValue = (element, value) => {
          const descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')
          descriptor.set.call(element, value)
          element.dispatchEvent(new Event('input', { bubbles: true }))
          element.dispatchEvent(new Event('change', { bubbles: true }))
        }
        setValue(usernameInput, ${JSON.stringify(username)})
        setValue(passwordInput, ${JSON.stringify(password)})
        document.querySelector('.login-button')?.click()
        return true
      })()
    `,
  )
  await waitForCondition(
    page,
    `window.location.pathname === '/dashboard' && !!document.querySelector('.layout-logo-title')`,
    20000,
  )
}

async function inspectRoute(page, routePath) {
  await navigate(page, `${baseUrl}${routePath}`)
  await waitForCondition(
    page,
    `window.location.pathname === '${routePath}' || !!document.querySelector('.login-card') || !!document.querySelector('.layout-logo-title')`,
    15000,
  )
  await installErrorHooks(page)
  await sleep(800)
  const routeLiteral = JSON.stringify(routePath)
  return evaluate(
    page,
    `
      (() => {
        const routePath = ${routeLiteral}
        const text = document.body.innerText || ''
        const titleNode =
          document.querySelector('.table-toolbar__title') ||
          document.querySelector('.layout-tags .tag-item.active') ||
          document.querySelector('h1') ||
          document.querySelector('h3')
        return {
          path: window.location.pathname,
          title: titleNode ? titleNode.textContent.trim() : '',
          hasLayout: !!document.querySelector('.layout-logo-title'),
          notFound: text.includes('页面不存在'),
          forbidden: text.includes('当前路由尚未被授权加载') || text.includes('无权访问'),
          apiFallback: text.includes('接口暂不可用'),
          rowCount: document.querySelectorAll('.el-table__row').length,
          emptyState: !!document.querySelector('.el-empty'),
          agentWorkbenchVisible: routePath === '/ai/agents'
            ? text.includes('智能体运行台') && text.includes('运行结果')
            : true,
          loginVisible: !!document.querySelector('.login-card'),
          errors: Array.isArray(window.__techenSmokeErrors) ? window.__techenSmokeErrors.slice(-5) : []
        }
      })()
    `,
  )
}

function summarize(result) {
  const issues = []
  const loginRedirectOk = expectLoginRedirect && result.loginVisible && result.path === '/login'
  if (!result.hasLayout && !loginRedirectOk) {
    issues.push('layout-missing')
  }
  if (result.loginVisible && !loginRedirectOk) {
    issues.push('redirected-to-login')
  }
  if (result.notFound) {
    issues.push('404')
  }
  if (result.forbidden) {
    issues.push('403')
  }
  if (result.apiFallback) {
    issues.push('api-fallback')
  }
  if (!result.agentWorkbenchVisible) {
    issues.push('agent-workbench-missing')
  }
  if (result.errors?.length) {
    issues.push('runtime-error')
  }
  return {
    ...result,
    status: issues.length ? 'FAIL' : 'PASS',
    expectedLoginRedirect: loginRedirectOk,
    issues: issues.join(', '),
  }
}

async function main() {
  const browserPath = findBrowser()
  if (!browserPath) {
    throw new Error('Chrome or Edge was not found on this machine.')
  }

  const userDataDir = mkdtempSync(path.join(os.tmpdir(), 'techen-frontend-smoke-'))
  let browserProcess
  let browserClient
  let pageClient

  try {
    browserProcess = spawn(
      browserPath,
      [
        '--headless=new',
        '--disable-gpu',
        '--no-first-run',
        '--no-default-browser-check',
        `--remote-debugging-port=${debugPort}`,
        `--user-data-dir=${userDataDir}`,
        'about:blank',
      ],
      {stdio: 'ignore'},
    )

    const version = await waitForJson(`http://127.0.0.1:${debugPort}/json/version`)
    browserClient = new CdpClient(version.webSocketDebuggerUrl)
    await browserClient.ready()

    const target = await browserClient.send('Target.createTarget', {url: 'about:blank'})
    const targets = await waitForJson(`http://127.0.0.1:${debugPort}/json/list`)
    const pageTarget = targets.find((item) => item.id === target.targetId)
    if (!pageTarget?.webSocketDebuggerUrl) {
      throw new Error('Unable to find page websocket target.')
    }

    pageClient = new CdpClient(pageTarget.webSocketDebuggerUrl)
    await pageClient.ready()
    await pageClient.send('Page.enable')
    await pageClient.send('Runtime.enable')

    console.log(`Using browser: ${browserPath}`)
    console.log(`Checking frontend: ${baseUrl}`)
    if (!skipLogin) {
      console.log(`Logging in as: ${username}`)
    }

    if (!skipLogin) {
      await performLogin(pageClient)
    }

    const results = []
    for (const routePath of routesToCheck) {
      const inspection = await inspectRoute(pageClient, routePath)
      results.push(summarize(inspection))
    }

    console.log('')
    console.log('Frontend Smoke Summary')
    console.table(
      results.map((item) => ({
        path: item.path,
        status: item.status,
        title: item.title,
        rows: item.rowCount,
        empty: item.emptyState,
        issues: item.issues,
      })),
    )

    const failed = results.filter((item) => item.status !== 'PASS')
    if (failed.length) {
      throw new Error(`Frontend smoke failed for: ${failed.map((item) => item.path).join(', ')}`)
    }

    console.log('All frontend smoke checks passed.')
  } finally {
    pageClient?.close()
    browserClient?.close()
    if (browserProcess && !browserProcess.killed) {
      browserProcess.kill()
    }
    await sleep(300)
    try {
      rmSync(userDataDir, {recursive: true, force: true})
    } catch (error) {
      console.warn(`Temporary browser profile cleanup skipped: ${error.message}`)
    }
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
