import _, {omit} from 'lodash';
import formatJSON from 'format-json';

import { NEW_SESSION_REQUESTED, NEW_SESSION_BEGAN, NEW_SESSION_DONE,
         SAVE_SESSION_REQUESTED, SAVE_SESSION_DONE, GET_SAVED_SESSIONS_REQUESTED,
         GET_SAVED_SESSIONS_DONE, SESSION_LOADING, SESSION_LOADING_DONE,
  SET_CAPABILITY_PARAM, ADD_CAPABILITY, REMOVE_CAPABILITY, SET_CAPS_AND_SERVER, SET_CAPS, SET_APP,
         SWITCHED_TABS, SAVE_AS_MODAL_REQUESTED, HIDE_SAVE_AS_MODAL_REQUESTED, SET_SAVE_AS_TEXT,
         DELETE_SAVED_SESSION_REQUESTED, DELETE_SAVED_SESSION_DONE,
         CHANGE_SERVER_TYPE, SET_SERVER_PARAM, SET_SERVER, SET_ATTACH_SESS_ID,
         GET_SESSIONS_REQUESTED, GET_SESSIONS_DONE,
         ENABLE_DESIRED_CAPS_EDITOR, ABORT_DESIRED_CAPS_EDITOR, SAVE_RAW_DESIRED_CAPS, SET_RAW_DESIRED_CAPS, SHOW_DESIRED_CAPS_JSON_ERROR,
         IS_ADDING_CLOUD_PROVIDER, SET_PROVIDERS, SET_ADD_VENDOR_PREFIXES, SET_STATE_FROM_URL, SET_STATE_FROM_SAVED,
  ServerTypes, WRITE_BACK_SERVER_ARGS, SET_USE_EXISTING_WINDOW, SET_URL, SET_BROWSER_DRIVER
} from '../actions/Session';
import { notification } from 'antd';

const visibleProviders = []; // Pull this from "electron-settings"
const server = {
  local: {},
  remote: {},
  advanced: {},
};

for (const serverName of _.keys(ServerTypes)) {
  server[serverName] = {};
}

// Make sure there's always at least one cap
const INITIAL_STATE = {
  savedSessions: [],
  tabKey: 'new',
  serverType: ServerTypes.remote,
  visibleProviders,
  server: {
    local: {},
    remote: {},
    sauce: {
      dataCenter: 'us-west-1',
    },
    headspin: {},
    browserstack: {},
    lambdatest: {},
    advanced: {},
    bitbar: {},
    kobiton: {},
    perfecto: {},
    pcloudy: {},
    testingbot: {},
    experitest: {},
    roboticmobi: {},
  },
  attachSessId: null,

  // Make sure there's always at least one cap
  caps: [{
    type: 'text',
  }],
  testApp: '',
  isUseExistingWindow: false,
  browserDriver: null,

  isCapsDirty: true,
  gettingSessions: false,
  runningAppiumSessions: [],
  isEditingDesiredCaps: false,
  isValidCapsJson: true,
  isValidatingCapsJson: false,
  isAddingCloudProvider: false,
  addVendorPrefixes: true,
};

let nextState;

// returns false if the attachSessId is not present in the runningSessions list
const isAttachSessIdValid = (runningSessions, attachSessId) => {
  if (!attachSessId || !runningSessions) {
    return false;
  }
  for (const session of runningSessions) {
    if (session.id === attachSessId) {
      return true;
    }
  }
  return false;
};

