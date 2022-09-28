// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import 'bootstrap/dist/css/bootstrap.css'
import React from 'react'
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import { withStyles } from '@material-ui/core/styles';

/**
 * Palette
 * https://material-ui.com/customization/palette/
 */
const StyledTableCell = withStyles((theme) => ({
    head: {
        backgroundColor: theme.palette.primary.main,
        color: theme.palette.common.white,
    },
    body: {
        fontSize: 14,
    },
}))(TableCell);

const StyledTableRow = withStyles((theme) => ({
    root: {
        '&:nth-of-type(odd)': {
            backgroundColor: theme.palette.action.hover,
        },
    },
}))(TableRow);
export default class VideoNavView extends React.Component {
    state = {
        currentPlayTest: null,
        currentSpeed: 1,
        widthPx: 600,
        heightPx: 800
    }

    render() {
        const details = this.props.videos
        const info = this.props.videoInfo
        const thisEleObj = this
        const title = 'Test Video Navigator'
        let element = null;
        const re = /\d+\..*/;
        const processedInfo = []
        info.forEach((i) => {
            // console.log(i)
            const testDesc = Object.keys(i)[0];
            const timeValueMillis = i[Object.keys(i)[0]];
            if (re.test(testDesc)) {
                // element start
                element = {}
                element.testDesc = testDesc
                element.startTime = timeValueMillis
            } else if (testDesc.endsWith('.end')) {
                element.endTime = timeValueMillis
                processedInfo.push(element)
                element = null
            } else if (testDesc.endsWith('.fail')) {
                if (element) {
                    element.failed = true
                }
            } else {
                if (!element) {
                    element = {}
                    element.testDesc = testDesc
                    element.startTime = timeValueMillis
                    processedInfo.push(element)
                    element = null
                }
            }
        })
        return <center>
            <TableContainer component={Paper}>
                <Table aria-label="simple table" size="small">
                    <TableHead>
                        <TableRow key="title-1">
                            <StyledTableCell colSpan={2}><b>{title}</b></StyledTableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        <StyledTableRow  >
                            <TableCell align='center'>
                                <div className='mt-1' style={{ maxHeight: '680px', overflowY: 'auto' }}>
                                    <table className='table table-hover table-striped table-sm' style={{ fontSize: '0.8rem' }}>
                                        <thead>
                                            <tr className='table-primary'>
                                                <th>Navigator: Events</th>
                                                <th>Time</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {
                                                processedInfo.map((i, index) => {
                                                    var testDesc = i.testDesc
                                                    var time = (i.startTime / 1000).toFixed(1)
                                                    var saveInf = thisEleObj.state.currentPlayTest
                                                    var showCurrentBg = false
                                                    if (saveInf) {
                                                        showCurrentBg = saveInf.testDesc === testDesc
                                                    }
                                                    var min = Math.floor(time / 60)
                                                    var sec = Math.floor(time % 60)
                                                    return <tr className={showCurrentBg ? 'table-info' : null} key={testDesc + index}
                                                        onClick={function () {
                                                            document.querySelector('.test_video').currentTime = time
                                                        }} style={i.failed ? { color: 'red' } : null}>
                                                        <td>
                                                            {testDesc}
                                                        </td>
                                                        <td>
                                                            {min}:{sec >= 10 ? sec : ('0' + sec)}
                                                        </td>
                                                    </tr>
                                                })
                                            }
                                        </tbody>
                                    </table>
                                </div>
                            </TableCell>
                            {details.map((link, index) => {
                                const vElement = <video style={{width: thisEleObj.state.widthPx+"px", height: thisEleObj.state.heightPx+"px"}}
                                                        controls key={index}
                                                        className='test_video'
                                                        onCanPlay={function () {
                                                            const vDom = document.querySelector('.test_video');
                                                            thisEleObj.decideVideoBoxSizeAndUpdate(vDom.videoWidth, vDom.videoHeight)
                                                            vDom.defaultPlaybackRate = thisEleObj.state.currentSpeed
                                                            vDom.playbackRate = thisEleObj.state.currentSpeed
                                                            vDom.ontimeupdate = function () {
                                                                const ct = document.querySelector('.test_video').currentTime
                                                                for (var j in processedInfo) {
                                                                    var inf = processedInfo[j]
                                                                    const tt = (inf.startTime / 1000).toFixed(1)
                                                                    if (ct <= tt) {
                                                                        var highlightTest = processedInfo[j - 1 > 0 ? j - 1 : 0]
                                                                        if (!thisEleObj.state.currentPlayTest) {
                                                                            thisEleObj.setState({
                                                                                currentPlayTest: highlightTest
                                                                            })
                                                                            console.log('update')
                                                                        }
                                                                        var savedDesc = thisEleObj.state.currentPlayTest.testDesc
                                                                        if (savedDesc !== highlightTest.testDesc) {
                                                                            thisEleObj.setState({
                                                                                currentPlayTest: highlightTest
                                                                            })
                                                                            console.log('update: ' + savedDesc)
                                                                        }
                                                                        return
                                                                    }
                                                                }
                                                            };
                                                        }}>
                                    <source src={link} type="video/mp4"/>
                                </video>;
                                const speeds = [0.5, 1, 1.5, 2];
                                return <TableCell align='center' key={link}>
                                    {vElement}
                                    <div className='mt-1' style={{ fontSize: '1.05rem' }}>
                                        <a className='badge badge-secondary m-1' href={link} download>Download Video</a>
                                        <span className='badge badge-info'>Play speed: {this.state.currentSpeed}x</span>
                                        {speeds.map((s) => <button className='badge badge-warning ml-1' key={s} onClick={() => {
                                            console.log('New speed: ' + s)
                                            thisEleObj.setState({
                                                currentSpeed: s
                                            })
                                            const vDom = document.querySelector('.test_video');
                                            vDom.defaultPlaybackRate = s
                                            vDom.playbackRate = s
                                        }}>{s}x</button>)}
                                    </div>
                                </TableCell>
                            })}
                        </StyledTableRow>
                    </TableBody>
                </Table>
            </TableContainer>
            <table className="table table-borderless">
                <tbody>
                    <tr>
                        <td align='center'>
                        </td>
                    </tr>
                </tbody>
            </table>
        </center>
    }

    decideVideoBoxSizeAndUpdate(videoWidth, videoHeight) {
        const maxW = 1000
        const maxH = 900
        var targetW = videoWidth > maxW ? maxW : videoHeight
        var targetH = targetW / videoWidth * videoHeight
        if (targetH > maxH) {
            targetH = maxH
            targetW = targetH / videoHeight * videoWidth
        }
        if (targetH != this.state.heightPx) {
            this.setState({
                heightPx: targetH,
                widthPx: targetW
            })
        }
    }

    componentDidMount() {
    }

}