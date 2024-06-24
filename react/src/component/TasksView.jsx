// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import * as React from 'react';
import axios from '@/axios'
import Table from '@mui/material/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Typography from '@material-ui/core/Typography';
import TestReportView from '@/component/TestReportView';
import AnalysisReportView from '@/component/AnalysisReportView';
import 'bootstrap/dist/css/bootstrap.css'
import {Cell, Pie, PieChart} from 'recharts';
import moment from 'moment';
import Select from '@mui/material/Select';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Skeleton from "@mui/material/Skeleton";
import Alert from "@mui/material/Alert";
import Snackbar from "@mui/material/Snackbar";

import Box from '@mui/material/Box';

import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";

import Button from "@mui/material/Button";
import {Backdrop, CircularProgress} from "@mui/material";
import BaseView, {darkTheme, StyledTableCell, StyledTableRow} from "@/component/BaseView";
import Pagination from '@mui/material/Pagination';
import Stack from '@mui/material/Stack';
import {withRouter} from 'react-router-dom';
import Checkbox from '@mui/material/Checkbox';
import ListItemText from '@mui/material/ListItemText';
import TextField from "@mui/material/TextField";
import {ThemeProvider} from '@mui/material/styles';
import IconButton from '@mui/material/IconButton';
import copy from 'copy-to-clipboard';

const pieCOLORS = ['#00C49F', '#FF8042', '#808080'];
const taskRowHeight = 35

const allTag = 'All'
const TestType = {
    "INSTRUMENTATION": "Espresso",
    "APPIUM": "Appium",
    "SMART": "Smart",
    "MONKEY": "Monkey",
    "APPIUM_MONKEY": "Appium Monkey",
    "APPIUM_CROSS": "Appium E2E",
    "T2C_JSON": "JSON-Described Test",
    "XCTEST": "XCTest",
    "MAESTRO":"Maestro",
    "PYTHON": "Python",
    "APK_SCANNER": "APK Scanner",
    "All": "All"
}

let params = {
    Timestamp: ["Last 24 Hours", "Last 7 Days", "Last 30 Days", "All"],
    TestType: ["INSTRUMENTATION", "APPIUM", "SMART", "MONKEY", "APPIUM_MONKEY", "APPIUM_CROSS", "T2C_JSON", "XCTEST", "MAESTRO", "PYTHON","APK_SCANNER"],
    Result: ["Passed", "Failed"],
    TriggerType: ["PullRequest", "IndividualCI", "API"]
};

let defaultSelectedParams = {
    time: "Last 24 Hours",
    suite: '',
    TestType: ["INSTRUMENTATION", "APPIUM", "SMART", "MONKEY", "APPIUM_MONKEY", "APPIUM_CROSS", "T2C_JSON", "XCTEST", "MAESTRO", "PYTHON","APK_SCANNER"],
    Result: ["Passed", "Failed"],
    TriggerType: ["PullRequest", "IndividualCI", "API"]
}

let ls = require('local-storage');

class TasksView extends BaseView {
    constructor(props) {
        super(props);
        this.state = {
            tasks: null,
            testDetailInfo: null,

            displayReportTaskId: null,
            runningTasks: null,
            queuedTasks: null,
            loading: false,
            showingType: allTag,
            showingTestType: allTag,
            allTypes: null,
            allTestTypes: null,

            hideSkeleton: false,
            showBackDrop: false,

            rerunTestDialogIsShown: false,

            openTestDetail: false,
            pageCount: 20,
            page: this.props.page ? this.props.page : 1,

            testTimeOutSec: null,
            testRunArgs: null,

            selectedParams: ls.get('selectedParams') ? ls.get('selectedParams') : defaultSelectedParams,
        };
        this.lastClick= null;
    }

