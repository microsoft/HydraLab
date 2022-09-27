import React from 'react'
import axios from '@/axios'
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Typography from '@material-ui/core/Typography';
import 'bootstrap/dist/css/bootstrap.css'
import { withStyles } from '@material-ui/core/styles';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Radio from '@material-ui/core/Radio';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import TextField from "@mui/material/TextField";
import DialogActions from "@mui/material/DialogActions";
import DialogContentText from "@mui/material/DialogContentText";
import Alert from "@mui/material/Alert";
import Snackbar from "@mui/material/Snackbar";
import LoadingButton from '@mui/lab/LoadingButton';
import Stepper from '@mui/material/Stepper';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import Box from '@mui/material/Box';
import Skeleton from "@mui/material/Skeleton";
import IconButton from "@mui/material/IconButton";
import Tooltip from '@mui/material/Tooltip';
import BaseView from "@/component/BaseView";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import {FormHelperText} from "@mui/material";

/**
 * Palette
 * https://material-ui.com/customization/palette/
 */
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
export default class RunnerView extends BaseView {

    state = {
        hideSkeleton: false,

        appSetList: null,
        deviceList: null,
        groupList: null,
        agentList: null,

        uploading: false,
        uploadDialogIsShown: false,
        uploadAppInstallerFile: null,
        uploadTestPackageFile: null,
        uploadTestDesc: null,

        currentAppId: "",
        currentAppInstallerType: "",
        currentAppPackageName: "",
        currentTestPackageName: "",

        running: false,
        runTestDialogIsShown: false,
        activeStep: 0,

        runTestType: "APPIUM",
        testSuiteClass: "",
        currentRunnable: "",
        groupTestType: "SINGLE",
        maxStepCount: "",
        deviceTestCount: "",
        testTimeOutSec: "",
        instrumentationArgs: "",
        frameworkType: "JUnit4",

        teamList: null,
        selectedTeamName: null
    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state
        const { uploadDialogIsShown, uploading } = this.state

        const { runTestDialogIsShown, running } = this.state

        const {teamList} = this.state

        const activeStep = this.state.activeStep
        const steps = ['Running type', 'Choose device', 'Task configuration']

        const appSetList = this.state.appSetList
        const rows = []
        const heads = []
        const headItems = ['', 'App name', 'Package name', 'Version', 'Build Description']

        if (appSetList) {
            appSetList.forEach((appSet) => {
                rows.push(<StyledTableRow key={appSet.id} id={appSet.id}
                    onClick={this.handleRunTestSelected} hover>
                    <TableCell id={appSet.id} align="center">
                        <Radio className="p-0 m-1" id={appSet.id} color="primary"
                            checked={this.state.currentAppId === appSet.id} />
                    </TableCell>
                    <TableCell id={appSet.id} align="center">
                        {appSet.appName}
                    </TableCell>
                    <TableCell id={appSet.id} align="center">
                        {appSet.packageName}
                    </TableCell>
                    <TableCell id={appSet.id} align="center">
                        {appSet.version}
                    </TableCell>
                    <TableCell id={appSet.id} align="center" style={{ wordBreak: "break-all" }}>
                        {appSet.commitMessage}
                    </TableCell>
                </StyledTableRow>)
            })
        }

        headItems.forEach((k) => heads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        return <div>
            <Typography variant="h4" className="m-3">
                Run Test Suite</Typography>
            <Stack className='m-3' direction="row" spacing={2}
                justifyContent="flex-end">
                <LoadingButton
                    variant="contained"
                    className="pl-4 pr-4"
                    loading={uploading}
                    loadingPosition="end"
                    onClick={() => this.handleStatus("uploadDialogIsShown", true)}
                    endIcon={<span
                        className="material-icons-outlined">file_upload</span>}>
                    Upload
                </LoadingButton>
                <LoadingButton
                    variant="contained"
                    className="pl-4 pr-4"
                    loading={running}
                    loadingPosition="end"
                    onClick={() => this.handleRunTest()}
                    endIcon={<span className="material-icons-outlined">send</span>}>
                    Run
                </LoadingButton>
            </Stack>
            <TableContainer style={{ margin: "auto", overflowY: 'initial', height: '960px' }}>
                <Table size="medium">
                    <TableHead>
                        <TableRow>
                            {heads}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        <TableRow>
                            <TableCell colSpan="5" align="center"
                                hidden={this.state.hideSkeleton}>
                                <Skeleton variant="text" className="w-100 p-3"
                                    height={100} />
                                <Skeleton variant="text" className="w-100 p-3"
                                    height={100} />
                                <Skeleton variant="text" className="w-100 p-3"
                                    height={100} />
                            </TableCell>
                        </TableRow>
                        {rows}
                    </TableBody>
                </Table>
            </TableContainer>
            <Dialog open={uploadDialogIsShown}
                fullWidth={true}
                onClose={() => this.handleStatus("uploadDialogIsShown", false)}>
                <DialogTitle>Upload Apk</DialogTitle>
                <DialogContent>
                    <FormControl required fullWidth={true}>
                        <Button
                            component="label"
                            variant="outlined"
                            startIcon={<UploadFileIcon />}
                        >
                            {this.state.uploadAppInstallerFile ? this.state.uploadAppInstallerFile.name : 'APK/IPA file'}
                            <input id="uploadAppInstallerFile"
                                   type="file"
                                   accept=".apk,.ipa"
                                   hidden
                                   onChange={this.handleFileUpload}
                            />
                        </Button>
                        <FormHelperText> </FormHelperText>
                    </FormControl> <br/>
                    <FormControl required fullWidth={true}>
                        <Button
                            component="label"
                            variant="outlined"
                            startIcon={<UploadFileIcon />}
                        >
                            {this.state.uploadTestPackageFile ? this.state.uploadTestPackageFile.name : 'Test APK/JAR/JSON file'}
                            <input id="uploadTestPackageFile"
                                   type="file"
                                   accept=".apk,.jar,.json"
                                   hidden
                                   onChange={this.handleFileUpload}
                            />
                        </Button>
                        <FormHelperText> </FormHelperText>
                    </FormControl> <br/>
                    <FormControl required fullWidth={true}>
                        <InputLabel id="agent-team-select-label" >Team</InputLabel>
                        <Select
                            labelId="agent-team-select-label"
                            id="agent-team-select"
                            label="Team"
                            size="small"
                            value={teamList ? this.state.selectedTeamName : 'None_Team'}
                            onChange={(select) => this.handleStatus('selectedTeamName', select.target.value)}
                        >
                            {teamList ? null : <MenuItem value={'None_Team'}>No team available</MenuItem>}
                            {teamList ? teamList.map((team, index) => (
                                <MenuItem value={team.teamName} key={team.teamName}>{team.teamName}</MenuItem>
                            )) : null}
                        </Select>
                        <FormHelperText> </FormHelperText>
                    </FormControl> <br/>
                    <FormControl variant="standard" fullWidth={true}>
                        <TextField
                            name="uploadTestDesc"
                            label="Description"
                            multiline
                            margin="dense"
                            onChange={this.handleValueChange}
                            fullWidth
                            rows={4}
                        />
                    </FormControl> <br/>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => this.handleStatus("uploadDialogIsShown", false)}>Cancel</Button>
                    <Button
                        variant="contained"
                        endIcon={<span className="material-icons-outlined">file_upload</span>}
                        onClick={() => this.uploadApk()}>Upload</Button>
                </DialogActions>
            </Dialog>
            <Dialog open={runTestDialogIsShown}
                fullWidth={true} maxWidth='lg'
                onClose={() => this.handleStatus("runTestDialogIsShown", false)}>
                <DialogTitle>Run Test</DialogTitle>
                <DialogContent>
                    <Stepper activeStep={activeStep}>
                        {steps.map((label) => {
                            const stepProps = {};
                            const labelProps = {};
                            return (
                                <Step key={label} {...stepProps}>
                                    <StepLabel {...labelProps}>{label}</StepLabel>
                                </Step>
                            );
                        })}
                    </Stepper>
                    <React.Fragment>
                        {this.renderSwitch()}
                        <Box sx={{ display: 'flex', flexDirection: 'row', pt: 2 }}>
                            <Button
                                color="inherit"
                                onClick={() => {
                                    this.setState({
                                        activeStep: activeStep - 1
                                    })
                                }}
                                disabled={activeStep === 0}
                                sx={{ mr: 1 }}>
                                Back
                            </Button>
                            <Box sx={{ flex: '1 1 auto' }} />
                            {activeStep !== steps.length - 1 ? <Button onClick={() => {
                                this.setState({
                                    activeStep: activeStep + 1
                                })
                            }}> Next </Button> : <Button
                                onClick={() => this.runTest()}
                                endIcon={<span
                                    className="material-icons-outlined">send</span>}
                                variant="contained">
                                Run
                            </Button>}
                        </Box>
                    </React.Fragment>
                </DialogContent>
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
                    severity={snackbarSeverity}
                    sx={{ width: '100%' }}
                    onClose={() => this.handleStatus("snackbarIsShown", false)}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </div>
    }

