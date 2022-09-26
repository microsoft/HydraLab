import React, { Component } from 'react';
import _, { groupBy } from 'lodash';
import { getLocators } from './shared';
import styles from './Inspector.css';
import { Button, Row, Col, Input, Modal, Table, Alert, Tooltip, Select, AutoComplete } from 'antd';
import { withTranslation } from '../../util';
import { clipboard, shell } from '../../polyfills';
import {
  LoadingOutlined,
  CopyOutlined,
  AimOutlined,
  EditOutlined,
  UndoOutlined,
  HourglassOutlined,
  RadiusBottomrightOutlined,
  AppstoreAddOutlined,
  ScheduleOutlined,
} from '@ant-design/icons';
import { ROW, ALERT } from '../../../../gui-common/components/AntdTypes';
const { Option } = AutoComplete;

const ButtonGroup = Button.Group;
const NATIVE_APP = 'NATIVE_APP';

function selectedElementTableCell(text, copyToClipBoard) {
  if (copyToClipBoard) {
    return (
      <div className={styles['selected-element-table-cells']}>
        <Tooltip title='Copied!' trigger='click'>
          <span className={styles['element-cell-copy']} onClick={() => clipboard.writeText(text)}>
            {text}
          </span>
        </Tooltip>
      </div>
    );
  } else {
    return <div className={styles['selected-element-table-cells']}>{text}</div>;
  }
}
const generateId = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c == 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
/**
 * Shows details of the currently selected element and shows methods that can
 * be called on the elements (tap, sendKeys)
 */
class SelectedElement extends Component {
  constructor(props) {
    super(props);
    this.handleSendKeys = this.handleSendKeys.bind(this);
    this.contextSelect = this.contextSelect.bind(this);
    this.handleGetInfo = this.handleGetInfo.bind(this);
    this.handleDoAppAction = this.handleDoAppAction.bind(this);
    this.handleAssertModal = this.handleAssertModal.bind(this)
    this.state = {
      keyOfInfo: '',
      infoValue: '',
      appAction: '',
      appPackegeName: '',
      attribute: '',
      expectedValue: '',
    }
  }

  handleSendKeys() {
    const { sendKeys, applyClientMethod, hideSendKeysModal, selectedElementId: elementId } = this.props;
    applyClientMethod({ methodName: 'sendKeys', elementId, args: [sendKeys.toString()] });
    hideSendKeysModal();
  }

  handleGetInfo() {
    const { applyCustomMethod, hideGetInfoModal } = this.props;
    const id = generateId();
    applyCustomMethod({ methodName: 'getInfo', args: [id, this.state.keyOfInfo, this.state.infoValue] });
    this.setState({ keyOfInfo: '' });
    hideGetInfoModal();
  }

