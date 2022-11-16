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
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@mui/material/Checkbox";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";


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
        toBeAddedUserMailAddress: null,
        toBeAddedUserRole: false,

        userRemoveDialogIsShown: false,
        toBeRemovedUserId: null,

        hideSkeleton: true
    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state

        const userHeadItems = ['User Name', 'Role', 'Operation']
        const userHeads = []
        const userRows = []

        const { manageable, userList } = this.state

        if (userList) {
            userList.sort((a, b) => {
                if ((a.teamAdmin && b.teamAdmin) || (!a.teamAdmin && !b.teamAdmin)) {
                    return a.name > b.name ? 1 : -1
                }
                else {
                    return a.teamAdmin ? -1 : 1
                }
            })
            userList.forEach((t) => {
                userRows.push(<StyledTableRow key={t.userId} id={t.userId} hover>
                    <TableCell id={t.userId} align="center">
                        {t.name}
                    </TableCell>
                    <TableCell id={t.userId} align="center">
                        {t.teamAdmin ? "Manager" : "Member"}
                    </TableCell>
                    <TableCell id={t.userId} align="center">
                        <IconButton onClick={() => this.openRemoveUserDialog(t.userId)} disabled={!manageable && this.state.userInfo && this.state.userInfo.userId === t.userId}>
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
                <DialogTitle>Add Member</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        name="toBeAddedUserMailAddress"
                        label="User Mail Address"
                        type="text"
                        fullWidth
                        variant="outlined"
                        size="small"
                        onChange={this.handleValueChange}
                    /> <br/>
                    <FormControlLabel
                        label="Team Manager"
                        control={<Checkbox checked={this.state.toBeAddedUserRole} onChange={(event) => {this.setState({toBeAddedUserRole: event.target.checked});}} />}
                    />
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
                <DialogTitle> Remove this member? </DialogTitle>
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

    addMember = () => {
        this.setState({
            userAddDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("mailAddress", this.state.toBeAddedUserMailAddress)
        formParams.append("teamId", this.state.teamId)
        formParams.append("isTeamAdmin", this.state.toBeAddedUserRole)

        axios.post('/api/userTeam/addRelation', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Member Added!",
                    snackbarIsShown: true,
                    toBeAddedUserMailAddress: null
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
        this.getUserInfo()
        this.getTeamMemberInfo()
    }

    componentWillUnmount() {
        // cancel requests
    }
}