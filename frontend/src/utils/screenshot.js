import html2canvas from 'html2canvas'

export function startScreenshot() {
  return new Promise((resolve) => {
    const overlay = document.createElement('div')
    overlay.id = 'screenshot-overlay'
    overlay.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:99999;cursor:crosshair;'

    const hint = document.createElement('div')
    hint.textContent = '拖拽选择截图区域，按 Esc 取消'
    hint.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.7);color:#fff;padding:8px 20px;border-radius:6px;font-size:14px;z-index:100001;pointer-events:none;'
    overlay.appendChild(hint)

    const box = document.createElement('div')
    box.style.cssText = 'position:absolute;border:2px dashed #2563EB;background:rgba(37,99,235,0.08);pointer-events:none;display:none;z-index:100000;'
    overlay.appendChild(box)

    let startX = 0, startY = 0
    let dragging = false

    function cleanup() {
      overlay.removeEventListener('mousedown', onMouseDown)
      overlay.removeEventListener('mousemove', onMouseMove)
      overlay.removeEventListener('mouseup', onMouseUp)
      overlay.remove()
    }

    function onKeyDown(e) {
      if (e.key === 'Escape') {
        cleanup()
        document.removeEventListener('keydown', onKeyDown)
        resolve(null)
      }
    }
    document.addEventListener('keydown', onKeyDown)

    function onMouseDown(e) {
      startX = e.clientX
      startY = e.clientY
      dragging = true
      box.style.display = 'block'
      box.style.left = startX + 'px'
      box.style.top = startY + 'px'
      box.style.width = '0px'
      box.style.height = '0px'
    }

    function onMouseMove(e) {
      if (!dragging) return
      const x = Math.min(e.clientX, startX)
      const y = Math.min(e.clientY, startY)
      const w = Math.abs(e.clientX - startX)
      const h = Math.abs(e.clientY - startY)
      box.style.left = x + 'px'
      box.style.top = y + 'px'
      box.style.width = w + 'px'
      box.style.height = h + 'px'
    }

    async function onMouseUp(e) {
      if (!dragging) return
      dragging = false
      const x = Math.min(e.clientX, startX)
      const y = Math.min(e.clientY, startY)
      const w = Math.abs(e.clientX - startX)
      const h = Math.abs(e.clientY - startY)

      overlay.style.display = 'none'
      document.removeEventListener('keydown', onKeyDown)
      cleanup()

      if (w < 10 || h < 10) {
        resolve(null)
        return
      }

      try {
        const canvas = await html2canvas(document.body, {
          x, y, width: w, height: h,
          useCORS: true, allowTaint: true, backgroundColor: null
        })
        const dataUrl = canvas.toDataURL('image/png')
        resolve({ dataUrl, width: w, height: h })
      } catch {
        resolve(null)
      }
    }

    overlay.addEventListener('mousedown', onMouseDown)
    overlay.addEventListener('mousemove', onMouseMove)
    overlay.addEventListener('mouseup', onMouseUp)
    document.body.appendChild(overlay)
  })
}

