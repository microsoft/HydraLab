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
import Stack from "@mui/material/Stack";
import Skeleton from '@mui/material/Skeleton';
import BaseView from "@/component/BaseView";
import moment from 'moment';
import TeamUserManagement from "@/component/TeamUserManagement";


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
withStyles((theme) => ({
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
            backgroundColor: theme.palette.action.selected,
        },
    },
}))(TableRow);

export default class TeamManagement extends BaseView {
    state = {
        hideSkeleton: true,
        teamCreateDialogIsShown: false,

        teamDeleteDialogIsShown: false,
        toBeDeletedTeamId: null,

        teamMemberDetailIsShown: false,
        selectedTeamId: null,
        selectedTeamManageable: false,

        teamList: null,
        teamName: null,

    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state

        const teamHeadItems = ['Team Name', 'Create Time', 'Operation']
        const teamHeads = []
        const teamRows = []

        const { userInfo, teamList } = this.state

        let admin = false;

        if (userInfo) {
            admin = (userInfo.roleName === 'ADMIN' || userInfo.roleName === 'SUPER_ADMIN')
        }

        if (teamList) {
            teamList.sort((a, b) => a.createTime > b.createTime ? 1 : -1)
            teamList.forEach((t) => {
                teamRows.push(<StyledTableRow key={t.teamId} id={t.teamId} hover>
                    <TableCell id={t.teamId} align="center">
                        {t.teamName}
                    </TableCell>
                    <TableCell id={t.teamId} align="center">
                        {moment(t.createTime).format('yyyy-MM-DD HH:mm:ss')}
                    </TableCell>
                    <TableCell id={t.teamId} align="center">
                        <IconButton onClick={() => this.showTeamInfo(t.teamId, t.manageable)}>
                            <span className="material-icons-outlined">info</span>
                        </IconButton>
                        <IconButton onClick={() => this.openDeleteTeamDialog(t.teamId)} disabled={t.teamName === 'Default' || !t.manageable}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        teamHeadItems.forEach((k) => teamHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        return <div>
            <TableContainer style={{margin: "auto"}}>
                <Table size="medium">
                    <TableHead>
                        <TableRow>
                            <TableCell colSpan="2">
                                <Typography variant="h4" className="mt-2 mb-2">
                                    {`Team Management (${this.state.userInfo ? this.state.userInfo.roleName : 'User'})`} </Typography>
                            </TableCell>
                            <TableCell colSpan="1">
                                <Stack direction="row" spacing={2}
                                       justifyContent="flex-end">
                                    <Button variant="contained" endIcon={<span className="material-icons-outlined">add</span>} onClick={() => this.handleStatus("teamCreateDialogIsShown", true)} disabled={!admin}>
                                        Add Team
                                    </Button>
                                </Stack>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            {teamHeads}
                        </TableRow>
                    </TableHead>
                    <TableCell colSpan="3" align="center" hidden={this.state.hideSkeleton}>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                    </TableCell>
                    <TableBody>
                        {teamRows}
                    </TableBody>
                </Table>
            </TableContainer>
            <Dialog open={this.state.teamCreateDialogIsShown}
                    fullWidth={true}
                    onClose={() => this.handleStatus("teamCreateDialogIsShown", false)}>
                <DialogTitle>Create Team</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        name="teamName"
                        label="Team Name"
                        type="text"
                        fullWidth
                        variant="outlined"
                        size="small"
                        onChange={this.handleValueChange}
                    /> <br/>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("teamCreateDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.createTeam()}>
                        Create
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={this.state.teamDeleteDialogIsShown}
                onClose={() => this.handleStatus("teamDeleteDialogIsShown", false)}
            >
                <DialogTitle> Delete this team? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to delete this team, this operation is irreversible
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("teamDeleteDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.deleteTeam()}>
                        Confirm
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={this.state.teamMemberDetailIsShown}
                    fullWidth
                    maxWidth="lg"
                    onClose={() => this.handleStatus("teamMemberDetailIsShown", false)}>
                <DialogContent>
                    <TeamUserManagement teamId={this.state.selectedTeamId} manageable={this.state.selectedTeamManageable} />
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => this.handleStatus("teamMemberDetailIsShown", false)}>Close</Button>
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
                    severity={snackbarSeverity}
                    sx={{width: '100%'}}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </div>
    }

    createTeam = () => {
        console.log("createTeam")

        this.setState({
            teamCreateDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("teamName", this.state.teamName)

        axios.post('/api/team/create', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Team successfully created",
                    snackbarIsShown: true,
                })
                this.refreshTeamList()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    openDeleteTeamDialog = (teamId) => {
        console.log()
        this.handleStatus("teamDeleteDialogIsShown", true);
        this.handleStatus('toBeDeletedTeamId', teamId)
    }

    deleteTeam = () => {
        this.setState({
            teamDeleteDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("teamId", this.state.toBeDeletedTeamId)

        axios.post('/api/team/delete', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Team deleted!",
                    snackbarIsShown: true,
                    toBeDeletedTeamId: null
                })
                this.refreshTeamList()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    showTeamInfo(teamId, manageable) {
        console.log(teamId)
        this.setState({
            teamMemberDetailIsShown: true,
            selectedTeamId: teamId,
            selectedTeamManageable: manageable
        })
    }

    componentDidMount() {
        this.getUserInfo()
        this.refreshTeamList()
    }

    componentWillUnmount() {
        // cancel requests
    }
}