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

export default class HeaderView extends BaseView {

    state = {
        userName: null,
        avatarOpen: false,
        helpOpen: false,
        portalVersion: "",
    }

    render() {
        const settings = [this.state.userName, 'Logout'];
        const helpSettings = [
            { text: 'Feedback', href: 'https://forms.office.com/r/0wnc2Sk0tp' },
            { text: 'Wiki', href: 'https://github.com/microsoft/HydraLab/wiki' },
            { text: 'About', href: 'https://microsoft.github.io/HydraLab/' }
        ];

        return (
            <Stack direction="row" spacing={2} sx={{flexGrow: 1}}
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
                            <MenuItem key={setting}>
                                <Typography textAlign="center">
                                    <a target="_blank" href={setting.href} rel="noopener noreferrer">{setting.text}</a>
                                </Typography>
                            </MenuItem>
                        ))}
                    </Menu>
                    <Tooltip title={"Open user menu"}>
                        <IconButton onClick={() => this.handleStatus("avatarOpen", true)}
                                    sx={{p: 0}}>
                            <Avatar alt={this.state.userName} src={"/api/auth/getUserPhoto"}/>
                        </IconButton>
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
                            <MenuItem key={setting}>
                                <Typography textAlign="center">{setting}</Typography>
                            </MenuItem>
                        ))}
                    </Menu>
                </Box>
            </Stack>);
    }

    getLoginInfo = () => {
        axios.get('/api/auth/getUser').then(res => {
            if (res.data.code === 200) {
                const userInfo = res.data.content;
                console.log(userInfo)
                this.setState({
                    userName: userInfo.userName
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    portalCheck() {
        if (location.hostname === "localhost" || location.hostname === "127.0.0.1") {
            window.location.href = '/';
        } else {
            return "/portal/#/"
        }
    }

    componentDidMount() {
        this.getLoginInfo()
    }
}