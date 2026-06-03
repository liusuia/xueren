const { app, BrowserWindow, Tray, Menu, nativeImage, Notification } = require('electron')
const path = require('path')

let mainWindow = null
let tray = null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1100,
    height: 720,
    minWidth: 800,
    minHeight: 600,
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    },
    titleBarStyle: 'default',
    show: false // 等 ready 后再显示避免白屏
  })

  // 加载前端（开发模式用 Vite，生产模式用 dist）
  const isDev = !app.isPackaged
  if (isDev) {
    mainWindow.loadURL('http://localhost:5173')
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow.show()
  })

  mainWindow.on('close', (event) => {
    if (tray) {
      event.preventDefault()
      mainWindow.hide()
    }
  })
}

// 系统托盘
function createTray() {
  const icon = nativeImage.createEmpty()
  tray = new Tray(icon)
  const contextMenu = Menu.buildFromTemplate([
    { label: '显示窗口', click: () => mainWindow.show() },
    { label: '退出', click: () => { tray = null; app.quit() } }
  ])
  tray.setToolTip('轻语')
  tray.setContextMenu(contextMenu)
  tray.on('double-click', () => mainWindow.show())
}

app.whenReady().then(() => {
  createWindow()
  // createTray() // 根据需求开启托盘
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow()
})
