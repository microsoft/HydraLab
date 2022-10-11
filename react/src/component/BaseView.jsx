// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React from "react";
import axios from "@/axios";

export default class BaseView extends React.Component {
    state = {
        snackbarIsShown: false,
        snackbarSeverity: null,
        snackbarMessage: null,
        teamList: null,
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
        this.setState({
            snackbarIsShown: true,
            snackbarSeverity: "error",
            snackbarMessage: msg
        })
    }

    handleStatus = (target, state) => {
        this.setState({
            [target]: state
        })
        console.log(target + " => " + JSON.stringify(state))
    }

    handleValueChange = (event) => {
        this.setState({
            [event.target.name]: event.target.value
        });
        console.log([event.target.name] + " => " + event.target.value)
    }

    handleFileUpload = (event) => {
        this.setState({
            [event.target.id]: event.target.files[0]
        })
        console.log(event.target.files[0].name)
    }

    refreshTeamList() {
        this.setState({
            teamList: null,
        })
        axios.get('/api/userTeam/listSelfTeam').then(res => {
            console.log(res.data)
            if (res.data && res.data.code === 200) {
                this.setState({
                    teamList: res.data.content,
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }
}