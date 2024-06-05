// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React from "react";
import axios from "@/axios";
import {withStyles} from "@material-ui/core/styles";
import TableCell from "@material-ui/core/TableCell";
import TableRow from "@material-ui/core/TableRow";
import {createTheme} from "@mui/material/styles";
import { initialize } from 'react-native-clarity';

initialize("kj0ror1pki");

/**
 * Palette
 * https://material-ui.com/customization/palette/
 */
export const StyledTableCell = withStyles((theme) => ({
    head: {
        backgroundColor: theme.palette.primary.dark,
        color: theme.palette.common.white,
    },
    body: {
        fontSize: 14,
    },
}))(TableCell);

export const StyledTableRow = withStyles((theme) => ({
    root: {
        '&:nth-of-type(odd)': {
            backgroundColor: theme.palette.action.selected,
        },
    },
}))(TableRow);

export const darkTheme = createTheme({
    palette: {
        mode: 'dark',
    },
});

export default class BaseView extends React.Component {
    state = {
        snackbarIsShown: false,
        snackbarSeverity: null,
        snackbarMessage: null,

        userInfo: null,
        teamList: null,

        selectedTeamId: null,
        selectedTeamName: null,
    }

    snackBarFail = (res) => {
        let message = res + ""
        if (res.data && res.data.message) {
            message = res.data.message
        } else if (res.response && res.response.data && res.response.data.message) {
            message = res.response.data.message
        }
        this.setState({
            snackbarIsShown: true,
            snackbarSeverity: "error",
            snackbarMessage: message + "",
        })
    }

    snackBarError = (error) => {
        console.log(error)
        this.setState({
            snackbarSeverity: "error",
            snackbarMessage: error + "",
            snackbarIsShown: true,
        })
    }

    snackBarMsg = (msg) => {
        console.log(msg)
        this.setState({
            snackbarIsShown: true,
            snackbarSeverity: "error",
            snackbarMessage: msg
        })
    }

    handleStatus = (target, state) => {
        if (target !== null) {
            this.setState({
                [target]: state
            })
            console.log(target + " => " + JSON.stringify(state))
        }
    }

    handleValueChange = (event) => {
        this.setState({
            [event.target.name]: event.target.value
        });
        console.log([event.target.name] + " => " + event.target.value)
    }
    
    handleValueSwitched = (event) => {
        this.setState({
            [event.target.name]: event.target.checked
        });
        console.log([event.target.name] + " => " + event.target.checked)
    }

    handleFileUpload = (event) => {
        this.setState({
            [event.target.id]: event.target.files[0]
        })
        console.log(event.target.files[0].name)
    }

    getUserInfo = () => {
        this.axiosGet('/api/auth/getUser', (content) => { this.setState({
            userInfo: content,
            selectedTeamId: content.defaultTeamId,
            selectedTeamName: content.defaultTeamName,
        }) })

    }

    refreshTeamList() {
        this.axiosGet('/api/userTeam/listSelfTeam', (content) => { this.setState({ teamList: content }) })
    }

    axiosGet(url, handleResponse) {
        axios.get(url).then(res => {
            if (res.data && res.data.code === 200) {
                handleResponse(res.data.content)
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => { this.snackBarError(error) })
    }

    axiosPost(url, handleResponse, postBody, formParams, formData, headers) {
        if (postBody) {
            axios.post(url, postBody).then(res => {
                if (res.data && res.data.code === 200) {
                    handleResponse(res.data.content)
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => { this.snackBarError(error) })
        } else if (formParams && headers) {
            axios.post(url, formParams, { headers: headers }).then(res => {
                if (res.data && res.data.code === 200) {
                    handleResponse(res.data.content)
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => { this.snackBarError(error) })
        } else if (formData && headers) {
            axios.post(url, formData, { headers: headers }).then(res => {
                if (res.data && res.data.code === 200) {
                    handleResponse(res.data.content)
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => { this.snackBarError(error) })
        }
    }
}