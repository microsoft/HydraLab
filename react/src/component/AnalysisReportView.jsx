// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React, { PureComponent, useEffect } from 'react'
import { Link } from "react-router-dom";
import 'bootstrap/dist/css/bootstrap.css'
import _ from 'lodash';
import { AreaChart, Area, XAxis, YAxis, Tooltip, PieChart, Pie, Cell, BarChart, CartesianGrid, Legend, Bar } from 'recharts';
import moment from 'moment';
import CloudDownloadIcon from '@material-ui/icons/CloudDownload';
import Button from "@mui/material/Button";
import axios from "@/axios";
import BaseView from "@/component/BaseView";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import IconButton from "@mui/material/IconButton";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import { withStyles } from '@material-ui/core/styles';
import { default as MUITooltip } from "@mui/material/Tooltip";
import Stack from "@mui/material/Stack";
import copy from 'copy-to-clipboard';

const COLORS = ['#00C49F', '#FF8042', '#0088FE', '#FFBB28'];

class CustomizedLabel extends PureComponent {
    render() {
        const { x, y, stroke, value } = this.props;

        return (
            <text x={x} y={y} dy={-6} fill={stroke} fontSize={12} textAnchor="middle">
                {value}
            </text>
        );
    }
}
const formatDate = "MM/DD HH:mm"
const PieCustomizedLabel = ({ name, value }) => {
    let sizeString = "0B";
    var num = 1024.00; //byte
    if (value < num)
        sizeString = value + "B";
    else if (value < Math.pow(num, 2))
        sizeString = (value / num).toFixed(2) + "KB";
    else if (value < Math.pow(num, 3))
        sizeString = (value / Math.pow(num, 2)).toFixed(2) + "MB";
    else if (value < Math.pow(num, 4))
        sizeString = (value / Math.pow(num, 3)).toFixed(2) + "GB";
    else if (value >= Math.pow(num, 4))
        sizeString = (value / Math.pow(num, 4)).toFixed(2) + "TB";
    const labelName = name + ' ' + sizeString;
    return labelName;
};

const PieCustomizedToolTip = ({ active, payload }) => {
    if (active && payload && payload.length) {
        let name = payload[0].name;
        let value = payload[0].value;
        let sizeString = "0B";
        var num = 1024.00; //byte
        if (value < num)
            sizeString = value + "B";
        else if (value < Math.pow(num, 2))
            sizeString = (value / num).toFixed(2) + "KB";
        else if (value < Math.pow(num, 3))
            sizeString = (value / Math.pow(num, 2)).toFixed(2) + "MB";
        else if (value < Math.pow(num, 4))
            sizeString = (value / Math.pow(num, 3)).toFixed(2) + "GB";
        else if (value >= Math.pow(num, 4))
            sizeString = (value / Math.pow(num, 4)).toFixed(2) + "TB";
        const labelName = name + ' ' + sizeString;
        return labelName;
    }

    return null;
};
const StyledTableCell = withStyles((theme) => ({
    head: {
        backgroundColor: theme.palette.primary.dark,
        color: theme.palette.common.white,
    },
    body: {
        fontSize: 14,
    },
}))(TableCell);

const StyledTableRow = withStyles((theme) => ({
    root: {
        '&:nth-of-type(odd)': {
            backgroundColor: theme.palette.action.selected,
        },
    },
}))(TableRow);
export default class AnalysisReportView extends BaseView {
    state = {
        task: this.props.testTask,
        lastTask: null,
        overNode: null,
        selectedPath: [],
        canaryFileId: null,
        canaryReport: [],
        renderer: null,
        hoveredNode: null,
        hoveredNeighbors: undefined,
        attachmentsDiaglogIsShow: false,
        attachmentsRows: [],
    };

