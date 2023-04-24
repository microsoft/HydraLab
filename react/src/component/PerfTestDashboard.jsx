// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import Select from 'react-select'
import makeAnimated from 'react-select/animated';
import React from 'react'
import axios from "@/axios";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from 'recharts';

const animatedComponents = makeAnimated();
const COLORS = [
    '#007FFF', '#FFA500', '#8B8970', '#800000', '#FFCC00', '#808000', '#4B0080', '#8884d8', '#8B008B',
    '#EE7600', '#CD5C5C', '#BC8F8F', '#8B8B7A', '#006400', '#FF69B4', '#90EE90', '#A4D3EE', '#8884d8',
    '#8884d8'
]
const perfTitleMap = {
    PARSER_ANDROID_BATTERY_INFO: { title: "Android Battery", chartLabel: "Battery usage (mAh)" },
    PARSER_ANDROID_MEMORY_INFO: { title: "Android Memory", chartLabel: "Memory usage (MB)" },
    PARSER_WIN_MEMORY: { title: "Windows Memeory", chartLabel: "Memory usage (MB)" },
    PARSER_IOS_ENERGY: { title: "iOS Energy", chartLabel: "Energy Usage (mW)" },
    PARSER_IOS_MEMORY: { title: "iOS Memeory", chartLabel: "Memory usage (MB)" }

}
const androidBatteryOptions = [
    { value: 'appUsage', label: 'appUsage', color: COLORS[0] },
    { value: 'ratio', label: 'ratio', color: COLORS[1] },
    { value: 'cpu', label: 'cpu', color: COLORS[2] },
    { value: 'systemService', label: 'systemService', color: COLORS[3] },
    { value: 'screen', label: 'screen', color: COLORS[4] },
    { value: 'wakelock', label: 'wakelock', color: COLORS[5] },
    { value: 'wifi', label: 'wifi', color: COLORS[6] },
    // { value: 'total', label: 'total' },
]
const androidMemoryOptions = [
    { value: 'javaHeapPss', label: 'javaHeapPss', color: COLORS[0] },
    { value: 'nativeHeapPss', label: 'nativeHeapPss', color: COLORS[1] },
    { value: 'codePss', label: 'codePss', color: COLORS[2] },
    { value: 'stackPss', label: 'stackPss', color: COLORS[3] },
    { value: 'graphicsPss', label: 'graphicsPss', color: COLORS[4] },
    { value: 'privateOtherPss', label: 'privateOtherPss', color: COLORS[5] },
    { value: 'systemPss', label: 'systemPss', color: COLORS[6] },
    { value: 'totalPss', label: 'totalPss', color: COLORS[7] },
    { value: 'totalRss', label: 'totalRss', color: COLORS[8] },
    { value: 'totalSwapPss', label: 'totalSwapPss', color: COLORS[9] },
    { value: 'javaHeapRss', label: 'javaHeapRss', color: COLORS[10] },
    { value: 'nativeHeapRss', label: 'nativeHeapRss', color: COLORS[11] },
    { value: 'codeRss', label: 'codeRss', color: COLORS[12] },
    { value: 'stackRss', label: 'stackRss', color: COLORS[13] },
    { value: 'graphicsRss', label: 'graphicsRss', color: COLORS[14] },
    { value: 'privateOtherRss', label: 'privateOtherRss', color: COLORS[15] },
    { value: 'systemRss', label: 'systemRss', color: COLORS[16] },
    { value: 'unknownPss', label: 'unknownPss', color: COLORS[17] },
    { value: 'unknownRss', label: 'unknownRss', color: COLORS[18] }
]
const windowsMemoryOptions = [
    { value: 'nonpagedSystemMemorySize64', label: 'nonpagedSystemMemorySize64', color: COLORS[0] },
    { value: 'pagedMemorySize64', label: 'pagedMemorySize64', color: COLORS[1] },
    { value: 'pagedSystemMemorySize64', label: 'pagedSystemMemorySize64', color: COLORS[2] },
    { value: 'peakPagedMemorySize64', label: 'peakPagedMemorySize64', color: COLORS[3] },
    { value: 'peakWorkingSet64', label: 'peakWorkingSet64', color: COLORS[4] },
    { value: 'privateMemorySize64', label: 'privateMemorySize64', color: COLORS[5] },
    { value: 'workingSet64', label: 'workingSet64', color: COLORS[6] },
    { value: 'peakVirtualMemorySize64', label: 'peakVirtualMemorySize64', color: COLORS[7] },
]


