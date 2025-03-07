import React, { useEffect, useState } from 'react';
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import axios from '@/axios';
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import { AspectRatio } from 'react-aspect-ratio';

const DeviceDialog = ({ open, onClose, selectedDevice }) => {
    const [count, setCount] = useState(0);
    const [selectedDeviceWidth, setSelectedDeviceWidth] = useState(0);
    const [selectedDeviceRatio, setSelectedDeviceRatio] = useState(0);

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
            setCount(0); // reset count
        }

        return () => clearInterval(interval);
    }, [open]);

    const handleDeviceClick = (e) => {
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
            const interval = setInterval(() => {
                setCount(prevCount => prevCount + 1);
            }, 300); // 每秒自增一次

            setTimeout(() => {
                clearInterval(interval);
            }, 2000); // 2秒后停止计数器
        })
    };

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
                        <img style={{ maxWidth: '448px' , border:"1px solid #000"}} src={selectedDevice.screenshotImageUrl + '?rand=' + count + '&' + require('local-storage').get('FileToken') }
                            alt="Android Phone" 
                            onClick={handleDeviceClick}
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