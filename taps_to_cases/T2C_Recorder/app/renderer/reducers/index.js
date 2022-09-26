import { combineReducers } from 'redux';
import { connectRouter } from 'connected-react-router';
import session from './Session';
import inspector from './Inspector';
import startServer from './StartServer';
import serverMonitor from './ServerMonitor';
import updater from '../../../gui-common/reducers/Updater';

// create our root reducer
export default function createRootReducer (history) {
  return combineReducers({
    router: connectRouter(history),
    session,
    inspector,
    startServer,
    serverMonitor,
    updater,
  });
}
