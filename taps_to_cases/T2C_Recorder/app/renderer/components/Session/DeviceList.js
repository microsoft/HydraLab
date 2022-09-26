import React, { Component } from 'react';
import { Modal, Form, Row, Col, Select, Radio, Space, Button, Checkbox } from 'antd';
import FormattedCaps from './FormattedCaps';
import SessionStyles from './Session.css';
import { ROW } from '../../../../gui-common/components/AntdTypes';
import { POWER_SHELL_PATH } from '../../actions/Session';


export default class DeviceList extends Component {

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
        udid: '',
        platform: 'null',
      },
      isAppListVisiable: false,
      currentAppList: [],
      selectedApp: {
        packageName: '',
        appName: '',
        platform: 'null',
      },
      selectedUrl: 'https://',
    }
    this.getAndroidList();
    this.getIOSList();
    this.getWinInfo();
    this.onSelectedDeviceChange = this.onSelectedDeviceChange.bind(this);
  }

  onAppSelect(e) {
    let app = this.state.currentAppList.find(a => a.packageName == e);
    this.setState({
      isAppListVisiable: false,
      selectedApp: app,
    });

    let { setCapabilityParam, caps, setApp } = this.props;
    if (caps[0].value == 'windows') {
      setCapabilityParam(1, 'value', app.packageName + '!App');
    }

    setApp(app.packageName.trim());
  }

  showAppList(platform, device) {
    console.log('device=' + platform);
    this.setState({ currentAppList: [] });
    this.setState({ isAppListVisiable: true });

    let process = require('child_process');
    var iconv = require('iconv-lite');
    var encoding = 'cp437';
    var binaryEncoding = 'binary';

    //Android
    if (platform == 'android') {
      process.exec('adb -s ' + device.udid + ' shell pm list packages', { encoding: 'utf-8' }, (error, stdout, stderr) => {

        let androidAppStr = stdout.trim().split('\n');
        let androidAppList = [];
        for (var j = 0; j < androidAppStr.length; j++) {
          androidAppList[j] = {
            packageName: (androidAppStr[j].split(':'))[1].trim(),
            platform: 'android',
            udid: device.udid.trim(),
          }
        }
        this.setState({ currentAppList: androidAppList });
      });
    }

    //iOS
    else if (platform == 'iOS') {
      process.exec('tidevice -u ' + device.udid + ' applist', { encoding: 'utf-8' }, (error, stdout, stderr) => {
        let iOSAppStr = stdout.trim().split('\n');
        let iOSAppList = [];
        for (var j = 0; j < iOSAppStr.length; j++) {
          let index = iOSAppStr[j].indexOf(' ');
          iOSAppList[j] = {
            packageName: iOSAppStr[j].substring(0, index).trim(),
            appName: iOSAppStr[j].substring(index + 1).trim(),
            platform: 'iOS',
            udid: device.udid.trim(),
          }
        }
        this.setState({ currentAppList: iOSAppList });
      });
    }

    //Windows
    else if (platform == 'windows') {
      process.exec(POWER_SHELL_PATH + ' "get-appxpackage | select Name, PackageFamilyname"', { encoding: 'utf-8' }, (error, stdout, stderr) => {
        let winAppStr = stdout.trim().split('\n');
        let winAppList = [];
        for (var k = 2; k < winAppStr.length; k++) {
          winAppStr[k].replace('/\s+/g', ' ')
          let index = winAppStr[k].indexOf(' ');
          winAppList[k - 2] = {
            packageName: winAppStr[k].substring(index + 1).trim(),
            appName: winAppStr[k].substring(0, index).trim(),
            platform: 'windows',
            udid: device.udid.trim(),
          }
        }
        this.setState({ currentAppList: winAppList });
      });
    }


  }

  /**
   * Set Appium capabilities
   */
  onSelectedDeviceChange(e) {
    let { setCapabilityParam, caps, addCapability, setCaps, isCapsDirty, setApp } = this.props;
    let udid = e.target.value.split(' ')[0];
    let platform = e.target.value.split(' ')[1];
    let name = e.target.value.replace(udid + ' ' + platform + ' ', '');
    console.log(e.target.value);
    let current = {
      udid: udid,
      name: name,
      platform: platform,
    };

    // Windows
    if (current.platform == 'windows') {
      let winCaps = [
        { type: "text", name: "platformName", value: "windows" },
        // Default: Root
        { type: "text", name: "appium:app", value: "Root" },
        { type: "text", name: "appium:deviceName", value: "WindowsPC" },
      ];
      setCaps(winCaps)
    }

    // Android
    else if (current.platform == 'android') {
      let udid = current.udid;
      let androidCaps = [
        { type: "text", name: "platformName", value: "android" },
        { type: "text", name: "appium:udid", value: udid },
        { type: "boolean", name: "clearDeviceLogonStart", value: true },
      ];
      setCaps(androidCaps);
    }

    // iOS
    else if (current.platform == 'iOS') {
      let iOSCaps = [
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
      setCaps(iOSCaps);
    }

    // Browser
    else if (current.platform == 'browser') {
      let browserCaps = [
        { type: "text", name: "platformName", value: "browser" },
        { type: "text", name: "appium:app", value: "Microsoft.MicrosoftEdge_8wekyb3d8bbwe!App" },
        { type: "text", name: "appium:deviceName", value: "WindowsPC" },
      ];
      setCaps(browserCaps);
    }

    console.log(caps);
    this.setState({
      selectedDevice: current,
      selectedApp: {
        packageName: '',
        appName: '',
        platform: 'null',
      }
    });

    console.log('selectedDevice:' + current.udid + ' ' + current.platform + ' ' + current.name);
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

      this.setState({ android: androidList });
    });
  }

  /**
   * Example:
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
      if (iOSdevices !== "") {
        var iOSList = JSON.parse(iOSdevices);
        // console.log('Get iOS list: ' + iOSList[0].udid + '---' + iOSList[0].name);
        for (var i = 0; i < iOSList.length; i++) {
          iOSList[i].platform = 'iOS';
        }
        this.setState({ iOS: iOSList });
      }
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

  setUrl() {
    let { setUrl } = this.props;
    const Dialogs = require('dialogs')
    const dialogs = Dialogs()
    dialogs.prompt('Set Url', this.state.selectedUrl, result => {
      console.log('Selected Url', result)
      this.setState({ selectedUrl: result });
      setUrl(result)
    })
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

  render() {
    const { setCapabilityParam, caps, addCapability, removeCapability, saveSession, hideSaveAsModal,
      saveAsText, showSaveAsModal, setSaveAsText, isEditingDesiredCaps, t,
      setAddVendorPrefixes, addVendorPrefixes, server, serverType, setCaps, isUseExistingWindow, setUseExistingWindow } = this.props;

    const onSaveAsOk = () => saveSession(server, serverType, caps, { name: saveAsText });

    return <>
      <Row type={ROW.FLEX} align="top" justify="start" className={SessionStyles.capsFormRow}>
        <Col order={1} span={12} className={`${SessionStyles.capsFormCol} ${isEditingDesiredCaps ? SessionStyles.capsFormDisabled : ''}`}>
          <Form
            className={SessionStyles.newSessionForm}
          >
            <Row>
              <h2>Android</h2>
            </Row>
            <Row>
              <Radio.Group onChange={this.onSelectedDeviceChange} value={this.state.selectedDevice.udid + ' ' + this.state.selectedDevice.platform + ' ' + this.state.selectedDevice.name}>
                <Space direction="vertical">
                  {this.state.android.map((device, index) =>
                    <Radio key={device.udid} value={device.udid + ' ' + device.platform + ' ' + device.name}>
                      {device.name + '-----' + device.udid + '    '}
                      <Button onClick={() => this.showAppList('android', device)}>{this.state.selectedApp.udid == device.udid ? this.state.selectedApp.packageName : 'Select app'}</Button>
                    </Radio>
                  )}
                </Space>
              </Radio.Group>
            </Row>
            <Row>
              <h2>iOS</h2>
            </Row>
            <Row>
              <Radio.Group onChange={this.onSelectedDeviceChange} value={this.state.selectedDevice.udid + ' ' + this.state.selectedDevice.platform + ' ' + this.state.selectedDevice.name}>
                <Space direction="vertical">
                  {this.state.iOS.map((device, index) =>
                    <Radio key={device.udid} value={device.udid + ' ' + device.platform + ' ' + device.name}>
                      {device.name + '-----' + device.market_name + '-----' + device.udid + '    '}
                      <Button onClick={() => this.showAppList('iOS', device)}>{this.state.selectedApp.udid == device.udid ? this.state.selectedApp.appName : 'Select app'}</Button>
                    </Radio>
                  )}
                </Space>
              </Radio.Group>
            </Row>
            <Row>
              <h2>Windows</h2>
            </Row>
            <Row>
              <Radio.Group onChange={this.onSelectedDeviceChange} value={this.state.selectedDevice.udid + ' ' + this.state.selectedDevice.platform + ' ' + this.state.selectedDevice.name}>
                <Space direction="vertical">
                  {this.state.windows.map((device, index) =>
                    <Radio key={device.udid} value={device.udid + ' ' + device.platform + ' ' + device.name}>
                      {device.name + '    '}
                      <Space>
                        <Button onClick={() => this.showAppList('windows', device)}>{this.state.selectedApp.udid == device.udid ? this.state.selectedApp.appName : 'Select app'}</Button>
                        <Checkbox disabled={!(this.state.selectedDevice.platform === 'windows')} checked={isUseExistingWindow} onChange={(e) => { setUseExistingWindow(e.target.checked) }}>Use Existing App Windows</Checkbox>
                      </Space>
                    </Radio>
                  )}
                </Space>
              </Radio.Group>
            </Row>
            <Row>
              <h2>Browser</h2>
            </Row>
            <Row>
              <Radio.Group onChange={this.onSelectedDeviceChange} value={this.state.selectedDevice.udid + ' ' + this.state.selectedDevice.platform + ' ' + this.state.selectedDevice.name}>
                <Space direction="vertical">
                  {this.state.browser.map((device, index) =>
                    <Radio key={device.udid} value={device.udid + ' ' + device.platform + ' ' + device.name}>
                      {device.name + '    '}
                      <Button style={{ maxWidth: 300, overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }} onClick={() => this.setUrl()}>{this.state.selectedUrl ? this.state.selectedUrl : 'Set Url'}</Button>
                    </Radio>
                  )}
                </Space>
              </Radio.Group>
            </Row>
          </Form>
        </Col>
        <Col order={2} span={12} className={SessionStyles.capsFormattedCol}>
          <FormattedCaps {...this.props} />
        </Col>
      </Row>
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
    </>;
  }
}
