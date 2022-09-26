import i18n from './configs/i18next.config';
import { app, BrowserWindow, Menu, webContents, ipcMain, dialog } from 'electron';
import { initializeIpc_newWindow } from './main/appium';
import { installExtensions } from '../gui-common/debug';
import { setupMainWindow } from '../gui-common/windows';
import { rebuildMenus } from './main/menus';
import settings from './shared/settings';
import { APPIUM_SESSION_EXTENSION, getAppiumSessionFilePath } from './main/helpers';

let mainWindow = null;
const isDev = process.env.NODE_ENV === 'development';

if (isDev) {
  require('electron-debug')(); // eslint-disable-line global-require
}

let openFilePath = getAppiumSessionFilePath(process.argv, app.isPackaged, isDev);

app.on('open-file', (event, filePath) => {
  openFilePath = filePath;
});

app.on('window-all-closed', () => {
  app.quit();
});

app.on('ready', async () => {
  await installExtensions();

  mainWindow = new BrowserWindow({
    show: false,
    width: 1280,
    height: 800,
    minWidth: 1000,
    minHeight: 800,
    titleBarStyle: 'hiddenInset',
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      enableRemoteModule: true,
      additionalArguments: openFilePath ? [`filename=${openFilePath}`] : [],
    },
  });

  ipcMain.on('save-file-as', async () => {
    const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
      title: 'Save Appium File',
      filters: [
        {name: 'Appium Session Files', extensions: [APPIUM_SESSION_EXTENSION]},
      ]
    });
    if (!canceled) {
      mainWindow.webContents.send('save-file', filePath);
    }
  });

  const splashWindow = new BrowserWindow({
    width: 300,
    height: 300,
    minWidth: 300,
    minHeight: 300,
    frame: false,
  });

  setupMainWindow({
    mainWindow,
    splashWindow,
    mainUrl: `file://${__dirname}/index.html`,
    splashUrl: `file://${__dirname}/splash.html`,
    isDev,
    Menu,
    i18n,
    rebuildMenus,
    settings,
    webContents,
    shouldShowFileMenu: true,
  });

  initializeIpc_newWindow(mainWindow);
});
