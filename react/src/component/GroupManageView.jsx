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
import {withStyles} from '@material-ui/core/styles';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import Skeleton from "@mui/material/Skeleton";
import Stack from '@mui/material/Stack';
import BaseView from "@/component/BaseView";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";

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

export default class GroupManageView extends BaseView {

    state = {
        hideSkeleton: true,
        groups: null,

        createGroupDialog: false,
        createGroupName: null,

        currentGroup: null,
        deviceList: null,

        addDeviceDialog: false,
        deviceSerial: null,
        deviceAccessKey: null,

        groupToken: null,

        groupDeleteDialogIsShown: false,
        toBeDeletedGroupName: null,

        teamList: null,
        selectedTeamId: null

    }

    render() {
        const {groups, deviceList, teamList} = this.state
        const {createGroupDialog, addDeviceDialog, groupDeleteDialogIsShown} = this.state
        const {snackbarIsShown, snackbarSeverity, snackbarMessage} = this.state
        const {groupToken} = this.state

        const headItems = ['Group Name', 'Group Type', 'Owner', 'Team', 'Locked', 'Operation']
        const rows = []
        const heads = []

        const deviceRows = []
        const deviceHeads = []
        const deviceHeadItems = ['Serial Number', 'Agent', 'Model', 'Status', 'Operation']

        if (groups) {
            groups.forEach((t) => {
                rows.push(<StyledTableRow key={t.groupName} id={t.groupName} hover>
                    <TableCell id={t.groupName} align="center">
                        {t.groupDisplayName}
                    </TableCell>
                    <TableCell id={t.groupName} align="center">
                        {t.groupType}
                    </TableCell>
                    <TableCell id={t.groupName} align="center">
                        {t.owner}
                    </TableCell>
                    <TableCell id={t.groupName} align="center">
                        {t.teamName}
                    </TableCell>
                    <TableCell id={t.groupName} align="center">
                        {t.isPrivate ?
                            <span className="material-icons">check_circle_outline</span> :
                            <span className="material-icons"> highlight_off </span>}
                    </TableCell>
                    <TableCell id={t.groupName} align="center">
                        <IconButton onClick={() => {
                            this.groupManage(t.groupName)
                        }}>
                            <span className="material-icons-outlined">settings</span>
                        </IconButton>
                        <IconButton onClick={() => this.verifyChange(t.groupName, t.isPrivate)}>
                            {t.isPrivate ? <span className="material-icons-outlined">lock</span>
                                : <span className="material-icons-outlined">lock_open</span>
                            }
                        </IconButton>
                        <IconButton onClick={() => this.genericToken(t.groupName)}>
                            <span className="material-icons-outlined">key</span>
                        </IconButton>
                        <IconButton onClick={() => this.openDeleteGroupDialog(t.groupName)}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        headItems.forEach((k) => heads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        if (deviceList) {
            deviceList.forEach((t) => {
                deviceRows.push(<StyledTableRow key={t.id} id={t.id} hover>
                    <TableCell id={t.id} align="center">
                        {t.serialNum}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.agentId}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.model}
                    </TableCell>
                    <TableCell id={t.status} align="center">
                        {t.status}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <IconButton onClick={() => this.deleteDevice(t.serialNum)}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        deviceHeadItems.forEach((k) => deviceHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        return <div>
            <center>
                <TableContainer style={{margin: "auto"}}>
                    <Table size="medium">
                        <TableHead>
                            <TableRow>
                                <TableCell colSpan="4">
                                    <Typography variant="h4" className="mt-2 mb-2">
                                        Device Group</Typography>
                                </TableCell>
                                <TableCell colSpan="2">
                                    <Stack direction="row" spacing={2}
                                           justifyContent="flex-end">
                                        <Button variant="contained"
                                                endIcon={<span
                                                    className="material-icons-outlined">add</span>}
                                                onClick={() => this.handleStatus("createGroupDialog", true)}>
                                            Add Group
                                        </Button>
                                    </Stack>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                {heads}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            <TableRow>
                                <TableCell colSpan="5" align="center"
                                           hidden={this.state.hideSkeleton}>
                                    <Skeleton variant="text" className="w-100 p-3"
                                              height={100}/>
                                    <Skeleton variant="text" className="w-100 p-3"
                                              height={100}/>
                                    <Skeleton variant="text" className="w-100 p-3"
                                              height={100}/>
                                </TableCell>
                            </TableRow>
                            {rows}
                        </TableBody>
                    </Table>
                </TableContainer>
            </center>
            <Dialog open={groupToken !== null}
                    fullWidth={true}
                    onClose={() => this.handleStatus("groupToken", null)}>
                <DialogTitle>Group token generated</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Access Key:
                        {groupToken}&nbsp;
                        <IconButton onClick={() => {
                            navigator.clipboard.writeText(groupToken)
                        }}>
                            <span className="material-icons-outlined">content_copy</span>
                        </IconButton>
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("groupToken", null)}>
                        Done</Button>
                </DialogActions>
            </Dialog>
            <Dialog open={createGroupDialog}
                    fullWidth={true}
                    onClose={() => this.handleStatus("createGroupDialog", false)}>
                <DialogTitle>Add Device Group</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        name="createGroupName"
                        label="Group Name"
                        type="text"
                        fullWidth
                        required
                        variant="standard"
                        onChange={this.handleValueChange}
                    /><br/>
                    <FormControl required variant="standard" fullWidth={true}>
                        <InputLabel id="agent-team-select-label" >Team</InputLabel>
                        <Select
                            labelId="agent-team-select-label"
                            id="agent-team-select"
                            label="Team"
                            size="small"
                            value={teamList ? this.state.selectedTeamId : 'None_Team'}
                            onChange={(select) => this.handleStatus('selectedTeamId', select.target.value)}
                        >
                            {teamList ? null : <MenuItem value={'None_Team'}>No team available</MenuItem>}
                            {teamList ? teamList.map((team, index) => (
                                <MenuItem value={team.teamId} key={team.teamId}>{team.teamName}</MenuItem>
                            )) : null}
                        </Select>
                    </FormControl> <br/>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("createGroupDialog", false)}>
                        Cancel</Button>
                    <Button onClick={() => this.createGroup()}>Save</Button>
                </DialogActions>
            </Dialog>
            <Dialog open={deviceList !== null}
                    fullWidth={true}
                    onClose={() => this.handleStatus("deviceList", null)}>
                <DialogTitle>Device Group
                    <Stack direction="row" spacing={2}
                           justifyContent="flex-end">
                        <Button onClick={() => {
                            this.handleStatus("addDeviceDialog", true)
                        }}>
                            Add device
                        </Button></Stack>
                </DialogTitle>
                <DialogContent>
                    <TableContainer>
                        <Table aria-label="simple table">
                            <TableHead>
                                <TableRow>
                                    {deviceHeads}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {deviceRows}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("deviceList", null)}>Done</Button>
                </DialogActions>
            </Dialog>
            <Dialog open={addDeviceDialog}
                    fullWidth={true}
                    onClose={() => this.handleStatus("addDeviceDialog", false)}>
                <DialogTitle>Add Device</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        name="deviceSerial"
                        label="Device serial"
                        type="text"
                        fullWidth
                        variant="standard"
                        onChange={this.handleValueChange}
                    />
                    <TextField
                        margin="dense"
                        name="deviceAccessKey"
                        label="Access key"
                        type="text"
                        fullWidth
                        variant="standard"
                        onChange={this.handleValueChange}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("addDeviceDialog", false)}>
                        Cancel</Button>
                    <Button onClick={() => this.addDevice()}>Save</Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={groupDeleteDialogIsShown}
                onClose={() => this.handleStatus("groupDeleteDialogIsShown", false)}
            >
                <DialogTitle> Delete this group? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to delete this group, this operation is irreversible
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("groupDeleteDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.deleteGroup()}>
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
                    onClose={() => this.handleStatus("snackbarIsShown", false)}
                    severity={snackbarSeverity}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </div>
    }

    createGroup = () => {
        if (!this.state.selectedTeamId) {
            this.snackBarMsg("Please select a team")
            return
        }

        this.setState({
            createGroupDialog: false
        })

        let params = {
            "teamId": this.state.selectedTeamId,
            "groupName": this.state.createGroupName
        }

        axios.get('/api/deviceGroup/create/', {params} ).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Group successfully created",
                    snackbarIsShown: true,
                })
                this.refreshGroups()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    openDeleteGroupDialog = (groupName) => {
        this.handleStatus("groupDeleteDialogIsShown", true);
        this.handleStatus('toBeDeletedGroupName', groupName)
    }

    deleteGroup = () => {
        this.handleStatus("groupDeleteDialogIsShown", false);
        axios.get('/api/deviceGroup/delete/?groupName=' + this.state.toBeDeletedGroupName).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarIsShown: true,
                    snackbarSeverity: "success",
                    snackbarMessage: "Group successfully deleted",
                    toBeDeletedGroupName: null
                })
                this.refreshGroups()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    groupManage = (groupName) => {
        this.refreshDevices(groupName)
        this.setState({
            currentGroup: groupName
        })
    }

    addDevice = () => {
        const formParams = new URLSearchParams()
        formParams.append("accessKey", this.state.deviceAccessKey)
        formParams.append("deviceSerial", this.state.deviceSerial)
        formParams.append("groupName", this.state.currentGroup)

        axios.post('/api/deviceGroup/addRelation/', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    addDeviceDialog: false,
                    deviceAccessKey: null,
                    deviceSerial: null,
                })
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Device successfully added",
                    snackbarIsShown: true,
                })
                this.refreshDevices(this.state.currentGroup)
            } else {
                this.snackBarFail(res)
            }
            this.setState({
                addDeviceDialog: false,
                deviceAccessKey: null,
                deviceSerial: null,
            })
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    deleteDevice = (serialNum) => {
        const formParams = new URLSearchParams()
        formParams.append("deviceSerial", serialNum)
        formParams.append("groupName", this.state.currentGroup)

        axios.post('/api/deviceGroup/deleteRelation/', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Device successfully deleted",
                    snackbarIsShown: true
                })
                this.refreshDevices(this.state.currentGroup)
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    verifyChange = (groupName, isPrivate) => {
        axios.get('/api/deviceGroup/' + (isPrivate ? 'disableVerify' : 'enableVerify') + '/?groupName=' + groupName)
            .then(res => {
                if (res.data && res.data.code === 200) {
                    this.setState({
                        snackbarIsShown: true,
                        snackbarSeverity: "success",
                        snackbarMessage: isPrivate ? "Group unlocked" : "Group locked"
                    })
                    this.refreshGroups()
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => {
            this.snackBarError(error)
        })
    }

    genericToken = (groupName) => {
        axios.get('/api/deviceGroup/generate?deviceIdentifier=' + groupName)
            .then(res => {
                if (res.data && res.data.code === 200) {
                    this.setState({
                        groupToken: res.data.content.key
                    })
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => {
            this.snackBarError(error)
        })
    }

    refreshGroups() {
        this.setState({
            groups: null,
            hideSkeleton: false
        })
        axios.get('/api/deviceGroup/queryGroups').then(res => {
                if (res.data && res.data.code === 200) {
                    const groups = res.data.content;
                    console.log(groups)
                    this.setState({
                        groups: groups,
                    })
                } else {
                    this.snackBarFail(res)
                }
                this.setState({
                    hideSkeleton: true
                })
            }
        ).catch((error) => {
            this.snackBarError(error)
            this.setState({
                hideSkeleton: true
            })
        })
    }

    refreshDevices = (groupName) => {
        axios.get('/api/deviceGroup/queryDeviceList/?groupName=' + groupName).then(res => {
            if (res.data && res.data.code === 200) {
                const devices = res.data.content;
                console.log(devices)
                this.setState({
                    deviceList: devices,
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    componentDidMount() {
        this.getUserInfo();
        this.refreshGroups()
        this.refreshTeamList()
    }
}