const iosEnergyOptions = [
    { value: 'energy.cost', label: 'totalCost', color: COLORS[0] },
    { value: 'energy.cpu.cost', label: 'cpuCost', color: COLORS[1] },
    { value: 'energy.gpu.cost', label: 'gpuCost', color: COLORS[2] },
    { value: 'energy.networking.cost', label: 'networkingCost', color: COLORS[3] },
    { value: 'energy.appstate.cost', label: 'appStateCost', color: COLORS[4] },
    { value: 'energy.location.cost', label: 'locationCost', color: COLORS[5] },
    { value: 'energy.thermalstate.cost', label: 'thermalStateCost', color: COLORS[6] },
    { value: 'energy.overhead', label: 'totalOverhead', color: COLORS[7] },
    { value: 'energy.cpu.overhead', label: 'cpuOverhead', color: COLORS[8] },
    { value: 'energy.gpu.overhead', label: 'gpuOverhead', color: COLORS[9] },
    { value: 'energy.networkning.overhead', label: 'networkingOverhead', color: COLORS[10] },
    { value: 'energy.appstate.overhead', label: 'appStateOverhead', color: COLORS[11] },
    { value: 'energy.location.overhead', label: 'locationOverhead', color: COLORS[12] },
    { value: 'energy.thermalstate.overhead', label: 'thermalStateOverhead', color: COLORS[13] }
]
const iosMemoryOptions = [
    { value: 'value', label: 'memoryMB', color: COLORS[0] }
]

export default class PerfTestDashboard extends React.Component {
    state = {
        perfTestResult: this.props.perfTestResult,
        testTask: this.props.testTask,
        perfHistoryList: [],
        androidMemoryInfo: undefined,
        androidBatteryInfo: undefined,
        windowsMemoryInfo: undefined,
        iosEnergyInfo: undefined,
        iosMemoryInfo: undefined,
        androidBatteryAppsOptions: [],
        androidMemoryAppsOptions: [],
        windowsMemoryAppsOptions: [],
        iosEnergyAppsOptions: [],
        iosMemoryAppsOptions: [],
        selectedAndroidBatteryOptions: androidBatteryOptions.slice(0, 7),
        selectedAndroidMemoryOptions: androidMemoryOptions.slice(0, 10),
        selectedWindowsMemoryOptions: windowsMemoryOptions.slice(0, 7),
        selectedIosEnergyOptions: iosEnergyOptions.slice(0, 7)
    };

