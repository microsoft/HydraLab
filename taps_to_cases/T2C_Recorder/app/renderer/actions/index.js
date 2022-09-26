import * as inspectorActions from './Inspector';
import * as sessionActions from './Session';
import * as serverMonitorActions from './ServerMonitor';
import * as startServerActions from './StartServer';
import * as updaterActions from '../../../gui-common/actions/Updater';

export default {
  ...inspectorActions,
  ...sessionActions,
  ...serverMonitorActions,
  ...startServerActions,
  ...updaterActions,
};