  handleDoAppAction() {
    const { applyClientMethod, hideGetAppActionModal } = this.props;
    applyClientMethod({ methodName: this.state.appAction, args: [this.state.appPackegeName], skipRefresh: false });
    hideGetAppActionModal();
  }
  handleAssertModal() {
    const { applyCustomMethod, hideAssertModal } = this.props;
    applyCustomMethod({ methodName: 'assert', args: [this.state.attribute, this.state.expectedValue] })
    hideAssertModal();
  }
  isCanBeAssert(attribute) {
    if (attribute === 'index' || attribute == 'x' || attribute == 'y' || attribute == 'width' || attribute == 'height') {
      return false;
    }
    return true;
  }
  contextSelect() {
    let { applyClientMethod, contexts, currentContext, setContext, t } = this.props;

    return (
      <Tooltip title={t('contextSwitcher')}>
        <Select
          value={currentContext}
          onChange={(value) => {
            setContext(value);
            applyClientMethod({ methodName: 'switchContext', args: [value] });
          }}
          className={styles['locator-strategy-selector']}
        >
          {contexts.map(({ id, title }) => (
            <Select.Option key={id} value={id}>
              {title ? `${title} (${id})` : id}
            </Select.Option>
          ))}
        </Select>
      </Tooltip>
    );
  }
  infoOptions() {
    const { getInfoList } = this.props;
    let getInfoArray = [];
    const iterator = getInfoList[Symbol.iterator]();
    for (let item of iterator) {
      getInfoArray.push(item);
    }
    getInfoArray.map((item) => (
      <Option key={item[0]} value={item[1].value}>{item[1].key}</Option>
    ))
    return (
      { getInfoArray }
    )
  }
  getDefaultExpectedValue(attrArray, e) {
    for (let attr of attrArray) {
      if (attr[0] == e) {
        return attr[1];
      }
    }
    return null;
  }
  render() {
    let {
      applyClientMethod,
      contexts,
      currentContext,
      setFieldValue,
      getFindElementsTimes,
      findElementsExecutionTimes,
      isFindingElementsTimes,
      selectedElement,
      sendKeysModalVisible,
      showSendKeysModal,
      hideSendKeysModal,
      selectedElementId: elementId,
      sourceXML,
      elementInteractionsNotAvailable,
      t,
      showGetInfoModal,
      getInfoModalVisible,
      hideGetInfoModal,
      doAppActionVisible,
      showGetAppActionModal,
      hideGetAppActionModal,
      assertModalVisible,
      showAssetModal,
      hideAssertModal,
      getInfoList
    } = this.props;
    const { attributes, classChain, predicateString, xpath } = selectedElement;
    const isDisabled = !elementId || isFindingElementsTimes;

    if (!currentContext) {
      currentContext = NATIVE_APP;
    }
    // Get the columns for the attributes table
    let attributeColumns = [
      {
        title: t('Attribute'),
        dataIndex: 'name',
        key: 'name',
        width: 100,
        render: (text) => selectedElementTableCell(text, false),
      },
      {
        title: t('Value'),
        dataIndex: 'value',
        key: 'value',
        render: (text) => selectedElementTableCell(text, true),
      },
    ];

    // Get the data for the attributes table
    let attrArray = _.toPairs(attributes).filter(([key]) => key !== 'path');
    console.log('attrArray');
    console.log(attrArray);
    let dataSource = attrArray.map(([key, value]) => ({
      key,
      value,
      name: key,
    }));
    dataSource.unshift({ key: 'elementId', value: elementId, name: 'elementId' });
    // Get the columns for the strategies table
    let findColumns = [
      {
        title: t('Find By'),
        dataIndex: 'find',
        key: 'find',
        width: 100,
        render: (text) => selectedElementTableCell(text, false),
      },
      {
        title: t('Selector'),
        dataIndex: 'selector',
        key: 'selector',
        render: (text) => selectedElementTableCell(text, true),
      },
    ];

    if (findElementsExecutionTimes.length > 0) {
      findColumns.push({
        title: t('Time'),
        dataIndex: 'time',
        key: 'time',
        align: 'right',
        width: 100,
        render: (text) => selectedElementTableCell(text, false),
      });
    }

    // Get the data for the strategies table
    let findDataSource = _.toPairs(getLocators(attributes, sourceXML)).map(([key, selector]) => ({
      key,
      selector,
      find: key,
    }));

    // If XPath is the only provided data source, warn the user about it's brittleness
    let showXpathWarning = false;
    if (findDataSource.length === 0) {
      showXpathWarning = true;
    }

    // Add class chain to the data source as well
    if (classChain && currentContext === NATIVE_APP) {
      const classChainText = (
        <span>
          -ios class chain
          <strong>
            <a
              onClick={(e) =>
                e.preventDefault() ||
                shell.openExternal(
                  'https://github.com/facebookarchive/WebDriverAgent/wiki/Class-Chain-Queries-Construction-Rules'
                )
              }
            >
              &nbsp;(docs)
            </a>
          </strong>
        </span>
      );

      findDataSource.push({
        key: '-ios class chain',
        find: classChainText,
        selector: classChain,
      });
    }

    // Add predicate string to the data source as well
    if (predicateString && currentContext === NATIVE_APP) {
      const predicateStringText = (
        <span>
          -ios predicate string
          <strong>
            <a
              onClick={(e) =>
                e.preventDefault() ||
                shell.openExternal(
                  'https://github.com/facebookarchive/WebDriverAgent/wiki/Predicate-Queries-Construction-Rules'
                )
              }
            >
              &nbsp;(docs)
            </a>
          </strong>
        </span>
      );

      findDataSource.push({
        key: '-ios predicate string',
        find: predicateStringText,
        selector: predicateString,
      });
    }

    // Add XPath to the data source as well
    if (xpath) {
      findDataSource.push({
        key: 'xpath',
        find: 'xpath',
        selector: xpath,
      });
    }

    // Replace table data with table data that has the times
    if (findElementsExecutionTimes.length > 0) {
      findDataSource = findElementsExecutionTimes;
    }

    let tapIcon = <AimOutlined />;
    if (!(elementInteractionsNotAvailable || elementId)) {
      tapIcon = <LoadingOutlined />;
    }

    let getInfoArray = [];
    const iterator = getInfoList[Symbol.iterator]();
    for (let item of iterator) {
      getInfoArray.push(item);
    }
    return (
      <div>
        {elementInteractionsNotAvailable && (
          <Row type={ROW.FLEX} gutter={10}>
            <Col>
              <Alert type={ALERT.INFO} message={t('Interactions are not available for this element')} showIcon />
            </Col>
          </Row>
        )}
        <Row justify='center' type={ROW.FLEX} align='middle' gutter={10} className={styles.elementActions}>
          <Col>
            <ButtonGroup>
              <Tooltip title={t('Tap')}>
                <Button
                  disabled={isDisabled}
                  icon={tapIcon}
                  id='btnTapElement'
                  onClick={() => applyClientMethod({ methodName: 'click', elementId })}
                />
              </Tooltip>
              <Tooltip title={t('Send Keys')}>
                <Button
                  disabled={isDisabled}
                  id='btnSendKeysToElement'
                  icon={<EditOutlined />}
                  onClick={() => showSendKeysModal()}
                />
              </Tooltip>
              <Tooltip title={t('Clear')}>
                <Button
                  disabled={isDisabled}
                  id='btnClearElement'
                  icon={<UndoOutlined />}
                  onClick={() => applyClientMethod({ methodName: 'clear', elementId })}
                />
              </Tooltip>
              <Tooltip title={t('Copy Attributes to Clipboard')}>
                <Button
                  disabled={isDisabled}
                  id='btnCopyAttributes'
                  icon={<CopyOutlined />}
                  onClick={() => clipboard.writeText(JSON.stringify(dataSource))}
                />
              </Tooltip>
              <Tooltip title={t('Get Timing')}>
                <Button
                  disabled={isDisabled}
                  id='btnGetTiming'
                  icon={<HourglassOutlined />}
                  onClick={() => getFindElementsTimes(findDataSource)}
                />
              </Tooltip>
            </ButtonGroup>
          </Col>
        </Row>
        <Row justify='center' type={ROW.FLEX} align='middle' gutter={10} className={styles.elementActions}>
          <Col>
            <ButtonGroup>
              <Tooltip title={t('Get Element Info')}>
                <Button title={t('Get Element Info')} id='btnGetElementInfo' icon={<RadiusBottomrightOutlined />}
                  onClick={() => showGetInfoModal()} />
              </Tooltip>
              <Tooltip title={t('Operate App')}>
                <Button title={t('Operate App')} id='btnOperateApp' icon={<AppstoreAddOutlined />}
                  onClick={() => showGetAppActionModal()} />
              </Tooltip>
              <Tooltip title={t('Assert')}>
                <Button title={t('Assert')} id='btnAssert' icon={<ScheduleOutlined />}
                  onClick={() => showAssetModal()} />
              </Tooltip>
            </ButtonGroup>
          </Col>
        </Row>
        {findDataSource.length > 0 && (
          <Row>
            <Table
              columns={findColumns}
              dataSource={findDataSource}
              size='small'
              tableLayout='fixed'
              pagination={false}
            />
          </Row>
        )}
        <br />
        {currentContext === NATIVE_APP && showXpathWarning && (
          <div>
            <Alert message={t('usingXPathNotRecommended')} type={ALERT.WARNING} showIcon />
            <br />
          </div>
        )}
        {currentContext === NATIVE_APP && contexts && contexts.length > 1 && (
          <div>
            <Alert message={t('usingSwitchContextRecommended')} type={ALERT.WARNING} showIcon />
            <br />
          </div>
        )}
        {currentContext !== NATIVE_APP && (
          <div>
            <Alert message={t('usingWebviewContext')} type={ALERT.WARNING} showIcon />
            <br />
          </div>
        )}
        {contexts && contexts.length > 1 && (
          <div>
            {this.contextSelect()}
            <br />
            <br />
          </div>
        )}
        {dataSource.length > 0 && (
          <Row>
            <Table columns={attributeColumns} dataSource={dataSource} size='small' pagination={false} />
          </Row>
        )}
        <Modal
          title={t('Send Keys')}
          visible={sendKeysModalVisible}
          okText={t('Send Keys')}
          cancelText={t('Cancel')}
          onCancel={hideSendKeysModal}
          onOk={this.handleSendKeys}
        >
          <AutoComplete
            style={{ width: '100%', }}
            placeholder={t('Enter keys')}
            onChange={(e) => setFieldValue('sendKeys', e)}
          >
            {
              // getInfoList.map((info) => <Option key={info.key} value={info.value}>{info.key}</Option>)
              getInfoArray.map((item) => (
                <Option key={item[0]} value={item[1].value}>{item[1].key}</Option>
              ))
            }
          </AutoComplete>
        </Modal>
        <Modal
          centered
          title={t('Get Infomation of Element')}
          visible={getInfoModalVisible}
          cancelText={t('Cancel')}
          okText={t('Comfirm')}
          onCancel={hideGetInfoModal}
          onOk={this.handleGetInfo}
        >
          <Select
            style={{ width: '100%' }}
            showSearch
            placeholder="Choose the Arttibute"
            optionFilterProp="children"
            filterOption={(input, option) => option.children.includes(input)}
            value={this.state.keyOfInfo}
            onChange={(e) => {
              let value = null;
              for (let attr of attrArray) {
                if (attr[0] == e) {
                  value = attr[1];
                  break;
                }
              }
              this.setState({ keyOfInfo: e, infoValue: value });
            }
            }
          >{
              attrArray.map((attr) => <Option key={attr[0]} value={attr[0]}>{attr[0]}</Option>)
            }
          </Select>
        </Modal>
        <Modal
          centered
          visible={doAppActionVisible}
          title={t('App Actions')}
          cancelText={t('Cancel')}
          okText={t('Comfirm')}
          onCancel={hideGetAppActionModal}
          onOk={this.handleDoAppAction}
        >
          <Row gutter={16}>
            <Col>
              <Select
                style={{ width: 200 }}
                placeholder="Choose App Actions"
                onChange={(e) => this.setState({ appAction: e })}
              >
                <Select.Option value="activateApp">activateApp</Select.Option>
                <Select.Option value="terminateApp">terminateApp</Select.Option>
                <Select.Option value="resetApp">resetApp</Select.Option>
              </Select>
            </Col>
            <Col>
              <Input placeholder='Enter App Package Name'
                value={this.state.appPackegeName}
                onChange={(e) => this.setState({ appPackegeName: e.target.value })}
              />
            </Col>
          </Row>
        </Modal>
        <Modal
          centered
          visible={assertModalVisible}
          title={t('Assert')}
          cancelText={t('Cancel')}
          okText={t('Comfirm')}
          onCancel={hideAssertModal}
          onOk={this.handleAssertModal}
        >
          <Row gutter={16}>
            <Col>
              <Select
                style={{ width: 200 }}
                placeholder="Choose Attribute to Assert"
                onChange={(e) => this.setState({ attribute: e, expectedValue: this.getDefaultExpectedValue(attrArray, e) })}
              >{
                  attrArray.map((attr) => {
                    if (!this.isCanBeAssert(attr[0])) { return; }
                    return <Option key={attr[0]} value={attr[0]}>{attr[0]}</Option>
                  })
                }
              </Select>
            </Col>
            <Col>
              <Input placeholder='Enter Expected Value'
                value={this.state.expectedValue}
                onChange={(e) => this.setState({ expectedValue: e.target.value })}
              />
            </Col>
          </Row>
        </Modal>
      </div>
    );
  }
}

export default withTranslation(SelectedElement);
