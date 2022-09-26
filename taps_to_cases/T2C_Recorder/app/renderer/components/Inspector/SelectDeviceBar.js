import React, { Component } from 'react';
import { Select, Button, Modal, Checkbox, Space } from 'antd';
import { NEW_SESSION_TIMEOUT_SEC, POWER_SHELL_PATH } from '../../actions/Session';

export default class SelectDeviceBar extends Component {

  constructor(props) {
    super(props);
    this.state = {
      android: [],
      iOS: [],
      windows: [],
      browser: [{
        name: 'Edge',
        udid: '1',
        platform: 'browser',
      }],
      selectedDevice: {
        name: '',
        udid: props.sessionDetails.desiredCapabilities['appium:udid'] ? props.sessionDetails.desiredCapabilities['appium:udid'] : props.sessionDetails.desiredCapabilities['platformName'] === 'windows' ? '0' : '1',
        platform: props.sessionDetails.desiredCapabilities['platformName'],
      },
      isAppListVisiable: false,
      currentAppList: [],
      selectedApp: {
        packageName: null,
        appName: null,
        platform: null,
      },
      caps: [],
      selectedUrl: null,
    }

    this.getAndroidList();
    this.getIOSList();
    this.getWinInfo();
  }


  async resetSwitchingStateWithDelay() {
    const { setSwitching } = this.props;
    var sleep = (delayTime = NEW_SESSION_TIMEOUT_SEC * 1000) => {
      return new Promise(resolve => setTimeout(resolve, delayTime))
    };

    sleep(NEW_SESSION_TIMEOUT_SEC * 1000).then(() => {
      setSwitching(false);
    });
  }

  getAndroidList() {
    let process = require('child_process');
    process.exec('adb devices -l', (error, stdout, stderr) => {
      var androidDevices = stdout.trim().split('\n');
      androidDevices.shift();
      var androidList = [];
      for (var i = 0; i < androidDevices.length; i++) {
        let androidUdid = (androidDevices[i].split(' '))[0];
        let androidName = ((androidDevices[i].split('model:'))[1].split(' '))[0];
        androidList[i] = {
          udid: androidUdid,
          name: androidName,
          platform: 'android'
        }
      }

      // console.log('Get android list: ' + androidDevices);
      this.setState({ android: androidList });
    });
  }

  /**
   * Get iOS device list by tidevice
   * 
   * Device json:
   *  {
        "udid": "00008110-000634E414FB801E",
        "serial": "R3CG62YY9M",
        "name": "iPhone_ran",
        "market_name": "iPhone 13",
        "product_version": "15.5",
        "conn_type": "usb"
    }
   */
  getIOSList() {
    let process = require('child_process');

    process.exec('tidevice list --json', (error, stdout, stderr) => {
      var iOSdevices = stdout.trim();
      var iOSList = JSON.parse(iOSdevices);
      // console.log('Get iOS list: ' + iOSList[0].udid + '---' + iOSList[0].name);
      for (var i = 0; i < iOSList.length; i++) {
        iOSList[i].platform = 'ios';
      }
      this.setState({ iOS: iOSList });
    });
  }

  getWinInfo() {
    let process = require('child_process');
    process.exec('systeminfo | findstr "Host"', (error, stdout, stderr) => {
      var winInfo = stdout.trim().replace('/\s+/g', '');
      // console.log('Get win info: ' + winInfo);
      let winList = [];
      winList[0] = {
        name: winInfo,
        udid: '0',
        platform: 'windows',
      }
      this.setState({ windows: winList });
    });
  }

  componentDidMount() {
    this.timerID = setInterval(() => {
      this.getAndroidList();
      this.getIOSList();
      this.getWinInfo();
    }, 5000);
  }

  componentWillUnmount() {
    clearInterval(this.timerID)
  }