    render() {
        console.log("render")
        const task = this.state.task
        const lastTask = this.state.lastTask
        const canaryReport = this.state.canaryReport
        const taskRun = task.taskRunList[0]
        const taskResult = taskRun ? taskRun.taskResult : null
        const apkSizeReport = taskResult ? taskResult.apkSizeReport : null
        const leakReport = taskResult ? taskResult.leakInfoList : null
        const chartData = apkSizeReport ? [
            { name: 'dex', value: apkSizeReport.dexSize },
            { name: 'arsc', value: apkSizeReport.arscSize },
            { name: 'so', value: apkSizeReport.soSize },
            { name: 'png', value: apkSizeReport.pngSize },
            { name: 'xml', value: apkSizeReport.xmlSize },
            { name: 'webg', value: apkSizeReport.webgSize },
            { name: 'other', value: apkSizeReport.otherSize },
        ] : []
        const dexReport = canaryReport && canaryReport.length > 0 ? canaryReport.filter(report => 4 == report.taskType) : null
        const dexData = dexReport && dexReport.length > 0 ? dexReport[0].groups.slice(0, 15) : []
        const duplicatedFiles = apkSizeReport ? apkSizeReport.duplicatedFileList : []
        const largeFiles = apkSizeReport ? apkSizeReport.bigSizeFileList : []

        const lastSizeReport = lastTask && lastTask.taskRunList && lastTask.taskRunList[0] && lastTask.taskRunList[0].taskResult && lastTask.taskRunList[0].taskResult.apkSizeReport ? lastTask.taskRunList[0].taskResult.apkSizeReport : null

        const totalSizeDiff = lastSizeReport && apkSizeReport && apkSizeReport.totalSize > 0 && lastSizeReport.totalSize > 0 ? apkSizeReport.totalSize - lastSizeReport.totalSize : 0
        const downloadSizeDiff = lastSizeReport && apkSizeReport && apkSizeReport.downloadSize > 0 && lastSizeReport.downloadSize > 0 ? apkSizeReport.downloadSize - lastSizeReport.downloadSize : 0

        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state
        const { attachmentsDiaglogIsShow } = this.state
        const attachmentsHeads = []
        const attachmentsHeadItems = ['File Name', 'File Size', 'Actions']

        const overallPieSize = 220
        let overallAreaWidth = overallPieSize * 2;
        const dtrSuccFailMap = _.groupBy(task.taskRunList, 'success')

        var suggestions = [];
        console.log('suggestions:', suggestions);
        attachmentsHeadItems.forEach((k) => attachmentsHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))
        return <div id='test_report' style={{ padding: '20px' }}>
            <div id='test_report_head'>
                <table className='table table-borderless'>
                    <thead>
                        <tr className="table-info">
                            <th colSpan='2' style={{ backgroundColor: '4472C4', lineHeight: '100%' }}>
                                <center><p className='mt-4'
                                    style={{ color: 'white', fontSize: '2.5rem' }}>Hydra Lab Package Scan Report</p></center>
                                <p style={{
                                    color: 'white',
                                    textAlign: 'right',
                                    fontSize: '1.1rem'
                                }}>{moment(task.startDate).format("MMM Do HH:mm")}</p>
                            </th>
                        </tr>
                        <tr style={{ backgroundColor: '#2F5496' }}>
                            <th colSpan='2' className="table-info"
                                style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                Overview
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style={{ width: '500px' }}>
                                <h5 className='mt-1'>Scan <b>{task.taskAlias}</b></h5>
                                <h5>Triggerd by <span
                                    className='badge badge-info'>{task.triggerType}</span> <span
                                        className='badge badge-info'>{task.status.toUpperCase()}</span></h5>
                                <h6>{moment(task.startDate).format("yyyy-MM-DD")}, {moment(task.startDate).format("HH:mm:ss")} ~ {moment(task.endDate).format("HH:mm:ss")}
                                    <span className='badge badge-primary ml-2'
                                        style={{ fontSize: '0.9rem' }}>{((task.endDate - task.startDate) / 1000).toFixed(1)}s</span>
                                </h6>
                                <Link to={"/info/task/" + task.id} target='_blank' rel="noopener noreferrer">
                                    <Button variant="outlined" color="info">This Report Link</Button>
                                </Link>
                                {task.pipelineLink ?
                                    <p className='mt-3'><a href={task.pipelineLink} rel="noopener noreferrer">Link to PipeLine</a></p> : null}

