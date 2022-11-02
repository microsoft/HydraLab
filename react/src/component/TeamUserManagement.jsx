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
import Stack from "@mui/material/Stack";
import Skeleton from '@mui/material/Skeleton';
import BaseView from "@/component/BaseView";


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

export default class TeamUserManagement extends BaseView {
    state = {
        teamId: this.props.teamId,
        manageable: this.props.manageable,

        userList: null,

        userAddDialogIsShown: false,
        toBeAddedUserId: null,

        userRemoveDialogIsShown: false,
        toBeRemovedUserId: null,

        hideSkeleton: true
    }

    render() {
        const userHeadItems = ['User Name', 'Role', 'Operation']
        const userHeads = []
        const userRows = []

        const { manageable, userList } = this.state

        if (userList) {
            userList.sort((a, b) => a.name > b.name ? 1 : -1)
            userList.forEach((t) => {
                userRows.push(<StyledTableRow key={t.userId} id={t.userId} hover>
                    <TableCell id={t.userId} align="center">
                        {t.name}
                    </TableCell>
                    <TableCell id={t.userId} align="center">
                        Member
                    </TableCell>
                    <TableCell id={t.userId} align="center">
                        <IconButton onClick={() => this.openRemoveUserDialog(t.userId)} disabled={!manageable}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        userHeadItems.forEach((k) => userHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        return <div>
            <TableContainer style={{margin: "auto"}}>
                <Table size="medium">
                    <TableHead>
                        <TableRow>
                            <TableCell colSpan="2">
                                <Typography variant="h4" className="mt-2 mb-2">
                                    {`Members`} </Typography>
                            </TableCell>
                            <TableCell colSpan="1">
                                <Stack direction="row" spacing={2}
                                       justifyContent="flex-end">
                                    <Button variant="contained" endIcon={<span className="material-icons-outlined">add</span>} onClick={() => this.handleStatus("userAddDialogIsShown", true)} disabled={!manageable}>
                                        Add Member
                                    </Button>
                                </Stack>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            {userHeads}
                        </TableRow>
                    </TableHead>
                    <TableCell colSpan="3" align="center" hidden={this.state.hideSkeleton}>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                    </TableCell>
                    <TableBody>
                        {userRows}
                    </TableBody>
                </Table>
            </TableContainer>
            <Dialog open={this.state.userAddDialogIsShown}
                    fullWidth={true}
                    onClose={() => this.handleStatus("userAddDialogIsShown", false)}>
                <DialogTitle>Create Team</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        name="toBeAddedUserId"
                        label="User Id"
                        type="text"
                        fullWidth
                        variant="standard"
                        onChange={this.handleValueChange}
                    /> <br/>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("userAddDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.addMember()}>
                        Add
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={this.state.userRemoveDialogIsShown}
                onClose={() => this.handleStatus("userRemoveDialogIsShown", false)}
            >
                <DialogTitle> Delete this team? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to remove this member.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("userRemoveDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.removeMember()}>
                        Confirm
                    </Button>
                </DialogActions>
            </Dialog>
        </div>
    }

    addMember = () => {
        this.setState({
            userAddDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("userId", this.state.toBeAddedUserId)
        formParams.append("teamId", this.state.teamId)
        formParams.append("isTeamAdmin", "false")

        axios.post('/api/userTeam/addRelation', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Member Added!",
                    snackbarIsShown: true,
                    toBeAddedUserId: null
                })
                this.getTeamMemberInfo()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    openRemoveUserDialog = (userId) => {
        console.log(userId)
        this.handleStatus("userRemoveDialogIsShown", true);
        this.handleStatus('toBeRemovedUserId', userId)
    }

    removeMember = () => {
        this.setState({
            userRemoveDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("userId", this.state.toBeRemovedUserId)
        formParams.append("teamId", this.state.teamId)

        axios.post('/api/userTeam/deleteRelation', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Member removed!",
                    snackbarIsShown: true,
                    toBeRemovedUserId: null
                })
                this.getTeamMemberInfo()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }
    getTeamMemberInfo(teamId) {
        this.setState({
            hideSkeleton: false
        })

        const formParams = new URLSearchParams()
        formParams.append("teamId", this.state.teamId)

        axios.post('/api/userTeam/queryUsers', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    userList: res.data.content,
                    hideSkeleton: true
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })

    }

    componentDidMount() {
        this.getTeamMemberInfo()
    }

    componentWillUnmount() {
        // cancel requests
    }
}