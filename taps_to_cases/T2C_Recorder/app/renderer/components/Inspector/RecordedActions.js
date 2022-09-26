import { clipboard } from '../../polyfills';
import _, { add } from 'lodash';
import React, { Component } from 'react';
import { Card, Select, Tooltip, Button, Row, Col, Tag, Input, Mentions, Form, notification, Switch } from 'antd';
import InspectorStyles from './Inspector.css';
import frameworks from '../../lib/client-frameworks';
import { highlight } from 'highlight.js';
import { withTranslation } from '../../util';
import { ExportOutlined, CopyOutlined, DeleteOutlined, CloseOutlined, CodeOutlined, MenuOutlined, CaretLeftOutlined, RocketOutlined } from '@ant-design/icons';
import { BUTTON } from '../../../../gui-common/components/AntdTypes';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';
import { FSM } from '../../lib/touch_state_machine';
import { parseCoordinates, updateOverlapsAngles, isElementContainer, isElementOverElement, isAncestor, RENDER_CENTROID_AS } from './shared';
const { CENTROID, OVERLAP, EXPAND } = RENDER_CENTROID_AS;
const Option = Select.Option;
const ButtonGroup = Button.Group;

const reorder = (list, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);
  return result;
};
const grid = 2;
const getItemStyle = (isDragging, draggableStyle) => ({
  userSelect: 'none',
  padding: grid,
  backgroundColor: isDragging ? 'lightgrey' : 'white',

  ...draggableStyle,
});

