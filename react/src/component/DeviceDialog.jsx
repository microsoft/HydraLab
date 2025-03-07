import React, { useEffect, useState } from 'react';
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import axios from '@/axios';
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import { AspectRatio } from 'react-aspect-ratio';
import { set } from 'local-storage';

const DeviceDialog = ({ open, onClose, selectedDevice }) => {
    const [count, setCount] = useState(0);
    const [selectedDeviceWidth, setSelectedDeviceWidth] = useState(0);
    const [selectedDeviceRatio, setSelectedDeviceRatio] = useState(0);
    const [mouseDownTime, setMouseDownTime] = useState(null);
    const [startPosition, setStartPosition] = useState(null);

    useEffect(() => {
        let interval;
        if (open) {
            if (selectedDevice) {
                const deviceScreenLength = selectedDevice.screenSize.split("x");
                setSelectedDeviceWidth(deviceScreenLength[0]);
                setSelectedDeviceRatio(deviceScreenLength[0] / deviceScreenLength[1]);
                refreshDeviceScreen();
            }
        } else {
            setCount(0); // 重置计数器
        }

        return () => clearInterval(interval);
    }, [open]);

    const handleMouseDown = (e) => {
        setMouseDownTime(Date.now());
        setStartPosition({ x: e.clientX, y: e.clientY });
    };

    const handleMouseUp = (e) => {
        const pressDuration = Date.now() - mouseDownTime;
        const positionDistance = Math.sqrt(Math.pow(startPosition.x - e.clientX, 2) + Math.pow(startPosition.y - e.clientY, 2));
        if (positionDistance > 10) {
            handleSwipe(e);
        } else if (pressDuration < 500) {
            handleClick(e);
        } else {
            handleLongPress(e);
        }
    };

    const handleClick = (e) => {
        var positionX = e.clientX - e.target.getBoundingClientRect().left;
        var positionY = e.clientY - e.target.getBoundingClientRect().top;
        console.log("positionX: " + positionX + " positionY: " + positionY);
        var ratio = selectedDeviceWidth / 448;
        positionX = positionX * ratio;
        positionY = positionY * ratio;
        console.log("positionX in Phone: " + positionX + " positionY in Phone: " + positionY);
        console.log("ADB tab command is: adb shell input tap " + positionX + " " + positionY);
        // call /api/device/operate to send tap command
        const formParams = {
            deviceSerial: selectedDevice.serialNum,
            operationType: "TAP",
            fromPositionX:  positionX,
            fromPositionY:  positionY,
        }

        axios.post('/api/device/operate', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            refshImage();
        })
    };

    const handleLongPress = (e) => {
        var positionX = e.clientX - e.target.getBoundingClientRect().left;
        var positionY = e.clientY - e.target.getBoundingClientRect().top;
        console.log("Long Click positionX: " + positionX + " positionY: " + positionY);
        var ratio = selectedDeviceWidth / 448;
        positionX = positionX * ratio;
        positionY = positionY * ratio;
        console.log("Long Click positionX in Phone: " + positionX + " positionY in Phone: " + positionY);
        console.log("ADB long click command is: adb shell input swipe " + positionX + " " + positionY + " " + positionX + " " + positionY + " 2000");
        // call /api/device/operate to send long click command
        const formParams = {
            deviceSerial: selectedDevice.serialNum,
            operationType: "LONG_TAP",
            fromPositionX:  positionX,
            fromPositionY:  positionY,
        }

        axios.post('/api/device/operate', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            refshImage();
        })
    };

    const handleSwipe = (e) => {
        debugger
        var startX = startPosition.x - e.target.getBoundingClientRect().left;
        var startY = startPosition.y - e.target.getBoundingClientRect().top;
        var endX = e.clientX - e.target.getBoundingClientRect().left;
        var endY = e.clientY - e.target.getBoundingClientRect().top;
        console.log("Swipe startX: " + startX + " startY: " + startY + " endX: " + endX + " endY: " + endY);
        var ratio = selectedDeviceWidth / 448;
        startX = startX * ratio;
        startY = startY * ratio;
        endX = endX * ratio;
        endY = endY * ratio;
        console.log("Swipe startX in Phone: " + startX + " startY in Phone: " + startY + " endX in Phone: " + endX + " endY in Phone: " + endY);
        console.log("ADB swipe command is: adb shell input swipe " + startX + " " + startY + " " + endX + " " + endY);
        // call /api/device/operate to send swipe command
        const formParams = {
            deviceSerial: selectedDevice.serialNum,
            operationType: "SWIPE",
            fromPositionX:  startX,
            fromPositionY:  startY,
            toPositionX:  endX,
            toPositionY:  endY,
        }

        axios.post('/api/device/operate', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            refshImage();
        })
    };

    const refshImage = () => {
        const interval = setInterval(() => {
            setCount(prevCount => prevCount + 1);
        }, 300);

        setTimeout(() => {
            clearInterval(interval);
        }, 3000);
    }
    
    const refreshDeviceScreen = () => {
        const formParams = {
            deviceSerial: selectedDevice.serialNum,
            operationType: "WAKEUP"
        }
        axios.post('/api/device/operate', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            const interval = setInterval(() => {
                setCount(prevCount => prevCount + 1);
            }, 300); // refresh every 0.3s

            setTimeout(() => {
                clearInterval(interval);
            }, 3000); // stop after 3s
        })
    }

    return (
        <Dialog open={open}
                fullWidth
                maxWidth="sm"
                onClose={onClose}
                style={{alignItems: "center"}}
        >
            <DialogTitle>
                {selectedDevice ? selectedDevice.serialNum : ''}
                <Button onClick={refreshDeviceScreen} style={{ float: 'right' }}>
                    Refresh
                </Button>
            </DialogTitle>
            <div style={{display: "flex", justifyContent: "center"}}>
                <div className='deviceScreen' style={{width: `450px`, alignItems: "center", justifyContent: "center", border:"1px solid #000"}}>
                    <AspectRatio ratio={selectedDeviceRatio} style={{alignItems: "center", justifyContent: "center"}}>
                        <img style={{ maxWidth: '448px' , border:"1px solid #000"}} 
                            src={selectedDevice.screenshotImageUrl + '?rand=' + count + '&' + require('local-storage').get('FileToken') }
                            alt="Android Phone" 
                            draggable="false"
                            onMouseDown={handleMouseDown}
                            onMouseUp={handleMouseUp}
                            />
                    </AspectRatio>
                </div>
            </div>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
            </DialogActions>
        </Dialog>
    );
};

export default DeviceDialog;