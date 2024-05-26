import asyncio
import websockets
import gzip
import json
import logging
import websocket_client
import platform

class Const:
    class Path:
        AUTH = "/auth"
        AGENT_INIT = "/agent/init"
        HEARTBEAT = "/heartbeat"
        DEVICE_UPDATE = "/device/update"
        AGENT_UPDATE = "/agent/update"
        AGENT_RESTART = "/agent/restart"
        DEVICE_LIST = "/device/list"
        TEST_TASK_CANCEL = "/test/task/cancel"
        TEST_TASK_RUN = "/test/task/run"
        TEST_TASK_UPDATE = "/test/task/update"
        TEST_TASK_RETRY = "/test/task/retry"
        TEST_TASK_COMPLETE = "/test/task/complete"
        TEST_TASK_START = "/test/task/start"

class SerializeUtil:
    @staticmethod
    def message_to_byte_arr(message):
        json_str = json.dumps(message, separators=(',', ':'), ensure_ascii=False)
        return gzip.compress(json_str.encode('utf-8'))

    @staticmethod
    def byte_arr_to_message(byte_arr):
        json_str = gzip.decompress(byte_arr).decode('utf-8')
        return json.loads(json_str)

class AgentUser:
    def __init__(self, id=None, name=None, secret=None, hostname=None, ip=None, os=None, version_name=None, version_code=None):
        self.id = id
        self.name = name
        self.secret = secret
        self.hostname = hostname
        self.ip = ip
        self.os = os
        self.version_name = version_name
        self.version_code = version_code

class Message:
    def __init__(self, path=None, body=None, session_id=None):
        self.path = path
        self.body = body
        self.session_id = session_id

    def to_dict(self):
        return {
            'path': self.path,
            'body': self.body,
            'sessionId': self.session_id
        }

class AgentWebSocketClientService:
    def __init__(self):
        self.agent_user = None
        self.send_message_callback = None
        self.is_agent_init = False
        self.is_storage_client_init = False
        self.is_prometheus_enabled = False

    def on_message(self, message):
        logging.info(f"onMessage Receive bytes message {message}")
        path = message.get('path')
        response = None
        if path == Const.Path.AUTH:
            self.provide_auth_info(message)
            return
        elif path == Const.Path.AGENT_INIT:
            self.heartbeat_response(message)
            if not self.is_agent_init:
                self.register_agent_metrics()
                # device_control_service.device_driver_init()
                self.is_agent_init = True
                # device_control_service.provide_device_list(self.agent_user.battery_strategy)
            return
        elif path == Const.Path.HEARTBEAT:
            self.heartbeat_response(message)
            # device_control_service.provide_device_list(self.agent_user.battery_strategy)
            return
        elif path == Const.Path.DEVICE_UPDATE:
            # device_control_service.update_device_scope(...)
            response = Message(path=message.get('path'), session_id=message.get('sessionId'), body={"device": "updated"})
            logging.info(f"/api/device/update device SN: updated")
        elif path == Const.Path.AGENT_UPDATE:
            # agent_manage_service.update_agent_package(...)
            pass
        elif path == Const.Path.AGENT_RESTART:
            # agent_manage_service.restart_agent(...)
            pass
        elif path == Const.Path.DEVICE_LIST:
            if self.agent_user.battery_strategy is None:
                response = Message(path=Const.Path.HEARTBEAT, session_id=message.get('sessionId'))
            else:
                # device_control_service.provide_device_list(self.agent_user.battery_strategy)
                pass
        elif path == Const.Path.TEST_TASK_CANCEL:
            # test_task_engine_service.cancel_test_task_by_id(...)
            pass
        elif path == Const.Path.TEST_TASK_RUN:
            response = self.handle_test_task_run(message)
        if response:
            self.send(response)

    def provide_auth_info(self, message):
        response_auth = Message()
        response_auth.session_id = message.get('sessionId')
        if self.agent_user is None:
            self.agent_user = AgentUser()
        self.agent_user.id = "agent_id"
        self.agent_user.name = "agent_name"
        self.agent_user.secret = "agent_secret"
        self.agent_user.hostname = websocket_client.gethostname()
        self.agent_user.ip = "127.0.0.1"
        self.agent_user.os = platform.system()
        self.agent_user.version_name = "version_name"
        self.agent_user.version_code = "version_code"
        response_auth.body = self.agent_user.__dict__
        response_auth.path = message.get('path')
        self.send(response_auth)

    def heartbeat_response(self, message):
        agent_metadata = message.get('body')
        if not self.is_storage_client_init:
            # storage_service_client_proxy.init_agent_storage_client(agent_metadata['storageType'])
            self.is_storage_client_init = True
        # storage_service_client_proxy.update_access_token(agent_metadata['accessToken'])
        self.sync_agent_status(agent_metadata['agentUser'])
        self.prometheus_pushgateway_init(agent_metadata)
        self.app_center_reporter_init(agent_metadata)

    def sync_agent_status(self, passed_agent):
        self.agent_user.team_id = passed_agent['teamId']
        self.agent_user.team_name = passed_agent['teamName']
        self.agent_user.battery_strategy = passed_agent['batteryStrategy']

    def prometheus_pushgateway_init(self, agent_metadata):
        if self.is_prometheus_enabled:
            # push_gateway.set_connection_factory(...)
            logging.info("Pushgateway has set basic auth now, data can be pushed correctly.")

    def app_center_reporter_init(self, agent_metadata):
        # app_center_reporter.init_app_center_reporter(...)
        pass

    def handle_test_task_run(self, message):
        task = None
        try:
            test_task_spec = message.get('body')
            # test_task_spec.update_with_default_values()
            logging.info(f"TestTaskSpec: {test_task_spec}")
            # task = Task.RunnerType.valueOf(test_task_spec['runningType']).transfer_to_task(test_task_spec)
            # task = test_task_engine_service.run_test_task(task)
            response = Message(path=Const.Path.TEST_TASK_UPDATE, session_id=message.get('sessionId'), body={"task": "details"})
        except Exception as e:
            logging.error(e)
            response = Message(path=Const.Path.TEST_TASK_RETRY, session_id=message.get('sessionId'), body={"error": str(e)})
        return response

    def send(self, message):
        if self.send_message_callback:
            self.send_message_callback.send(message)

    def set_send_message_callback(self, callback):
        self.send_message_callback = callback

    def register_agent_metrics(self):
        logging.info("Metrics registered.")