  onSelectDeviceChanged(e) {
    let udid = e;
    let current = this.state.android.find(device => device.udid == udid);
    if (!current) current = this.state.iOS.find(device => device.udid == udid);
    if (!current) current = this.state.windows.find(device => device.udid == udid);
    if (!current) current = this.state.browser.find(device => device.udid == udid);

    console.log('select device:' + current.name + "--" + current.udid + "--" + current.platform)

    let modifiedCaps;
    // Windows
    if (current.platform == 'windows') {
      modifiedCaps = [
        { type: "text", name: "platformName", value: "windows" },
        // Default: Root
        { type: "text", name: "appium:app", value: "Root" },
        { type: "text", name: "appium:deviceName", value: "WindowsPC" },
      ];
    }

    // Android
    else if (current.platform == 'android') {
      let udid = current.udid;
      modifiedCaps = [
        { type: "text", name: "platformName", value: "android" },
        { type: "text", name: "appium:udid", value: udid },
        { type: "boolean", name: "clearDeviceLogonStart", value: true },
      ];
    }

    // iOS
    else if (current.platform == 'ios') {
      modifiedCaps = [
        { type: "text", name: "platformName", value: "ios" },
        { type: "text", name: "appium:udid", value: current.udid },
        { type: "text", name: "appium:automationName", value: "XCUITest" },
        { type: "text", name: "appium:xcodeSigningId", value: "iPhone Developer" },
        { type: "text", name: "appium:deviceName", value: current.name },
        { type: "boolean", name: "appium:useXctestrunFile", value: false },
        { type: "boolean", name: "appium:skipLogCapture", value: true },
        { type: "boolean", name: "appium:usePrebuiltWDA", value: false },
        { type: "text", name: "appium:webDriverAgentUrl", value: "http://127.0.0.1:8100" },
      ];
    }

    // Browser
    else if (current.platform == 'browser') {
      modifiedCaps = [
        { type: "text", name: "platformName", value: "browser" },
        { type: "text", name: "appium:app", value: "Microsoft.MicrosoftEdge_8wekyb3d8bbwe!App" },
        { type: "text", name: "appium:deviceName", value: "WindowsPC" },
      ];
    }

    this.setState({
      selectedDevice: current,
      caps: modifiedCaps,
      selectedApp: {
        packageName: null,
        appName: null,
        platform: null,
      },
      selectedUrl: null,
    });
  }

  /**
   * Show select app dialog
   */
  showAppList() {
    this.setState({ currentAppList: [] });
    this.setState({ isAppListVisiable: true });

    //Make sure the caps are not empty
    if (this.state.caps.length == 0) {
      this.onSelectDeviceChanged(this.state.selectedDevice.udid);
    }

    let process = require('child_process');
    let device = this.state.selectedDevice;
    let platform = device.platform;

    //Android
    if (platform == 'android') {
      process.exec('adb -s ' + device.udid + ' shell pm list packages', (error, stdout, stderr) => {
        let androidAppStr = stdout.trim().split('\n');
        let androidAppList = [];
        for (var j = 0; j < androidAppStr.length; j++) {
          androidAppList[j] = {
            packageName: ((androidAppStr[j].split(':'))[1]).trim(),
            platform: 'android',
            udid: device.udid,
          }
        }
        this.setState({ currentAppList: androidAppList });
      });
    }

    //iOS
    else if (platform == 'ios') {
      process.exec('tidevice -u ' + device.udid + ' applist', (error, stdout, stderr) => {
        let iOSAppStr = stdout.trim().split('\n');
        let iOSAppList = [];
        for (var j = 0; j < iOSAppStr.length; j++) {
          let index = iOSAppStr[j].indexOf(' ');
          iOSAppList[j] = {
            packageName: iOSAppStr[j].substring(0, index).trim(),
            appName: iOSAppStr[j].substring(index + 1).trim(),
            platform: 'ios',
            udid: device.udid,
          }
        }
        this.setState({ currentAppList: iOSAppList });
      });
    }

    //Windows
    else if (platform == 'windows') {
      process.exec(POWER_SHELL_PATH + ' "get-appxpackage | select Name, PackageFamilyname"', (error, stdout, stderr) => {
        let winAppStr = stdout.trim().split('\n');
        let winAppList = [];
        for (var k = 2; k < winAppStr.length; k++) {
          winAppStr[k].replace('/\s+/g', ' ')
          let index = winAppStr[k].indexOf(' ');
          winAppList[k - 2] = {
            packageName: winAppStr[k].substring(index + 1).trim(),
            appName: winAppStr[k].substring(0, index).trim(),
            platform: 'windows',
            udid: device.udid,
          }
        }
        this.setState({ currentAppList: winAppList });
      });
    }
  }

