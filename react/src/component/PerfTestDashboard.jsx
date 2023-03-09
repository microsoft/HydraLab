// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import Select, { MultiValue } from 'react-select'
import makeAnimated from 'react-select/animated';
import React, { PureComponent } from 'react'
import axios from "@/axios";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from 'recharts';

const COLORS = ['#007FFF', '#FFA500', '#8B8970', '#800000', '#FFCC00', '#808000', '#4B0080', '#8884d8', '#7EC0EE',
    '#8B008B', '#EE7600', '#CD5C5C', '#BC8F8F', '#8B8B7A', '#006400', '#FF69B4', '#90EE90', '#A4D3EE',
    '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8'];
const animatedComponents = makeAnimated();
const androidBatteryOptions = [
    { value: 'appUsage', label: 'appUsage' },
    { value: 'ratio', label: 'ratio' },
    { value: 'cpu', label: 'cpu' },
    { value: 'systemService', label: 'systemService' },
    { value: 'screen', label: 'screen' },
    { value: 'wakelock', label: 'wakelock' },
    { value: 'wifi', label: 'wifi' },
    // { value: 'total', label: 'total' },
]
const androidMemoryOptions = [
    { value: 'javaHeapPss', label: 'javaHeapPss' },
    { value: 'nativeHeapPss', label: 'nativeHeapPss' },
    { value: 'codePss', label: 'codePss' },
    { value: 'stackPss', label: 'stackPss' },
    { value: 'graphicsPss', label: 'graphicsPss' },
    { value: 'privateOtherPss', label: 'privateOtherPss' },
    { value: 'systemPss', label: 'systemPss' },
    { value: 'totalPss', label: 'totalPss' },
    { value: 'totalRss', label: 'totalRss' },
    { value: 'totalSwapPss', label: 'totalSwapPss' },
    { value: 'javaHeapRss', label: 'javaHeapRss' },
    { value: 'nativeHeapRss', label: 'nativeHeapRss' },
    { value: 'codeRss', label: 'codeRss' },
    { value: 'stackRss', label: 'stackRss' },
    { value: 'graphicsRss', label: 'graphicsRss' },
    { value: 'privateOtherRss', label: 'privateOtherRss' },
    { value: 'systemRss', label: 'systemRss' },
    { value: 'unknownPss', label: 'unknownPss' },
    { value: 'unknownRss', label: 'unknownRss' }
]

const memoryKey = [
    'javaHeapPss',
    // 'javaHeapRss',
    // 'nativeHeapRss',
    'nativeHeapPss',
    'codePss',
    // 'codeRss',
    'stackPss',
    // 'stackRss',
    'graphicsPss',
    // 'graphicsRss',
    'privateOtherPss',
    // 'privateOtherRss',
    'systemPss',
    // 'systemRss',
    // 'unknownPss',
    // 'unknownRss',
    'totalPss',
    'totalRss',
    'totalSwapPss'
];

export default class PerfTestDashboard extends React.Component {
    state = {
        perfTestResult: this.props.perfTestResult,
        memoryInfo: undefined,
        batteryInfo: undefined,
        selectedAndroidBatteryOptions: androidBatteryOptions,
        selectedAndroidMemoryOptions: androidMemoryOptions,
    };

