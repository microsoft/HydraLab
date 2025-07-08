// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React from 'react'
import axios from '@/axios'
import 'bootstrap/dist/css/bootstrap.css'
import AdaptivePropertyTable from '@/component/PropertyTable'
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormControl from '@material-ui/core/FormControl';
import TestReportView from '@/component/TestReportView';
import AnalysisReportView from '@/component/AnalysisReportView';
import VideoNavView from '@/component/VideoNavView';
import Typography from "@material-ui/core/Typography";
import TextField from "@mui/material/TextField";
import Stack from "@mui/material/Stack";
import Alert from "@mui/material/Alert";
import Snackbar from "@mui/material/Snackbar";
import LoadingButton from "@mui/lab/LoadingButton";
import Skeleton from "@mui/material/Skeleton";
import BaseView from "@/component/BaseView";
import Container from '@mui/material/Container';

export default class SearchView extends BaseView {
    state = {
        infoDisplay: <center>
            <div />
        </center>,
        infoId: this.props.infoId ? this.props.infoId : "",
        requestPath: this.props.infoType ? this.props.infoType : "case",
        querying: false,
    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state

        const dV = this.props.infoType ? this.props.infoType : "case"
        return <div>
            <div style={{ margin: "16px" }}>
                <FormControl component="fieldset" style={{ width: '100%' }}>
                    <Typography variant="h4" className="mt-2 mb-2">Search type</Typography>
                    <RadioGroup row aria-label="position" name="position" defaultValue={dV}
                        onChange={this.searchTypeChange}>
                        <FormControlLabel
                            value="case"
                            control={<Radio color="primary" />}
                            label="Test Case"
                            labelPlacement="end" />
                        <FormControlLabel
                            value="crash"
                            control={<Radio color="primary" />}
                            label="Crash" />
                        <FormControlLabel
                            value="videos"
                            control={<Radio color="primary" />}
                            label="Videos" />
                        <FormControlLabel
                            value="task"
                            control={<Radio color="primary" />}
                            label="Task report" />
                    </RadioGroup>
                    <Stack direction="row" spacing={2} alignItems="center" style={{ width: '720px' }}>
                        <TextField fullWidth
                            type="text" label="Enter info id" aria-label="Enter info id"
                            aria-describedby="basic-addon2" value={this.state.infoId}
                            onChange={this.infoIdChanged}
                            onKeyDown={(e) => this.onkeydown(e)} />
                        <div className="input-group-append">
                            <LoadingButton
                                variant="contained"
                                type="button"
                                onClick={this.queryInfoById}
                                loading={this.state.querying}
                                loadingPosition="end"
                                endIcon={<span
                                    className="material-icons-outlined">search</span>}>
                                Query
                            </LoadingButton>
                        </div>
                    </Stack>
                    <Stack direction="row" alignItems="center">
                        <Container>{this.state.infoDisplay}</Container>
                    </Stack>
                </FormControl>
            </div>
            <div hidden={!this.state.querying}>
                <Skeleton variant="text" className="w-100 p-3"
                    height={100} />
                <Skeleton variant="text" className="w-100 p-3"
                    height={100} />
                <Skeleton variant="text" className="w-100 p-3"
                    height={100} />
            </div>
            <Snackbar
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'center'
                }}
                open={snackbarIsShown}
                autoHideDuration={3000}
                onClose={() => this.handleStatus("snackbarIsShown", false)}>
                <Alert
                    severity={snackbarSeverity}
                    sx={{ width: '100%' }}
                    onClose={() => this.handleStatus("snackbarIsShown", false)}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
            <div className="mt-2" />
        </div>
    }

    infoIdChanged = (element) => {
        this.setState({
            infoId: element.target.value
        })
    }
    searchTypeChange = (element) => {
        console.log(element.target.value)
        this.setState({
            requestPath: element.target.value
        })
    }

    onkeydown = (e) => {
        if (e.keyCode === 13) {
            this.queryInfoById()
        }
    }

    queryInfoById = () => {
        if (this.state.querying) {
            return
        }
        console.log("start query")
        var pattern = /\w{8}(-\w{4}){3}-\w{12}/
        if (!pattern.test(this.state.infoId)) {
            this.setState({
                infoId: "",
                snackbarSeverity: "error",
                snackbarMessage: "Only UUID is allowed!",
                snackbarIsShown: true,
            })
            return
        }

        this.setState({
            querying: true
        })

        const requestPath = this.state.requestPath
        console.log(requestPath)

        axios.get('/api/test/' + requestPath + '/' + this.state.infoId,).then(res => {
            const details = res.data.content
            if (requestPath === "videos") {
                for (let i = 0; i < details.videos.length; i++) {
                    details.videos[i] = details.videos[i] + '?' + require('local-storage').get('FileToken');
                }
                const vList = details.videos
                const info = details.videoInfo
                this.setState({
                    infoDisplay: <VideoNavView videoInfo={info} videos={vList} />,
                    querying: false
                })
            } else if (requestPath === "crash") {
                const properties = []
                properties.push({ k: "LogHTML", v: details })
                this.setState({
                    infoDisplay: <center><AdaptivePropertyTable properties={properties}
                        title='Test Case Details'
                        propertyValueProcessor={(key, value) => {
                            if (key.toLowerCase().includes('stack')) {
                                return <pre>{value.toString()}</pre>
                            }
                            if (key.toLowerCase().includes("html")) {
                                return <pre><div
                                    dangerouslySetInnerHTML={{ __html: value.toString() }} /></pre>
                            }
                            return null
                        }} /></center>,
                    querying: false
                })
            } else if (requestPath === "task") {
                if (res.data && res.data.code === 200) {
                    const details = res.data.content;
                    this.setState({
                        infoDisplay: details.analysisConfigs ? <center><AnalysisReportView testTask={details} /></center> : <center><TestReportView testTask={details} /></center>,
                        querying: false
                    })
                } else {
                    this.snackBarFail(res)
                }
            } else {
                axios.get('/api/test/task/device/' + details.deviceTestResultId,).then(res => {
                    if (res.data && res.data.code === 200) {


                        const vList = [res.data.content.videoBlobUrl + '?' + require('local-storage').get('FileToken')]
                        const info = res.data.content.videoTimeTagArr
                        const properties = []
                        const suggestions = []

                        for (var k in details) {
                            if (k === "stream") {
                                continue
                            }
                            if (k === "suggestion") {
                                const obj = JSON.parse(details[k]);
                                for (const [key, value] of Object.entries(obj)) {
                                    suggestions.push({ k: key, v: value })
                                }
                            }
                            else {
                                properties.push({ k: k, v: details[k] })
                            }
                        }

                        this.setState({
                            infoDisplay: <center>
                                <VideoNavView videoInfo={info} videos={vList} />
                                {
                                    suggestions.length > 0 ?
                                    <AdaptivePropertyTable properties={suggestions}
                                        title='LLM Suggestions'
                                        lineTextCount='1'
                                        propertyValueProcessor={(key, value) => {
                                            return <div>{value}</div>
                                        }} /> : null
                                }
                                <AdaptivePropertyTable properties={properties}
                                    title='Test Case Details'
                                    propertyValueProcessor={(key, value) => {
                                        if (key.toLowerCase().includes('stack')) {
                                            return <pre>{value.toString()}</pre>
                                        }
                                        if (key.toLowerCase().includes("html")) {
                                            return <pre><div
                                                dangerouslySetInnerHTML={{ __html: value.toString() }} /></pre>
                                        }
                                        if (key === "deviceTestResultId" || key === "id" || key === "relEndTimeInVideo"
                                            || key === "startTimeMillis" || key === "numtests"
                                            || key === "testIndex" || key === "testTaskId" || key === "statusCode"
                                            || key === "relStartTimeInVideo" || key === "endTimeMillis") {
                                            return "SKIP"
                                        }
                                        return null
                                    }} />
                            </center>,
                            querying: false
                        })
                    } else {
                        this.snackBarFail(res)
                    }
                }).catch(this.snackBarError)
            }
        }).catch(this.snackBarError)
    }

    componentDidMount() {
        if (this.state.infoId) {
            this.queryInfoById()
        }
    }

    componentWillUnmount() {
        // cancel requests
        console.log("componentWillUnmount")
    }

    shouldComponentUpdate(nextProps, nextState, nextContext) {
        console.log("shouldComponentUpdate:" + nextProps + ", " + nextState + ", " + nextContext)
        return true
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        console.log("componentDidUpdate:" + prevProps + ", " + prevState + ", " + snapshot)
    }

}