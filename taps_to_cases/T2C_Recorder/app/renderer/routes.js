import React, { Suspense } from 'react';
import { Switch, Route } from 'react-router';
import App from './containers/App';
import SessionPage from './containers/SessionPage';
import InspectorPage from './containers/InspectorPage';
import StartServerPage from './containers/StartServerPage';
import ServerMonitorPage from './containers/ServerMonitorPage';
import Spinner from '../../gui-common/components/Spinner/Spinner';
import { ipcRenderer } from './polyfills';
import i18n from '../configs/i18next.config.renderer';

ipcRenderer.on('appium-language-changed', (event, message) => {
  if (i18n.language !== message.language) {
    i18n.changeLanguage(message.language);
  }
});

export default () => (
  <Suspense fallback={<Spinner />}>
    <App>
      <Switch>
        <Route exact path="/" component={SessionPage} />
        <Route path="/session" component={SessionPage} />
        <Route path="/inspector" component={InspectorPage} />
        <Route path="/server" component={StartServerPage} />
        <Route path="/monitor" component={ServerMonitorPage} />
      </Switch>
    </App>
  </Suspense>
);