    render() {
        let tasks = this.state.tasks;
        const runningTasks = this.state.runningTasks
        const queuedTasks = this.state.queuedTasks
        const rows = []
        const heads = []
        const selectedParams = this.state.selectedParams
        const thisEleObj = this

        let tableBodyHeight = 900

        // const headItems = ['Time', 'Suite', 'Devices', 'Test Type', 'Success/Total', 'Trigger Type']
        // const headItems = ['Devices', 'Test Type', 'Success/Total', 'Trigger Type']
        if (tasks) {
            tasks.forEach((t) => {
                rows.push(thisEleObj.getTaskRow(t, false))
            })

            if (runningTasks) {
                runningTasks.forEach((rt) => {
                    rows.unshift(thisEleObj.getTaskRow(rt, true))
                })
            }
            if (queuedTasks) {
                queuedTasks.forEach((rt) => {
                    rows.unshift(thisEleObj.formatTaskSpecRow(rt))
                })
            }
            if (rows.length * 90 < tableBodyHeight) {
                tableBodyHeight = rows.length * 90
            }

            heads.push(
                <StyledTableCell key={'Timestamp'} align="center">
                    <ThemeProvider theme={darkTheme}>
                        <FormControl className="ml-0" fullWidth={true}>
                            <InputLabel id="start-time-range-select-label">Start Time Range</InputLabel>
                            <Select
                                labelId="start-time-range-select-label"
                                id="start-time-range-select"
                                label="Start Time Range"
                                size="small"
                                value={selectedParams.time}
                                onChange={this.selectTimeChange}
                            >
                                {params.Timestamp.map((time, index) => (
                                    <MenuItem value={time} key={time}>{time}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </ThemeProvider>
                </StyledTableCell>
            )

            heads.push(
                <StyledTableCell key={'Suite'} align="center">
                    <ThemeProvider theme={darkTheme}>
                        <FormControl className="ml-0" fullWidth={true}>
                            <TextField label="Suite" size="small" defaultValue={this.state.selectedParams.suite} onBlur={this.selectSuiteChange} />
                        </FormControl>
                    </ThemeProvider>
                </StyledTableCell>
            )

            heads.push(
                <StyledTableCell key={'TestType'} align="center">
                    <ThemeProvider theme={darkTheme}>
                        <FormControl className="ml-0" fullWidth={true}>
                            <InputLabel id="test-type-select-label" >Task Type</InputLabel>
                            <Select
                                labelId="test-type-select-label"
                                id="test-type-select"
                                label="Task Type"
                                size="small"
                                multiple
                                value={selectedParams.TestType}
                                onChange={this.selectTestTypeChange}
                                onClose={() => { this.queryTask() }}
                                renderValue={(selected) => {
                                    if (selected.length === 0) {
                                        return 'Placeholder';
                                    } else if (selected.length <= 2) {
                                        return selected.map(type => TestType[type]).join(', ');
                                    } else if (selected.length === params.TestType.length) {
                                        return 'All';
                                    } else {
                                        return `${selected.length} Selected`;
                                    }
                                }}
                            >
                                {params.TestType.map((type, index) => (
                                    <MenuItem key={type} value={type} onClick={ () => { this.lastClick = type } }>
                                        <Checkbox checked={selectedParams.TestType.indexOf(type) > -1} />
                                        <ListItemText primary={TestType[type]} />
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </ThemeProvider>
                </StyledTableCell>
            )
            heads.push(
                <StyledTableCell key={'Result'} align="center">
                    <ThemeProvider theme={darkTheme}>
                        <FormControl className="ml-0" fullWidth={true}>
                            <InputLabel id="result-select-label" >Result</InputLabel>
                            <Select
                                labelId="result-select-label"
                                id="result-select"
                                label="Result"
                                size="small"
                                multiple
                                value={selectedParams.Result}
                                onChange={this.selectResultChange}
                                renderValue={(selected) => {
                                    if (selected.length === 0) {
                                        return 'Placeholder';
                                    } else if (selected.length === 1) {
                                        return selected.join(', ');
                                    } else if (selected.length === params.Result.length) {
                                        return 'All';
                                    } else {
                                        return `${selected.length} Selected`;
                                    }
                                }}
                            >
                                {params.Result.map((result, index) => (
                                    <MenuItem key={result} value={result} disabled={selectedParams.Result.indexOf(result) > -1 && selectedParams.Result.length === 1}>
                                        <Checkbox checked={selectedParams.Result.indexOf(result) > -1} />
                                        <ListItemText primary={result} />
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </ThemeProvider>
                </StyledTableCell>
            )
            heads.push(
                <StyledTableCell key={'Trigger_Type'} align="center">
                    <ThemeProvider theme={darkTheme}>
                        <FormControl className="ml-0" fullWidth={true} style={{minWidth: 120}}>
                            <InputLabel id="trigger-type-select-label" >Trigger Type</InputLabel>
                            <Select
                                labelId="trigger-type-select-label"
                                id="trigger-type-select"
                                label="Trigger Type"
                                size="small"
                                multiple
                                value={selectedParams.TriggerType}
                                onChange={this.selectTriggerTypeChange}
                                onClose={() => { this.queryTask() }}
                                renderValue={(selected) => {
                                    if (selected.length === 0) {
                                        return 'Placeholder';
                                    } else if (selected.length <= 2) {
                                        return selected.join(', ');
                                    } else if (selected.length === params.TriggerType.length) {
                                        return 'All';
                                    } else {
                                        return `${selected.length} Selected`;
                                    }
                                }}

                            >
                                {params.TriggerType.map((type, index) => (
                                    <MenuItem key={type} value={type} onClick={ () => { this.lastClick = type } }>
                                        <Checkbox checked={selectedParams.TriggerType.indexOf(type) > -1} />
                                        <ListItemText primary={type} />
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </ThemeProvider>
                </StyledTableCell>
            )

            heads.push(
                <StyledTableCell key={'Actions'} align="center">
                    <ThemeProvider theme={darkTheme}>
                        Actions
                    </ThemeProvider>
                </StyledTableCell>
            )
        }

        return <div>
            <Typography variant="h4" className="m-2">
                Task History</Typography>
            <TableContainer style={{ margin: "auto", overflowY: 'initial', height: `${tableBodyHeight + 100}px` }}>
                <Table stickyHeader size="medium">
                    <TableHead>
                        <TableRow>
                            {heads}
                        </TableRow>
                    </TableHead>
                    <TableBody align="center" hidden={this.state.hideSkeleton}>
                        <TableRow>
                            <TableCell colSpan="8">
                                <Skeleton variant="text" className="w-100 p-3"
                                    height={100} />
                                <Skeleton variant="text" className="w-100 p-3"
                                    height={100} />
                                <Skeleton variant="text" className="w-100 p-3"
                                    height={100} />
                            </TableCell>
                        </TableRow>
                    </TableBody>
                    <TableBody align="center" style={{ height: `${tableBodyHeight}px` }}>
                        {rows}
                    </TableBody>
                </Table>
            </TableContainer>
            <Dialog open={this.state.openTestDetail}
                    fullWidth
                    maxWidth="lg"
                    onClose={() => this.handleCloseDetailDialog()}
            >
                {this.state.testDetailInfo && this.state.testDetailInfo.analysisConfigs ? <AnalysisReportView testTask={this.state.testDetailInfo} /> : <TestReportView testTask={this.state.testDetailInfo} />}
                
                <DialogActions>
                    <Button
                        onClick={() => this.handleCloseDetailDialog()}>Close</Button>
                </DialogActions>
            </Dialog>
            {this.getRerunDialog()}
            <Backdrop
                sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
                open={this.state.showBackDrop}
            >
                <CircularProgress color="inherit" />
            </Backdrop>
            <Snackbar
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'center'
                }}
                open={this.state.snackbarIsShown}
                autoHideDuration={3000}
                onClose={() => this.handleStatus("snackbarIsShown", false)}>
                <Alert
                    onClose={() => this.handleStatus("snackbarIsShown", false)}
                    severity={this.state.snackbarSeverity}>
                    {this.state.snackbarMessage}
                </Alert>
            </Snackbar>
            <Stack
                direction="row"
                justifyContent="flex-end"
                alignItems="flex-end"
                spacing={2}
            >
                <Pagination count={this.state.pageCount} size="large" page={this.state.page} onChange={this.handlePageChange} />
            </Stack>
        </div>
    }

    handlePageChange = (event, value) => {
        console.log(`Page change to ${value}`)
        ls.set('selectedParams', this.state.selectedParams)
        let { history } = this.props
        console.log(history)
        history.push({
            pathname: `/tasks/${value}`,
            state: {
                selectedParams: this.state.selectedParams
            }
        })
        history.go()
    };

    selectTimeChange = (element) => {
        console.log(element.target.value)
        let newSelectedParams = this.state.selectedParams
        newSelectedParams.time = element.target.value
        this.setState({
            selectedParams: newSelectedParams
        })
        console.log(this.state.selectedParams)
        ls.set('selectedParams', this.state.selectedParams)
        this.queryTask()
    }

    selectSuiteChange = (element) => {
        console.log(event.target.value)
        let newSelectedParams = this.state.selectedParams
        newSelectedParams.suite = element.target.value
        this.setState({ selectedParams: newSelectedParams })
        ls.set('selectedParams', this.state.selectedParams)
        this.queryTask()
    }

    selectTestTypeChange = (element) => {
        console.log(2)
        let value = element.target.value
        let newSelectedParams = this.state.selectedParams

        if (!Array.isArray(value)) {
            value = value.split(',')
        }

        let newSelectedTestType = params.TestType.filter((element) => value.indexOf(element) === -1)

        if (value.length === params.TestType.length - 1 && value.length > 1 && this.lastClick && newSelectedTestType.indexOf(this.lastClick) !== -1) {
            newSelectedParams.TestType = newSelectedTestType
        } else if (value.length === 0) {
            newSelectedParams.TestType = params.TestType
        } else {
            newSelectedParams.TestType = value
        }

        this.setState({ selectedParams: newSelectedParams })
        ls.set('selectedParams', this.state.selectedParams)
    }

    selectResultChange = (element) => {
        let value = element.target.value
        let newSelectedParams = this.state.selectedParams
        newSelectedParams.Result = typeof value === 'string' ? value.split(',') : value
        this.setState({
            selectedParams: newSelectedParams
        })
        ls.set('selectedParams', this.state.selectedParams)
        this.queryTask()
    }

    selectTriggerTypeChange = (element) => {
        console.log(2)

        let value = element.target.value
        let newSelectedParams = this.state.selectedParams

        if (!Array.isArray(value)) {
            value = value.split(',')
        }

        console.log(this.lastClick)
        console.log(value)

        let newSelectedTriggerType = params.TriggerType.filter((element) => value.indexOf(element) === -1)

        if (value.length === params.TriggerType.length - 1 && value.length > 1 && this.lastClick && newSelectedTriggerType.indexOf(this.lastClick) !== -1) {
            newSelectedParams.TriggerType = newSelectedTriggerType
        } else if (value.length === 0) {
            newSelectedParams.TriggerType = params.TriggerType
        } else {
            newSelectedParams.TriggerType = value
        }

        this.setState({selectedParams: newSelectedParams})
        ls.set('selectedParams', this.state.selectedParams)
    }

    formatTaskSpecRow(task) {
        return <StyledTableRow key={task.testTaskSpec.testTaskId} id={task.testTaskSpec.testTaskId}>
            <TableCell id={task.testTaskSpec.testTaskId} align="center">
                <span className='badge badge-success'
                      style={{fontSize: 16}}>Queue Position : {task.queuedInfo[0]}</span>
            </TableCell>
            <TableCell id={task.testTaskSpec.testTaskId} align="center" style={{maxWidth: '400px'}}>
                {task.testTaskSpec.testSuiteClass.length > 100 ? task.testTaskSpec.testSuiteClass.substring(0, 100) + '...' : task.testTaskSpec.testSuiteClass}
                <IconButton onClick={() => this.copyContent(task.testTaskSpec.testSuiteClass)}>
                        <span className="material-icons-outlined">content_copy</span>
                </IconButton>
            </TableCell>
            <TableCell id={task.testTaskSpec.testTaskId} align="center">
                {this.getTestType(task.testTaskSpec)}
            </TableCell>
            <TableCell id={task.testTaskSpec.testTaskId} align="center">
                -
            </TableCell>
            <TableCell id={task.testTaskSpec.testTaskId} align="center">
                {task.testTaskSpec.type}
            </TableCell>
            <TableCell id={task.testTaskSpec.testTaskId} align="center">
                <Button variant="outlined" color="warning" size='small'
                        onClick={(e) => this.clickCancel(e, task.testTaskSpec.testTaskId)}
                        className='badge badge-warning ml-1'>Cancel
                </Button>
            </TableCell>
        </StyledTableRow>
    }

    getTaskRow(task, isRunning) {
        if (isRunning) {
            return <StyledTableRow key={task.id} id={task.id}>
                <TableCell id={task.id} align="center">
                    {moment(task.startDate).format('yyyy-MM-DD HH:mm:ss')} <span
                    className='badge badge-success'>Running</span>
                </TableCell>
                <TableCell id={task.id} align="center" style={{maxWidth: '400px'}}>
                    {task.taskAlias.length > 100 ? task.taskAlias.substring(0, 100) + '...' : task.taskAlias}
                    <IconButton onClick={() => this.copyContent(task.taskAlias)}>
                            <span className="material-icons-outlined">content_copy</span>
                    </IconButton>
                </TableCell>
                <TableCell id={task.id} align="center">
                    {this.getTestType(task)}
                </TableCell>
                <TableCell id={task.id} align="center">
                    -
                </TableCell>
                <TableCell id={task.id} align="center">
                    {task.triggerType}
                </TableCell>
                <TableCell id={task.id} align="center">
                    <Button variant="outlined" color="warning" size='small'
                            onClick={(e) => this.clickCancel(e, task.id)}
                            className='badge badge-warning ml-1'>Cancel
                    </Button>
                </TableCell>
            </StyledTableRow>
        }
        const chartData = [
            { type: 'success', count: task.totalTestCount - task.totalFailCount },
            { type: 'fail', count: task.totalFailCount },
            { type: 'background', count: task.totalTestCount === 0 ? 1 : 0 },
        ]
        return <StyledTableRow key={task.id} id={task.id}
                align="center" hover>
                {/* style={task.id === this.state.displayReportTaskId ? { background: "#99e4fe" } : null} */}
                <TableCell id={task.id} align="center">
                    {moment(task.startDate).format('yyyy-MM-DD HH:mm:ss') + ' - ' + moment(task.endDate).format('HH:mm:ss')}
                </TableCell>
                <TableCell id={task.id} align="center" style={{maxWidth: '400px'}}>
                    {task.taskAlias.length > 100 ? task.taskAlias.substring(0, 100) + '...' : task.taskAlias}
                    <IconButton onClick={() => this.copyContent(task.taskAlias)}>
                            <span className="material-icons-outlined">content_copy</span>
                    </IconButton>
                </TableCell>
                <TableCell id={task.id} align="center">
                    {this.getTestType(task)}
                </TableCell>
                <TableCell id={task.id} align="center">
                    <table border='0'>
                        <tbody>
                            <tr>
                                <td>
                                    <PieChart width={taskRowHeight} height={taskRowHeight}>
                                        <Pie
                                            data={chartData}
                                            labelLine={false}
                                            fill="#8884d8"
                                            dataKey="count">
                                            {chartData.map((entry, index) => (
                                                <Cell key={`cell-${index}`}
                                                    fill={pieCOLORS[index % pieCOLORS.length]} />
                                            ))}
                                        </Pie>
                                    </PieChart>
                                </td>
                                <td style={{ fontSize: '0.875rem' }}>
                                    {task.overallSuccessRate}
                                    {task.analysisConfigs ? task.status : '(' +(task.totalTestCount - task.totalFailCount) + '/' + task.totalTestCount+ ')'}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </TableCell>
                <TableCell id={task.id} align="center">
                    {task.triggerType}
                </TableCell>
                <TableCell id={task.id} align="center">
                    {/* color definition: https://mui.com/material-ui/customization/palette/#adding-new-colors */}
                    <Button variant="outlined" color="primary" size='small' onClick={(e) => this.taskRowClicked(e, task)}
                        className='badge badge-warning ml-2'>Report
                    </Button>
                    {task.pipelineLink ?
                        <Button variant="outlined" color="info" size='small' onClick={(e) => this.clickGetLink(e, task)}
                            className='badge badge-warning ml-2'>Link
                        </Button> :
                        null
                    }
                    {/* <Button variant="outlined" color="success" size='small' onClick={(e) => this.clickRerun(e, task)}
                        className='badge badge-info ml-2'>Rerun
                    </Button> */}
                </TableCell>
            </StyledTableRow>
    }

    copyContent(taskAlias) {
        copy(taskAlias)
        this.setState({
            snackbarIsShown: true,
            snackbarSeverity: "success",
            snackbarMessage: "suiteName copied!"
        })
    }
    taskRowClicked = (element, task) => {
        if (this.state.loading) {
            return
        }

        this.setState({
            showBackDrop: true,
        })
        console.log(this.state.showBackDrop)

        if (!this.state.displayReportTaskId) {
            this.setState({
                loading: true,
            })
        }
        axios.get('/api/test/task/' + task.id).then(res => {
            console.log(res.data)
            if (res.data && res.data.code === 200) {
                this.setState({
                    displayReportTaskId: task.id,
                    testDetailInfo: res.data.content,
                    loading: false
                })

                this.setState({
                    showBackDrop: false,
                    openTestDetail: true,
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }

    getTestType = (t) => {
        return TestType[t.runnerType]
    }

    clickCancel = (element, taskId) => {
        this.setState({
            loading: true
        })
        axios.get('/api/test/task/cancel/' + taskId + "?reason=manually").then(res => {
            if (res.data && res.data.code === 200) {
                console.log(res.data)
                this.setState({
                    loading: false
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }

    clickGetLink = (element, task) => {
        window.open(task.pipelineLink)
    }

    clickRerun = (element, task) => {
        if (this.state.loading) {
            return
        }

        this.setState({
            showBackDrop: true,
            loading: true
        })

        axios.get('/api/test/task/' + task.id).then(res => {
            console.log(res.data)
            if (res.data && res.data.code === 200) {
                this.setState({
                    showBackDrop: false,
                    loading: false,
                    rerunTaskDetail: res.data.content,
                    rerunTestDialogIsShown: true
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }

    handleCloseDetailDialog() {
        this.setState({
            displayReportTaskId: null,
        })
        this.handleStatus("openTestDetail", false)
    }

    taskParamUpdate(queryParams) {
        if (this.state.selectedParams.suite !== '') {
            queryParams.push({
                "key": "taskAlias",
                "op": "like",
                "likeRule": "all",
                "value": this.state.selectedParams.suite
            })
        }

        if (this.state.selectedParams.Result.length < 2) {
            if (this.state.selectedParams.Result.includes('Passed')) {
                queryParams.push({
                    "key": "isSucceed",
                    "op": "equal",
                    "value": 1
                })
            }
            if (this.state.selectedParams.Result.includes('Failed')) {
                queryParams.push({
                    "key": "isSucceed",
                    "op": "equal",
                    "value": 0
                })
            }
        }

        if (this.state.selectedParams.time === "Last 24 Hours") {
            queryParams.push({
                "key": "startDate",
                "op": "gt",
                "value": moment().subtract(1, 'days').format("yyyy-MM-DD HH:mm:ss.S"),
                "dateFormatString": "yyyy-MM-dd HH:mm:ss.S"
            })
        } else if (this.state.selectedParams.time === "Last 7 Days") {
            queryParams.push({
                "key": "startDate",
                "op": "gt",
                "value": moment().subtract(7, 'days').format("yyyy-MM-DD HH:mm:ss.S"),
                "dateFormatString": "yyyy-MM-dd HH:mm:ss.S"
            })
        } else if (this.state.selectedParams.time === "Last 30 Days") {
            queryParams.push({
                "key": "startDate",
                "op": "gt",
                "value": moment().subtract(30, 'days').format("yyyy-MM-DD HH:mm:ss.S"),
                "dateFormatString": "yyyy-MM-dd HH:mm:ss.S"
            })
        }
    }

    queryTask() {
        console.log(this.state.selectedParams)
        // completed tasks
        let queryParams = [
            {
                "key": "status",
                "op": "ne",
                "value": "running"
            },
            {
                "key": "runnerType",
                "op": "in",
                "value": this.state.selectedParams.TestType.length > 0 ? this.state.selectedParams.TestType : params.TestType
            },
            {
                "key": "triggerType",
                "op": "in",
                "value": this.state.selectedParams.TriggerType
            }
        ]
        this.taskParamUpdate(queryParams)

        let postBody = {
            'page': this.state.page - 1,
            'pageSize': -1,
            'queryParams': queryParams
        }

        this.axiosPost(`/api/test/task/list`, (content) => {
            this.setState({
                tasks: content.content,
                hideSkeleton: true,
                pageCount: content.totalPages
            })

        }, postBody, null, null, null)

        if (this.state.page === 1) {
            // queued tasks
            axios.get('/api/test/task/queue').then(res => {
                if (res.data && res.data.code === 200) {
                    if (res.data.content) {
                        this.setState({
                            queuedTasks: res.data.content,
                            hideSkeleton: true,
                        })
                    }
                } else {
                    this.snackBarFail(res)
                }
            }).catch(this.snackBarError)

            // running tasks
            let queryParams = [
                {
                    "key": "status",
                    "op": "equal",
                    "value": "running"
                },
                {
                    "key": "runnerType",
                    "op": "in",
                    "value": this.state.selectedParams.TestType.length > 0 ? this.state.selectedParams.TestType : params.TestType
                },
                {
                    "key": "triggerType",
                    "op": "in",
                    "value": this.state.selectedParams.TriggerType
                }
            ]
            this.taskParamUpdate(queryParams)

            let postBody = {
                'page': 0,
                'pageSize': -1,
                'queryParams': queryParams
            }

            this.axiosPost(`/api/test/task/list`, (content) => {
                this.setState({
                    runningTasks: content.content.reverse(),
                    hideSkeleton: true,
                })

            }, postBody, null, null, null)
        }
    }

    componentDidMount() {
        console.log(this.props)
        this.setState({
            hideSkeleton: false,
        })

        this.queryTask()
    }

    componentWillUnmount() {
        // cancel requests
    }

    getRerunDialog() {
        return <Dialog open={this.state.rerunTestDialogIsShown}
            fullWidth={true} maxWidth='lg'
            onClose={() => this.handleStatus("rerunTestDialogIsShown", false)}>
            <DialogTitle>Rerun Test Task</DialogTitle>
            <DialogContent>
                {this.getRerunForm()}
            </DialogContent>
        </Dialog>
    }

    getRerunForm() {
        const task = this.state.rerunTaskDetail
        if (!task) {
            return null
        }
        const runTestType = task.runnerType

        return <Box sx={{ display: 'flex', flexDirection: 'column', pt: 3 }}>
            <TextField
                disabled
                margin="dense"
                name="packageName"
                label="Package Name"
                type="text"
                fullWidth
                variant="standard"
                value={task.pkgName}
            />
            <TextField
                disabled
                margin="dense"
                name="packageNameTest"
                type="text"
                label="Test Package Name"
                fullWidth
                variant="standard"
                value={task.testPkgName}
            />
            <br/>
            <FormControl fullWidth>
                <InputLabel>Task type</InputLabel>
                <Select
                    disabled
                    margin="dense"
                    value={runTestType}
                    fullWidth
                    size="small"
                    name="runTestType"
                    onChange={this.handleValueChange}>
                    <MenuItem value={"INSTRUMENTATION"} disabled={this.state.currentApkType === 'ipa'}>Espresso</MenuItem>
                    <MenuItem value={"APPIUM"}>Appium</MenuItem>
                    <MenuItem value={"SMART"} disabled={this.state.currentApkType === 'ipa'}>Smart</MenuItem>
                    <MenuItem value={"MONKEY"} disabled={this.state.currentApkType === 'ipa'}>Monkey</MenuItem>
                    <MenuItem value={"APPIUM_MONKEY"} disabled={this.state.currentApkType !== 'ipa'}>Appium Monkey</MenuItem>
                    <MenuItem value={"APPIUM_CROSS"} disabled={this.state.currentApkType === 'ipa'}>Appium E2E</MenuItem>
                </Select>
            </FormControl>
            <TextField
                disabled
                required={runTestType === "INSTRUMENTATION" || runTestType === "APPIUM" || runTestType === "APPIUM_CROSS"}
                margin="dense"
                name="testSuiteClass"
                type="text"
                label="Test Suite Class"
                fullWidth
                variant="standard"
                value={task.taskAlias}
                onChange={this.handleValueChange}
            />
            <br />
            <FormControl fullWidth>
                <InputLabel>Test Framework</InputLabel>
                <Select
                    disabled
                    required={runTestType === "APPIUM" || runTestType === "APPIUM_CROSS"}
                    margin="dense"
                    value={task.frameworkType}
                    fullWidth
                    size="small"
                    name="frameworkType"
                    onChange={this.handleValueChange}>
                    <MenuItem value={"JUnit4"}>JUnit4</MenuItem>
                    <MenuItem value={"JUnit5"}>JUnit5</MenuItem>
                </Select>
            </FormControl>
            <TextField
                disabled
                margin="dense"
                name="packageNameTest"
                type="text"
                label={"Device Identifier"}
                fullWidth
                variant="standard"
                value={task.deviceIdentifier}
            />
            <TextField
                disabled
                margin="dense"
                name="packageNameTest"
                type="text"
                label={"Device Group Type"}
                fullWidth
                variant="standard"
                value={task.groupTestType}
            />
            <TextField
                disabled
                margin="dense"
                name="packageNameTest"
                type="text"
                label={"Test Device Count"}
                fullWidth
                variant="standard"
                value={task.deviceTestCount}
            />
            <TextField
                disabled
                margin="dense"
                name="packageNameTest"
                type="text"
                label={"Max Step"}
                fullWidth
                variant="standard"
                value={task.maxStepCount}
            />
            <TextField
                margin="dense"
                name="packageNameTest"
                type="text"
                label={"Time out seconds"}
                fullWidth
                variant="standard"
                value={this.state.timeOutSecond}
                onChange={this.handleValueChange}
            />
            <TextField
                margin="dense"
                name="testRunArgs"
                type="text"
                label="Test config"
                fullWidth
                variant="standard"
                value={this.state.testRunArgs}
                onChange={this.handleValueChange}
            />
            <Button
                onClick={() => this.rerunTest()}
                endIcon={<span
                    className="material-icons-outlined">send</span>}
                variant="contained">
                Run
            </Button>
        </Box>
    }

    rerunTest() {
        const task = this.state.rerunTaskDetail
        if (!task) {
            return
        }
        let argsObj = {}
        if (this.state.testRunArgs !== "") {
            try {
                argsObj = JSON.parse(this.state.testRunArgs)
            } catch (error) {
                this.snackBarMsg("Error Test config, please input JSON Object")
                return
            }
        }
        if (!(typeof argsObj === 'object')) {
            this.snackBarMsg("Error Test config, please input JSON Object")
            return
        }
        const formParams = {
            apkSetId: task.apkSetId,
            pkgName: task.pkgName,
            testPkgName: task.testPkgName,
            runnerType: task.runnerType,
            testSuiteClass: task.taskAlias,
            deviceIdentifier: task.deviceIdentifier,
            groupTestType: task.groupTestType,
            maxStepCount: task.maxStepCount,
            deviceTestCount: task.deviceTestCount,
            frameworkType: task.frameworkType,

            testTimeOutSec: this.state.timeOutSecond,
            testRunArgs: argsObj
        }

        axios.post('/api/test/task/run/', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Test cases successfully run",
                    snackbarIsShown: true
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarFail(error)
        })
        this.setState({
            testTimeOutSec: null,
            testRunArgs: null
        })
    }
}

export default withRouter(TasksView)
