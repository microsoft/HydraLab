import React, { Component } from 'react';

import SimpleTab from './SimpleTab';
import styles from './StartServer.css';

import AppiumLogo from '../../images/appium_logo.png';


export default class StartServer extends Component {

  render () {
    return (
      <div className={styles.container}>
        <div className={styles.formAndLogo}>
          <img src={AppiumLogo} className={styles.logo} />
          {<SimpleTab {...this.props} />}
        </div>
      </div>
    );
  }
}