    renderSwitch() {
        const groupTestTypes = ["SINGLE", "REST", "ALL"]

        const { deviceList, groupList, agentList } = this.state
        const { runTestType, groupTestType } = this.state


        const runnableRows = []
        const runnableHeads = []
        const runnableHeadItems = ['', 'Type', 'Device name', 'Status', 'Description']

        const brandMap = new Map();
        brandMap.set('apk', 'Android');
        brandMap.set('ipa', 'Apple');

        if (agentList || groupList || deviceList) {
            agentList.forEach((agent) => {
                runnableRows.push(<StyledTableRow key={agent.agentId} id={agent.agentId}
                    onClick={() => this.handleStatus('currentRunnable', agent.agentId)}
                    hover>
                    <TableCell id={agent.agentId} align="center">
                        <Radio className="p-0 m-1" id={agent.agentId} color="primary"
                            checked={this.state.currentRunnable === agent.agentId} />
                    </TableCell>
                    <TableCell id={agent.agentId} align="center">
                        Agent
                    </TableCell>
                    <TableCell id={agent.agentId} align="center">
                        {agent.agentName}
                    </TableCell>
                    <TableCell id={agent.agentId} align="center">
                        <span className="material-icons-outlined">lock_open</span>
                    </TableCell>
                    <TableCell id={agent.agentId} align="center">
                        {agent.devices[0].serialNum}
                    </TableCell>
                </StyledTableRow>)
            })

            if (this.state.runTestType !== 'T2C_JSON') {
                groupList.forEach((group) => {
                    runnableRows.push(<StyledTableRow key={group.groupName} id={group.groupName}
                        onClick={() => this.handleStatus('currentRunnable', group.groupName)}
                        hover>
                        <TableCell id={group.groupName} align="center">
                            <Radio className="p-0 m-1" id={group.groupName} color="primary"
                                checked={this.state.currentRunnable === group.groupName} />
                        </TableCell>
                        <TableCell id={group.groupName} align="center">
                            Group
                        </TableCell>
                        <TableCell id={group.groupName} align="center">
                            {group.groupDisplayName}
                        </TableCell>
                        <TableCell id={group.groupName} align="center">
                            {group.isPrivate ? <span className="material-icons-outlined">lock</span>
                                : <span className="material-icons-outlined">lock_open</span>}
                        </TableCell>
                        <TableCell id={group.groupName} align="center">
                            Created by {group.owner}
                        </TableCell>
                    </StyledTableRow>)
                })

                let selectedList
                if (this.state.currentAppInstallerType === 'ipa') {
                    selectedList = deviceList.filter((device) => device.brand === brandMap.get(this.state.currentAppInstallerType))
                } else {
                    selectedList = deviceList.filter((device) => device.brand !== 'Apple')
                }
                selectedList.forEach((device) => {
                    runnableRows.push(<StyledTableRow key={device.serialNum} id={device.serialNum}
                        onClick={() => this.handleStatus('currentRunnable', device.serialNum)}
                        hover>
                        <TableCell id={device.serialNum} align="center">
                            <Radio className="p-0 m-1" id={device.serialNum} color="primary"
                                checked={this.state.currentRunnable === device.serialNum} />
                        </TableCell>
                        <TableCell id={device.serialNum} align="center">
                            Device
                        </TableCell>
                        <TableCell id={device.serialNum} align="center">
                            {device.serialNum}
                        </TableCell>
                        <TableCell id={device.serialNum} align="center">
                            {device.isPrivate || device.status !== "ONLINE" ? <span className="material-icons-outlined">lock</span>
                                : <span className="material-icons-outlined">lock_open</span>
                            }
                        </TableCell>
                        <TableCell id={device.serialNum} align="center">
                            API{device.osSDKInt} / {device.brand} / {device.model} / {device.screenSize} /
                            DPI{device.screenDensity}
                        </TableCell>
                    </StyledTableRow>)
                })
            }
        }

        runnableHeadItems.forEach((k) => runnableHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        switch (this.state.activeStep) {
            case 0:
                return <Box sx={{ display: 'flex', flexDirection: 'column', pt: 3 }}>
                    <TextField
                        disabled
                        autoFocus
                        margin="dense"
                        name="packageName"
                        label="Package Name"
                        type="text"
                        fullWidth
                        variant="standard"
                        value={this.state.currentAppPackageName}
                    />
                    <TextField
                        autoFocus
                        margin="dense"
                        name="packageNameTest"
                        type="text"
                        label="Test Package Name"
                        fullWidth
                        variant="standard"
                        value={this.state.currentTestPackageName}
                    /><br />
                    <FormControl fullWidth>
                        <InputLabel>Test type</InputLabel>
                        <Select
                            margin="dense"
                            value={runTestType}
                            fullWidth
                            size="small"
                            name="runTestType"
                            onChange={this.handleValueChange}>
                            <MenuItem value={"INSTRUMENTATION"} disabled={this.state.currentAppInstallerType === 'ipa' || this.state.runTestType === 'T2C_JSON'}>Espresso</MenuItem>
                            <MenuItem value={"APPIUM"} disabled={this.state.runTestType === 'T2C_JSON'}>Appium</MenuItem>
                            <MenuItem value={"SMART"} disabled={this.state.currentAppInstallerType === 'ipa' || this.state.runTestType === 'T2C_JSON'}>Smart</MenuItem>
                            <MenuItem value={"MONKEY"} disabled={this.state.currentAppInstallerType === 'ipa' || this.state.runTestType === 'T2C_JSON'}>Monkey</MenuItem>
                            <MenuItem value={"APPIUM_MONKEY"} disabled={this.state.currentAppInstallerType !== 'ipa'}>Appium Monkey</MenuItem>
                            <MenuItem value={"APPIUM_CROSS"} disabled={this.state.currentAppInstallerType === 'ipa' || this.state.runTestType === 'T2C_JSON'}>Appium E2E</MenuItem>
                            <MenuItem value={"T2C_JSON"} disabled={this.state.runTestType !== 'T2C_JSON'}>JSON-Described Test</MenuItem>
                        </Select>
                    </FormControl>
                    <TextField
                        disabled={runTestType !== "INSTRUMENTATION" && runTestType !== "APPIUM" && runTestType !== "APPIUM_CROSS"}
                        required={runTestType === "INSTRUMENTATION" || runTestType === "APPIUM" || runTestType === "APPIUM_CROSS"}
                        margin="dense"
                        name="testSuiteClass"
                        type="text"
                        label="Test Suite Class"
                        fullWidth
                        variant="standard"
                        value={this.state.testSuiteClass}
                        onChange={this.handleValueChange}
                    />
                    <br />
                    <FormControl fullWidth>
                        <InputLabel>Test Framework</InputLabel>
                        <Select
                            disabled={runTestType !== "APPIUM" && runTestType !== "APPIUM_CROSS"}
                            required={runTestType === "APPIUM" || runTestType === "APPIUM_CROSS"}
                            margin="dense"
                            value={this.state.frameworkType}
                            fullWidth
                            size="small"
                            name="frameworkType"
                            onChange={this.handleValueChange}>
                            <MenuItem value={"JUnit4"}>JUnit4</MenuItem>
                            <MenuItem value={"JUnit5"}>JUnit5</MenuItem>
                        </Select>
                    </FormControl>
                </Box>
            case 1:
                return <Box sx={{ display: 'flex', flexDirection: 'column', pt: 2 }}>
                    <TableContainer style={{ margin: "auto", overflowY: 'initial', height: '960px' }}>
                        <Table size="medium">
                            <TableHead>
                                <TableRow>
                                    {runnableHeads}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {runnableRows}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Box>
            case 2:
                return <Box sx={{ display: 'flex', flexDirection: 'column', pt: 2 }}>
                    <br />
                    <FormControl fullWidth>
                        <InputLabel>Group test type</InputLabel>
                        <Select
                            size="small"
                            margin="dense"
                            disabled={!this.state.currentRunnable.startsWith("G.")}
                            value={groupTestType}
                            fullWidth
                            name="groupTestType"
                            onChange={this.handleValueChange}>
                            {groupTestTypes.map((c) => <MenuItem value={c} key={c}>{c}</MenuItem>)}
                        </Select>
                    </FormControl>
                    <Stack direction="row" alignItems="flex-end">
                        <TextField
                            required={runTestType === "SMART" || runTestType === "MONKEY" || runTestType === "APPIUM_MONKEY"}
                            disabled={runTestType !== "SMART" && runTestType !== "MONKEY" && runTestType !== "APPIUM_MONKEY"}
                            margin="dense"
                            name="maxStepCount"
                            type="text"
                            label="Max step count"
                            fullWidth
                            variant="standard"
                            value={this.state.maxStepCount}
                            onChange={this.handleValueChange}
                        />
                        <Tooltip title="The number of steps performed by a single test">
                            <IconButton style={{ height: "100%" }}>
                                <span className="material-icons-outlined">info</span>
                            </IconButton>
                        </Tooltip>
                    </Stack>
                    <Stack direction="row" alignItems="flex-end">
                        <TextField
                            required={runTestType === "SMART"}
                            disabled={runTestType !== "SMART"}
                            margin="dense"
                            name="deviceTestCount"
                            type="text"
                            label="Device test count"
                            fullWidth
                            variant="standard"
                            value={this.state.deviceTestCount}
                            onChange={this.handleValueChange}
                        />
                        <Tooltip title="The number of rounds to test on one device">
                            <IconButton style={{ height: "100%" }}>
                                <span className="material-icons-outlined">info</span>
                            </IconButton>
                        </Tooltip>
                    </Stack>
                    <Stack direction="row" alignItems="flex-end">
                        <TextField
                            margin="dense"
                            name="testTimeOutSec"
                            type="text"
                            label="Test time out second"
                            fullWidth
                            variant="standard"
                            value={this.state.testTimeOutSec}
                            onChange={this.handleValueChange}
                        />
                        <Tooltip title="The maximum time of a single test">
                            <IconButton style={{ height: "100%" }}>
                                <span className="material-icons-outlined">info</span>
                            </IconButton>
                        </Tooltip>
                    </Stack>
                    <Stack direction="row" alignItems="flex-end">
                        <TextField
                            margin="dense"
                            name="instrumentationArgs"
                            type="text"
                            label="Test config"
                            fullWidth
                            variant="standard"
                            value={this.state.instrumentationArgs}
                            onChange={this.handleValueChange}
                        />
                        <Tooltip title="Additional parameters">
                            <IconButton style={{ height: "100%" }}>
                                <span className="material-icons-outlined">info</span>
                            </IconButton>
                        </Tooltip>
                    </Stack>
                </Box>
        }
    }

    handleRunTestSelected = (element) => {
        console.log(element.target)
        const currentId = element.target.id
        const currentAppInfo = this.state.appSetList.find(x => x.id === currentId)
        axios.get('/api/package/' + currentId).then(res => {
            console.log(res.data)
            let currentRunTestType = this.state.runTestType
            let currentTestPackageType, testPkgName, currentAppInstallerType;

            const currentAppInfo = res.data.content;
            currentAppInfo.attachments.forEach((attachment) => {
                if (attachment.fileType == 'APP') {
                    currentAppInstallerType = attachment.fileName.split(".").pop()
                } else if (attachment.fileType == 'TEST_APP') {
                    testPkgName = attachment.fileParser.pkgName
                    currentTestPackageType = attachment.fileName.split(".").pop()
                }
            })

            if (currentTestPackageType === "json") {
                currentRunTestType = "T2C_JSON"
            } else if (currentAppInstallerType === "apk") {
                currentRunTestType = "INSTRUMENTATION"
            } else {
                currentRunTestType = "APPIUM"
            }

            this.setState({
                currentAppId: currentId,
                currentAppInstallerType: currentTestPackageType,
                currentAppPackageName: currentAppInfo.packageName,
                currentTestPackageName: testPkgName,
                runTestType: currentRunTestType,
                testSuiteClass: "",
                currentRunnable: "",
                groupTestType: "SINGLE",
                maxStepCount: "",
                deviceTestCount: "",
                testTimeOutSec: "",
                instrumentationArgs: "",
            })
        })

    }

    handleRunTest = () => {
        if (this.state.currentAppId === "") {
            this.snackBarMsg("Please select package first")
            return
        }
        this.handleStatus("runTestDialogIsShown", true)
    }

    uploadApk = () => {
        const formData = new FormData()
        formData.append("teamName", this.state.selectedTeamName)
        formData.append("appFile", this.state.uploadAppInstallerFile)
        formData.append("testAppFile", this.state.uploadTestPackageFile)
        formData.append("commitMessage", this.state.uploadTestDesc)

        axios.post('/api/package/add/', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        }).then(res => {
            if (res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Package successfully uploaded",
                    snackbarIsShown: true,
                    uploading: false
                })
                this.refreshPackageList()
            } else {
                this.snackBarFail(res)
                this.setState({
                    uploading: false
                })
            }
        }).catch((error) => {
            this.snackBarError(error)
            this.setState({
                uploading: false
            })
        });
        this.setState({
            uploadDialogIsShown: false,
            uploading: true,
            uploadAppInstallerFile: null,
            uploadTestPackageFile: null,
            uploadTestDesc: null,
            selectedTeamName: null
        })
    }

    runTest = () => {
        let argsObj = {}
        if (this.state.instrumentationArgs !== "") {
            try {
                argsObj = JSON.parse(this.state.instrumentationArgs)
            } catch (error) {
                this.snackBarMsg("Error Test config, please input JSON Object")
                this.setState({
                    running: false
                })
                return
            }
        }
        if (!(typeof argsObj === 'object')) {
            this.snackBarMsg("Error Test config, please input JSON Object")
            this.setState({
                running: false
            })
            return
        }
        const formParams = {
            fileSetId: this.state.currentAppId,
            pkgName: this.state.currentAppPackageName,
            testPkgName: this.state.currentTestPackageName,
            runningType: this.state.runTestType,
            testSuiteClass: this.state.testSuiteClass,
            deviceIdentifier: this.state.currentRunnable,
            groupTestType: this.state.groupTestType,
            maxStepCount: this.state.maxStepCount,
            deviceTestCount: this.state.deviceTestCount,
            testTimeOutSec: this.state.testTimeOutSec,
            instrumentationArgs: argsObj,
            frameworkType: this.state.frameworkType
        }

        axios.post('/api/test/task/run/', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            if (res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Test cases successfully run",
                    snackbarIsShown: true,
                    running: false
                })
            } else {
                this.snackBarFail(res)
                this.setState({
                    running: false
                })
            }
        }).catch((error) => {
            this.snackBarFail(error)
            this.setState({
                running: false
            })
        })
        this.setState({
            running: true,
            runTestDialogIsShown: false,
            activeStep: 0,
        })
    }

    refreshRunnableList() {
        axios.get('/api/device/runnable').then(res => {
            console.log(res.data.content)
            const devices = res.data.content.devices;
            const groups = res.data.content.groups;
            const agents = res.data.content.agents;
            this.setState({
                deviceList: devices,
                groupList: groups,
                agentList: agents
            })
        })
    }

    refreshPackageList() {
        this.setState({
            hideSkeleton: false
        })
        const formParams = {}
        axios.post('/api/package/list', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            console.log(res.data)
            const pageData = res.data.content;
            this.setState({
                appSetList: pageData.content,
                hideSkeleton: true
            })
        })
    }

    componentDidMount() {
        this.refreshPackageList()
        this.refreshRunnableList()
        this.refreshTeamList()
    }

}