const getListStyle = (isDraggingOver) => ({
  background: isDraggingOver ? 'lightblue' : 'lightgrey',
});
const compare = (e1, e2) => {
  if ((e1.properties.width === null || e1.properties.height === null) && (e2.properties.width === null || e2.properties.height === null)) {
    return 0;
  }

  if ((e1.properties.width === 0 || e1.properties.height === 0) && (e2.properties.width === 0 || e2.properties.height === 0)) {
    return 0;
  }

  if (e1.properties.width === 0 || e1.properties.height === 0 || e1.properties.width === null || e1.properties.height === null) {
    return 1;
  }

  if (e2.properties.width === 0 || e2.properties.height === 0 || e2.properties.width === null || e2.properties.height === null) {
    return -1;
  }

  return (e2.properties.width * e2.properties.height) - (e1.properties.width * e1.properties.height);
}
const getCaseAction = (action) => {
  switch (action.actionType) {
    case 'click':
      action.arguments = {};
      break;
    case 'longClick':
      action.arguments = { duration: action.arguments[0] };
      break;
    case 'clear':
      action.arguments = {};
      break;
    case 'input':
      action.arguments = action.arguments.length > 1 ?
        { content: action.arguments[0], id: action.arguments[1] }
        :
        { content: action.arguments[0] };
      break;
    case 'activateApp':
      action.arguments = { appPackageName: action.arguments[0] };
      break;
    case 'terminateApp':
      action.arguments = { appPackageName: action.arguments[0] };
      break;
    case 'resetApp':
      action.arguments = { appPackageName: action.arguments[0] };
      break;
    case 'pressKeyCode':
      action.arguments = { keyCode: action.arguments[0] };
      break;
    case 'move':
      action.arguments = {
        xVector: action.arguments[0],
        yVector: action.arguments[1],
        duration: action.arguments[2]
      }
      break;
    case 'swipe':
      action.arguments = { direction: action.arguments[0] };
      break;
    case 'getInfo':
      action.arguments = { id: action.arguments[0], attribute: action.arguments[1] };
      break;
    case 'assert':
      action.arguments = { attribute: action.arguments[0], expectedValue: action.arguments[1] };
      break;
    case 'sleep':
      action.arguments = { duration: action.arguments[0] };
      break;
    case 'dragAndDrop':
      action.arguments = {
        xVector: action.arguments[0],
        yVector: action.arguments[1],
        duration: action.arguments[2]
      };
      break;
    default:
      break;
  }
  return action;
}
class RecordedActions extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isGetingEvent: false,
      isConnectDisable: false,
      canSave: true,
    }
    this.child = require('child_process');
    this.touchState = new FSM();
    this.onDragEnd = this.onDragEnd.bind(this);
    this.listEndRef = React.createRef();
  }

  scrollToBottom() {
    if (this.listEndRef.current != null) {
      this.listEndRef.current.scrollIntoView({ behavior: "smooth", alignToTop: false, block: 'nearest' });
    }
  }

  componentWillUnmount() {
    if (this.child.pid != undefined) {
      let kill = require('tree-kill');
      kill(this.child.pid);
    }
  }
  componentDidUpdate() {
    this.scrollToBottom();
  }
  async getEventFromPhone() {
    const { isRecording } = this.props;

    if (this.state.isGetingEvent == false) {
      this.setState({ isGetingEvent: true });
      let { spawn } = require('child_process');
      //select device connected
      let deviceUid = this.SelectConnectedDevice();
      console.log(deviceUid);

      let DeviceMassage = this.getDeviceMassage(deviceUid);
      let resolution = DeviceMassage.resolution;
      let coordinate = DeviceMassage.coordinate;
      console.log(resolution, coordinate);
      //TODO: Alert: if Massage cant get pop an error alert

      this.touchState.onReady(resolution, coordinate);
      this.child = spawn('adb -s ' + `${deviceUid}` + ' shell getevent', { shell: true });
      let isMultiTouch = false;
      try {
        this.child.stdout.on('data', (data) => {
          let lines = `${data}`.split('\n');
          for (let i = 0; i < lines.length; i++) {
            let line = lines[i];
            let sections = lines[i].split(' ');
            let eventType = sections[2];
            if (eventType == '014a') {
              if (this.touchState.can('keyDown')) {
                this.touchState.keyDown();
              } else if (this.touchState.can('keyUp')) {
                let action = this.touchState.keyUp();
                if (isMultiTouch) {
                  isMultiTouch = false;
                  this.openNotificationWithIcon('warning', 'Multi-touch detected', 'Mutitouch is not surpported.');
                  this.props.applyClientMethod({ methodName: 'getPageSource', ignoreResult: true });
                  continue;
                }
                this.tapToTestCase(action);
              }
            } else if (eventType == '0035' || eventType == '0036') {
              if (this.touchState.can('touchMove')) {
                this.touchState.touchMove();
                this.touchState.onTouchMove(line.toString());
              } else if (this.touchState.is('move')) {
                this.touchState.onTouchMove(line.toString());
              }
            } else if (eventType == '002f') {
              isMultiTouch = true;
            }
          }
        });
      } catch (e) {
        console.error(e.message);
      }

      this.child.stderr.on('data', (data) => {
        console.error(`Error: ${data}`);
      });

      this.child.on('close', (code) => {
        console.log(`child process exited with code ${code}`);
      });
    } else {
      this.setState({ isGetingEvent: false });
      let kill = require('tree-kill');
      kill(this.child.pid);
      this.touchState = new FSM();
    }

  }

  SelectConnectedDevice() {
    const { sessionDetails } = this.props;
    const { desiredCapabilities } = sessionDetails;
    let localDevices = new Set();
    const { execSync } = require('child_process');
    let result = execSync('adb devices').toString();
    try {
      let lines = `${result}`.split('\n');
      for (let line of lines) {
        line = line.trim();
        let sections = `${line}`.split('	');
        if (sections.length === 2) {
          let device = sections[0];
          let deviceState = sections[1];
          console.log('device:' + device + ' state:' + deviceState);
          if (deviceState == 'device') {
            localDevices.add(device);
          }
        }
      }
      if (localDevices.length === 0) {
        throw Error('No device connected locally');
      }
    } catch (e) {
      this.openNotificationWithIcon('error', 'No Local Device', e.message);
    }

    if (desiredCapabilities.hasOwnProperty('appium:udid') && localDevices.has(desiredCapabilities['appium:udid'])) {
      return desiredCapabilities['appium:udid'];
    } else {
      return null;
    }
  }

  getDeviceMassage(deviceUid) {
    let { execSync, spawnSync } = require('child_process');
    let resolution = { x: 0, y: 0 };
    let coordinate = {
      xMin: 0,
      xMax: 0,
      yMin: 0,
      yMax: 0,
    };
    //get resolution
    let result = execSync('adb -s ' + `${deviceUid}` + ' shell wm size').toString();
    try {
      let sections = `${result}`.split(' ');
      let resolutionXAndY = sections[2].split('x');
      resolution.x = Number(resolutionXAndY[0]);
      resolution.y = Number(resolutionXAndY[1]);
      if (isNaN(resolution.x) || isNaN(resolution.y)) {
        throw Error('Failed to get resolution resolution');
      }
    } catch (e) {
      resolution = null;
      console.error(e.message);
    }
    //get the maxValue and minValue of horizontal and vertical coordinates
    result = spawnSync('adb -s ' + `${deviceUid}` + ' shell getevent -p', { shell: true });
    try {
      let lines = `${result.stdout}`.split('\n');
      for (let i in lines) {
        let newLine = lines[i].trim();
        if (newLine.startsWith('0035')) {
          coordinate.xMin = this.getMinAndMax(newLine)[0];
          coordinate.xMax = this.getMinAndMax(newLine)[1];
        } else if (newLine.startsWith('0036')) {
          coordinate.yMin = this.getMinAndMax(newLine)[0];
          coordinate.yMax = this.getMinAndMax(newLine)[1];
        }
      }
      if (isNaN(coordinate.xMin) || isNaN(coordinate.xMax) || isNaN(coordinate.yMin) || isNaN(coordinate.yMax)) {
        throw Error('Failed to get coordinate');
      }
    } catch (e) {
      coordinate = null;
      console.error(e.message);
    }
    const DeviceMassage = { resolution: resolution, coordinate: coordinate };
    return DeviceMassage;
  }

  openNotificationWithIcon(type, message, desc) {
    notification[type]({
      message: message,
      description: desc,
    });
  };
  async tapToTestCase(action) {
    const { source, testCases, sessionDetails, updateDriverList, updateRecordedTestCases } = this.props;
    const { desiredCapabilities } = sessionDetails;
    let testCase = {};
    let elements = this.getElements(source);
    let orderedElements = elements.sort(compare);
    //convert x,y to element
    let elementInfo;
    if (action.actionType === 'move' || action.actionType === 'dragAndDrop') {
      elementInfo = this.convertCoordinateToElement(action.fromX, action.fromY, orderedElements);
    } else if (action.actionType === 'swipe') {
      elementInfo = {};
    } else {
      elementInfo = this.convertCoordinateToElement(action.x, action.y, orderedElements);
    }
    testCase.elementInfo = elementInfo;

    if (Object.keys(testCase.elementInfo).length !== 0) {
      testCase.element = { strategy: 'xpath', locator: testCase.elementInfo.xpath };
    } else {
      testCase.element = null;
    }
    //get current driver
    testCase.driverID = desiredCapabilities['appium:udid'];
    testCase.device = desiredCapabilities.platformName;
    updateDriverList(testCase.driverID);
    //get action
    testCase.action = { actionType: action.actionType, arguments: action.args || [] };
    //set isOption default value false
    testCase.isOption = false;
    console.log('testCase');
    console.log(testCase);
    //update testCases list
    let newTestCaseList = testCases;
    newTestCaseList.push(testCase);
    updateRecordedTestCases(newTestCaseList).then(() => { });
    //upadate PageSource
    await this.props.applyClientMethod({ methodName: 'getPageSource', ignoreResult: true });
  }

  convertCoordinateToElement(x, y, elements) {
    console.log('x:' + x + ' y:' + y);
    for (let i = elements.length - 1; i >= 0; i--) {
      if (elements[i]['type'] === 'expand') {
        continue;
      }
      let eleProperties = elements[i]['properties'];
      if (x > eleProperties.left && x < eleProperties.left + eleProperties.width && y > eleProperties.top && y < eleProperties.top + eleProperties.height) {
        let elementInfo = elements[i]['element']['attributes'];
        elementInfo.xpath = elements[i]['element']['xpath'];
        return elementInfo;
      }
    }
    return {};
  }

  getElements(source) {
    const elementsByOverlap = this.buildElementsWithProps(source, null, [], {});
    let elements = [];
    // Adjust overlapping elements
    for (const key of Object.keys(elementsByOverlap)) {
      if (elementsByOverlap[key].length > 1) {
        const { centerX, centerY } = elementsByOverlap[key][0].properties;

        // Create new element obj which will be a +/- centroid

        const element = {
          type: EXPAND,
          element: null,
          parent: null,
          properties: {
            left: null,
            top: null,
            width: null,
            height: null,
            centerX,
            centerY,
            angleX: null,
            angleY: null,
            path: key,
            keyCode: key,
            container: null,
            accessible: null
          }
        };
        elements = [...elements, element,
        ...updateOverlapsAngles(elementsByOverlap[key], key)];
      } else {
        elements.push(elementsByOverlap[key][0]);
      }
    }
    return elements;
  }
  //the func create a new object for each element and determines ites properties
  buildElementsWithProps(source, prevElement, elements, overlaps) {
    if (!source) {
      return {};
    }
    const { scaleRatio } = this.props;
    const { x1, y1, x2, y2 } = parseCoordinates(source);
    // console.log('x1:' + x1 + ' X2:' + x2 + ' y1:' + y1 + ' y2:' + y2);
    const xOffset = 0;
    const centerPoint = (v1, v2) =>
      Math.round(v1 + ((v2 - v1) / 2));
    const obj = {
      type: CENTROID,
      element: source,
      parent: prevElement,
      properties: {
        left: x1 + xOffset,
        top: y1 + xOffset,
        width: (x2 - x1),
        height: (y2 - y1),
        centerX: centerPoint(x1, x2) + xOffset,
        centerY: centerPoint(y1, y2),
        angleX: null,
        angleY: null,
        path: source.path,
        keyCode: null,
        container: false,
        accessible: source.attributes.accessible
      }
    };
    const coordinates = `${obj.properties.centerX}.${obj.properties.centerY}`;
    obj.properties.container = isElementContainer(obj, elements);

    elements.push(obj);

    if (source.path) {
      if (overlaps[coordinates]) {
        overlaps[coordinates].push(obj);
      } else {
        overlaps[coordinates] = [obj];
      }
    }

    if (source.children) {
      for (const childEl of source.children) {
        this.buildElementsWithProps(childEl, source, elements, overlaps);
      }
    }

    return overlaps;
  }

  getMinAndMax(line) {
    let min = 0;
    let max = 0;
    let sections = line.split(',');
    for (let i in sections) {
      if (sections[i].startsWith(' min')) {
        min = Number(sections[i].substring(5, sections[i].length));
      }
      if (sections[i].startsWith(' max')) {
        max = Number(sections[i].substring(5, sections[i].length));
      }
    }
    return new Array(min, max);
  }

  saveTestCases() {
    const { testCases, driverList } = this.props;
    let save = {};
    let cases = [];
    for (let i = 0; i < testCases.length; i++) {
      let newCase = {};
      newCase.index = i;
      newCase.elementInfo = testCases[i].elementInfo;
      newCase.driverId = testCases[i].driverID;
      let action = { ...testCases[i].action };
      newCase.action = getCaseAction(action);
      console.log(action);
      newCase.isOption = testCases[i].isOption;
      cases.push(newCase);
    }
    save.drivers = driverList;
    save.cases = cases;
    const json = JSON.stringify(save, null, 2);
    console.log(json);
    this.save(json, 'testFile.json', 'application/json')
  }

  clearTestCase() {
    const { updateRecordedTestCases, clearDriverList } = this.props;
    updateRecordedTestCases([]).then(() => { });
    clearDriverList();
  }

  save(content, fileName, contentType) {
    var a = document.createElement("a");
    var file = new Blob([content], { type: contentType });
    a.href = URL.createObjectURL(file);
    a.download = fileName;
    a.click();
  }

  code(raw = true) {
    let { showBoilerplate, sessionDetails, recordedActions, actionFramework } = this.props;
    let { host, port, path, https, desiredCapabilities } = sessionDetails;

    let framework = new frameworks[actionFramework](host, port, path, https, desiredCapabilities);
    framework.actions = recordedActions;
    let rawCode = framework.getCodeString(showBoilerplate);
    if (raw) {
      return rawCode;
    }
    return highlight(framework.language, rawCode, true).value;
  }

  actionBar() {
    const { showBoilerplate, recordedActions, setActionFramework, toggleShowBoilerplate, clearRecording, closeRecorder, actionFramework, isRecording, t, testCases } = this.props;

    let frameworkOpts = Object.keys(frameworks).map((f) => (
      <Option value={f} key={f}>
        {frameworks[f].readableName}
      </Option>
    ));

    return (
      <div>
        <Row>
          {(!!testCases.length || !isRecording) && (
            <ButtonGroup size='small'>
              {/* {!!recordedActions.length && (
                <Tooltip title={t('Show/Hide Boilerplate Code')}>
                  <Button onClick={toggleShowBoilerplate} icon={<ExportOutlined />} type={showBoilerplate ? BUTTON.PRIMARY : BUTTON.DEFAULT} />
                </Tooltip>
              )} */}
              {!!testCases.length && (
                <Tooltip title={t('Save Cases')}>
                  <Button icon={<CopyOutlined />} onClick={() => this.saveTestCases()
                  } />
                </Tooltip>
              )}
              {!!testCases.length && (
                <Tooltip title={t('Clear Cases')}>
                  <Button icon={<DeleteOutlined />} onClick={() => this.clearTestCase()} />
                </Tooltip>
              )}
              {!isRecording && (
                <Tooltip title={t('Run Cases')}>
                  <Button icon={<CaretLeftOutlined />} onClick={closeRecorder} />
                </Tooltip>
              )}
            </ButtonGroup>
          )}
          {(
            <Tooltip title={t('Tap to Case')}>
              <Button size='small' icon={<RocketOutlined />} onClick={() => this.getEventFromPhone()} type={this.state.isGetingEvent ? BUTTON.PRIMARY : BUTTON.DEFAULT} />
            </Tooltip>
          )}
        </Row>
      </div>
    );
  }

  onAttributeChange(index, value, argIndex) {
    const { testCases, updateRecordedTestCases } = this.props;
    let newTestCases = testCases;
    newTestCases[index].action.arguments[argIndex] = value;
    console.log(newTestCases);
    updateRecordedTestCases(newTestCases).then(() => { });
  }

  actionAttributesCol(index, action) {
    let actionChange = { ...action };
    const actionFormatted = getCaseAction(actionChange);
    const attributes = actionFormatted.arguments;
    console.log(attributes + index);
    let attrCols = [];
    attrCols = Object.keys(attributes).map((attr, argIndex) => {
      //TODO: set canSvae is false
      //if (attributes[attr] == null) { this.setState({ canSave: false }); }
      return (
        <Col style={{ width: '100%' }} key={argIndex}>
          <Tooltip title={attr}>
            {
              attributes[attr] == null ?
                <Input status='error' placeholder={attr} value={attributes[attr]}
                  onChange={(e) => this.onAttributeChange(index, e.target.value, argIndex)} /> :
                <Input placeholder={attr} value={attributes[attr]}
                  onChange={(e) => this.onAttributeChange(index, e.target.value, argIndex)} />
            }
          </Tooltip>
        </Col>
      )
    });
    return (
      <div>
        {attrCols.length !== 0 ?
          <Row gutter={8} wrap={false}>
            {attrCols}
          </Row> :
          (
            <Row wrap={false}>
              <Col style={{ width: '100%' }}><Input placeholder='no attribute' disabled={true} /></Col>
            </Row>
          )}
      </div>
    )
  }

  onDragEnd(result) {
    const { testCases, updateRecordedTestCases } = this.props;
    const { source, destination, draggableId } = result;
    if (!destination) {
      return;
    }
    const orderedTestCases = reorder(testCases, source.index, destination.index);
    updateRecordedTestCases(orderedTestCases).then(() => { });
  }

  onActionChange(value, index) {
    const { testCases, updateRecordedTestCases } = this.props;
    let newTesCases = testCases
    newTesCases[index].action.actionType = value;
    newTesCases[index].action.arguments = [];
    updateRecordedTestCases(newTesCases).then(() => { });
  }
  isOptionChange(checked, index) {
    const { testCases, updateRecordedTestCases } = this.props;
    let newTestCases = testCases;
    console.log('option' + 'index:' + index + 'checked' + checked);
    newTestCases[index].isOption = checked;
    updateRecordedTestCases(newTestCases).then(() => { });
  }
  DeleteTestCase(index) {
    const { testCases, updateRecordedTestCases } = this.props;
    let deletedTestCases = [];
    for (let i = 0; i < testCases.length; i++) {
      if (index == i) { continue; }
      deletedTestCases.push(testCases[i]);
    }
    updateRecordedTestCases(deletedTestCases).then(() => {
      console.log(testCases);
    });
  }

  render() {
    const { recordedActions, t, sessionDetails, testCases } = this.props;
    // const highlightedCode = this.code(false);

    return (
      <Card
        title={
          <span>
            <CodeOutlined /> {t('Recorder')}
          </span>
        }
        className={InspectorStyles['recorded-actions']}
        extra={this.actionBar()}>
        {!testCases.length && <div className={InspectorStyles['no-recorded-actions']}>{t('Perform some actions to see case show up here')}</div>}
        {!!testCases.length && (
          <div
            // className={InspectorStyles['recorded-code']} dangerouslySetInnerHTML={{ __html: highlightedCode }}
            >
            <DragDropContext onDragEnd={this.onDragEnd}>
              <Droppable droppableId='droppable'>
                  {(provided, snapshot) => (
                  <div {...provided.droppableProps} ref={provided.innerRef} style={getListStyle(snapshot.isDraggingOver)}>
                      {testCases.map((testCase, index) => (
                        <Draggable key={index} draggableId={`${index}`} index={index}>
                          {(provided, snapshot) => (
                            <div ref={provided.innerRef} {...provided.draggableProps} {...provided.dragHandleProps} style={getItemStyle(snapshot.isDragging, provided.draggableProps.style)}>
                              {Object.keys(testCase).length !== 0 && (
                                <Row ustify='space-around' align='top' gutter={16} wrap={false}>
                                  <Col className='index'>
                                    <Button>{index + 1}</Button>
                                  </Col>
                                  <Col className='device'>
                                    <Input disabled='true' placeholder={`${testCase['device']}`} />
                                  </Col>
                                  <Col className='element' style={{ width: 200 }}>
                                    {testCase.element != undefined ? (
                                      <Tooltip placement='top' title={`${testCase.element.strategy}` + ':' + `${testCase.element.locator}`}>
                                        <Mentions style={{ width: '100%' }} placeholder={`${testCase.element.strategy}` + ':' + `${testCase.element.locator}`} readOnly />
                                      </Tooltip>
                                    ) : (
                                      <Tooltip placement='top' title='no element'>
                                        <Mentions style={{ width: '100%' }} placeholder='no element' readOnly />
                                      </Tooltip>
                                    )}
                                  </Col>
                                  <Col>
                                    <Select value={`${testCase['action']['actionType']}`} style={{ width: 100 }}
                                      onChange={(e) => this.onActionChange(e, index)}
                                    >
                                      <Option value='click'>Click</Option>
                                      <Option value='input'>Input</Option>
                                      <Option value='clear'>Clear</Option>
                                      <Option value='longClick'>LongClick</Option>
                                      <Option value='swipe'>Swipe</Option>
                                      <Option value='move'>Move</Option>
                                      <Option value='activateApp'>ActivateApp</Option>
                                      <Option value='terminateApp'>TerminateApp</Option>
                                      <Option value='resetApp' >ResetApp</Option>
                                      <Option value='home' >Home</Option>
                                      <Option value='back' >Back</Option>
                                      <Option value='visitUrl' disabled>visitUrl</Option>
                                      <Option value='assert' >assert</Option>
                                    </Select>
                                  </Col>
                                  <Col style={{ width: 280 }}>
                                    {this.actionAttributesCol(index, testCase['action'])}
                                  </Col>
                                  <Col>
                                    <Button
                                      icon={<DeleteOutlined />}
                                      onClick={() => {
                                        this.DeleteTestCase(index);
                                      }}
                                    />
                                  </Col>
                                  <Col style={{ paddingTop: '5px' }}>
                                    <Switch checkedChildren="isOptional" unCheckedChildren="unoptional" defaultChecked={testCase['isOption']}
                                      onChange={(checked, event) => this.isOptionChange(checked, index)} />
                                  </Col>
                                  <Col>
                                    <MenuOutlined style={{ paddingTop: '10px' }} />
                                  </Col>
                                </Row>
                              )}
                            </div>
                          )}
                        </Draggable>
                      ))}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              <div ref={this.listEndRef} ></div>
              </DragDropContext>
          </div>
        )}
      </Card>
    );
  }
}

export default withTranslation(RecordedActions);
