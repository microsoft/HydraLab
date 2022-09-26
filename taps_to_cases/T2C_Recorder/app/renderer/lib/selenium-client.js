import _ from 'lodash';
import Bluebird from 'bluebird';
import { getWebviewStatusAddressBarHeight, parseSource, setHtmlElementAttributes } from './webview-helpers';
import { SCREENSHOT_INTERACTION_MODE, APP_MODE } from '../components/Inspector/shared';

export const NATIVE_APP = 'NATIVE_APP';
let _instance = null;

export default class SeleniumClient {
  constructor(driver) {
    this.driver = driver;
    this.elementCache = {};
    this.elVarCount = 0;
    this.elArrayVarCount = 0;
  }

  async run(params) {
    const {
      methodName, // Optional. Name of method being provided
      strategy, // Optional. Element locator strategy
      selector, // Optional. Element fetch selector
      fetchArray = false, // Optional. Are we fetching an array of elements or just one?
      elementId, // Optional. Element being operated on
      args = [], // Optional. Arguments passed to method
      skipRefresh = false, // Optional. Do we want the updated source and screenshot?
      skipScreenshot = false, // Optional. Do we want to skip getting screenshot alone?
      appMode = APP_MODE.NATIVE, // Optional. Whether we're in a native or hybrid mode
    } = params;

    console.log("SeleniumClient run params")
    console.log(params)

    if (methodName === 'quit') {
      try {
        await this.driver.quit();
      } catch (ign) {
      }

      _instance = null;

      // when we've quit the session, there's no source/screenshot to send
      // back
      return {
        source: null,
        screenshot: null,
        windowSize: null,
        result: null
      };
    }

    let res = {};
    if (methodName) {
      if (elementId) {
        console.log(`Handling client method request with method '${methodName}', args ${JSON.stringify(args)} and elementId ${elementId}`); // eslint-disable-line no-console
        res = await this.executeMethod({ elementId, methodName, args, skipRefresh, skipScreenshot, appMode });
      } else {
        console.log(`Handling client method request with method '${methodName}' and args ${JSON.stringify(args)}`); // eslint-disable-line no-console
        res = await this.executeMethod({ methodName, args, skipRefresh, skipScreenshot, appMode });
      }
    } else if (strategy && selector) {
      if (fetchArray) {
        console.log(`Fetching elements with selector '${selector}' and strategy ${strategy}`); // eslint-disable-line no-console
        res = await this.fetchElements({ strategy, selector });
      } else {
        console.log(`Fetching an element with selector '${selector}' and strategy ${strategy}`); // eslint-disable-line no-console
        res = await this.fetchElement({ strategy, selector });
      }
    }

    return res;
  }

