/**
 * 消息提示音 — 仿微信 "叮" 声
 * 使用 Web Audio API，无需外部音频文件
 */

let audioCtx = null

function getCtx() {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)()
  }
  // 某些浏览器需要 resume（自动播放策略）
  if (audioCtx.state === 'suspended') {
    audioCtx.resume()
  }
  return audioCtx
}

/** 播放提示音：简短清脆的 "叮～咚" */
export function playMessageSound() {
  try {
    const ctx = getCtx()
    const now = ctx.currentTime

    // 第一个音：叮 (高频短促)
    const osc1 = ctx.createOscillator()
    const gain1 = ctx.createGain()
    osc1.type = 'sine'
    osc1.frequency.setValueAtTime(1200, now)
    osc1.frequency.exponentialRampToValueAtTime(800, now + 0.08)
    gain1.gain.setValueAtTime(0.3, now)
    gain1.gain.exponentialRampToValueAtTime(0.01, now + 0.12)
    osc1.connect(gain1).connect(ctx.destination)
    osc1.start(now)
    osc1.stop(now + 0.12)

    // 第二个音：咚 (低沉)
    const osc2 = ctx.createOscillator()
    const gain2 = ctx.createGain()
    osc2.type = 'sine'
    osc2.frequency.setValueAtTime(600, now + 0.12)
    osc2.frequency.exponentialRampToValueAtTime(400, now + 0.28)
    gain2.gain.setValueAtTime(0.01, now + 0.10)
    gain2.gain.linearRampToValueAtTime(0.25, now + 0.14)
    gain2.gain.exponentialRampToValueAtTime(0.01, now + 0.35)
    osc2.connect(gain2).connect(ctx.destination)
    osc2.start(now + 0.12)
    osc2.stop(now + 0.35)
  } catch {
    // 静默失败：某些极端环境不支持 Web Audio
  }
}
