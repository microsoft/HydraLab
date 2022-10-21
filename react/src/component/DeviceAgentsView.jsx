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
            const topRadius = '10px'
            agents.map((agent) => {
                agentRows.push(
                    <tbody key={agent.agentId}>
                        <tr style={{ color: 'white' }}>
                            <th width="100%" style={{ backgroundColor: folderHeadBgColor, color: 'white', borderRadius: topRadius + ' 0px 0px 0px' }} onClick={() => this.changeCollapseStatus(agent.agentId)}>
                                {agent.agentName}: {agent.devices.length}
                            </th>
                            <th style={{ backgroundColor: folderHeadBgColor, color: 'white' }}>
                                {agent.agentVersionCode > this.state.latestAgentVersion ?
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
                                    : agent.agentStatus === "UPDATING" ?
                                        <Tooltip
                                            title={this.state.agentUpdateStatus}
                                            onOpen={() => this.getUpdateStatus(agent.agentId)}
                                            style={{ padding: "0" }}>
                                            <Button color="inherit" size="small" style={{ padding: "0" }}>
                                                Updating
                                            </Button>
                                        </Tooltip> :
                                        <Button variant="contained" color="error" size="small"
                                            style={{ padding: "0" }}
                                            onClick={() => this.updateAgent(agent)}>
                                            Update
                                        </Button>
                                }
                            </th>
                            <th onClick={() => this.changeCollapseStatus(agent.agentId)} style={{ backgroundColor: folderHeadBgColor }}>
                                {agent.userName}
                            </th>
                            <th align='right' onClick={() => this.changeCollapseStatus(agent.agentId)} style={{ backgroundColor: folderHeadBgColor, borderRadius: '0px ' + topRadius + ' 0px 0px' }}>
                                {this.state.collapseStatus[agent.agentId] ?
                                    <span className="material-icons-outlined">expand_less</span> :
                                    <span className="material-icons-outlined">expand_more</span>}
                            </th>
                        </tr>
                        <tr>
                            <td align='center'
                                colSpan='3'>
                                {agent.groupedDevices.map((group) => {
                                    return <table className="table table-borderless scrollable">
                                        <tbody key={group[0].name}>
                                            <tr><Collapse in={this.state.collapseStatus[agent.agentId]}>
                                                {group.map((item) => {
                                                    return <td key={item.deviceId}
                                                        align='center'>
                                                        <DeviceDetailView deviceItem={item} />
                                                    </td>
                                                })}
                                            </Collapse>
                                            </tr>
                                        </tbody>
                                    </table>
                                }
                                )}
                            </td>
                        </tr>
                    </tbody>)
            }
            )
        }
        return <div>
            <table className="table table-borderless">
                <thead>
                    <tr>
                        <th colSpan="2">
                            <Typography variant="h4" className="mt-2 mb-2">
                                Connected Devices</Typography>
                        </th>
                        <th colSpan="2" style={{ textAlign: "right", verticalAlign: "middle" }}>
                            <LoadingButton
                                variant="contained"
                                className="pl-4 pr-4"
                                loading={refreshing}
                                loadingPosition="end"
                                endIcon={<span
                                    className="material-icons-outlined">sync</span>}
                                onClick={this.updateDeviceListData}

                            >
                                Refresh
                            </LoadingButton>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td colSpan='3' align="center" hidden={!this.state.refreshing}>
                            <Skeleton variant="text" className="w-100 p-3"
                                height={100} />
                            <Skeleton variant="text" className="w-100 p-3"
                                height={100} />
                            <Skeleton variant="text" className="w-100 p-3"
                                height={100} />
                        </td>
                    </tr>
                </tbody>
                {agentRows}
            </table>
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
    }

    changeCollapseStatus(agentId) {
        const collapseStatusNew = this.state.collapseStatus
        collapseStatusNew[agentId] = !this.state.collapseStatus[agentId]
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
                    for (let j = 0; j < agents[i].devices.length; j += 4) {
                        agents[i].groupedDevices.push(agents[i].devices.slice(j, j + 4))
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