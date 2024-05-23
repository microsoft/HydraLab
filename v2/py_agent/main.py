import asyncio
import websockets

async def connect_to_websocket():
    uri = "ws://localhost:8081/agent/connect"
    async with websockets.connect(uri) as websocket:
        # 发送消息给服务器
        await websocket.send("Hello, WebSocket server!")
        print("Message sent to the server.")

        # 接收来自服务器的消息
        response = await websocket.recv()
        print(f"Message received from the server: {response}")

# 运行事件循环来执行协程
asyncio.get_event_loop().run_until_complete(connect_to_websocket())
