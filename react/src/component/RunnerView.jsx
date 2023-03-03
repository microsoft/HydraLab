// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

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
import { FormHelperText } from "@mui/material";
import { DialogContentText } from '@material-ui/core';

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
        currentAppInfo: null,
        currentAppInstallerType: "",
        currentAppPackageName: "",
        currentTestPackageName: "",

        running: false,
        runTestDialogIsShown: false,
        activeStep: 0,

        attachmentsDiaglogISshow: false,
        addAttachmentIsShow: false,
        attachmentDeleteDialogIsShow: false,
        attachmentUploading: false,
        fileType: "COMMON",
        loadType: "COPY",
        loadDir: "",
        uploadAttachmentFile: null,
        toBeDeletedAttachmentId: null,

        runTestType: "APPIUM",
        testScope: "CLASS",
        testSuiteClass: "",
        currentRunnable: "",
        groupTestType: "SINGLE",
        maxStepCount: "",
        deviceTestCount: "",
        testTimeOutSec: "",
        instrumentationArgs: "",
        frameworkType: "JUnit4",
        testRunnerName: "androidx.test.runner.AndroidJUnitRunner",
        neededPermissions: null,
        deviceActions: null,

        teamList: null,
        selectedTeamName: null
    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state
        const { uploadDialogIsShown, uploading } = this.state

        const { runTestDialogIsShown, running } = this.state
        const { attachmentsDiaglogISshow, addAttachmentIsShow, attachmentUploading } = this.state
        const { fileType, loadType, loadDir } = this.state

        const { teamList } = this.state

        const activeStep = this.state.activeStep
        const steps = ['Running type', 'Choose device', 'Task configuration']

        const appSetList = this.state.appSetList
        const rows = []
        const heads = []
        const headItems = ['', 'App name', 'Package name', 'Version', 'Build Description', 'Attachments']
        const attachmentsRows = []
        const attachmentsHeads = []
        const attachmentsHeadItems = ['File Name', 'Actions', 'File Type', ' LoadType', 'Load Dir']

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
                    <TableCell id={appSet.id} align="center">
                        <IconButton id={appSet.id} onClick={() => { this.handleStatus("attachmentsDiaglogISshow", true) }}>
                            <span id={appSet.id} className="material-icons-outlined">info</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        headItems.forEach((k) => heads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        if (this.state.currentAppInfo && this.state.currentAppInfo.attachments) {
            this.state.currentAppInfo.attachments.forEach((t) => {
                if (t.fileType !== "APP" && t.fileType !== "TEST_APP") {
                    attachmentsRows.push(<StyledTableRow key={t.fileId} id={t.fileId} hover>
                        <TableCell id={t.fileId} align="center">
                            {t.fileName}
                        </TableCell>
                        <TableCell id={t.fileId} align="center">
                            <IconButton id={t.fileId} onClick={this.showDeleteDialog}>
                                <span id={t.fileId} className="material-icons-outlined">delete</span>
                            </IconButton>
                            <IconButton id={t.fileId} href={t.blobUrl + '?' + require('local-storage').get('FileToken')}>
                                <span id={t.fileId} className="material-icons-outlined">download</span>
                            </IconButton>
                        </TableCell>
                        <TableCell id={t.fileId} align="center">
                            {t.fileType}
                        </TableCell>
                        <TableCell id={t.fileId} align="center">
                            {t.loadType}
                        </TableCell>
                        <TableCell id={t.fileId} align="center">
                            {t.loadDir}
                        </TableCell>
                    </StyledTableRow>)
                }
            })
        }

        attachmentsHeadItems.forEach((k) => attachmentsHeads.push(<StyledTableCell key={k} align="center">
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
                    </FormControl> <br />
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
                    </FormControl> <br />
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
                    </FormControl> <br />
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
                    </FormControl> <br />
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
            <Dialog open={attachmentsDiaglogISshow}
                fullWidth={true} maxWidth='lg'
                onClose={() => this.handleStatus("attachmentsDiaglogISshow", false)}>
                <DialogTitle>Attachments</DialogTitle>
                <DialogContent>
                    <TableContainer style={{ margin: "auto" }}>
                        <Table size="medium">
                            <TableHead>
                                <TableRow>
                                    {attachmentsHeads}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {attachmentsRows}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => this.handleStatus("attachmentsDiaglogISshow", false)}>Cancel</Button>
                    <LoadingButton
                        variant="contained"
                        className="pl-4 pr-4"
                        loading={attachmentUploading}
                        loadingPosition="end"
                        onClick={() => this.handleStatus("addAttachmentIsShow", true)}
                        endIcon={<span
                            className="material-icons-outlined">file_upload</span>}>
                        Add attachments
                    </LoadingButton>
                </DialogActions>
            </Dialog>
            <Dialog open={addAttachmentIsShow}
                fullWidth={true}
                onClose={() => this.handleStatus("addAttachmentIsShow", false)}>
                <DialogTitle>Add Attachments</DialogTitle>
                <DialogContent>
                    <Box sx={{ display: 'flex', flexDirection: 'column', pt: 3 }}>
                        <FormControl required fullWidth={true}>
                            <Button
                                component="label"
                                variant="outlined"
                                startIcon={<UploadFileIcon />}>
                                {this.state.uploadAttachmentFile ? this.state.uploadAttachmentFile.name : 'Attachment file'}
                                <input id="uploadAttachmentFile"
                                    type="file"
                                    accept=".*"
                                    hidden
                                    onChange={this.handleFileUpload}
                                />
                            </Button>
                        </FormControl> <br />
                        <FormControl required fullWidth>
                            <InputLabel>File type</InputLabel>
                            <Select
                                margin="dense"
                                value={fileType}
                                fullWidth
                                size="small"
                                name="fileType"
                                onChange={this.handleValueChange}>
                                <MenuItem value={"WINAPP"} >Windows app</MenuItem>
                                <MenuItem value={"COMMON"} >Common</MenuItem>
                                <MenuItem value={"T2C_JSON"} >T2C JSON</MenuItem>
                            </Select>
                        </FormControl>
                        <br />
                        <FormControl fullWidth>
                            <InputLabel>Load type</InputLabel>
                            <Select
                                disabled={fileType !== 'COMMON'}
                                margin="dense"
                                value={loadType}
                                fullWidth
                                size="small"
                                name="loadType"
                                onChange={this.handleValueChange}>
                                <MenuItem value={"COPY"} >Copy</MenuItem>
                                <MenuItem value={"UNZIP"} >Unzip</MenuItem>
                            </Select>
                        </FormControl>
                        <FormControl variant="standard" fullWidth>
                            <TextField
                                autoFocus
                                disabled={fileType !== 'COMMON'}
                                margin="dense"
                                name="loadDir"
                                type="text"
                                label="Load Dir"
                                fullWidth
                                onChange={this.handleValueChange}
                                value={loadDir}
                            />
                        </FormControl>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => this.handleStatus("addAttachmentIsShow", false)}>Cancel</Button>
                    <Button
                        variant="contained"
                        onClick={() => this.uploadAttachment()}
                        endIcon={<span className="material-icons-outlined">file_upload</span>}>
                        Add attachments
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={this.state.attachmentDeleteDialogIsShow}
                onClose={() => this.handleStatus("attachmentDeleteDialogIsShow", false)}
            >
                <DialogTitle> Delete this attachment? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to delete this attachment, this operation is irreversible
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("attachmentDeleteDialogIsShow", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.deleteAttachment()}>
                        Confirm
                    </Button>
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
        const { runTestType, groupTestType, testScope } = this.state


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
                        name="currentTestPackageName"
                        type="text"
                        label="Test Package Name"
                        fullWidth
                        variant="standard"
                        value={this.state.currentTestPackageName}
                        onChange={this.handleValueChange}
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
                            <MenuItem value={"T2C_JSON"}>JSON-Described Test</MenuItem>
                        </Select>
                    </FormControl>
                    <br />
                    <FormControl fullWidth>
                        <InputLabel>Espresso test scope</InputLabel>
                        <Select
                            disabled={this.state.runTestType !== 'INSTRUMENTATION'}
                            margin="dense"
                            value={testScope}
                            fullWidth
                            size="small"
                            name="testScope"
                            onChange={this.handleValueChange}>
                            <MenuItem value={"TEST_APP"} disabled={this.state.runTestType !== 'INSTRUMENTATION'}>Test app</MenuItem>
                            <MenuItem value={"PACKAGE"} disabled={this.state.runTestType !== 'INSTRUMENTATION'}>Package</MenuItem>
                            <MenuItem value={"CLASS"} disabled={this.state.runTestType !== 'INSTRUMENTATION'}>Class</MenuItem>
                        </Select>
                    </FormControl>
                    <TextField
                        disabled={(runTestType !== "INSTRUMENTATION" && runTestType !== "APPIUM" && runTestType !== "APPIUM_CROSS") || (runTestType === "INSTRUMENTATION" && testScope === "TEST_APP")}
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
                    <TextField
                        disabled={runTestType !== "INSTRUMENTATION"}
                        required={runTestType === "INSTRUMENTATION"}
                        margin="dense"
                        name="testRunnerName"
                        type="text"
                        label="Espresso Runner Name"
                        fullWidth
                        variant="standard"
                        value={this.state.testRunnerName}
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
                    <TextField
                        margin="dense"
                        name="neededPermissions"
                        type="text"
                        label="Needed Permissions"
                        fullWidth
                        value={this.state.neededPermissions}
                        onChange={this.handleValueChange}
                    />
                    <TextField
                        margin="dense"
                        name="deviceActions"
                        type="text"
                        label="Device Actions"
                        fullWidth
                        multiline={true}
                        value={this.state.deviceActions}
                        onChange={this.handleValueChange}
                    />
                </Box>
        }
    }

    handleRunTestSelected = (element) => {
        console.log(element.target)
        const currentId = element.target.id
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
                currentAppInfo: currentAppInfo,
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
        if (!this.state.selectedTeamName || !this.state.uploadAppInstallerFile) {
            this.snackBarMsg("Please upload APK/IPA file and select a team")
            return
        }
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
            if (res.data && res.data.code === 200) {
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
        let instrumentationArgsObj = {};
        let neededPermissionsObj = [];
        let deviceActionsObj = {};
        try {
            instrumentationArgsObj = this.handleJSONParams(this.state.instrumentationArgs);
            neededPermissionsObj = this.handleJSONParams(this.state.neededPermissions, []);
            deviceActionsObj = this.handleJSONParams(this.state.deviceActions);
        } catch (error) {
            this.snackBarMsg("Error Test config, please input JSON Object")
            this.setState({ running: false })
            return
        }

        const formParams = {
            fileSetId: this.state.currentAppId,
            pkgName: this.state.currentAppPackageName,
            testPkgName: this.state.currentTestPackageName,
            runningType: this.state.runTestType,
            testScope: this.state.testScope,
            testSuiteClass: this.state.testSuiteClass,
            deviceIdentifier: this.state.currentRunnable,
            groupTestType: this.state.groupTestType,
            maxStepCount: this.state.maxStepCount,
            deviceTestCount: this.state.deviceTestCount,
            testTimeOutSec: this.state.testTimeOutSec,
            instrumentationArgs: instrumentationArgsObj,
            frameworkType: this.state.frameworkType,
            testRunnerName: this.state.testRunnerName,
            neededPermissions: neededPermissionsObj,
            deviceActions: deviceActionsObj
        }

        axios.post('/api/test/task/run/', formParams, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            if (res.data && res.data.code === 200) {
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

    uploadAttachment = () => {
        const formData = new FormData()
        formData.append("fileSetId", this.state.currentAppId)
        formData.append("fileType", this.state.fileType)
        formData.append("loadType", this.state.loadType)
        formData.append("loadDir", this.state.loadDir)
        formData.append("attachment", this.state.uploadAttachmentFile)

        if (this.state.fileType === "COMMON" && this.state.loadDir === "") {
            this.snackBarError("Load Dir should not be empty")
            return false
        }

        axios.post('/api/package/addAttachment/', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Attachment successfully uploaded",
                    snackbarIsShown: true,
                    attachmentUploading: false
                })
                this.refreshCurrentAppInfo()
            } else {
                this.snackBarFail(res)
                this.setState({
                    attachmentUploading: false
                })
            }
        }).catch((error) => {
            this.snackBarError(error)
            this.setState({
                attachmentUploading: false
            })
        });
        this.setState({
            addAttachmentIsShow: false,
            attachmentUploading: true,
            uploadAttachmentFile: null,
            loadDir: "",
        })
    }

    showDeleteDialog = (element) => {
        console.log("fileId = " + element.target.id)
        this.setState({
            toBeDeletedAttachmentId: element.target.id,
            attachmentDeleteDialogIsShow: true,
        })
    }

    deleteAttachment = () => {
        const formData = new FormData()
        formData.append("fileSetId", this.state.currentAppId)
        formData.append("fileId", this.state.toBeDeletedAttachmentId)

        axios.post('/api/package/removeAttachment', formData, {
            headers: { 'content-type': 'application/json' }
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Attachment successfully deleted",
                    snackbarIsShown: true,
                })
                this.refreshCurrentAppInfo()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarFail(error)
        })
        this.setState({
            attachmentDeleteDialogIsShow: false,
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

    refreshCurrentAppInfo() {
        axios.get('/api/package/' + this.state.currentAppId).then(res => {
            console.log(res.data)
            const pageData = res.data.content;
            this.setState({
                currentAppInfo: pageData,
            })
        })
    }

    handleJSONParams(argString, initObj={}){
        let argObj = initObj;
        if (argString) {
            argObj = JSON.parse(argString)
        }
        if (typeof argObj !== 'object') {
            throw new Error()
        }
        return argObj
    }

    componentDidMount() {
        this.getUserInfo()
        this.refreshPackageList()
        this.refreshRunnableList()
        this.refreshTeamList()
    }

}