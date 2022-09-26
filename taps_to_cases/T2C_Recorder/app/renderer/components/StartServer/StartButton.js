import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import { withTranslation } from '../../util';

import styles from './StartButton.css';
import { BUTTON, INPUT } from '../../../../gui-common/components/AntdTypes';

class StartButton extends Component {
  isEnabled () {
    return !(this.props.serverStarting || this.props.disabledOverride);
  }

  noop (evt) {
    evt.preventDefault();
  }

  render () {
    const {startServer, serverStarting, serverVersion, t} = this.props;
    const buttonProps = {};
    if (!this.isEnabled()) {
      buttonProps.disabled = true;
    }

    return (
      <div>
        <Button {...buttonProps} id='startServerBtn'
          className={styles.startButton}
          type={BUTTON.PRIMARY}
          onClick={this.isEnabled() ? startServer : this.noop}
        >
          {serverStarting ? t('Starting…') : t('startServer', {serverVersion})}
        </Button>
        <input type={INPUT.SUBMIT} hidden={true} />
      </div>
    );
  }
}

StartButton.propTypes = {
  serverStarting: PropTypes.bool.isRequired,
  startServer: PropTypes.func.isRequired,
  disabledOverride: PropTypes.bool,
};

export default withTranslation(StartButton);
