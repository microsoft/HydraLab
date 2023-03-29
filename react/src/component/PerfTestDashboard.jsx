// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import Select from 'react-select'
import makeAnimated from 'react-select/animated';
import React from 'react'
import axios from "@/axios";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from 'recharts';

const animatedComponents = makeAnimated();
const androidBatteryOptions = [
    { value: 'appUsage', label: 'appUsage', color: '#007FFF' },
    { value: 'ratio', label: 'ratio', color: '#FFA500' },
    { value: 'cpu', label: 'cpu', color: '#8B8970' },
    { value: 'systemService', label: 'systemService', color: '#800000' },
    { value: 'screen', label: 'screen', color: '#FFCC00' },
    { value: 'wakelock', label: 'wakelock', color: '#808000' },
    { value: 'wifi', label: 'wifi', color: '4B0080' },
    // { value: 'total', label: 'total' },
]
const androidMemoryOptions = [
    { value: 'javaHeapPss', label: 'javaHeapPss', color: '#007FFF' },
    { value: 'nativeHeapPss', label: 'nativeHeapPss', color: '#FFA500' },
    { value: 'codePss', label: 'codePss', color: '#8B8970' },
    { value: 'stackPss', label: 'stackPss', color: '#800000' },
    { value: 'graphicsPss', label: 'graphicsPss', color: '#FFCC00' },
    { value: 'privateOtherPss', label: 'privateOtherPss', color: '#808000' },
    { value: 'systemPss', label: 'systemPss', color: '#4B0080' },
    { value: 'totalPss', label: 'totalPss', color: '#8884d8' },
    { value: 'totalRss', label: 'totalRss', color: '#8B008B' },
    { value: 'totalSwapPss', label: 'totalSwapPss', color: '#EE7600' },
    { value: 'javaHeapRss', label: 'javaHeapRss', color: '#CD5C5C' },
    { value: 'nativeHeapRss', label: 'nativeHeapRss', color: '#BC8F8F' },
    { value: 'codeRss', label: 'codeRss', color: '#8B8B7A' },
    { value: 'stackRss', label: 'stackRss', color: '#006400' },
    { value: 'graphicsRss', label: 'graphicsRss', color: '#FF69B4' },
    { value: 'privateOtherRss', label: 'privateOtherRss', color: '#90EE90' },
    { value: 'systemRss', label: 'systemRss', color: '#A4D3EE' },
    { value: 'unknownPss', label: 'unknownPss', color: '#8884d8' },
    { value: 'unknownRss', label: 'unknownRss', color: '#8884d8' }
]
const windowsMemoryOptions = [
    { value: 'nonpagedSystemMemorySize64', label: 'nonpagedSystemMemorySize64', color: '#007FFF' },
    { value: 'pagedMemorySize64', label: 'pagedMemorySize64', color: '#FFA500' },
    { value: 'pagedSystemMemorySize64', label: 'pagedSystemMemorySize64', color: '#8B8970' },
    { value: 'peakPagedMemorySize64', label: 'peakPagedMemorySize64', color: '#800000' },
    { value: 'peakWorkingSet64', label: 'peakWorkingSet64', color: '#808000' },
    { value: 'privateMemorySize64', label: 'privateMemorySize64', color: '#4B0080' },
    { value: 'workingSet64', label: 'workingSet64', color: '#8884d8' },
    { value: 'peakVirtualMemorySize64', label: 'peakVirtualMemorySize64', color: '#FFCC00' },
]

export default class PerfTestDashboard extends React.Component {
    state = {
        perfTestResult: this.props.perfTestResult,
        androidMemoryInfo: undefined,
        androidBatteryInfo: undefined,
        windowsMemoryInfo: undefined,
        selectedAndroidBatteryOptions: androidBatteryOptions.slice(0, 4),
        selectedAndroidMemoryOptions: androidMemoryOptions.slice(0, 10),
        selectedWindowsMemoryOptions: windowsMemoryOptions.slice(0, 7)
    };

    render() {
        const androidMemoryInfo = this.state.androidMemoryInfo;
        const androidMemoryMetrics = [];
        const androidBatteryInfo = this.state.androidBatteryInfo;
        const androidBatteryMetrics = [];
        const windowsMemoryInfo = this.state.windowsMemoryInfo;
        const windowsMemoryMetrics = [];

        /**
         * Android Battery Info
         */
        var isAndroidBatteryInfoEnabled = androidBatteryInfo && androidBatteryInfo.performanceInspectionResults && androidBatteryInfo.performanceInspectionResults.length > 0
        var isAndroidBatteryInfoEmpty = true

        if (isAndroidBatteryInfoEnabled) {
            let startTime = androidBatteryInfo.performanceInspectionResults[0].timestamp;
            androidBatteryInfo.performanceInspectionResults.forEach((inspectionResult) => {
                if (inspectionResult.parsedData !== null) {
                    let result = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    result.ratio = inspectionResult.parsedData.ratio * 100;
                    isAndroidBatteryInfoEmpty = false;

                    androidBatteryMetrics.push(result);
                }
            })
        }

        const androidBatteryMultiSelect = (
            <Select
                defaultValue={androidBatteryOptions.slice(0, 4)}
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
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" />
                <YAxis yAxisId="left" label={{ value: 'Battery usage (mAh)', angle: -90, position: 'left' }} />
                <YAxis yAxisId="right" label={{ value: 'Ratio', angle: -90, position: 'right' }} unit="%" orientation="right" />
                {this.state.selectedAndroidBatteryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId={key.value == "ratio" ? "right" : "left"} dataKey={key.value} stroke={key.color} />
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
                if (inspectionResult.parsedData !== null) {
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
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" />
                <YAxis yAxisId="left" label={{ value: 'Memory usage (MB)', angle: -90, position: 'left' }} />
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

                if (inspectionResult !== null && inspectionResult.parsedData !== null) {
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
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" />
                <YAxis yAxisId="left" label={{ value: 'Memory usage (MB)', angle: -90, position: 'left' }} />
                {this.state.selectedWindowsMemoryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key.value} stroke={key.color} dot={false}/>
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />

            </LineChart>)


        return <div id='perf_dashboard'>
            {isAndroidBatteryInfoEnabled &&
                <div>
                    <h3> Android Battery report</h3>
                    {isAndroidBatteryInfoEmpty ? 
                        <div>
                            There is something wrong when parsing the android battery report data, please check the request param or agent.
                        </div>
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
                    {isAndroidMemoryInfoEmpty ? 
                        <div>
                            There is something wrong when parsing the android memory report data, please check the request param or agent.
                        </div>
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
                    {isWindowsMemoryInfoEmpty ? 
                        <div>
                            There is something wrong when parsing the windows memory report data, please check the request param or agent.
                        </div>
                        :
                        <div>
                            {windowsMemoryMultiSelect}
                            {renderWindowsMemoryChart}
                       </div>
                    }
                </div>
            }
        </div>
    };

    getPerfReportJson() {
        axios.get("api/test/performance/" + this.state.perfTestResult.fileId, {
        }).then(res => {
            console.log(res.data);
            for (var info of res.data.content) {
                console.log(info);
                if (info.parserType == 'PARSER_ANDROID_BATTERY_INFO') {
                    this.setState({ androidBatteryInfo: info });
                } else if (info.parserType == 'PARSER_ANDROID_MEMORY_INFO') {
                    this.setState({ androidMemoryInfo: info });
                } else if (info.parserType == 'PARSER_WIN_MEMORY') {
                    this.setState({ windowsMemoryInfo: info });
                }
            };
        })
    }

    componentDidMount() {
        this.getPerfReportJson();
    }
}