    render() {
        const memoryInfo = this.state.memoryInfo;
        const memoryMetrics = [];
        const batteryInfo = this.state.batteryInfo;
        const batteryMetrics = [];

        /**
         * Battery Info
         */
        if (batteryInfo && batteryInfo.performanceInspectionResults && batteryInfo.performanceInspectionResults.length > 0) {
            let startTime = batteryInfo.performanceInspectionResults[0].timestamp;
            batteryInfo.performanceInspectionResults.forEach((inspectionResult) => {
                if (inspectionResult.parsedData !== null) {
                    let result = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    result.ratio = inspectionResult.parsedData.ratio * 100;
                    batteryMetrics.push(result);
                }
            })
        }

        const batteryMultiSelect = (
            <Select
                defaultValue={[androidBatteryOptions[0], androidBatteryOptions[1], androidBatteryOptions[2], androidBatteryOptions[3]]}
                isMulti
                components={animatedComponents}
                options={androidBatteryOptions}
                className="android-battery-select"
                classNamePrefix="select"
                onChange={(e) => { this.setState({ selectedAndroidBatteryOptions: e }) }}
            />
        );

        const renderBatteryChart = (
            <LineChart width={800} height={400} data={batteryMetrics} margin={{ top: 20, right: 100, bottom: 20, left: 20 }}>
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" />
                <YAxis yAxisId="left" label={{ value: 'Battery usage (mAh)', angle: -90, position: 'left' }} />
                <YAxis yAxisId="right" label={{ value: 'Ratio', angle: -90, position: 'right' }} unit="%" orientation="right" />
                {this.state.selectedAndroidBatteryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId={key.value == "ratio" ? "right" : "left"} dataKey={key.value} stroke={COLORS[index]} />
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />
                <Legend verticalAlign="top" />
            </LineChart>)


        /**
         * Memory Info
         */
        if (memoryInfo && memoryInfo.performanceInspectionResults && memoryInfo.performanceInspectionResults.length > 0) {
            let startTime = memoryInfo.performanceInspectionResults[0].timestamp;
            memoryInfo.performanceInspectionResults.forEach((inspectionResult) => {
                if (inspectionResult.parsedData !== null) {
                    let result = { ...inspectionResult.parsedData };
                    result.time = (inspectionResult.timestamp - startTime) / 1000;
                    Object.keys(inspectionResult.parsedData).forEach((key) => {
                        if (inspectionResult.parsedData[key] == -1) {
                            result[key] = 0;
                        } else if (memoryKey.indexOf(key) > -1) {
                            // KB to MB 
                            result[key] = inspectionResult.parsedData[key] / 1024;
                        }
                    })
                    memoryMetrics.push(result);
                }
            })
        }

        const memoryMultiSelect = (
            <Select
                defaultValue={[androidMemoryOptions[0], androidMemoryOptions[1], androidMemoryOptions[2], androidMemoryOptions[3]]}
                isMulti
                components={animatedComponents}
                options={androidMemoryOptions}
                className="android-battery-select"
                classNamePrefix="select"
                onChange={(e) => { this.setState({ selectedAndroidMemoryOptions: e }) }}
            />
        );

        const renderMemoryChart = (
            <LineChart width={800} height={400} data={memoryMetrics} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                <Legend verticalAlign="top" />
                <XAxis dataKey="time" label={{ value: 'Time', position: 'bottom' }} unit="s" />
                <YAxis yAxisId="left" label={{ value: 'Memory usage (MB)', angle: -90, position: 'left' }} />
                {this.state.selectedAndroidMemoryOptions.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key.value} stroke={COLORS[index]} />
                ))}
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />

            </LineChart>)

        return <div id='perf_dashboard'>
            {batteryInfo && <h3> Battery report</h3>}
            {batteryInfo && batteryMultiSelect}
            {batteryInfo && renderBatteryChart}
            {memoryInfo && <h3> Memory report</h3>}
            {memoryInfo && renderMemoryChart}
        </div>
    };

    getPerfReportJson() {
        axios.get(this.state.perfTestResult.blobUrl + '?' + require('local-storage').get('FileToken'), {
        }).then(res => {
            console.log(res.data);
            for (var info of res.data) {
                console.log(info);
                if (info.parserType == 'PARSER_ANDROID_BATTERY_INFO') {
                    this.setState({ batteryInfo: info });
                } else if (info.parserType == 'PARSER_ANDROID_MEMORY_INFO') {
                    this.setState({ memoryInfo: info });
                }
            };
        })
    }

    componentDidMount() {
        this.getPerfReportJson();
    }
}