  onAppSelect(e) {
    let app = this.state.currentAppList.find(a => a.packageName == e);
    this.setState({
      isAppListVisiable: false,
      selectedApp: app,
    });

    if (this.state.caps[0].value == 'windows') {
      this.state.caps[1].value = app.packageName + '!App';
    }
  }

  setUrl() {
    //Make sure the caps are not empty
    this.onSelectDeviceChanged(this.state.selectedDevice.udid);
    const Dialogs = require('dialogs')
    const dialogs = Dialogs()
    dialogs.prompt('Set Url', this.state.selectedUrl, result => {
      console.log('Selected Url', result)
      this.setState({ selectedUrl: result });
    })
  }

  switchDevice() {
    const { quitSessionAndRestart } = this.props;
    quitSessionAndRestart(this.state.caps, this.state.selectedApp.packageName ? this.state.selectedApp.packageName.trim() : null, this.state.selectedUrl);
    this.resetSwitchingStateWithDelay();
  }

  render() {
    const { sessionDetails, applyClientMethod, setUseExistingWindowForSession } = this.props;
    const isBrowser = this.state.selectedDevice.platform === 'browser';

    return <>
      <div>
        <Space>
          <Select style={{ width: 200 }} defaultValue={this.state.selectedDevice.udid ? this.state.selectedDevice.udid : ''}
            onChange={(e) => this.onSelectDeviceChanged(e)}>
            {this.state.android.map((device) => (
              <Select.Option key={device.udid}>{device.name + ', Android'}</Select.Option>
            ))}
            {this.state.iOS.map((device) => (
              <Select.Option key={device.udid}>{device.name + ', iOS'}</Select.Option>
            ))}
            {this.state.windows.map((device) => (
              <Select.Option key={device.udid}>{device.name + ', Windows'}</Select.Option>
            ))}
            {this.state.browser.map((device) => (
              <Select.Option key={device.udid}>{device.name + ', Browser'}</Select.Option>
            ))}
          </Select>
          {isBrowser && <Button style={{ maxWidth: 300, overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }} onClick={() => this.setUrl()}>{this.state.selectedUrl ? this.state.selectedUrl : 'Set Url'}</Button>}
          {!isBrowser && <Button onClick={() => this.showAppList()}>
            {this.state.selectedApp.appName ? this.state.selectedApp.appName : (this.state.selectedApp.packageName ? this.state.selectedApp.packageName : 'Select App')}
          </Button>}
          <Button onClick={() => this.switchDevice()}>{'Switch'}</Button>
          <Checkbox disabled={!(this.state.selectedDevice.platform === 'windows')} defaultChecked={sessionDetails.isUseExistingWindow} onChange={(e) => { setUseExistingWindowForSession(e.target.checked) }}>Use Existing App Windows</Checkbox>
        </Space>
      </div>

      <Modal title='App List' visible={this.state.isAppListVisiable} onCancel={() => this.setState({ isAppListVisiable: false })} onOk={() => this.setState({ isAppListVisiable: false })}>
        <Select style={{ width: 400 }}
          showSearch
          placeholder="Select a app"
          optionFilterProp="children"
          onSelect={(e) => this.onAppSelect(e)}
          filterOption={(input, option) => option.children.toLowerCase().includes(input.toLowerCase())}>
          {this.state.currentAppList.map((app, index) => (
            <Select.Option key={index} value={app.packageName}>{app.appName ? app.appName : app.packageName}</Select.Option>
          ))}
        </Select>
      </Modal>
    </>
  }
}