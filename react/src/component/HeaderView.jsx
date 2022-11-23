// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import BaseView from "@/component/BaseView";
import Stack from "@mui/material/Stack";
import Tooltip from "@mui/material/Tooltip";
import IconButton from "@material-ui/core/IconButton";
import Box from "@mui/material/Box";
import Avatar from "@mui/material/Avatar";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Typography from "@mui/material/Typography";
import axios from "@/axios";
import React from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import {Badge, styled} from "@mui/material";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";

const StyledBadge = styled(Badge)(({ theme }) => ({
    '& .MuiBadge-badge': {
        right: 10,
        top: 45,
        border: `2px solid ${theme.palette.background.paper}`,
        padding: '0 4px',
    },
}));


export default class HeaderView extends BaseView {

    state = {
        avatarOpen: false,
        helpOpen: false,
        portalVersion: "",

        changeDefaultTeamIsShown: false,
        selectedTeamId: null
    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state

        const settings = [
            { text: this.state.userInfo ? this.state.userInfo.userName : 'Loading', dialog: null },
            { text: `Default Team: ${this.state.userInfo && this.state.userInfo.defaultTeamName ? this.state.userInfo.defaultTeamName : 'Loading'}`, dialog: 'changeDefaultTeamIsShown' },
            { text: 'Logout', dialog: null }
        ];
        const helpSettings = [
            { text: 'Feedback', href: 'https://forms.office.com/r/0wnc2Sk0tp' },
            { text: 'Wiki', href: 'https://github.com/microsoft/HydraLab/wiki' },
            { text: 'About', href: 'https://microsoft.github.io/HydraLab/' }
        ];
        const {teamList, changeDefaultTeamIsShown} = this.state

        return <Stack direction="row" spacing={2} sx={{flexGrow: 1}}
                   justifyContent="flex-start">
                <Stack direction="column" justifyContent="center"
                       alignItems="flex-start" spacing={2}>
                    <a href="#" onClick={this.portalCheck} target="_self">
                        <img src="images/MsLogoHydraLab.png"
                             style={{
                                 "maxHeight": "40px",
                             }} alt={"Microsoft Logo"}/></a>
                </Stack>
                <Stack direction="column" justifyContent="center"
                       alignItems="flex-start" spacing={2}
                       sx={{flexGrow: 1}}>
                </Stack>
                <Box sx={{ flexGrow: 0 }}>
                    <Tooltip title={"Open help menu"}>
                        <IconButton onClick={() => this.handleStatus("helpOpen", true)}
                            sx={{ p: 0 }}>
                            <span className="material-icons-outlined" style={{ 'color': 'white' }}>help_outline</span>
                        </IconButton>
                    </Tooltip>
                    <Menu
                        sx={{ mt: '45px'}}
                        id="menu-help"
                        anchorOrigin={{
                            vertical: 'top',
                            horizontal: 'right',
                        }}
                        keepMounted
                        transformOrigin={{
                            vertical: 'top',
                            horizontal: 'right',
                        }}
                        open={this.state.helpOpen}
                        onClose={() => this.handleStatus("helpOpen", false)}
                    >
                        {helpSettings.map((setting) => (
                            <MenuItem key={setting.text}>
                                <Typography textAlign="center">
                                    <a target="_blank" href={setting.href} rel="noopener noreferrer">{setting.text}</a>
                                </Typography>
                            </MenuItem>
                        ))}
                    </Menu>
                    <Tooltip title={"Open user menu"}>
                        <StyledBadge
                            badgeContent={'Admin'}
                            color={"secondary"}
                            invisible={!this.state.userInfo || this.state.userInfo.roleName === 'USER'}
                        >
                            <IconButton onClick={() => this.handleStatus("avatarOpen", true)}
                                        sx={{p: 0}}>
                                <Avatar alt={this.state.userInfo ? this.state.userInfo.userName : 'Loading'} src={"/api/auth/getUserPhoto"}/>
                            </IconButton>
                        </StyledBadge>
                    </Tooltip>
                    <Menu
                        sx={{mt: '45px'}}
                        id="menu-appbar"
                        anchorOrigin={{
                            vertical: 'top',
                            horizontal: 'right',
                        }}
                        keepMounted
                        transformOrigin={{
                            vertical: 'top',
                            horizontal: 'right',
                        }}
                        open={this.state.avatarOpen}
                        onClose={() => this.handleStatus("avatarOpen", false)}
                    >
                        {settings.map((setting) => (
                            <MenuItem key={setting.text} onClick={() => {
                                this.handleStatus(setting.dialog, true)
                                this.getUserInfo()
                                this.refreshTeamList()
                            }}>
                                <Typography textAlign="center">{setting.text}</Typography>
                            </MenuItem>
                        ))}
                    </Menu>
                </Box>
                <Dialog open={changeDefaultTeamIsShown}
                        fullWidth={true}
                        onClose={() => this.handleStatus("changeDefaultTeamIsShown", false)}>
                    <DialogTitle>Default Team</DialogTitle>
                    <DialogContent>
                        <br/>
                        <FormControl required fullWidth={true}>
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
                                    <MenuItem value={team.teamId} key={team.teamId} disabled={this.state.userInfo && (team.teamId === this.state.userInfo.defaultTeamId)}>{team.teamName}</MenuItem>
                                )) : null}
                            </Select>
                        </FormControl> <br/>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => this.handleStatus("changeDefaultTeamIsShown", false)}>
                            Cancel
                        </Button>
                        <Button onClick={() => this.changeDefaultTeam()}>
                            Save
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
        </Stack>
    }

    portalCheck() {
        if (location.hostname === "localhost" || location.hostname === "127.0.0.1") {
            window.location.href = '/';
        } else {
            return "/portal/#/"
        }
    }

    changeDefaultTeam() {
        if (!this.state.selectedTeamId || (this.state.userInfo && this.state.selectedTeamId === this.state.userInfo.defaultTeamId)) {
            this.snackBarMsg("Please select a team different from the current default team")
        } else {
            const formParams = new URLSearchParams()
            formParams.append("teamId", this.state.selectedTeamId)

            axios.post('/api/userTeam/switchDefaultTeam', formParams, {
                headers: {'content-type': 'application/x-www-form-urlencoded'}
            }).then(res => {
                if (res.data.code === 200) {
                    this.setState({
                        snackbarSeverity: "success",
                        snackbarMessage: "Default team successfully updated",
                        snackbarIsShown: true,
                        selectedTeamId: null
                    })
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => {
                this.snackBarError(error)
            })
            this.setState({
                changeDefaultTeamIsShown: false,
            })
        }
    }

    componentDidMount() {
        this.getUserInfo()
        this.refreshTeamList()
    }
}