export default function session (state = INITIAL_STATE, action) {
  switch (action.type) {
    case NEW_SESSION_REQUESTED:
      return {
        ...state,
        newSessionRequested: true,
      };

    case NEW_SESSION_BEGAN:
      nextState = {
        ...state,
        newSessionBegan: true,
      };
      return omit(nextState, 'newSessionRequested');

    case NEW_SESSION_DONE:
      return omit(state, 'newSessionBegan');


    case ADD_CAPABILITY:
      return {
        ...state,
        caps: [
          ...state.caps,
          {type: 'text'},
        ],
      };

    case REMOVE_CAPABILITY:
      return {
        ...state,
        caps: state.caps.filter((cap, index) => index !== action.index),
      };

    case SET_CAPABILITY_PARAM:
      return {
        ...state,
        isCapsDirty: true,
        caps: state.caps.map((cap, index) => index !== action.index ? cap : {
          ...cap,
          [action.name]: action.value
        }),
      };

    case SET_CAPS_AND_SERVER:
      nextState = {
        ...state,
        server: action.server,
        serverType: action.serverType,
        caps: action.caps,
        capsUUID: action.uuid,
      };
      return omit(nextState, 'isCapsDirty');

    case SET_CAPS:
      return {
        ...state,
        isCapsDirty: true,
        caps: action.caps,
      }

    case SET_APP:
      return {
        ...state,
        testApp: action.testApp,
        testUrl: null,
      }

    case SET_URL:
      return {
        ...state,
        testApp: null,
        testUrl: action.testUrl,
      }

    case SET_BROWSER_DRIVER:
      return {
        ...state,
        browserDriver: action.browserDriver,
      }

    case SET_USE_EXISTING_WINDOW:
      return {
        ...state,
        isUseExistingWindow: action.isUseExistingWindow,
      }
    case SAVE_SESSION_REQUESTED:
      nextState = {
        ...state,
        saveSessionRequested: true,
      };
      return omit(nextState, 'showSaveAsModal');

    case SAVE_SESSION_DONE:
      return omit(state, ['saveSessionRequested', 'saveAsText']);

    case GET_SAVED_SESSIONS_REQUESTED:
      return {
        ...state,
        getSavedSessionsRequested: true,
      };

    case GET_SAVED_SESSIONS_DONE:
      nextState = {
        ...state,
        savedSessions: action.savedSessions || [],
      };
      return omit(nextState, 'getSavedSessionsRequested');

    case DELETE_SAVED_SESSION_REQUESTED:
      return {
        ...state,
        deletingSession: true,
      };

    case DELETE_SAVED_SESSION_DONE:
      return {
        ...state,
        deletingSession: false,
        capsUUID: null
      };

    case SWITCHED_TABS:
      return {
        ...state,
        tabKey: action.key,
      };

    case SAVE_AS_MODAL_REQUESTED:
      return {
        ...state,
        'showSaveAsModal': true,
      };

    case HIDE_SAVE_AS_MODAL_REQUESTED:
      return omit(state, ['saveAsText', 'showSaveAsModal']);

    case SET_SAVE_AS_TEXT:
      return {
        ...state,
        saveAsText: action.saveAsText,
      };

    case CHANGE_SERVER_TYPE:
      return {
        ...state,
        serverType: action.serverType,
      };

    case SET_SERVER_PARAM:
      return {
        ...state,
        server: {
          ...state.server,
          [action.serverType]: {
            ...state.server[action.serverType],
            [action.name]: action.value,
          }
        },
      };

    case SET_SERVER:
      return {
        ...state,
        server: {
          ...(function extendCurrentServerStateWithNewServerState (currentServerState, newServerState) {
            // Copy current server state and extend it with new server state
            const nextServerState = _.cloneDeep(currentServerState || {});

            // Extend each server (sauce, remote, kobiton, etc...)
            for (let serverName of _.keys(nextServerState)) {
              nextServerState[serverName] = {
                ...(nextServerState[serverName] || {}),
                ...(newServerState[serverName] || {}),
              };
            }
            return nextServerState;
          })(state.server, action.server),
        },
        serverType: action.serverType || ServerTypes.local,
      };

    case SET_ATTACH_SESS_ID:
      return {
        ...state,
        attachSessId: action.attachSessId
      };

    case SESSION_LOADING:
      return {
        ...state,
        sessionLoading: true,
      };

    case SESSION_LOADING_DONE:
      return omit(state, 'sessionLoading');

    case GET_SESSIONS_REQUESTED:
      return {
        ...state,
        gettingSessions: true,
      };

    case GET_SESSIONS_DONE: {
      const attachSessId = isAttachSessIdValid(action.sessions, state.attachSessId) ? state.attachSessId : null;
      return {
        ...state,
        gettingSessions: false,
        attachSessId: (action.sessions && action.sessions.length > 0 && !attachSessId) ? action.sessions[0].id : attachSessId,
        runningAppiumSessions: action.sessions || [],
      };
    }

    case ENABLE_DESIRED_CAPS_EDITOR:
      return {
        ...state,
        isEditingDesiredCaps: true,
        rawDesiredCaps: formatJSON.plain(
          // Translate the caps definition to a proper capabilities JS Object
          _.reduce(
            state.caps,
            (result, obj) => ({
              ...result,
              [obj.name]: obj.value,
            }),
            {}
          )
        ),
        isValidCapsJson: true,
        isValidatingCapsJson: false, // Don't start validating JSON until the user has attempted to save the JSON
      };

    case ABORT_DESIRED_CAPS_EDITOR:
      return {
        ...state,
        isEditingDesiredCaps: false,
        rawDesiredCaps: null,
      };

    case SAVE_RAW_DESIRED_CAPS:
      return {
        ...state,
        isEditingDesiredCaps: false,
        caps: action.caps,
      };

    case SHOW_DESIRED_CAPS_JSON_ERROR:
      return {
        ...state,
        invalidCapsJsonReason: action.message,
        isValidCapsJson: false,
        isValidatingCapsJson: true,
      };

    case SET_RAW_DESIRED_CAPS:
      return {
        ...state,
        rawDesiredCaps: action.rawDesiredCaps,
        isValidCapsJson: action.isValidCapsJson,
        invalidCapsJsonReason: action.invalidCapsJsonReason,
      };

    case IS_ADDING_CLOUD_PROVIDER:
      return {
        ...state,
        isAddingCloudProvider: action.isAddingProvider,
      };

    case SET_PROVIDERS:
      return {
        ...state,
        visibleProviders: action.providers || []
      };

    case SET_ADD_VENDOR_PREFIXES:
      return {
        ...state,
        addVendorPrefixes: action.addVendorPrefixes,
      };

    case SET_STATE_FROM_URL:
      return {
        ...state,
        server: {
          ...state.server,
          ...(action.state.server || {})
        },
        ...omit(action.state, ['server']),
      };

    case SET_STATE_FROM_SAVED:
      if (!Object.keys(ServerTypes).includes(action.state.serverType)) {
        notification.error({
          message: `Failed to load session: ${action.state.serverType} is not a valid server type`,
        });
        return state;
      }
      if (![...state.visibleProviders, ServerTypes.local, ServerTypes.remote].includes(action.state.serverType)) {
        state.visibleProviders.push(action.state.serverType);
      }
      return {
        ...state,
        ...action.state,
        filePath: action.filePath,
      };

    case WRITE_BACK_SERVER_ARGS:
      return {
        ...state,
        server: {
          ...state.server,
          ["remote"]: {
            ...state.server["remote"],
            "hostname": action.serverArgs.address,
            "port": action.serverArgs.port
          }
        },
      };

    default:
      return {...state};
  }
}