export function openAnnotationEditor(dataUrl) {
  return new Promise((resolve) => {
    const overlay = document.createElement('div')
    overlay.id = 'annotation-overlay'
    overlay.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:99998;background:rgba(0,0,0,0.5);display:flex;flex-direction:column;align-items:center;justify-content:center;'

    const container = document.createElement('div')
    container.style.cssText = 'position:relative;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,0.3);'

    const toolbar = document.createElement('div')
    toolbar.style.cssText = 'display:flex;align-items:center;gap:8px;padding:8px 12px;background:#f5f5f5;border-bottom:1px solid #ddd;'

    const tools = [
      { id: 'rect', label: '框选', icon: '▢' },
      { id: 'text', label: '文字', icon: 'T' },
      { id: 'arrow', label: '箭头', icon: '→' }
    ]
    let activeTool = 'rect'

    tools.forEach(t => {
      const btn = document.createElement('button')
      btn.textContent = t.icon + ' ' + t.label
      btn.dataset.tool = t.id
      btn.style.cssText = `padding:4px 10px;border:1px solid #ddd;border-radius:4px;cursor:pointer;font-size:13px;background:${t.id === activeTool ? '#2563EB' : '#fff'};color:${t.id === activeTool ? '#fff' : '#333'};`
      btn.onclick = () => {
        activeTool = t.id
        toolbar.querySelectorAll('button').forEach(b => {
          b.style.background = b.dataset.tool === activeTool ? '#2563EB' : '#fff'
          b.style.color = b.dataset.tool === activeTool ? '#fff' : '#333'
        })
      }
      toolbar.appendChild(btn)
    })

    const colorInput = document.createElement('input')
    colorInput.type = 'color'
    colorInput.value = '#F56C6C'
    colorInput.style.cssText = 'width:28px;height:28px;border:none;cursor:pointer;margin-left:4px;'
    toolbar.appendChild(colorInput)

    const spacer = document.createElement('span')
    spacer.style.cssText = 'flex:1;'
    toolbar.appendChild(spacer)

    const toolbarCancelBtn = document.createElement('button')
    toolbarCancelBtn.textContent = '✕ 关闭'
    toolbarCancelBtn.style.cssText = 'padding:4px 10px;border:1px solid #ddd;border-radius:4px;cursor:pointer;font-size:13px;background:#fff;color:#F56C6C;'
    toolbarCancelBtn.onclick = () => { clearTimeout(loadTimeout); overlay.remove(); resolve(null) }
    toolbar.appendChild(toolbarCancelBtn)

    container.appendChild(toolbar)

    const loadingTip = document.createElement('div')
    loadingTip.style.cssText = 'padding:40px 60px;color:#999;font-size:14px;text-align:center;'
    loadingTip.textContent = '图片加载中...'
    container.appendChild(loadingTip)

    const maxW = Math.min(window.innerWidth - 80, 900)
    const maxH = Math.min(window.innerHeight - 160, 600)
    const img = new Image()

    const loadTimeout = setTimeout(() => {
      overlay.remove()
      resolve(null)
    }, 10000)

    img.onerror = () => {
      clearTimeout(loadTimeout)
      overlay.remove()
      resolve(null)
    }

    img.onload = () => {
      clearTimeout(loadTimeout)
      loadingTip.remove()

      let scale = 1
      if (img.width > maxW) scale = maxW / img.width
      if (img.height * scale > maxH) scale = maxH / img.height
      const displayW = img.width * scale
      const displayH = img.height * scale

      const canvasWrap = document.createElement('div')
      canvasWrap.style.cssText = `position:relative;width:${displayW}px;height:${displayH}px;`

      const bgCanvas = document.createElement('canvas')
      bgCanvas.width = displayW
      bgCanvas.height = displayH
      bgCanvas.style.cssText = 'position:absolute;top:0;left:0;z-index:0;'
      const bgCtx = bgCanvas.getContext('2d')
      bgCtx.drawImage(img, 0, 0, displayW, displayH)
      canvasWrap.appendChild(bgCanvas)

      const drawCanvas = document.createElement('canvas')
      drawCanvas.width = displayW
      drawCanvas.height = displayH
      drawCanvas.style.cssText = 'position:absolute;top:0;left:0;z-index:1;cursor:crosshair;'
      const drawCtx = drawCanvas.getContext('2d')
      canvasWrap.appendChild(drawCanvas)

      container.appendChild(canvasWrap)

      const actions = document.createElement('div')
      actions.style.cssText = 'display:flex;justify-content:flex-end;gap:8px;padding:8px 12px;background:#f5f5f5;border-top:1px solid #ddd;'

      const resetBtn = document.createElement('button')
      resetBtn.textContent = '重置'
      resetBtn.style.cssText = 'padding:6px 16px;border:1px solid #ddd;border-radius:4px;cursor:pointer;font-size:13px;background:#fff;'
      resetBtn.onclick = () => { drawCtx.clearRect(0, 0, displayW, displayH) }

      const confirmBtn = document.createElement('button')
      confirmBtn.textContent = '确认插入'
      confirmBtn.style.cssText = 'padding:6px 16px;border:none;border-radius:4px;cursor:pointer;font-size:13px;background:#2563EB;color:#fff;'
      confirmBtn.onclick = () => {
        const merged = document.createElement('canvas')
        merged.width = displayW
        merged.height = displayH
        const mCtx = merged.getContext('2d')
        mCtx.drawImage(bgCanvas, 0, 0)
        mCtx.drawImage(drawCanvas, 0, 0)
        overlay.remove()
        resolve(merged.toDataURL('image/png'))
      }

      actions.appendChild(resetBtn)
      actions.appendChild(confirmBtn)
      container.appendChild(actions)

      let drawing = false
      let sx = 0, sy = 0
      let snapshot = null
      let isTextInputting = false

      drawCanvas.addEventListener('mousedown', (e) => {
        if (isTextInputting) return
        const rect = drawCanvas.getBoundingClientRect()
        sx = e.clientX - rect.left
        sy = e.clientY - rect.top
        drawing = true
        snapshot = drawCtx.getImageData(0, 0, displayW, displayH)

        if (activeTool === 'text') {
          drawing = false
          isTextInputting = true
          drawCanvas.style.pointerEvents = 'none'
          const input = document.createElement('input')
          input.type = 'text'
          input.placeholder = '输入文字后按 Enter 确认...'
          input.style.cssText = `position:absolute;left:${sx}px;top:${sy}px;z-index:20;border:1px solid #2563EB;outline:none;font-size:14px;padding:2px 4px;min-width:120px;background:#fff;`
          canvasWrap.appendChild(input)
          setTimeout(() => input.focus(), 0)
          const finishInput = (save) => {
            if (save && input.value) {
              drawCtx.font = '14px sans-serif'
              drawCtx.fillStyle = colorInput.value
              drawCtx.fillText(input.value, sx, sy + 14)
            }
            input.remove()
            drawCanvas.style.pointerEvents = 'auto'
            isTextInputting = false
          }
          let handled = false
          input.addEventListener('keydown', (ke) => {
            ke.stopPropagation()
            if (ke.key === 'Enter') {
              handled = true
              finishInput(true)
            }
            if (ke.key === 'Escape') {
              handled = true
              finishInput(false)
            }
          })
          input.addEventListener('blur', () => {
            if (!handled) {
              finishInput(true)
            }
          })
        }
      })

      drawCanvas.addEventListener('mousemove', (e) => {
        if (!drawing) return
        const rect = drawCanvas.getBoundingClientRect()
        const cx = e.clientX - rect.left
        const cy = e.clientY - rect.top
        drawCtx.putImageData(snapshot, 0, 0)

        if (activeTool === 'rect') {
          drawCtx.strokeStyle = colorInput.value
          drawCtx.lineWidth = 2
          drawCtx.strokeRect(sx, sy, cx - sx, cy - sy)
        } else if (activeTool === 'arrow') {
          drawCtx.strokeStyle = colorInput.value
          drawCtx.lineWidth = 2
          drawCtx.beginPath()
          drawCtx.moveTo(sx, sy)
          drawCtx.lineTo(cx, cy)
          drawCtx.stroke()
          const angle = Math.atan2(cy - sy, cx - sx)
          const headLen = 12
          drawCtx.beginPath()
          drawCtx.moveTo(cx, cy)
          drawCtx.lineTo(cx - headLen * Math.cos(angle - Math.PI / 6), cy - headLen * Math.sin(angle - Math.PI / 6))
          drawCtx.moveTo(cx, cy)
          drawCtx.lineTo(cx - headLen * Math.cos(angle + Math.PI / 6), cy - headLen * Math.sin(angle + Math.PI / 6))
          drawCtx.stroke()
        }
      })

      drawCanvas.addEventListener('mouseup', () => {
        drawing = false
      })
    }

    img.src = dataUrl

    overlay.appendChild(container)
    document.body.appendChild(overlay)
  })
}
