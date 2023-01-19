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
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import PropTypes from 'prop-types';
import Box from '@mui/material/Box';
import Stack from "@mui/material/Stack";
import Skeleton from '@mui/material/Skeleton';
import BaseView from "@/component/BaseView";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import copy from 'copy-to-clipboard';


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

function TabPanel(props) {
    const {children, value, index, ...other} = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}
        >
            {value === index && (
                <Box sx={{p: 3}}>
                    <Typography>{children}</Typography>
                </Box>
            )}
        </div>
    );
}

TabPanel.propTypes = {
    children: PropTypes.node,
    index: PropTypes.number.isRequired,
    value: PropTypes.number.isRequired,
};

export default class AuthView extends BaseView {
    state = {
        hideSkeleton: true,
        tabValue: 0,

        agentList: null,
        agentName: null,
        agentTeam: null,
        agentOS: null,
        currentAgent: null,
        agentCreateDialogIsShown: false,
        agentDeleteDialogIsShown: false,
        tokenDeleteDialogIsShown: false,

        toBeDeletedAgentId: null,
        toBeDeletedTokenId: null,

        tokenList: null,
        teamList: null,
        selectedTeamId: null
    }

    render() {
        const tabValue = this.state.tabValue
        const snackbarIsShown = this.state.snackbarIsShown
        const snackbarSeverity = this.state.snackbarSeverity
        const snackbarMessage = this.state.snackbarMessage

        const agentList = this.state.agentList;
        const agentHeadItems = ['Agent Name', 'Team', 'OS', 'Owner', 'Operation']
        const agentHeads = []
        const agentRows = []
        const currentAgent = this.state.currentAgent
        const agentCreateDialogIsShown = this.state.agentCreateDialogIsShown
        const agentDeleteDialogIsShown = this.state.agentDeleteDialogIsShown
        const tokenDeleteDialogIsShown = this.state.tokenDeleteDialogIsShown

        const tokenList = this.state.tokenList
        const tokenHeadItems = ['Token', 'Owner', 'Operation']
        const tokenHeads = []
        const tokenRows = []

        const teamList = this.state.teamList

        if (agentList) {
            agentList.forEach((t) => {
                agentRows.push(<StyledTableRow key={t.id} id={t.id} hover>
                    <TableCell id={t.id} align="center">
                        {t.name}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.teamName}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.os}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.mailAddress}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <IconButton onClick={() => this.downloadAgentConfigFile(t.id)}>
                            <span className="material-icons-outlined">download_for_offline</span>
                        </IconButton>
                        <IconButton onClick={() => this.getAgentInfo(t.id)}>
                            <span className="material-icons-outlined">info</span>
                        </IconButton>
                        <IconButton onClick={() => this.openDeleteAgentDialog(t.id)}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        agentHeadItems.forEach((k) => agentHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        if (tokenList) {
            tokenList.forEach((t) => {
                tokenRows.push(<StyledTableRow key={t.id} id={t.id} hover>
                    <TableCell id={t.id} align="center">
                        {t.token}&nbsp;
                        <IconButton onClick={() => this.copyContent(t.token)}>
                            <span className="material-icons-outlined">content_copy</span>
                        </IconButton>
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.creator}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <IconButton onClick={() => this.openDeleteTokenDialog(t.id)}>
                            <span className="material-icons-outlined">delete</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        tokenHeadItems.forEach((k) => tokenHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        return <div>
            <center>
                <Tabs value={tabValue} onChange={this.handleTabChange}
                      aria-label="basic tabs example">
                    <Tab label="Agents" id="simple-tab-0"
                         aria-controls="simple-tabpanel-0"/>
                    <Tab label="Tokens" id="simple-tab-1"
                         aria-controls="simple-tabpanel-1"/>
                </Tabs>
                <TabPanel index={0} value={tabValue}>
                    <TableContainer style={{margin: "auto"}}>
                        <Table size="medium">
                            <TableHead>
                                <TableRow>
                                    <TableCell colSpan="4">
                                        <Typography variant="h4" className="mt-2 mb-2">
                                            Agent Management</Typography>
                                    </TableCell>
                                    <TableCell colSpan="2">
                                        <Stack direction="row" spacing={2}
                                               justifyContent="flex-end">
                                            <Button variant="contained"
                                                    endIcon={<span
                                                        className="material-icons-outlined">add</span>}
                                                    onClick={() => this.handleStatus("agentCreateDialogIsShown", true)}>
                                                Add Agent
                                            </Button>
                                        </Stack>
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    {agentHeads}
                                </TableRow>
                            </TableHead>
                            <TableCell colSpan="5" align="center" hidden={this.state.hideSkeleton}>
                                <Skeleton variant="text" className="w-100 p-3"
                                          height={100}/>
                                <Skeleton variant="text" className="w-100 p-3"
                                          height={100}/>
                                <Skeleton variant="text" className="w-100 p-3"
                                          height={100}/>
                            </TableCell>
                            <TableBody>
                                {agentRows}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </TabPanel>
                <TabPanel index={1} value={tabValue}>
                    <TableContainer style={{margin: "auto"}}>
                        <Table size="medium">
                            <TableHead>
                                <TableRow>
                                    <TableCell colSpan="1">
                                        <Typography variant="h4" className="mt-2 mb-2">
                                            Token Management</Typography>
                                    </TableCell>
                                    <TableCell colSpan="2">
                                        <Stack direction="row" spacing={2}
                                               justifyContent="flex-end">
                                            <Button variant="contained"
                                                    endIcon={<span
                                                        className="material-icons-outlined">add</span>}
                                                    onClick={() => this.addToken()}>
                                                Add Token
                                            </Button>
                                        </Stack>
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    {tokenHeads}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {tokenRows}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </TabPanel>
            </center>
            <Dialog open={currentAgent}
                    fullWidth={true}
                    onClose={() => this.handleStatus("currentAgent", null)}>
                <DialogTitle>Agent detail</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Agent ID: {currentAgent != null ? currentAgent.id : null} <br/>
                        Agent secret: {currentAgent != null ? (currentAgent.secret ? currentAgent.secret : 'No permission') : null}
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("currentAgent", null)}>Done</Button>
                </DialogActions>
            </Dialog>
            <Dialog open={agentCreateDialogIsShown}
                    fullWidth={true}
                    onClose={() => this.handleStatus("agentCreateDialogIsShown", false)}>
                <DialogTitle>Add agent</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        name="agentName"
                        label="Name"
                        type="text"
                        required
                        fullWidth
                        variant="standard"
                        onChange={this.handleValueChange}
                    /> <br/>
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
                    <TextField
                        margin="dense"
                        name="agentOS"
                        label="OS"
                        type="text"
                        fullWidth
                        variant="standard"
                        onChange={this.handleValueChange}
                    /> <br/>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("agentCreateDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.createAgent()}>
                        Save
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={agentDeleteDialogIsShown}
                onClose={() => this.handleStatus("agentDeleteDialogIsShown", false)}
            >
                <DialogTitle> Delete this agent? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to delete this agent, this operation is irreversible
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("agentDeleteDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.deleteAgent()}>
                        Confirm
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={tokenDeleteDialogIsShown}
                onClose={() => this.handleStatus("tokenDeleteDialogIsShown", false)}
            >
                <DialogTitle> Delete this token? </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please confirm if you want to delete this token, this operation is irreversible
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => this.handleStatus("tokenDeleteDialogIsShown", false)}>
                        Cancel
                    </Button>
                    <Button onClick={() => this.deleteToken()}>
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

    handleTabChange = (event, value) => {
        this.setState({
            tabValue: value
        })
    }

    createAgent = () => {
        if (!this.state.selectedTeamId) {
            this.snackBarMsg("Please select a team")
            return
        }

        this.setState({
            agentCreateDialogIsShown: false
        })

        const formParams = new URLSearchParams()
        formParams.append("teamId", this.state.selectedTeamId)
        formParams.append("name", this.state.agentName)
        formParams.append("os", this.state.agentOS)

        axios.post('/api/agent/create/', formParams, {
            headers: {'content-type': 'application/x-www-form-urlencoded'}
        }).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Agent successfully created",
                    snackbarIsShown: true,
                    agentName: null,
                    agentTeam: null,
                    agentOS: null
                })
                this.refreshAgentList()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    openDeleteAgentDialog = (agentId) => {
        this.handleStatus("agentDeleteDialogIsShown", true);
        this.handleStatus('toBeDeletedAgentId', agentId)
    }

    deleteAgent = () => {
        // console.log(`Delete Agent ${this.state.toBeDeletedAgentId}`)
        this.handleStatus("agentDeleteDialogIsShown", false);
        axios.get('/api/auth/deleteAgent/' + this.state.toBeDeletedAgentId).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Agent deleted!",
                    snackbarIsShown: true,
                    toBeDeletedAgentId: null
                })
                this.refreshAgentList()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    addToken() {
        axios.get('/api/auth/create').then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Token created!",
                    snackbarIsShown: true
                })
                this.refreshTokenList()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    openDeleteTokenDialog = (tokenId) => {
        this.handleStatus("tokenDeleteDialogIsShown", true);
        this.handleStatus('toBeDeletedTokenId', tokenId)
    }

    deleteToken = () => {
        // console.log(`Delete ${this.state.toBeDeletedTokenId}`)
        this.handleStatus("tokenDeleteDialogIsShown", false)
        axios.get('/api/auth/deleteToken/' + this.state.toBeDeletedTokenId).then(res => {
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Token deleted!",
                    snackbarIsShown: true,
                    toBeDeletedTokenId: null
                })
                this.refreshTokenList()
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    copyContent(groupToken) {
        copy(groupToken)
        this.setState({
            snackbarIsShown: true,
            snackbarSeverity: "success",
            snackbarMessage: "Token copied!"
        })
    }

    refreshAgentList() {
        this.setState({
            hideSkeleton: false,
            agentList: null,
        })
        axios.get('/api/agent/list').then(res => {
            const agentList = res.data.content;
            console.log(agentList)
            this.setState({
                agentList: agentList,
                hideSkeleton: true
            })
        })
    }

    refreshTokenList() {
        this.setState({
            hideSkeleton: false,
            tokenList: null,
        })
        axios.get('/api/auth/querySelfToken').then(res => {
            const tokenList = res.data.content;
            console.log(tokenList)
            this.setState({
                tokenList: tokenList,
                hideSkeleton: true
            })
        })
    }

    getAgentInfo(agentId) {
        this.handleStatus('currentAgent', { 'id': agentId, "secret": 'Loading...' })
        axios.get(`/api/agent/${agentId}`).then(res => {
            console.log(res.data)
            if (res.data && res.data.code === 200) {
                this.setState({
                    currentAgent: res.data.content,
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }

    downloadAgentConfigFile(agentId) {
        axios({
            url: `/api/agent/downloadAgentConfigFile/${agentId}`,
            method: 'GET',
            responseType: 'blob'
        }).then((res) => {
            if (res.data.type.includes('application/json')) {
                let reader = new FileReader()
                reader.onload = function () {
                    let result = JSON.parse(reader.result)
                    if (result.code !==  200) {
                        this.setState({
                            snackbarIsShown: true,
                            snackbarSeverity: "error",
                            snackbarMessage: "The file could not be downloaded"
                        })
                    }
                }
                reader.readAsText(res.data)
            } else {
                const href = URL.createObjectURL(res.data);
                const link = document.createElement('a');
                link.href = href;
                link.setAttribute('download', 'application.yml');
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                URL.revokeObjectURL(href);

                if (res.data.code === 200) {
                    this.setState({
                        snackbarIsShown: true,
                        snackbarSeverity: "success",
                        snackbarMessage: "Agent config file downloaded"
                    })
                }
            }
        }).catch(this.snackBarError);
    }

    componentDidMount() {
        this.getUserInfo();
        this.refreshAgentList()
        this.refreshTeamList()
        this.refreshTokenList()
    }

    componentWillUnmount() {
        // cancel requests
    }
}