                                {taskRun ? <p className='mt-1'>
                                    <CloudDownloadIcon className='ml-1 mr-1'
                                        style={{ height: '21px' }} />
                                    <a className='badge badge-light m-1' target='_blank'
                                        href={this.getFileDownloadUrlAndDownload(taskRun.instrumentReportPath)} download rel="noopener noreferrer">Agent Log</a>
                                    <IconButton id={taskRun.id} onClick={() => {
                                        const tempAttachmentsRows = [];
                                        taskRun.attachments.forEach((t) => {
                                            tempAttachmentsRows.push(<StyledTableRow key={t.fileId} id={t.fileId} hover>
                                                <TableCell id={t.fileId} align="center">
                                                    {t.fileName}
                                                </TableCell>
                                                <TableCell id={t.fileId} align="center">
                                                    {this.getfilesize(t.fileLen)}
                                                </TableCell>
                                                <TableCell id={t.fileId} align="center">
                                                    <IconButton id={t.fileId} href={this.getFileDownloadUrlAndDownload(t.blobPath)}>
                                                        <span id={t.fileId} className="material-icons-outlined">download</span>
                                                    </IconButton>
                                                </TableCell>
                                            </StyledTableRow>)

                                        })
                                        this.setState({ attachmentsRows: tempAttachmentsRows });
                                        this.handleStatus("attachmentsDiaglogIsShow", true)
                                    }}>
                                        <span id={taskRun.id} className="material-icons-outlined">download</span>
                                    </IconButton>
                                </p> : null}

                            </td>
                            <td>
                                <h4>Scan result summary</h4>
                                {taskResult ? <li> Package info: {taskResult.buildInfo.buildFlavor ?
                                    <span> flavor is <b style={{ fontSize: '20' }}> {taskResult.buildInfo.buildFlavor} </b>, </span>
                                    : null}
                                    version name is <b style={{ fontSize: '20' }}>{taskResult.apkManifest.versionName}</b>, version code is <b style={{ fontSize: '20' }}>{taskResult.apkManifest.versionCode}</b>.</li> : null}
                                {apkSizeReport ? <li> The total size of the package is <b style={{ fontSize: '20' }}>{this.fileSizeInMB(apkSizeReport.totalSize)}MB</b>.
                                    There are <b style={{ fontSize: '20' }}>{duplicatedFiles.length}</b>  duplicated files and <b style={{ fontSize: '20' }}>{largeFiles.length}</b> large files in the package.</li> : null}

