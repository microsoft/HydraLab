import React, { Component } from 'react';
import { Button, Checkbox, Input, Modal, Form, Row, Col, Select, Tooltip } from 'antd';
import FormattedCaps from './FormattedCaps';
import CapabilityControl from './CapabilityControl';
import SessionStyles from './Session.css';
import {
  DeleteOutlined,
  PlusOutlined
} from '@ant-design/icons';
import { ROW } from '../../../../gui-common/components/AntdTypes';

const {Item: FormItem} = Form;
const {Option} = Select;
const whitespaces = /^\s|\s$/;

function whitespaceMsg (value) {
  const leadingSpace = /^\s/.test(value);
  const trailingSpace = /\s$/.test(value);

  if (leadingSpace && trailingSpace) {return 'Contains Leading & Trailing Whitespace';}
  if (leadingSpace) {return 'Contains Leading Whitespace';}
  if (trailingSpace) {return 'Contains Trailing Whitespace';}
}

export default class CapabilityEditor extends Component {

  constructor (props) {
    super(props);
    this.latestCapField = React.createRef();
  }

  /**
   * Callback when the type of a dcap is changed
   */
  handleSetType (index, type) {
    let {setCapabilityParam, caps} = this.props;
    setCapabilityParam(index, 'type', type);

    // Translate the current value to the new type
    let translatedValue = caps[index].value;
    switch (type) {
      case 'text':
        translatedValue = translatedValue + '';
        break;
      case 'boolean':
        if (translatedValue === 'true') {
          translatedValue = true;
        } else if (translatedValue === 'false') {
          translatedValue = false;
        } else {
          translatedValue = !!translatedValue;
        }
        break;
      case 'number':
        translatedValue = parseInt(translatedValue, 10) || 0;
        break;
      case 'json_object':
      case 'object':
        translatedValue = translatedValue + '';
        break;
      case 'file':
        translatedValue = '';
        break;
      default:
        break;
    }
    setCapabilityParam(index, 'value', translatedValue);
  }

  componentDidUpdate () {
    const {caps} = this.props;
    // if we have more than one cap and the most recent cap name is empty, it means we've just
    // added a new cap field, so focus that input element. But only do this once, so we don't annoy
    // the user if they decide to unfocus and do something else.
    if (
      caps.length > 1 &&
      !this.latestCapField.current.input.value &&
      !this.latestCapField.current.__didFocus
    ) {
      this.latestCapField.current.focus();
      this.latestCapField.current.__didFocus = true;
    }
  }

  render () {
    const {setCapabilityParam, caps, addCapability, removeCapability, saveSession, hideSaveAsModal,
           saveAsText, showSaveAsModal, setSaveAsText, isEditingDesiredCaps, t,
           setAddVendorPrefixes, addVendorPrefixes, server, serverType} = this.props;
    const numCaps = caps.length;
    const onSaveAsOk = () => saveSession(server, serverType, caps, {name: saveAsText});

    return <>
      <Row type={ROW.FLEX} align="top" justify="start" className={SessionStyles.capsFormRow}>
        <Col order={1} span={12} className={`${SessionStyles.capsFormCol} ${isEditingDesiredCaps ? SessionStyles.capsFormDisabled : ''}`}>
          <Form
            className={SessionStyles.newSessionForm}
          >

            {caps.map((cap, index) => <Row gutter={8} key={index}>
              <Col span={7}>
                <FormItem>
                  <Tooltip title={whitespaceMsg(cap.name)} visible={whitespaces.test(cap.name)}>
                    <Input disabled={isEditingDesiredCaps} id={`desiredCapabilityName_${index}`} placeholder={t('Name')}
                      value={cap.name} onChange={(e) => setCapabilityParam(index, 'name', e.target.value)}
                      ref={index === numCaps - 1 ? this.latestCapField : ''}
                      className={SessionStyles.capsBoxFont}
                    />
                  </Tooltip>
                </FormItem>
              </Col>
              <Col span={8}>
                <FormItem>
                  <Select disabled={isEditingDesiredCaps} onChange={(val) => this.handleSetType(index, val)} defaultValue={cap.type}>
                    <Option value='text'>{t('text')}</Option>
                    <Option value='boolean'>{t('boolean')}</Option>
                    <Option value='number'>{t('number')}</Option>
                    <Option value='object'>{t('JSON object')}</Option>
                    <Option value='file'>{t('filepath')}</Option>
                  </Select>
                </FormItem>
              </Col>
              <Col span={7}>
                <FormItem>
                  <Tooltip title={whitespaceMsg(cap.value)} visible={whitespaces.test(cap.value)}>
                    <CapabilityControl {...this.props} cap={cap} id={`desiredCapabilityValue_${index}`}
                      onSetCapabilityParam={(value) => setCapabilityParam(index, 'value', value)}
                      onPressEnter={(index === numCaps - 1) ? addCapability : () => {}}
                    />
                  </Tooltip>
                </FormItem>
              </Col>
              <Col span={2}>
                <div className={SessionStyles.btnDeleteCap}>
                  <FormItem>
                    <Button {...{disabled: caps.length <= 1 || isEditingDesiredCaps}}
                      icon={<DeleteOutlined/>}
                      onClick={() => removeCapability(index)}/>
                  </FormItem>
                </div>
              </Col>
            </Row>)}
            <Row>
              <Col span={22}>
                <FormItem>
                  <Checkbox
                    onChange={(e) => setAddVendorPrefixes(e.target.checked)}
                    checked={addVendorPrefixes}
                  >
                    {t('autoAddPrefixes')}
                  </Checkbox>
                </FormItem>
              </Col>
              <Col span={2}>
                <FormItem>
                  <Button
                    disabled={isEditingDesiredCaps} id='btnAddDesiredCapability'
                    icon={<PlusOutlined/>}
                    onClick={addCapability}
                    className={SessionStyles['add-desired-capability-button']} />
                </FormItem>
              </Col>
            </Row>
          </Form>
        </Col>
        <Col order={2} span={12} className={SessionStyles.capsFormattedCol}>
          <FormattedCaps {...this.props} />
          <Modal visible={showSaveAsModal}
            title={t('Save Capability Set As')}
            okText='Save'
            cancelText='Cancel'
            onCancel={hideSaveAsModal}
            onOk={onSaveAsOk}>
            <Input onChange={(e) => setSaveAsText(e.target.value)} addonBefore={t('Name')} value={saveAsText} onPressEnter={onSaveAsOk}/>
          </Modal>
        </Col>
      </Row>
    </>;
  }
}
