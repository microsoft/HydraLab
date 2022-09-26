import {load} from 'cheerio';
import {parseDocument} from 'htmlparser2';

/**
 * JS code that is executed in the webview to determine the status+address bar height
 *
 * NOTE:
 * object destructuring the arguments resulted in this error with iOS (not with Android)
 *
 * `Duplicate parameter 'e' not allowed in function with destructuring parameters.`
 *
 * That's why the object destructuring is done in the method itself
 */
export function getWebviewStatusAddressBarHeight (obj) {
  // Calculate the status + address bar height
  // Address bar height for iOS 11+ is 50, for lower it is 44,
  // but we take 50 as a default here
  // For Chrome it is 56 for Android 6 to 10
  const {platformName, statBarHeight} = obj;
  const isAndroid = platformName.toLowerCase() === 'android';
  // iOS uses CSS sizes for elements and screenshots, Android sizes times DRP
  const dpr = isAndroid ? window.devicePixelRatio : 1;
  const screenHeight = window.screen.height;
  const viewportHeight = window.innerHeight;
  // Need to determine this later for Chrome
  const osAddressBarDefaultHeight = isAndroid ? 56 : 50;
  const addressToolBarHeight = screenHeight - viewportHeight - statBarHeight;
  // When a manual scroll has been executed for iOS and Android
  // the address bar becomes smaller
  const addressBarHeight = (addressToolBarHeight >= 0) && (addressToolBarHeight - osAddressBarDefaultHeight) < 0
    ? addressToolBarHeight : osAddressBarDefaultHeight;

  return statBarHeight + (addressBarHeight * dpr);
}

/**
 * JS code that is executed in the webview to set the needed attributes on the DOM so the source can be used for the
 * native inspector window.
 *
 * NOTE:
 * object destructuring the arguments resulted in this error with iOS (not with Android)
 *
 * `Duplicate parameter 'e' not allowed in function with destructuring parameters.`
 *
 * That's why the object destructuring is done in the method itself
 */
export function setHtmlElementAttributes (obj) {
  const {platformName, webviewStatusAddressBarHeight} = obj;
  const htmlElements = document.body.getElementsByTagName('*');
  const isAndroid = platformName.toLowerCase() === 'android';
  // iOS uses CSS sizes for elements and screenshots, Android sizes times DRP
  const dpr = isAndroid ? window.devicePixelRatio : 1;

  Array.from(htmlElements).forEach((el) => {
    const rect = el.getBoundingClientRect();

    el.setAttribute('data-appium-desktop-width', Math.round(rect.width * dpr));
    el.setAttribute('data-appium-desktop-height', Math.round(rect.height * dpr));
    el.setAttribute('data-appium-desktop-x', Math.round(rect.left * dpr));
    el.setAttribute('data-appium-desktop-y', Math.round(webviewStatusAddressBarHeight + (rect.top * dpr)));
  });
}

/**
 * Parse the source if it's HTML:
 * - head and scripts need to be removed to clean the HTML tree
 * - all custom attributes need to be transformed to normal width/height/x/y
 */
export function parseSource (source) {
  // TODO this check is a bit brittle, figure out a better way to check whether we have a web
  // source vs something else. Just checking for <html in the source doesn't work because fake
  // driver app sources can include embedded <html elements even though the overall source is not
  // html. So for now just look for fake-drivery things like <app> or <mock...> and ensure we don't
  // parse that as html
  if (!source.includes('<html') || source.includes('<app ') || source.includes('<mock')) {
    return source;
  }

  const dom = parseDocument(source);
  const $ = load(dom);

  // Remove the head and the scripts
  const head = $('head');
  head.remove();
  const scripts = $('script');
  scripts.remove();

  // Clean the source
  $('*')
    // remove all existing width height or x/y attributes
    .removeAttr('width')
    .removeAttr('height')
    .removeAttr('x')
    .removeAttr('y')
    // remove all `data-appium-desktop-` prefixes so only the width|height|x|y are there
    .each(function () {
      const $el = $(this);

      ['width', 'height', 'x', 'y'].forEach((rectAttr) => {
        if ($el.attr(`data-appium-desktop-${rectAttr}`)) {
          $el.attr(rectAttr, $el.attr(`data-appium-desktop-${rectAttr}`));

          /* remove the old attribute */
          $el.removeAttr(`data-appium-desktop-${rectAttr}`);
        }
      });
    });

  return $.xml();
}