                                {leakReport ? <li> There are <b style={{ fontSize: '20' }}> {leakReport.length} </b> code leaks in the package</li> : null}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            {apkSizeReport ?
                <div id='size_report_content>'>
                    <div>
                        <table className='table table-borderless'>
                            <thead className="thead-info">
                                <tr className="table-info">
                                    <th style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                        APK size report
                                    </th>
                                </tr>
                            </thead>
                        </table>
                        <table className='table table-borderless'>
                            <tbody>
                                <tr>
                                    <td>
                                        <PieChart width={overallAreaWidth} height={overallPieSize}>
                                            <Pie
                                                data={chartData}
                                                labelLine={true}
                                                label={PieCustomizedLabel}
                                                dataKey="value">

                                                {chartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip
                                                content={PieCustomizedToolTip}
                                            > </Tooltip>
                                        </PieChart>
                                    </td>
                                    <td>
                                        <div><span style={{ fontSize: '80' }}>{this.fileSizeInMB(apkSizeReport.totalSize)}</span><span style={{ fontSize: '20' }}>MB</span></div>
                                        <div style={{ paddingTop: '10px' }}><span style={{ fontSize: '30' }}>Total Package Size</span></div>
                                        <div style={{
                                            color: totalSizeDiff <= 0 ?
                                                totalSizeDiff == 0 ? '#a9a9a9' : 'green'
                                                : 'red'
                                        }}><span >Compared to previous: {this.getArrow(totalSizeDiff)}</span>  {this.getfilesize(Math.abs(totalSizeDiff))}</div>
                                    </td>
                                    <td>
                                        <div><span style={{ fontSize: '80' }}>{this.fileSizeInMB(apkSizeReport.downloadSize)}</span><span style={{ fontSize: '20' }}>MB</span></div>
                                        <div style={{ paddingTop: '10px' }}><span style={{ fontSize: '30' }}>Package Download Size</span></div>
                                        <div style={{
                                            color: downloadSizeDiff <= 0 ?
                                                downloadSizeDiff == 0 ? '#a9a9a9' : 'green'
                                                : 'red'
                                        }}><span>Compared to previous: {this.getArrow(downloadSizeDiff)}</span>  {this.getfilesize(Math.abs(downloadSizeDiff))}</div>
                                    </td>
                                </tr>

                            </tbody>
                        </table>
                        <table className='table table-borderless'>
                            <tbody>
                                <tr>
                                    <td>
                                        <h4><span>Duplicated files whose size &gt; 20KB (Top 5)</span></h4>
                                    </td>
                                    <td>
                                        <h4><span>Large files whose size &gt; 100KB (Top 5)</span></h4>
                                    </td>
                                </tr>
                                <tr>
                                    <td style={{ width: '50%' }}>
                                        <table className='table table-hover table-striped table-sm table-border'>
                                            <tbody>
                                                <tr>
                                                    <th style={{ color: 'white', textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse', backgroundColor: '#156082' }}>Item Paths</th>
                                                    <th style={{ color: 'white', textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse', backgroundColor: '#156082' }}>Size</th>
                                                </tr>
                                                {duplicatedFiles.filter(file => file.size > 20480).slice(0, 5).map((d, index) => {
                                                    return <tr key={index}>
                                                        <td style={{ textAlign: 'left', border: '3px solid white', borderCollapse: 'collapse', whiteSpace: 'pre-line' }}>{d.fileList.join("\n")}</td>
                                                        <td style={{ textAlign: 'center', verticalAlign: 'middle', border: '3px solid white', borderCollapse: 'collapse' }}>{this.getfilesize(d.size)}</td>
                                                    </tr>
                                                })}
                                            </tbody>
                                        </table>
                                    </td>
                                    <td style={{ width: '50%' }}>
                                        <table className='table table-hover table-striped table-sm' >
                                            <tbody>
                                                <tr>
                                                    <th style={{ color: 'white', textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse', backgroundColor: '#156082' }}>Item Path</th>
                                                    <th style={{ color: 'white', textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse', backgroundColor: '#156082' }}>Size</th>
                                                </tr>
                                                {largeFiles.filter(file => file.size > 102400).slice(0, 5).map((d, index) => {
                                                    return <tr key={index}>
                                                        <td style={{ textAlign: 'left', border: '3px solid white', borderCollapse: 'collapse' }}>{d.fileName}</td>
                                                        <td style={{ textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse' }}>{this.getfilesize(d.size)}</td>
                                                    </tr>
                                                })}
                                            </tbody>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td colSpan='2'>
                                        <h4><span>Method count in DEX</span></h4>

                                        <BarChart width={1100} height={400} data={dexData}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis dataKey="name" />
                                            <YAxis dataKey="method-count" />
                                            <Tooltip />
                                            <Legend />
                                            <Bar dataKey="method-count" fill="#8884d8" />
                                        </BarChart>
                                    </td>
                                </tr>
                                <tr>

                                </tr>
                            </tbody>

                        </table>
                    </div>
                </div>
                : null}
            {leakReport ?
                <div id='leak_report_content>'>
                    <div>
                        <table className='table table-borderless'>
                            <thead className="thead-info">
                                <tr className="table-info">
                                    <th style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                        APK code leaks report
                                    </th>
                                </tr>
                            </thead>
                        </table>
                        <table className='table table-hover table-striped table-sm' style={{ width: '100%', tableLayout: 'fixed' }}>
                            <tbody>
                                <tr>
                                    <th colSpan='1' style={{ color: 'white', textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse', backgroundColor: '#156082' }}>Category name</th>
                                    <th colSpan='4' style={{ color: 'white', textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse', backgroundColor: '#156082' }}>Secret Value</th>
                                </tr>
                                {leakReport.map((d, index) => {
                                    return <tr key={index}>
                                        <td colSpan='1' style={{ textAlign: 'center', border: '3px solid white', borderCollapse: 'collapse' }}>{d.keyword}</td>
                                        <td colSpan='4' style={{ textAlign: 'left', border: '3px solid white', borderCollapse: 'collapse', whiteSpace: 'pre-line' }}>
                                            {/* <div dangerouslySetInnerHTML={{
                                            __html: d.leakWordList.slice(0, 3).map(leakWord => {
                                                if (leakWord.length > 100) {
                                                    return leakWord.substring(0, 100) + "<MUITooltip<a href=''>more</a>";
                                                } else {
                                                    return leakWord;
                                                }
                                            }).join("\n")
                                        }} ></div> */}

                                            <div>
                                                {
                                                    d.leakWordList[0].length > 90 ?
                                                        <div><span> {d.leakWordList[0].substring(0, 90)} </span>
                                                            <MUITooltip
                                                                title={
                                                                    <Stack>
                                                                        {d.leakWordList[0]}
                                                                    </Stack>}
                                                                style={{ padding: "0" }}>
                                                                <IconButton>
                                                                    <span style={{ color: 'gary', }} className="material-icons-outlined">more_horiz</span>
                                                                </IconButton>
                                                            </MUITooltip>
                                                            <IconButton onClick={() => this.copyContent(d.leakWordList[0])}>
                                                                <span style={{ color: 'gary', }} className="material-icons-outlined">content_copy</span>
                                                            </IconButton>
                                                        </div>
                                                        :
                                                        d.leakWordList[0]
                                                }
                                            </div>
                                            {d.leakWordList[1] ?
                                                <div>
                                                    {
                                                        d.leakWordList[1].length > 90 ?
                                                            <div><span> {d.leakWordList[1].substring(0, 90)} </span>
                                                                <MUITooltip
                                                                    title={
                                                                        <Stack>
                                                                            {d.leakWordList[1]}
                                                                        </Stack>}
                                                                    style={{ padding: "0" }}>
                                                                    <IconButton>
                                                                        <span style={{ color: 'gary', }}
                                                                            className="material-icons-outlined">more_horiz</span>
                                                                    </IconButton>
                                                                </MUITooltip>
                                                                <IconButton onClick={() => this.copyContent(d.leakWordList[1])}>
                                                                    <span style={{ color: 'gary', }} className="material-icons-outlined">content_copy</span>
                                                                </IconButton>
                                                            </div>
                                                            :
                                                            d.leakWordList[1]
                                                    }
                                                </div>
                                                : null
                                            }
                                            {d.leakWordList[2] ?
                                                <div>
                                                    {
                                                        d.leakWordList[2].length > 90 ?
                                                            <div><span> {d.leakWordList[2].substring(0, 90)} </span>
                                                                <MUITooltip
                                                                    title={
                                                                        <Stack>
                                                                            {d.leakWordList[2]}
                                                                        </Stack>}
                                                                    style={{ padding: "0" }}>
                                                                    <IconButton>
                                                                        <span style={{ color: 'gary', }}
                                                                            className="material-icons-outlined">more_horiz</span>
                                                                    </IconButton>
                                                                </MUITooltip>
                                                                <IconButton onClick={() => this.copyContent(d.leakWordList[2])}>
                                                                    <span style={{ color: 'gary', }} className="material-icons-outlined">content_copy</span>
                                                                </IconButton>
                                                            </div>
                                                            :
                                                            d.leakWordList[2]
                                                    }
                                                </div>
                                                : null
                                            }
                                        </td>
                                    </tr>
                                })}

                            </tbody>
                        </table>
                    </div>
                </div>
                : null}
            <Dialog open={attachmentsDiaglogIsShow}
                fullWidth={true} maxWidth='lg'
                onClose={() => this.handleStatus("attachmentsDiaglogIsShow", false)}>
                <DialogTitle>Test Results</DialogTitle>
                <DialogContent>
                    <TableContainer style={{ margin: "auto" }}>
                        <Table size="medium">
                            <TableHead>
                                <TableRow>
                                    {attachmentsHeads}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {this.state.attachmentsRows}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => this.handleStatus("attachmentsDiaglogIsShow", false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
            <Snackbar
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'center'
                }}
                open={snackbarIsShown}
                autoHideDuration={3000}
                onClose={() => this.handleStatus("snackbarIsShown", false)}>
                <Alert
                    onClose={() => this.handleStatus("snackbarIsShown", false)}
                    severity={snackbarSeverity}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </div >
    }
    queryTaskHistory() {
        let queryParams = [
            {
                "key": "runnerType",
                "op": "equal",
                "value": this.state.task.runnerType
            },
            {
                "key": "taskAlias",
                "op": "equal",
                "value": this.state.task.taskAlias
            },
            {
                "key": "startDate",
                "op": "lt",
                "value": moment(this.state.task.startDate).format("YYYY-MM-DD HH:mm:ss.SSS"),
                "dateFormatString": "yyyy-MM-dd HH:mm:ss.SSS"
            }
        ]

        let postBody = {
            'page': 0,
            'pageSize': 6,
            'queryParams': queryParams
        }

        console.log(postBody)

        axios.post(`/api/test/task/listFirst`, postBody).then(res => {
            if (res.data && res.data.code === 200) {
                const lastTask = res.data.content;
                console.log(res.data)
                this.setState({
                    lastTask: lastTask
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }

    componentDidMount() {
        console.log("componentDidMount")
        console.log(this.props.testTask)
        this.queryTaskHistory()
        this.queryDexData()
        console.log(this.state.lastTask)
    }

    queryDexData() {
        if (!(this.props.testTask && this.props.testTask.taskRunList && this.props.testTask.taskRunList[0] && this.props.testTask.taskRunList[0].attachments)) {
            return;
        }
        this.props.testTask.taskRunList[0].attachments.forEach((attachment) => {
            if (attachment.fileName.endsWith('canary_report.json')) {
                this.state.canaryFileId = attachment.fileId
            }
        })
        axios.get("/api/test/loadCanaryReport/" + this.state.canaryFileId).then((res) => {
            if (res.data && res.data.code === 200) {
                const lastTask = res.data.content;
                console.log(res.data)
                this.setState({
                    canaryReport: res.data.content
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)

    }

    getArrow(size) {
        if (size == 0)
            return "--"
        else if (size > 0)
            return '↑'
        else
            return '↓'
    }

    getfilesize(size) {
        if (size <= 0)
            return '--';
        var num = 1024.00; //byte
        if (size < num)
            return size + "B";
        if (size < Math.pow(num, 2))
            return (size / num).toFixed(2) + "KB";
        if (size < Math.pow(num, 3))
            return (size / Math.pow(num, 2)).toFixed(2) + "MB";
        if (size < Math.pow(num, 4))
            return (size / Math.pow(num, 3)).toFixed(2) + "GB";
        return (size / Math.pow(num, 4)).toFixed(2) + "TB";
    }

    fileSizeInMB(size) {
        if (size <= 0)
            return '--';
        return (size / 1024 / 1024).toFixed(2)
    }

    copyContent(taskAlias) {
        copy(taskAlias)
        this.setState({
            snackbarIsShown: true,
            snackbarSeverity: "success",
            snackbarMessage: "suiteName copied!"
        })
    }
}