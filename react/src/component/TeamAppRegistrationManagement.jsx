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

export default class TeamAppRegistrationManagement extends BaseView {
    state = {
        teamId: this.props.teamId,

        clientIdList: null,

        appLinkDialogIsShown: false,
        toBeAddedClientId: null,

        appRemoveDialogIsShown: false,
        toBeRemovedClientId: null,

        hideSkeleton: true
    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state

        const appRegHeadItems = ['Client ID', 'Operation']
        const appRegHeads = []
        const appRegRows = []

        const { clientIdList } = this.state

        if (clientIdList) {
            clientIdList.sort((a, b) => {
                if ((a.teamAdmin && b.teamAdmin) || (!a.teamAdmin && !b.teamAdmin)) {
                    return a.name > b.name ? 1 : -1
                }
                else {
                    return a.teamAdmin ? -1 : 1
                }
            })
            clientIdList.forEach((clientId) => {
                appRegRows.push(<StyledTableRow key={clientId} id={clientId} hover>
                    <TableCell id={clientId} align="center">
                        {clientId}
                    </TableCell>
                    <TableCell id={clientId} align="center">
                        <IconButton onClick={() => this.openRemoveClientIdDialog(clientId)}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        appRegHeadItems.forEach((k) => appRegHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        return <div>
            <TableContainer style={{margin: "auto"}}>
                <Table size="medium">
                    <TableHead>
                        <TableRow>
                            <TableCell colSpan="1">
                                <Typography variant="h4" className="mt-2 mb-2">
                                    {`App Registration List`} </Typography>
                            </TableCell>
                            <TableCell colSpan="1">
                                <Stack direction="row" spacing={2}
                                       justifyContent="flex-end">
                                    <Button variant="contained" endIcon={<span className="material-icons-outlined">add</span>} onClick={() => this.handleStatus("appLinkDialogIsShown", true)}>
                                        Link App
                                    </Button>
                                </Stack>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            {appRegHeads}
                        </TableRow>
                    </TableHead>
                    <TableCell colSpan="2" align="center" hidden={this.state.hideSkeleton}>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                        <Skeleton variant="text" className="w-100 p-3" height={100}/>
                    </TableCell>
                    <TableBody>
                        {appRegRows}
                    </TableBody>
                </Table>
            </TableContainer>
            <Dialog open={this.state.appLinkDialogIsShown}
                    fullWidth={true}
                    onClose={() => this.handleStatus("appLinkDialogIsShown", false)}>
                <DialogTitle>Link App</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        name="toBeAddedClientId"
                        label="Client ID"
                        type="text"
                        fullWidth
                        variant="outlined"
                        size="small"
                        onChange={this.handleValueChange}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("appLinkDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.linkApp()}>
                        Add
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={this.state.appRemoveDialogIsShown}
                onClose={() => this.handleStatus("appRemoveDialogIsShown", false)}
            >
                <DialogTitle> Remove this Client ID? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to remove this client ID.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("appRemoveDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.removeApp()}>
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

    linkApp = () => {
        this.setState({
            appLinkDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("appClientId", this.state.toBeAddedClientId)
        formParams.append("teamId", this.state.teamId)

        axios.post('/api/teamApp/addRelation', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Client ID Linked!",
                    snackbarIsShown: true,
                    toBeAddedClientId: null
                })
                this.getTeamClientInfo()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    openRemoveClientIdDialog = (clientId) => {
        console.log(clientId)
        this.handleStatus("appRemoveDialogIsShown", true);
        this.handleStatus('toBeRemovedClientId', clientId)
    }

    removeApp = () => {
        this.setState({
            appRemoveDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("appClientId", this.state.toBeRemovedClientId)
        formParams.append("teamId", this.state.teamId)

        axios.post('/api/teamApp/deleteRelation', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Client ID Removed!",
                    snackbarIsShown: true,
                    toBeRemovedClientId: null
                })
                this.getTeamClientInfo()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    getTeamClientInfo() {
        this.setState({
            hideSkeleton: false
        })

        const formParams = new URLSearchParams()
        formParams.append("teamId", this.state.teamId)

        axios.post('/api/team/clientIds', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    clientIdList: res.data.content,
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
        this.getTeamClientInfo()
    }

    componentWillUnmount() {
        // cancel requests
    }
}