class AgentWebSocketClient:
    def __init__(self, uri, agent_service):
        self.uri = uri
        self.agent_service = agent_service
        self.connection_active = False
        self.reconnect_time = 0
        self.agent_service.set_send_message_callback(self)

    async def connect(self):
        async with websockets.connect(self.uri) as websocket:
            self.websocket = websocket
            await self.on_open()
            try:
                while True:
                    message = await websocket.recv()
                    if isinstance(message, bytes):
                        await self.on_message(message)
                    else:
                        await self.on_message_str(message)
            except websockets.ConnectionClosed as e:
                await self.on_close(e.code, e.reason, e.was_clean)
            except Exception as e:
                await self.on_error(e)

    async def on_open(self):
        self.connection_active = True
        logging.info("Connection opened")

    async def on_message(self, bytes_message):
        self.connection_active = True
        message = SerializeUtil.byte_arr_to_message(bytes_message)
        self.agent_service.on_message(message)
        if message.get('path') == Const.Path.DEVICE_LIST:
            self.reconnect_time = 0

    async def on_message_str(self, message):
        self.connection_active = True
        logging.info(f"Received string message: {message}")

    async def on_close(self, code, reason, remote):
        logging.error(f"Connection closed: {code}, {reason}, {remote}")
        self.reconnect_time += 1
        self.connection_active = False
        if code in (1008, 1003):
            exit(code)

    async def on_error(self, exception):
        logging.error(f"Error: {exception}")
        self.reconnect_time += 1
        self.connection_active = False

    async def send(self, message):
        data = SerializeUtil.message_to_byte_arr(message.to_dict())
        logging.info(f"Sending message, path: {message.path}, data length: {len(data)}")
        await self.retry_and_sleep(self.websocket.send(data), 3, 10)

    async def retry_and_sleep(self, func, retries, sleep_time):
        for _ in range(retries):
            try:
                await func
                return
            except Exception as e:
                logging.error(f"Error sending message: {e}")
                await asyncio.sleep(sleep_time)
        raise Exception("Failed to send message")
