// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React, { PureComponent, useEffect } from 'react'
import { Link } from "react-router-dom";
import cssObj from '@/css/style.scss'
import 'bootstrap/dist/css/bootstrap.css'
import _ from 'lodash';
import { AreaChart, Area, XAxis, YAxis, Tooltip, PieChart, Pie, Cell, ReferenceLine } from 'recharts';
import moment from 'moment';
import CloudDownloadIcon from '@material-ui/icons/CloudDownload';
import Button from "@mui/material/Button";
import axios from "@/axios";
import PerfTestDashboard from './PerfTestDashboard';
import Sigma from "sigma";
import Graph from "graphology";
import { parse } from "graphology-gexf/browser";
import FA2Layout from "graphology-layout-forceatlas2/worker";
import forceAtlas2 from "graphology-layout-forceatlas2";
import BaseView from "@/component/BaseView";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";


const COLORS = ['#00C49F', '#FF8042'];
const badgeList = ['primary', 'info', 'secondary', 'light'];

class CustomizedLabel extends PureComponent {
    render() {
        const { x, y, stroke, value } = this.props;

        return (
            <text x={x} y={y} dy={-6} fill={stroke} fontSize={12} textAnchor="middle">
                {value}
            </text>
        );
    }
}
const formatDate = "MM/DD HH:mm"
const RADIAN = Math.PI / 180;
const PieCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
    const radius = innerRadius + (outerRadius - innerRadius) * 0.3;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
        <text x={x} y={y} fill="white" fontSize={10} textAnchor={x > cx ? 'start' : 'end'}
            dominantBaseline="central">
            {`${(percent * 100).toFixed(1)}%`}
        </text>
    );
};
export default class TestReportView extends BaseView {
    state = {
        task: this.props.testTask,
        history: null,
        overNode: null,
        selectedPath: [],
        graphFileId: null,
        graph: null,
        renderer: null,
        hoveredNode: null,
        hoveredNeighbors: undefined
    };