    render() {
        const androidMemoryInfo = this.state.androidMemoryInfo;
        const androidMemoryMetrics = [];
        const androidBatteryInfo = this.state.androidBatteryInfo;
        const androidBatteryMetrics = [];
        const windowsMemoryInfo = this.state.windowsMemoryInfo;
        const windowsMemoryMetrics = [];
        const iosEnergyInfo = this.state.iosEnergyInfo;
        const iosEnergyMetrics = [];
        const iosMemoryInfo = this.state.iosMemoryInfo;
        const iosMemoryMetrics = [];
        let perfHistoryList = this.state.perfHistoryList;

        const CustomTooltip = ({ active, payload, label }) => {
            if (active && payload && payload.length) {
                return (
                    <div style={{ backgroundColor: 'white', border: '1px solid #ccc', whiteSpace: 'nowrap', margin: 0, padding: 10 }}>
                        <p style={{ margin: 0, padding: 5 }}>{"Time: " + label + "S"}</p>
                        {payload[0].payload.testCase && payload[0].payload.testCase.length && <p style={{ margin: 0, padding: 5 }}>{"Test case: " + payload[0].payload.testCase}</p>}
                        {payload.map((key) => (
                            <p style={{ margin: 0, padding: 5 }}><font color={key.color}>{key.name + ": " + key.value}</font></p>
                        ))}
                        {payload[0].payload.description && payload[0].payload.description.length && <p style={{ margin: 0, padding: 5 }}>{"Description: " + payload[0].payload.description}</p>}
                    </div>
                )
            }

            return null;
        }

        /**
         * Android Battery Info
         */
        var isAndroidBatteryInfoEnabled = androidBatteryInfo && androidBatteryInfo.performanceInspectionResults && androidBatteryInfo.performanceInspectionResults.length > 0
        var isAndroidBatteryInfoEmpty = true

        if (isAndroidBatteryInfoEnabled) {
            let startTime = androidBatteryInfo.performanceInspectionResults[0].timestamp;
            androidBatteryInfo.performanceInspectionResults.forEach((inspectionResult) => {
                if (inspectionResult && inspectionResult.parsedData) {
                    let result = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    result.ratio = inspectionResult.parsedData.ratio * 100;
                    result.testCase = inspectionResult.testCaseName;
                    isAndroidBatteryInfoEmpty = false;

                    androidBatteryMetrics.push(result);
                }
            })
        }

        const androidBatteryMultiSelect = (
            <Select
                defaultValue={androidBatteryOptions.slice(0, 7)}
                isMulti
                components={animatedComponents}
                options={androidBatteryOptions}
                className="android-battery-select"
                classNamePrefix="select"
                onChange={(e) => { this.setState({ selectedAndroidBatteryOptions: e }) }}
            />
        );

        const renderAndroidBatteryChart = (
            <LineChart width={800} height={400} data={androidBatteryMetrics} margin={{ top: 20, right: 100, bottom: 20, left: 20 }}>
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" type='number' />
                <YAxis yAxisId="left" label={{ value: 'Battery usage (mAh)', angle: -90, position: 'left' }} />
                <YAxis yAxisId="right" label={{ value: 'Ratio', angle: -90, position: 'right' }} unit="%" orientation="right" />
                <Tooltip content={<CustomTooltip />} />
                {this.state.selectedAndroidBatteryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId={key.value == "ratio" ? "right" : "left"} dataKey={key.value} stroke={key.color} dot={false} />
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />
                <Legend verticalAlign="top" />
            </LineChart>)


        /**
         * Android Memory Info
         */
        var isAndroidMemoryInfoEnabled = androidMemoryInfo && androidMemoryInfo.performanceInspectionResults && androidMemoryInfo.performanceInspectionResults.length > 0
        var isAndroidMemoryInfoEmpty = true;

        if (isAndroidMemoryInfoEnabled) {
            let startTime = androidMemoryInfo.performanceInspectionResults[0].timestamp;
            androidMemoryInfo.performanceInspectionResults.forEach((inspectionResult) => {
                if (inspectionResult && inspectionResult.parsedData) {
                    let result = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    Object.keys(inspectionResult.parsedData).forEach((key) => {
                        if (inspectionResult.parsedData[key] == -1) {
                            result[key] = 0;
                        } else if (androidMemoryOptions.findIndex(item => item.value == key) != -1) {
                            // KB to MB 
                            result[key] = inspectionResult.parsedData[key] / 1024;
                            isAndroidMemoryInfoEmpty = false;
                        }
                    })
                    result.testCase = inspectionResult.testCaseName;
                    androidMemoryMetrics.push(result);
                }
            })
        }

        const androidMemoryMultiSelect = (
            <Select
                defaultValue={androidMemoryOptions.slice(0, 10)}
                isMulti
                components={animatedComponents}
                options={androidMemoryOptions}
                className="android-battery-select"
                classNamePrefix="select"
                onChange={(e) => { this.setState({ selectedAndroidMemoryOptions: e }) }}
            />
        );

        const renderAndroidMemoryChart = (
            <LineChart width={800} height={400} data={androidMemoryMetrics} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                <Legend verticalAlign="top" />
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" type='number' />
                <YAxis yAxisId="left" label={{ value: 'Memory usage (MB)', angle: -90, position: 'left' }} />
                <Tooltip content={<CustomTooltip />} />
                {this.state.selectedAndroidMemoryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key.value} stroke={key.color} dot={false}/>
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />

            </LineChart>)

        /**
         * Windows Memory Info
         */
        var isWindowsMemoryInfoEnabled = windowsMemoryInfo && windowsMemoryInfo.performanceInspectionResults && windowsMemoryInfo.performanceInspectionResults.length > 0
        var isWindowsMemoryInfoEmpty = true;

        if (isWindowsMemoryInfoEnabled) {
            let startTime = windowsMemoryInfo.performanceInspectionResults[0].timestamp;
            windowsMemoryInfo.performanceInspectionResults.forEach((inspectionResult) => {

                if (inspectionResult && inspectionResult.parsedData) {
                    var result = { ...inspectionResult.parsedData };
                    let parsedData = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;

                    let processIdProcessNameMap = new Map(Object.entries(parsedData.processIdProcessNameMap));
                    let processIdWindowsMemoryMetricsMap = new Map(Object.entries(parsedData.processIdWindowsMemoryMetricsMap));

                    processIdProcessNameMap.forEach((key, value) => {
                        // TODO: involve other processes.
                        if (key === 'PhoneExperienceHost') {
                            let windowsMemoryMetricsOfSingleProcess = processIdWindowsMemoryMetricsMap.get(value);

                            Object.keys(windowsMemoryMetricsOfSingleProcess).forEach((key) => {
                                if (windowsMemoryMetricsOfSingleProcess[key] == -1) {
                                    result[key] = 0;
                                } else if (windowsMemoryOptions.findIndex(item => item.value == key) != -1) {
                                    // Byte to MB
                                    result[key] = windowsMemoryMetricsOfSingleProcess[key] / 1024 / 1024;
                                    isWindowsMemoryInfoEmpty = false;
                                }
                            })
                        }
                    })

                    result.testCase = inspectionResult.testCaseName;
                    windowsMemoryMetrics.push(result);
                }
            })
        }

        const windowsMemoryMultiSelect = (
            <Select
                defaultValue={windowsMemoryOptions}
                isMulti
                components={animatedComponents}
                options={windowsMemoryOptions}
                className="android-battery-select"
                classNamePrefix="select"
                onChange={(e) => { this.setState({ selectedWindowsMemoryOptions: e }) }}
            />
        );

        const renderWindowsMemoryChart = (
            <LineChart width={800} height={400} data={windowsMemoryMetrics} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                <Legend verticalAlign="top" />
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" type='number' />
                <YAxis yAxisId="left" label={{ value: 'Memory usage (MB)', angle: -90, position: 'left' }} />
                <Tooltip content={<CustomTooltip />} />
                {this.state.selectedWindowsMemoryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key.value} stroke={key.color} dot={false}/>
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />

            </LineChart>)

        /**
         * iOS Energy Info
         */
        var isIosEnergyEnabled = iosEnergyInfo && iosEnergyInfo.performanceInspectionResults && iosEnergyInfo.performanceInspectionResults.length > 0
        var isIosEnergyInfoEmpty = true;

        if (iosEnergyInfo && iosEnergyInfo.performanceInspectionResults && iosEnergyInfo.performanceInspectionResults.length > 0) {
            let startTime = iosEnergyInfo.performanceInspectionResults[0].timestamp;
            iosEnergyInfo.performanceInspectionResults.forEach((inspectionResult) => {

                if (inspectionResult && inspectionResult.parsedData) {
                    var result = { ...inspectionResult.parsedData };
                    let parsedData = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    isIosEnergyInfoEmpty = false;
                    result.testCase = inspectionResult.testCaseName;

                    iosEnergyMetrics.push(result);
                }
            })
        }

        const iosEnergyMultiSelect = (
            <Select
                defaultValue={iosEnergyOptions.slice(0, 7)}
                isMulti
                components={animatedComponents}
                options={iosEnergyOptions}
                className="ios-energy-select"
                classNamePrefix="select"
                onChange={(e) => { this.setState({ selectedIosEnergyOptions: e }) }}
            />
        );

        const renderIosEnergyChart = (
            <LineChart width={800} height={400} data={iosEnergyMetrics} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                <Legend verticalAlign="top" />
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" type='number' />
                <YAxis yAxisId="left" label={{ value: 'Energy Usage (mW)', angle: -90, position: 'left' }} />
                <Tooltip content={<CustomTooltip />} />
                {this.state.selectedIosEnergyOptions.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key.value} name={key.label} stroke={key.color} dot={false} />
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />

            </LineChart>)

        /**
         * iOS Memory Info
         */

        var isIosMemoryInfoEnabled = iosMemoryInfo && iosMemoryInfo.performanceInspectionResults && iosMemoryInfo.performanceInspectionResults.length > 0
        var isIosMemoryInfoEmpty = true;

        if (iosMemoryInfo && iosMemoryInfo.performanceInspectionResults && iosMemoryInfo.performanceInspectionResults.length > 0) {
            let startTime = iosMemoryInfo.performanceInspectionResults[0].timestamp;
            iosMemoryInfo.performanceInspectionResults.forEach((inspectionResult) => {

                if (inspectionResult && inspectionResult.parsedData) {
                    var result = { ...inspectionResult.parsedData };
                    let parsedData = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    isIosMemoryInfoEmpty = false;
                    result.testCase = inspectionResult.testCaseName;
                    iosMemoryMetrics.push(result);
                }
            })
        }

        const renderIosMemoryChart = (
            <LineChart width={800} height={400} data={iosMemoryMetrics} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                <Legend verticalAlign="top" />
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" type='number' />
                <YAxis yAxisId="left" label={{ value: 'Memory usage (MB)', angle: -90, position: 'left' }} />
                <Tooltip content={<CustomTooltip />} />
                {iosMemoryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key.value} name={key.label} stroke={key.color} dot={false} />
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />

            </LineChart>)


        /**
         * Perf History
         */
        function HistoryChart(props) {
            let historyMetrics = [...props.data];
            let options = [];
            let isHistoryEmpty = true;
            for (let i = 1; i <= 5; i++) {
                if (!props.data[0]["metric" + i + "Key"]) {
                    continue;
                }

                options.push({
                    value: props.data[0]["metric" + i + "Key"],
                    label: props.data[0]["metric" + i + "Key"],
                    color: COLORS[i - 1],
                })

                for (let history of historyMetrics) {
                    if (history["metric" + i + "Value"] == -1) {
                        history[history["metric" + i + "Key"]] = 0;
                    } else {
                        isHistoryEmpty = false;
                        if (history.parserType == "PARSER_ANDROID_MEMORY_INFO") {
                            // KB to MB
                            history[history["metric" + i + "Key"]] = history["metric" + i + "Value"] / 1024;
                        } else if (history.parserType == "PARSER_WIN_MEMORY") {
                            // Byte to MB
                            history[history["metric" + i + "Key"]] = history["metric" + i + "Value"] / 1024 / 1024;
                        } else {
                            history[history["metric" + i + "Key"]] = history["metric" + i + "Value"];
                        }
                    }

                    history.dateFormat = moment.unix(history.date / 1000).format('MM/DD HH:mm');
                }
            }
            const [opt, setOptions] = React.useState({ selectedOptions: options });

            return <div> {!isHistoryEmpty && <div>
                <h3> {perfTitleMap[historyMetrics[0].parserType].title} history report </h3>
                <h4> {"App: " + historyMetrics[0].appId} </h4>
                <Select
                    defaultValue={options}
                    isMulti
                    components={animatedComponents}
                    options={options}
                    className="history-select"
                    classNamePrefix="select"
                    onChange={(e) => { setOptions({ ...opt, selectedOptions: e }) }}
                />
                <LineChart width={800} height={400} data={historyMetrics} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                    <Legend verticalAlign="top" />
                    <XAxis dataKey="dateFormat" label={{ value: 'Date', position: 'bottom' }} interval="preserveEnd" />
                    <YAxis yAxisId="left" label={{ value: perfTitleMap[historyMetrics[0].parserType].chartLabel, angle: -90, position: 'left' }} />
                    {opt.selectedOptions.map((key, index) => (
                        <Line type="linear" yAxisId="left" dataKey={key.value} stroke={key.color} dot={true} />
                    ))}
                    {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                    <Tooltip />
                </LineChart>
            </div>
            }
            </div>

        }

        function ErrorPlaceholder(props) {
            return <div>
                There is something wrong when parsing the {props.title} report data, please check the request param or agent log.
            </div>
        }

        return <div id='perf_dashboard'>
            {isAndroidBatteryInfoEnabled &&
                <div>
                    <h3> Android Battery report</h3>
                    {this.state.androidBatteryAppsOptions.length > 0 && <div style={{ marginBottom: '10px' }}>
                        <Select
                            defaultValue={this.state.androidBatteryAppsOptions[0]}
                            components={animatedComponents}
                            options={this.state.androidBatteryAppsOptions}
                            className="android-battery-app-select"
                            onChange={(e) => { this.setState({ androidBatteryInfo: e.value }) }}
                        />
                    </div>}
                    {isAndroidBatteryInfoEmpty ?
                        <ErrorPlaceholder title="Android battery" />
                        :
                        <div>
                            {androidBatteryMultiSelect}
                            {renderAndroidBatteryChart}
                       </div>
                    }
                </div>
            }

            {isAndroidMemoryInfoEnabled &&
                <div>
                    <h3> Android Memory report</h3>
                    {this.state.androidMemoryAppsOptions.length > 0 && <div style={{ marginBottom: '10px' }}>
                        <Select
                            defaultValue={this.state.androidMemoryAppsOptions[0]}
                            components={animatedComponents}
                            options={this.state.androidMemoryAppsOptions}
                            className="android-memory-app-select"
                            onChange={(e) => { this.setState({ androidMemoryInfo: e.value }) }}
                        />
                    </div>}
                    {isAndroidMemoryInfoEmpty ?
                        <ErrorPlaceholder title="Android memory" />
                        :
                        <div>
                            {androidMemoryMultiSelect}
                            {renderAndroidMemoryChart}
                       </div>
                    }
                </div>
            }

            {isWindowsMemoryInfoEnabled &&
                <div>
                    <h3> Windows Memory report</h3>
                    {this.state.windowsMemoryAppsOptions.length > 0 && <div style={{ marginBottom: '10px' }}>
                        <Select
                            defaultValue={this.state.windowsMemoryAppsOptions[0]}
                            components={animatedComponents}
                            options={this.state.windowsMemoryAppsOptions}
                            className="windows-memory-app-select"
                            onChange={(e) => { this.setState({ windowsMemoryInfo: e.value }) }}
                        />
                    </div>}
                    {isWindowsMemoryInfoEmpty ?
                        <ErrorPlaceholder title="Windows memory" />
                        :
                        <div>
                            {windowsMemoryMultiSelect}
                            {renderWindowsMemoryChart}
                       </div>
                    }
                </div>
            }
            {isIosEnergyEnabled &&
                <div>
                    <h3> iOS Energy report</h3>
                    {this.state.iosEnergyAppsOptions.length > 0 && <div style={{ marginBottom: '10px' }}>
                        <Select
                            defaultValue={this.state.iosEnergyAppsOptions[0]}
                            components={animatedComponents}
                            options={this.state.iosEnergyAppsOptions}
                            className="iOS-energy-app-select"
                            onChange={(e) => { this.setState({ iosEnergyInfo: e.value }) }}
                        />
                    </div>}
                    {isIosEnergyInfoEmpty ?
                        <ErrorPlaceholder title="iOS energy" />
                        :
                        <div>
                            {iosEnergyMultiSelect}
                            {renderIosEnergyChart}
                       </div>
                    }
                </div>}
            {isIosMemoryInfoEnabled &&
                <div>
                    <h3> iOS Memory report</h3>
                    {this.state.iosMemoryAppsOptions.length > 0 && <div style={{ marginBottom: '10px' }}>
                        <Select
                            defaultValue={this.state.iosMemoryAppsOptions[0]}
                            components={animatedComponents}
                            options={this.state.iosMemoryAppsOptions}
                            className="iOS-memory-app-select"
                            onChange={(e) => { this.setState({ iosMemoryInfo: e.value }) }}
                        />
                    </div>}
                    {isIosMemoryInfoEmpty ?
                        <ErrorPlaceholder title="iOS memory" />
                        :
                        <div>
                            {renderIosMemoryChart}
                        </div>
                    }
                </div>}

            {perfHistoryList.length > 0 &&
                <div>
                    <div style={{ backgroundColor: '#2F5496', color: 'white', padding: '10px', fontSize: 'medium', fontWeight: 'bold', marginBottom: '20px' }}>
                        Performance Test History:
                    </div>
                    {perfHistoryList.map((entry) =>
                        entry && entry.length > 0 && <HistoryChart state={this.state} data={entry} />
                    )}
                </div>}
        </div>
    };

    getPerfReportJson() {
        axios.get("api/test/performance/" + this.state.perfTestResult.fileId, {
        }).then(res => {
            console.log(res.data);
            for (var info of res.data.content) {
                console.log(info);
                if (info.parserType == 'PARSER_ANDROID_BATTERY_INFO') {
                    this.setState({ androidBatteryAppsOptions: this.updatePackageOptions(info, this.state.androidBatteryAppsOptions) });
                    if (!this.state.androidBatteryInfo) {
                        this.setState({ androidBatteryInfo: info });
                    }
                } else if (info.parserType == 'PARSER_ANDROID_MEMORY_INFO') {
                    this.setState({ androidMemoryAppsOptions: this.updatePackageOptions(info, this.state.androidMemoryAppsOptions) });
                    if (!this.state.androidMemoryInfo) {
                        this.setState({ androidMemoryInfo: info });
                    }
                } else if (info.parserType == 'PARSER_WIN_MEMORY') {
                    this.setState({ windowsMemoryAppsOptions: this.updatePackageOptions(info, this.state.windowsMemoryAppsOptions) });
                    if (!this.state.windowsMemoryInfo) {
                        this.setState({ windowsMemoryInfo: info });
                    }
                } else if (info.parserType == 'PARSER_IOS_ENERGY') {
                    this.setState({ iosEnergyAppsOptions: this.updatePackageOptions(info, this.state.iosEnergyAppsOptions) });
                    if (!this.state.iosEnergyInfo) {
                        this.setState({ iosEnergyInfo: info });
                    }
                } else if (info.parserType == 'PARSER_IOS_MEMORY') {
                    this.setState({ iosMemoryAppsOptions: this.updatePackageOptions(info, this.state.iosMemoryAppsOptions) });
                    if (!this.state.iosMemoryInfo) {
                        this.setState({ iosMemoryInfo: info });
                    }
                }

                //Get history list
                this.getPerfHistory(info);

            };
        })
    }

    updatePackageOptions(info, options) {
        if (info && info.performanceInspectionResults && info.performanceInspectionResults.length > 0) {
            let inspection = info.performanceInspectionResults[0].inspection;
            if (inspection && inspection.appId) {
                return [...options, { value: info, label: inspection.appId }];
            }
        }
    }

    getPerfHistory(perfResult) {
        if (perfResult && perfResult.performanceInspectionResults && perfResult.performanceInspectionResults.length > 0) {
            let inspection = perfResult.performanceInspectionResults[0].inspection;
            let postBody = [
                { key: "appId", value: inspection.appId, "op": "equal" },
                { key: "parserType", value: perfResult.parserType, "op": "equal" },
                { key: "testSuite", value: this.state.testTask.testSuite, "op": "equal" },
                { key: "runningType", value: this.state.testTask.runningType, "op": "equal" },
            ];
            axios.post("api/test/performance/history", postBody).then(res => {
                let newPerfHistoryList = [...this.state.perfHistoryList];
                newPerfHistoryList.push(res.data.content);
                this.setState({
                    perfHistoryList: newPerfHistoryList
                });
                console.log("Perf History", this.state.perfHistoryList);
            })
        }
    }

    componentDidMount() {
        this.getPerfReportJson();
    }
}