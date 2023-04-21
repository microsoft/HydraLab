// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React from 'react'
import 'bootstrap/dist/css/bootstrap.css'
import axios from '@/axios'
import DeviceDetailView from '@/component/DeviceDetailView';
import Collapse from '@mui/material/Collapse';
import Typography from "@material-ui/core/Typography";
import LoadingButton from "@mui/lab/LoadingButton";
import Skeleton from "@mui/material/Skeleton";
import BaseView from "@/component/BaseView";
import IconButton from "@material-ui/core/IconButton";
import Tooltip from "@mui/material/Tooltip";
import Stack from "@mui/material/Stack";
import Button from "@mui/material/Button";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import '../css/deviceAgentsView.css';

export default class DeviceAgentsView extends BaseView {

    state = {
        agents: null,
        collapseStatus: {},
        refreshing: false,
        latestAgentVersion: null,
        fileId: null,
        agentUpdateStatus: "UPDATING"
    }

    render() {
        const { agents, refreshing } = this.state;
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state
        const agentRows = []
        if (agents) {
            const folderHeadBgColor = '#1565c0'
            const topRadius = ' 10px '
            agents.map(
                (agent) => {
                    agentRows.push(
                        <div class='deviceAgents-agent'>
                            <div
                                class='deviceAgents-agentBanner'
                                style={{ backgroundColor: folderHeadBgColor }}
                                onClick={() => this.changeCollapseStatus(agent.agentId)}>

                                <div style={{ color: 'white', fontSize: 'large', fontWeight: 'bold'}}>
                                    {agent.agentName}: {agent.devices.length}
                                </div>

                                <div class='deviceAgents-agentBanner-tail'>
                                    <div style={{ color: 'white' }}>
                                        {
                                            Number(agent.agentVersionCode) >= Number(this.state.latestAgentVersion) ?
                                                <Tooltip
                                                    title={
                                                        <Stack>
                                                            Host:{agent.hostname}<br />
                                                            Version:{agent.agentVersionName}<br />
                                                            OS:{agent.agentOS}
                                                        </Stack>}
                                                    style={{ padding: "0" }}>
                                                    <IconButton>
                                                        <span style={{ color: 'white', }}
                                                            className="material-icons-outlined">info</span>
                                                    </IconButton>
                                                </Tooltip>
                                                :
                                                    agent.agentStatus === "UPDATING" ?
                                                    <Tooltip
                                                        title={this.state.agentUpdateStatus}
                                                        onOpen={() => this.getUpdateStatus(agent.agentId)}
                                                        style={{ padding: "0" }}>
                                                        <Button color="inherit" size="small" style={{ padding: "0" }}>
                                                            Updating
                                                        </Button>
                                                    </Tooltip>
                                                    :
                                                    <Button variant="contained" color="error" size="small"
                                                        style={{ padding: "0" }}
                                                        onClick={() => this.updateAgent(agent)}>
                                                        Update
                                                    </Button>
                                        }
                                    </div>
                                    <div style={{ color: 'white' }}>
                                        {agent.userName}
                                    </div>
                                    <div style={{ color: 'white' }}>
                                        {
                                            this.state.collapseStatus[agent.agentId] ?
                                                <span className="material-icons-outlined">expand_less</span> :
                                                <span className="material-icons-outlined">expand_more</span>
                                        }
                                    </div>
                                </div>
                            </div>
                            <Collapse in={this.state.collapseStatus[agent.agentId]}>
                                <div className='deviceAgents-devicesList'>
                                    {
                                        agent.devices.map((d) => {
                                            return <DeviceDetailView deviceItem={d} />
                                        })
                                    }
                                </div>
                            </Collapse>
                        </div>)
                })
        }
        return (
            <div class='deviceAgents'>
                <div class='deviceAgents-header'>
                    <Typography variant="h4" className="mt-2 mb-2">Connected Devices</Typography>
                    <div style={{ display: 'flex', gap: '16px' }}>
                        <Button
                            variant="outlined"
                            className="pl-4 pr-4"
                            endIcon={<span className="material-icons-outlined">auto_awesome_motion</span>}
                            onClick={this.setAllCollapseStatus}>
                            Collapse
                        </Button>
                        <LoadingButton
                            variant="contained"
                            className="pl-4 pr-4"
                            loading={refreshing}
                            loadingPosition="end"
                            endIcon={<span className="material-icons-outlined">sync</span>}
                            onClick={this.updateDeviceListData}>
                            Refresh
                        </LoadingButton>
                    </div>
                </div>
                {agentRows}
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
        )
    }

    changeCollapseStatus(agentId) {
        const collapseStatusNew = this.state.collapseStatus
        collapseStatusNew[agentId] = !this.state.collapseStatus[agentId]
        this.setState({
            collapseStatus: collapseStatusNew,
        })
    }

    setAllCollapseStatus = () => {
        const collapseStatusNew = this.state.collapseStatus
        for (const key in collapseStatusNew) {
            collapseStatusNew[key] = false
        }
        this.setState({
            collapseStatus: collapseStatusNew,
        })
    }

    updateDeviceListData = () => {
        this.setState({
            refreshing: true,
            agents: null,
        })
        axios.get('/api/device/list').then(res => {
            if (res.data && res.data.code === 200) {
                console.log(res.data)
                const agents = res.data.content
                const collapseStatus = {}
                for (let i = 0; i < agents.length; i++) {
                    agents[i].groupedDevices = []
                    const sliceLen = agents[i].agentDeviceType == 2 ? 2 : 4;
                    for (let j = 0; j < agents[i].devices.length; j += sliceLen) {
                        agents[i].groupedDevices.push(agents[i].devices.slice(j, j + sliceLen))
                    }
                    collapseStatus[agents[i].agentId] = true
                }

                this.setState({
                    agents: agents,
                    collapseStatus: collapseStatus,
                    refreshing: false,
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    getLatestAgentVersion = () => {
        axios.get('/api/center/info').then(res => {
            if (res.data && res.data.code === 200) {
                if (res.data.content && res.data.content.agentPkg) {
                    this.setState({
                        latestAgentVersion: res.data.content.agentPkg.fileParser.versionCode,
                        fileId: res.data.content.agentPkg.fileId
                    })
                }
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    getUpdateStatus(agentId) {
        axios.get('/api/agent/getUpdateInfo/' + agentId).then(res => {
            if (res.data && res.data.code === 200) {
                let messages = res.data.content.updateMsgs
                console.log(messages)
                if (messages.at(-1) !== null) {
                    console.log(messages.at(-1).message)
                    this.setState({
                        agentUpdateStatus: messages.at(-1).message,
                    })
                }
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    updateAgent(agent) {
        const formParams = new URLSearchParams()
        formParams.append("agentId", agent.agentId)
        formParams.append("fileId", this.state.fileId)

        axios.post('/api/agent/updateAgent/', formParams, {
            headers: { 'content-type': 'application/x-www-form-urlencoded' }
        }).then(res => {
            if (res.data && res.data.code === 200) {
                agent.agentStatus = "UPDATING"
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Start updating agent",
                    snackbarIsShown: true,
                })
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    }

    componentDidMount() {
        this.getLatestAgentVersion()
        this.updateDeviceListData()
    }
}