    render() {
        console.log("render")
        const task = this.state.task
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state
        let taskDatetime = moment(task.startDate).format(formatDate);
        const history = this.state.history
        let historyData = null;
        let historyDataLowBound = null;
        let historyDataHighBound = null;
        const overallPieSize = 180
        let overallAreaWidth = overallPieSize * 1.5;
        if (history) {
            let currentTestSuiteRec = _.groupBy(_.groupBy(history, 'testSuite')[task.testSuite], 'type')[task.type];
            if (!currentTestSuiteRec) {
                return
            }
            const indexOfTask = _.findIndex(currentTestSuiteRec, (t) => t.id === task.id)
            console.log(indexOfTask)
            if (indexOfTask > 8) {
                currentTestSuiteRec = _.takeRight(_.take(currentTestSuiteRec, indexOfTask + 6), 12)
            }

            historyData = _.take(
                _.sortBy(
                    currentTestSuiteRec.map((t) =>
                    ({
                        id: t.id, timestamp: t.startDate,
                        dateTime: moment(t.startDate).format(formatDate),
                        successRate: ((t.totalTestCount - t.totalFailCount) / t.totalTestCount * 100).toFixed(1)
                    })
                    ),
                    'timestamp').reverse(),
                12).reverse()
            for (var t in historyData) {
                if (historyData[t].id === task.id) {
                    taskDatetime = historyData[t].dateTime
                    break
                }
            }
            console.log("taskDatetime", taskDatetime)
            historyDataLowBound = _.max([_.min(historyData.map((h) => Number(h.successRate))) - 10, 0])
            historyDataHighBound = _.max(historyData.map((h) => Number(h.successRate))) + 8
            overallAreaWidth = Math.max(historyData.length * 35, overallAreaWidth)
        } else {
            return <h1>Task Details Loading....</h1>
        }

        const chartData = [
            { type: 'success', count: task.totalTestCount - task.totalFailCount },
            { type: 'fail', count: task.totalFailCount },
        ]

        const dtrSuccFailMap = _.groupBy(task.deviceTestResults, 'success')

        var perfResults = [];
        var suggestions = [];
        for (let testResult of task.deviceTestResults) {
            for (let attachment of testResult.attachments) {
                if (attachment.fileName == 'PerformanceReport.json') {
                    perfResults.push(attachment);
                    break;
                }
            }
            if (testResult.suggestion) {
                suggestions.push(testResult.suggestion.replace(/\r\n/g,"<br/>").replace(/\n/g,"<br/>").replace(/\s/g, '&nbsp;'));
            }
        }
        console.log('PerfResults:', perfResults);
        console.log('suggestions:', suggestions);

        var chunkedFailedDeviceResult = null
        var top3FailedCase = null
        if (dtrSuccFailMap['false']) {
            chunkedFailedDeviceResult = _.chunk(_.sortBy(dtrSuccFailMap['false'], 'failCount').reverse(), 4)
            const allUnits = _.flatMap(dtrSuccFailMap['false'], (dSum) => {
                return _.filter(dSum.testUnitList, (u) => !u.success)
            })
            var summary = _.countBy(_.compact(allUnits), (u) => u.title)
            var sOrder = []
            for (var k in summary) {
                sOrder.push({ k: k, c: summary[k] })
            }
            top3FailedCase = _.take(_.orderBy(_.filter(sOrder, (it) => it.c > 1), 'c').reverse(), 3)
        }
        var chunkedSuccDeviceResult = null
        if (dtrSuccFailMap['true']) {
            chunkedSuccDeviceResult = _.chunk(dtrSuccFailMap['true'], 6)
        }

        return <div id='test_report' style={{ padding: '20px' }}>
            <div id='test_report_head'>
                <table className='table table-borderless'>
                    <thead>
                        <tr className="table-info">
                            <th colSpan='2' style={{ backgroundColor: '4472C4', lineHeight: '100%' }}>
                                <center><p className='mt-4'
                                    style={{ color: 'white', fontSize: '2.5rem' }}>Device
                                    Laboratory Test Report</p></center>
                                <p style={{
                                    color: 'white',
                                    textAlign: 'right',
                                    fontSize: '1.1rem'
                                }}>{moment(task.startDate).format("MMM Do HH:mm")}</p>
                            </th>
                        </tr>
                        <tr style={{ backgroundColor: '#2F5496' }}>
                            <th colSpan='2' className="table-info"
                                style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                Overview:
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style={{ width: '500px' }}>
                                <h5 className='mt-1'>Ran <b>{task.testSuite}</b></h5>
                                <h5>On {task.testDevicesCount} devices <span
                                    className='badge badge-info'>{task.type}</span> <span
                                        className='badge badge-info'>{task.status.toUpperCase()}</span></h5>
                                <h6 className='badge badge-primary p-2' style={{ fontSize: '1rem' }}>
                                    <span
                                        className='badge badge-warning'>{dtrSuccFailMap['false'] ? dtrSuccFailMap['false'].length : 0}</span> devices
                                    had failures, <span
                                        className='badge badge-light'> {dtrSuccFailMap['true'] ? dtrSuccFailMap['true'].length : 0}</span> succeeded
                                </h6>
                                <h6>{moment(task.startDate).format("yyyy-MM-DD")}, {moment(task.startDate).format("HH:mm:ss")} ~ {moment(task.endDate).format("HH:mm:ss")}
                                    <span className='badge badge-primary ml-2'
                                        style={{ fontSize: '0.9rem' }}>{((task.endDate - task.startDate) / 1000).toFixed(1)}s</span>
                                </h6>
                                {top3FailedCase && top3FailedCase.length > 0 ?
                                    <table className='table table-sm'
                                        style={{ maxWidth: '450px', fontSize: '90%' }}>
                                        <thead>
                                            <tr>
                                                <th className="table-warning mt-2 ml-1">Top failed cases
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody style={{ fontSize: '90%' }}>
                                            {top3FailedCase.map((c) => <tr key={c.k} className={cssObj.bb}>
                                                <td>
                                                    {c.k} on {c.c} devices
                                                </td>
                                            </tr>)}
                                        </tbody>
                                    </table>
                                    : null}
                                <Link to={"/info/task/" + task.id} target='_blank' rel="noopener noreferrer">
                                    <Button variant="outlined" color="info">This Report Link</Button>
                                </Link>
                                {task.pipelineLink ?
                                    <p className='mt-3'><a href={task.pipelineLink} rel="noopener noreferrer">Link to PipeLine</a></p> : null}
                            </td>
                            {task.runningType !== 'SMART' ? <td>
                                <h4>Overall success rate {task.overallSuccessRate} <span
                                    className='badge badge-primary ml-2'
                                    style={{ fontSize: '1rem' }}>{task.totalTestCount - task.totalFailCount}/{task.totalTestCount}</span>
                                </h4>
                                <table>
                                    <tbody>
                                        <tr>
                                            <td>
                                                <PieChart width={overallPieSize} height={overallPieSize}>
                                                    <Pie
                                                        data={chartData}
                                                        labelLine={false}
                                                        label={PieCustomizedLabel}
                                                        dataKey="count">
                                                        {chartData.map((entry, index) => (
                                                            <Cell key={`cell-${index}`}
                                                                fill={COLORS[index % COLORS.length]} />
                                                        ))}
                                                    </Pie>
                                                </PieChart>
                                            </td>
                                            {historyData ?
                                                <td>
                                                    <AreaChart
                                                        width={overallAreaWidth}
                                                        height={overallPieSize}
                                                        data={historyData}
                                                        margin={{
                                                            right: 20,
                                                            left: 20
                                                        }}>
                                                        <YAxis width={0}
                                                            domain={[historyDataLowBound, historyDataHighBound]}
                                                            tick={false} tickLine={false}
                                                            axisLine={false} />
                                                        <XAxis dataKey="dateTime" height={0} tick={false}
                                                            tickLine={false} axisLine={false} />
                                                        <Tooltip />
                                                        <ReferenceLine x={taskDatetime} stroke={COLORS[1]}
                                                            strokeDasharray="3 3" isFront={true}
                                                            strokeWidth={2} />
                                                        <Area type="monotone" dataKey="successRate"
                                                            stroke="#8884d8" fill={COLORS[0]}
                                                            dot={{ stroke: "#8884d8", strokeWidth: 1 }}
                                                            label={<CustomizedLabel />} />
                                                    </AreaChart>
                                                </td>
                                                : <span>No historical data visuals. Data might be removed or deprecated.</span>
                                            }
                                        </tr>
                                    </tbody>
                                </table>
                            </td> : null}
                        </tr>
                    </tbody>
                </table>
            </div>
            <div id='test_report_content_1'>
                {chunkedFailedDeviceResult && task.runningType !== 'SMART' ? <div>
                    <table className='table table-borderless'>
                        <thead className="thead-info">
                            <tr className="table-info">
                                <th colSpan={chunkedFailedDeviceResult[0].length + ''}
                                    style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                    Test failed devices:
                                </th>
                            </tr>
                        </thead>
                    </table>
                    <table className='table table-borderless'>
                        <tbody>
                            {chunkedFailedDeviceResult.map((chunk, index) =>
                                <tr id={'test_report_content_1_' + index} key={chunk[0].id}>
                                    {chunk.map((d) => {
                                        const dChartData = [
                                            { type: 'success', count: d.totalCount - d.failCount },
                                            { type: 'fail', count: d.failCount }
                                        ]
                                        const failTestClassMap = _.groupBy(_.groupBy(d.testUnitList, 'success')['false'], 'testedClass')
                                        const rows = []
                                        for (var testClass in failTestClassMap) {
                                            rows.push(
                                                <tbody key={testClass}>
                                                    <tr className="table-danger">
                                                        <td>
                                                            {testClass.split('.').pop()}:
                                                        </td>
                                                    </tr>
                                                    {failTestClassMap[testClass].map((fu) => {
                                                        var inTop = false
                                                        top3FailedCase.forEach((kc) => {
                                                            if (kc.k === fu.title) {
                                                                inTop = true
                                                            }
                                                        })
                                                        if (!fu.testName || fu.testName === "null") {
                                                            fu.testName = "initialization failure"
                                                        }
                                                        return <tr key={fu.id}>
                                                            <td className='pl-2'>
                                                                <Link style={inTop ? { color: 'red' } : null}
                                                                    to={"/info/case/" + fu.id}
                                                                    target='_blank' rel="noopener noreferrer">.{_.truncate(fu.testName, 32)}</Link>
                                                                {fu.ownerName ?
                                                                    <a target='_blank'
                                                                        href={"mailto:" + fu.ownerEmail}
                                                                        rel="noopener noreferrer">{' - ' + fu.ownerName.split(' ')[0]}</a> :
                                                                    null
                                                                }
                                                            </td>
                                                        </tr>
                                                    }
                                                    )}
                                                </tbody>
                                            )
                                        }
                                        if (d.testErrorMessage) {
                                            console.log(_.truncate(d.testErrorMessage, 50))
                                        }
                                        return <td key={d.id}>
                                            <table>
                                                <tbody>
                                                    <tr>
                                                        <td>
                                                            <p><Link to={"/info/videos/" + d.id}
                                                                target='_blank'>
                                                                <img style={{ height: '105px' }}
                                                                    src={d.testGifBlobUrl ? d.testGifBlobUrl + '?' + require('local-storage').get('FileToken') : 'images/hydra_lab_logo.png'}
                                                                    alt={d.deviceName} />
                                                            </Link></p>
                                                        </td>
                                                        <td>
                                                            <p>
                                                                <span
                                                                    className={(d.totalCount - d.failCount) / d.totalCount < 0.7 ? 'badge badge-danger m-1' : 'badge badge-warning m-1'}>{d.successRate}</span>
                                                                <span
                                                                    className='badge badge-primary m-1'>{d.totalCount - d.failCount}/{d.totalCount}</span>
                                                            </p>
                                                            <PieChart width={90} height={90}>
                                                                <Pie
                                                                    data={dChartData}
                                                                    labelLine={false}
                                                                    dataKey="count">
                                                                    {dChartData.map((entry, index) => (
                                                                        <Cell key={`cell-${index}`}
                                                                            fill={COLORS[index % COLORS.length]} />
                                                                    ))}
                                                                </Pie>
                                                            </PieChart>
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                            {this.getDeviceLabel(d.deviceName)}{d.crashStackId ?
                                                <Link className='m-1 badge badge-danger' target='_blank'
                                                    to={"/info/crash/" + d.crashStackId}>CRASHED</Link> : null}
                                            <p className='mt-1'>
                                                <CloudDownloadIcon className='ml-1 mr-1'
                                                    style={{ height: '21px' }} />
                                                <a className='badge badge-light m-1' target='_blank'
                                                    href={d.logcatBlobUrl + '?' + require('local-storage').get('FileToken')} download rel="noopener noreferrer">Device Log</a>
                                                <a className='badge badge-light m-1' target='_blank'
                                                    href={d.testXmlReportBlobUrl + '?' + require('local-storage').get('FileToken')} download rel="noopener noreferrer">XML</a>
                                                <a className='badge badge-light m-1' target='_blank'
                                                    href={d.instrumentReportBlobUrl + '?' + require('local-storage').get('FileToken')} download rel="noopener noreferrer">Agent Log</a>
                                            </p>
                                            <div style={{
                                                maxHeight: '210px',
                                                overflowY: 'auto',
                                                overflowX: 'hidden'
                                            }}>
                                                <table
                                                    className='table table-sm table-light table-hover m-1'
                                                    style={{ fontSize: '75%' }}>
                                                    <thead>
                                                        <tr className="table-warning">
                                                            <th>
                                                                Failed cases:
                                                            </th>
                                                        </tr>
                                                    </thead>
                                                    {rows}
                                                </table>
                                                {d.testErrorMessage ? <div className='mb-3 mt-2'>
                                                    <a className="badge badge-warning"
                                                        href={d.instrumentReportBlobUrl + '?' + require('local-storage').get('FileToken')}
                                                        rel="noopener noreferrer"
                                                        style={{ whiteSpace: 'normal' }}
                                                        target='_blank'>{_.truncate(d.testErrorMessage, 50)}</a>
                                                </div> : null}
                                            </div>
                                        </td>
                                    })}
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div> : null}
            </div>
            <div id='test_report_content_2'>
                {chunkedSuccDeviceResult && task.runningType !== 'SMART' ? <div>
                    <table className='table table-borderless'>
                        <thead className="thead-info">
                            <tr className="table-info">
                                <th colSpan={chunkedSuccDeviceResult[0].length + ''}
                                    style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                    Test success devices:
                                </th>
                            </tr>
                        </thead>
                    </table>
                    <table className='table table-borderless'>
                        <tbody>
                            {chunkedSuccDeviceResult.map((chunk) =>
                                <tr key={chunk[0].id}>
                                    {chunk.map((d) => {
                                        const dChartData = [
                                            { type: 'success', count: d.totalCount - d.failCount },
                                            { type: 'fail', count: d.failCount }
                                        ]
                                        const size = 55
                                        return <tr>
                                            <td>
                                                <p><Link to={"/info/videos/" + d.id} target='_blank'>
                                                    <img style={{ height: '105px' }}
                                                        src={d.testGifBlobUrl ? d.testGifBlobUrl + '?' + require('local-storage').get('FileToken') : 'images/logo/m.png'}
                                                        alt={d.deviceName} />
                                                </Link></p>
                                                <p className='badge badge-light'>{d.displayTotalTime}</p>
                                            </td>
                                            <td key={d.id}>
                                                <p className='badge badge-success'>{d.successRate}</p>
                                                <PieChart width={size} height={size}>
                                                    <Pie
                                                        data={dChartData}
                                                        labelLine={false}
                                                        dataKey="count">
                                                        {dChartData.map((entry, index) => (
                                                            <Cell key={`cell-${index}`}
                                                                fill={COLORS[index % COLORS.length]} />
                                                        ))}
                                                    </Pie>
                                                </PieChart>
                                                <span
                                                    style={{ fontSize: '88%' }}>{this.getDeviceLabel(d.deviceName)}</span>
                                            </td>
                                        </tr>
                                    })}
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div> : null}
            </div>
            <div id='test_report_content_4>'>
                {task.runningType === 'SMART' ? <div>
                    <table className='table table-borderless'>
                        <thead className="thead-info">
                            <tr className="table-info">
                                <th style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                    Route Map:
                                </th>
                            </tr>
                        </thead>
                    </table>
                    <table className='table table-borderless'>
                        <tbody>
                            <tr>
                                <td>
                                    <h5 className='mt-1' style={{ width: '800px' }}>Selected Path: [ {this.state.selectedPath.join(' --> ')} ]</h5>
                                </td>
                                <td>
                                    <Button variant="outlined" color="info" onClick={() => {
                                        this.setState({ selectedPath: [] })
                                        this.setHoveredNode(undefined);
                                    }}>Clear</Button>
                                    <Button variant="outlined" color="info" onClick={() => { this.generateJSON() }}>Generate</Button>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <div ref={this.loadGEXF} id="sigma-container" style={{ width: '800px', height: '400px' }} />
                                </td>
                                <td >
                                    <div style={{ width: '200px', height: '400px' }}>
                                        {this.state.overNode ? <img style={{ width: '200px', height: '400px' }} src={"/api/test/loadNodePhoto/" + this.state.graphFileId + "?node=" + this.state.overNode} alt={this.state.overNode} /> : null}
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div> : null}
            </div>
            <div id='test_report_content_3>'>
                {perfResults.length > 0 ? <div>
                    <table className='table table-borderless'>
                        <thead className="thead-info">
                            <tr className="table-info">
                                <th colSpan={perfResults.length + ''}
                                    style={{ backgroundColor: '#2F5496', color: 'white' }}>
                                    Performance Test Results:
                                </th>
                            </tr>
                        </thead>
                    </table>
                    <table className='table table-borderless'>
                        <tbody>
                            {suggestions.length > 0
                                ? <div>
                                    <div style={{ width: '100%', height: "60px" }}>
                                        <img style={{ position: "absolute", width: '40px', height: '40px' }} src="images/chat_gpt_logo.svg" />
                                        <h3 className='mt-1' style={{position:"absolute",marginLeft:"45px"}} >Suggestions from GPT</h3>
                                    </div>
                                    <pre style={{ width: `calc(100% - 20px)`, marginBottom: "20px", paddingLeft: "20px", fontSize: "17"}}                                    >
                                        <div dangerouslySetInnerHTML={{ __html: suggestions.join(';').toString()}} />
                                    </pre>
                                </div>
                                : null}
                            {perfResults.map((perfTestResult) =>
                                <PerfTestDashboard perfTestResult={perfTestResult} testTask={task} />
                            )}
                        </tbody>
                    </table>
                </div> : null}
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
                    onClose={() => this.handleStatus("snackbarIsShown", false)}
                    severity={snackbarSeverity}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </div>
    }

    loadGEXF = c => {
        axios.get("/api/test/loadGraph/" + this.state.graphFileId).then((res) => {
            if (res.data.code && res.data.code === 500) {
                this.handleStatus("snackbarIsShown", true)
                this.handleStatus("snackbarSeverity", "error")
                this.handleStatus("snackbarMessage", res.data.message)
            } else {
                this.setState({
                    graph: parse(Graph, String(res.data)),
                    container: c
                })
                this.loadGraph()
            }
        })
    }

    loadGraph() {
        this.state.renderer = new Sigma(this.state.graph, this.state.container, {
            minCameraRatio: 0.5,
            maxCameraRatio: 1.5,
            defaultEdgeType: "arrow",
            allowInvalidContainer: true,
        });
        this.state.graph.dropNode(-1)
        // update the position of nodes by ForceAtlas2
        const sensibleSettings = forceAtlas2.inferSettings(this.state.graph);
        const fa2Layout = new FA2Layout(this.state.graph, {
            settings: sensibleSettings,
        });
        fa2Layout.start();

        this.state.renderer.on("clickNode", ({ node }) => {
            if (node === this.state.hoveredNode || (this.state.hoveredNeighbors && !this.state.hoveredNeighbors.has(node))) {
                return
            }
            this.setState({ selectedPath: this.state.selectedPath.concat([node]) })
            this.setHoveredNode(node);
        });

        this.state.renderer.on("enterNode", ({ node }) => {
            this.setState({ overNode: node })
        });

        this.state.renderer.setSetting("nodeReducer", (node, data) => {
            const res = { ...data };

            if (this.state.hoveredNeighbors && !this.state.hoveredNeighbors.has(node) && this.state.hoveredNode !== node) {
                res.label = "";
                res.color = "#f6f6f6";
            }

            if (this.state.hoveredNode === node) {
                res.highlighted = true;
            }

            return res;
        });

        this.state.renderer.setSetting("edgeReducer", (edge, data) => {
            const res = { ...data };
            if (this.state.hoveredNode && !(this.state.graph._edges.get(edge).source.key === this.state.hoveredNode)) {
                res.hidden = true;
            }
            return res;
        });
    }

    setHoveredNode(node) {
        if (node) {
            this.state.hoveredNode = node;
            this.state.hoveredNeighbors = new Set(this.state.graph.outNeighbors(node));
        } else {
            this.state.hoveredNode = undefined;
            this.state.hoveredNeighbors = undefined;
        }

        // Refresh rendering:
        this.state.renderer.refresh();
    }

    generateJSON() {
        if (this.state.selectedPath.length === 0) {
            this.setState({
                snackbarIsShown: true,
                snackbarSeverity: "error",
                snackbarMessage: "Please select a path!"
            })
            return
        }
        axios({
            url: `/api/test/generateT2C/` + this.state.graphFileId + "?testRunId=" + this.state.task.deviceTestResults[0].id + "&path=" + this.state.selectedPath.join(','),
            method: 'GET',
        }).then((res) => {
            var blob = new Blob([res.data.content]);
            const href = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = href;
            link.setAttribute('download', this.state.task.pkgName + '_t2c.json');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(href);

            if (res.data.code === 200) {
                this.setState({
                    snackbarIsShown: true,
                    snackbarSeverity: "success",
                    snackbarMessage: "T2C JSON file downloaded"
                })
            }
        }).catch(this.snackBarError);
    }

    getDeviceLabel = (name) => {
        const keys = _.split(name, '-');
        return keys.map((e, index) =>
            <span key={e}
                className={'badge badge-' + badgeList[index % badgeList.length] + ' m-1'}> {index < (keys.length - 1) ? e.toUpperCase() : ('SN: ' + e)}</span>
        )
    }

    queryTaskHistory() {
        let queryParams = [
            {
                "key": "runningType",
                "op": "equal",
                "value": this.state.task.runningType
            },
            // {
            //     "key":"type",
            //     "op":"equal",
            //     "value": this.state.task.type
            // },
            {
                "key": "testSuite",
                "op": "equal",
                "value": this.state.task.testSuite
            },
            {
                "key": "startDate",
                "op": "lt",
                "value": moment(this.state.task.startDate).format("YYYY-MM-DD HH:mm:ss.SSS"),
                "dateFormatString": "yyyy-MM-dd HH:mm:ss.SSS"
            }
        ]

        let postBody = {
            'page': 0,
            'pageSize': 6,
            'queryParams': queryParams
        }

        console.log(postBody)

        axios.post(`/api/test/task/list`, postBody).then(res => {
            if (res.data && res.data.code === 200) {
                const tasks = res.data.content.content;
                console.log(res.data)
                this.setState({
                    history: [
                        this.state.task,
                        ...tasks
                    ]
                })
                console.log(this.state.history)
            } else {
                this.snackBarFail(res)
            }
        }).catch(this.snackBarError)
    }

    componentDidMount() {
        console.log("componentDidMount")
        console.log(this.props.testTask)
        this.queryTaskHistory()
        console.log(this.state.history)
        this.props.testTask.deviceTestResults[0].attachments.forEach((attachment) => {
            if (attachment.fileName === 'smartTestResult.zip') {
                this.state.graphFileId = attachment.fileId
            }
        })
    }

}