  async executeMethod({ elementId, methodName, args, skipRefresh, skipScreenshot, appMode }) {
    let cachedEl;
    let res = {};
    if (!_.isArray(args) && !_.isUndefined(args)) {
      args = [args];
    }

    if (elementId) {
      // Give the cached element a variable name (el1, el2, el3,...) the first time it's used
      cachedEl = this.elementCache[elementId];

      if (!cachedEl.variableName) {
        // now that we are actually going to use this element, let's assign it a variable name
        // if it doesn't already have one
        this.elVarCount += 1;
        cachedEl.variableName = `el${this.elVarCount}`;
      }

      // and then execute whatever method we requested on the actual element
      res = await cachedEl.el[methodName].apply(cachedEl.el, args);
    } else {
      // Specially handle the tap and swipe method
      if (methodName === SCREENSHOT_INTERACTION_MODE.TAP) {
        const [x, y] = args;
        res = await this.driver.performActions([{
          type: 'pointer',
          id: 'finger1',
          parameters: { pointerType: 'touch' },
          actions: [
            { type: 'pointerMove', duration: 0, x, y },
            { type: 'pointerDown', button: 0 },
            { type: 'pause', duration: 100 },
            { type: 'pointerUp', button: 0 }
          ]
        }]);
      } else if (methodName === SCREENSHOT_INTERACTION_MODE.SWIPE) {
        const [startX, startY, endX, endY] = args;
        res = await this.driver.performActions([{
          type: 'pointer',
          id: 'finger1',
          parameters: { pointerType: 'touch' },
          actions: [
            { type: 'pointerMove', duration: 0, x: startX, y: startY },
            { type: 'pointerDown', button: 0 },
            { type: 'pointerMove', duration: 750, origin: 'viewport', x: endX, y: endY },
            { type: 'pointerUp', button: 0 }
          ]
        }]);
      } else if (methodName !== 'getPageSource' && methodName !== 'takeScreenshot') {
        res = await this.driver[methodName].apply(this.driver, args);
      }
    }

    // Give the source/screenshot time to change before taking the screenshot
    await Bluebird.delay(500);

    let contextUpdate = {}, sourceUpdate = {}, screenshotUpdate = {}, windowSizeUpdate = {};
    if (!skipRefresh) {
      if (!skipScreenshot) {
        console.log("getScreenshotUpdate")
        screenshotUpdate = await this.getScreenshotUpdate();
        console.log(screenshotUpdate)
      }
      console.log("getWindowUpdate")
      windowSizeUpdate = await this.getWindowUpdate();
      console.log(windowSizeUpdate)
      // only do context updates if user has selected web/hybrid mode (takes forever)
      console.log("getSourceUpdate")
      sourceUpdate = await this.getSourceUpdate();
      console.log(sourceUpdate)
    }
    return {
      ...cachedEl,
      ...contextUpdate,
      ...sourceUpdate,
      ...screenshotUpdate,
      ...windowSizeUpdate,
      commandRes: res,
    };
  }

  async fetchElements({ strategy, selector }) {
    const els = await this.driver.findElements(strategy, selector);

    this.elArrayVarCount += 1;
    const variableName = `els${this.elArrayVarCount}`;
    const variableType = 'array';

    const elements = {};
    // Cache the elements that we find
    const elementList = els.map((el, index) => {
      const res = {
        el,
        variableName,
        variableIndex: index,
        variableType: 'string',
        id: el.elementId,
        strategy,
        selector,
      };
      elements[el.elementId] = res;
      return res;
    });

    this.elementCache = { ...this.elementCache, ...elements };

    return { variableName, variableType, strategy, selector, elements: elementList };
  }

  async fetchElement({ strategy, selector }) {
    const start = Date.now();
    let element = null;
    try {
      element = await this.driver.findElement(strategy, selector);
    } catch (err) {
      return {};
    }

    const executionTime = Date.now() - start;

    const id = element.elementId;

    // Cache this ID along with its variable name, variable type and strategy/selector
    const elementData = {
      el: element,
      variableType: 'string',
      strategy,
      selector,
      id,
    };

    this.elementCache[id] = elementData;

    return {
      ...elementData,
      executionTime,
    };
  }

  async getWindowUpdate() {
    let windowSize, windowSizeError;
    try {
      // The call doesn't need to be made for Android for two reasons
      // - when appMode is hybrid Chrome driver doesn't know this command
      // - the data is already on the driver
      windowSize = await this.driver.manage().window().getRect();
    } catch (e) {
      windowSizeError = e;
    }

    return { windowSize, windowSizeError };
  }

  async getSourceUpdate() {
    try {
      const source = parseSource(await this.driver.getPageSource());
      return { source };
    } catch (err) {
      return { sourceError: err };
    }
  }

  async getScreenshotUpdate() {
    try {
      const screenshot = await this.driver.takeScreenshot();
      return { screenshot };
    } catch (err) {
      return { screenshotError: err };
    }
  }

}

SeleniumClient.instance = (driver) => {
  if (_instance === null) {
    console.log('get old driver');
    _instance = new SeleniumClient(driver);
  } else if (driver != _instance.driver) {
    console.log('new driver');
    _instance = new SeleniumClient(driver);
  }
  return _instance;
};
