import Select, { MultiValue } from 'react-select'
import React, { PureComponent } from 'react'
import axios from "@/axios";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from 'recharts';

const COLORS = ['#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8', '#8884d8'];
const batteryKey = ['appUsage', 'cpu', 'screen', 'wakeLock', 'systemService', 'wifi', 'total'];

export default class PerfTestDashboard extends React.Component {
    state = {
        task: this.props.testTask,
        detail: ''
    };

    render() {
        const task = this.state.task;
        const data = [];
        const detail = this.state.detail.performanceInspectionResults;
        const showBattery = detail && this.state.detail.parserType == 'PARSER_ANDROID_BATTERY_INFO';

        if (showBattery) {
            let startTime = detail[0].timestamp;
            detail.forEach((inspectionResult) => {
                inspectionResult.parsedData.time = (inspectionResult.timestamp - startTime) / 1000;
                inspectionResult.parsedData.ratio = inspectionResult.parsedData.ratio * 100;
                data.push(inspectionResult.parsedData);
            })
        }

        const renderbatteryChart = (
            <LineChart width={800} height={400} data={data} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
                <XAxis dataKey="time" label={{ value: 'Time' }} unit="S" />
                <YAxis yAxisId="left" label={{ value: 'Battery usage (mAh)', angle: -90, position: 'insideLeft' }} />
                <YAxis yAxisId="right" label={{ value: 'Ratio', angle: -90, position: 'insideRight' }} unit="%" orientation="right" />
                {batteryKey.map((key, index) => (
                    <Line type="monotone" yAxisId="left" dataKey={key} stroke={COLORS[index]} />
                ))}
                <Line type="monotone" yAxisId="right" dataKey="ratio" stroke='#8833d8' />
                {/* <CartesianGrid stroke="#ccc" strokeDasharray="5 5" /> */}
                <Tooltip />
                <Legend />
            </LineChart>)

        return <div id='perf_dashboard'>
            {showBattery && <h3> Battery report</h3>}
            {showBattery && renderbatteryChart}
        </div>
    };

    getPerfDetailJson() {
        axios.get('https://devicelabstorage.blob.core.windows.net/testresults/test/result/PerformanceReport.json' + '?' + require('local-storage').get('BlobSignature'), {
        }).then(res => {
            console.log(res.data[0])
            this.setState({ detail: res.data[0] })
        })
    }

    componentDidMount() {
        this.getPerfDetailJson();
    }


}







// function MetricByVersionLineChart(props) {
//     var _a = (0, react_1.useState)([]), selections = _a[0], setSelections = _a[1];
//     var canvasRef = (0, react_1.useRef)(null);
//     var chart = (0, react_1.useRef)();
//     var lastInitialSelections = (0, react_1.useRef)();
//     if (lastInitialSelections.current !== props.initialSelections) {
//         lastInitialSelections.current = props.initialSelections;
//         setSelections(props.initialSelections.length > 0 || props.metricsMap.size == 0 ? props.initialSelections : [props.metricsMap.keys().next().value]);
//     }
//     function onItemClick(ev, elements) {
//         var index = elements.length > 0 ? elements[0]._index : this.scales['x-axis-0'].getValueForPixel(ev.x);
//         var version = props.versions[index];
//         var testId = props.testIds[index];
//         window.open("/build/".concat(version, "/").concat(testId));
//     }
//     var handleChange = function (newValue) {
//         var newSelections = newValue.map(function (opt) { return opt.value; });
//         setSelections(newSelections);
//         props.onSelectionChange(newSelections);
//     };
//     (0, react_1.useLayoutEffect)(function () {
//         var chartDatasets = [];
//         var maxValue = Number.NEGATIVE_INFINITY;
//         var minValue = Number.POSITIVE_INFINITY;
//         selections.forEach(function (instanceName) {
//             var _a = StringUtil.randomColor(instanceName), r = _a[0], g = _a[1], b = _a[2];
//             var stableColor = "rgb(".concat(r, ", ").concat(g, ", ").concat(b, ")");
//             var metrics = props.metricsMap.get(instanceName);
//             var dataset = {
//                 label: instanceName,
//                 data: metrics,
//                 borderWidth: 2,
//                 borderColor: stableColor,
//                 backgroundColor: stableColor,
//                 pointRadius: 0,
//                 pointHoverRadius: 4,
//             };
//             chartDatasets.push(dataset);
//             maxValue = Math.max(maxValue, Math.max.apply(null, metrics === null || metrics === void 0 ? void 0 : metrics.filter(Number.isFinite)));
//             minValue = Math.min(minValue, Math.min.apply(null, metrics === null || metrics === void 0 ? void 0 : metrics.filter(Number.isFinite)));
//         });
//         if (props.failedVersions.length > 0) {
//             var failedVersionsData_1 = [];
//             props.versions.forEach(function (version) {
//                 if (props.failedVersions.includes(version)) {
//                     failedVersionsData_1.push(0);
//                 }
//                 else {
//                     failedVersionsData_1.push(undefined);
//                 }
//             });
//             var chartPoint = new Image();
//             chartPoint.src = "./static/exclamation-mark.png";
//             chartPoint.width = chartPoint.height = 12;
//             chartDatasets.push({
//                 label: "Failed build",
//                 data: failedVersionsData_1,
//                 type: "bubble",
//                 backgroundColor: "rgba(255, 0, 0, 1)",
//                 pointStyle: chartPoint,
//                 hoverBorderColor: "rgba(255, 0, 0, 1)",
//             });
//         }
//         var logMax = Math.floor(Math.log10(maxValue));
//         var logMin = Math.floor(Math.log10(minValue));
//         var logScale = logMax - logMin > 1 && props.failedVersions.length == 0;
//         var chartYAxe = {
//             type: logScale ? 'logarithmic' : 'linear',
//             scaleLabel: {
//                 display: true,
//                 labelString: props.metricName
//             },
//             ticks: {
//                 beginAtZero: true
//             }
//         };
//         if (!chart.current) {
//             chart.current = new chart_js_1.Chart(canvasRef.current, {
//                 type: 'line',
//                 data: {
//                     labels: props.versions,
//                     datasets: chartDatasets
//                 },
//                 options: {
//                     responsive: true,
//                     maintainAspectRatio: true,
//                     aspectRatio: 3.5,
//                     tooltips: {
//                         enabled: true,
//                         intersect: false,
//                     },
//                     hover: {
//                         mode: 'nearest',
//                         axis: 'xy',
//                         intersect: false,
//                         animationDuration: 200,
//                     },
//                     elements: {
//                         line: {
//                             tension: 0,
//                             fill: false,
//                         }
//                     },
//                     scales: {
//                         xAxes: [{
//                             gridLines: {
//                                 display: false,
//                             }
//                         }],
//                         yAxes: [chartYAxe]
//                     },
//                     legend: {
//                         display: true,
//                         labels: {
//                             filter: function (legendItem, data) {
//                                 return legendItem.text != "Failed build";
//                             }
//                         }
//                     },
//                     onClick: onItemClick
//                 }
//             });
//         }
//         else if (chart.current) {
//             chart.current.data.labels = props.versions;
//             chart.current.data.datasets = chartDatasets;
//             chart.current.options.scales.yAxes = [chartYAxe];
//             chart.current.options.onClick = onItemClick;
//             chart.current.update();
//         }
//     }, [props.versions, props.failedVersions, props.metricsMap, selections]);
//     function makeOptions(instances) {
//         return instances.sort().map(function (instanceName) { return ({ value: instanceName, label: instanceName }); });
//     }
//     return isMulti;
//     options = { useMemo: function () { } }();
//     makeOptions(__spreadArray([], props.metricsMap.keys(), true)), [props.metricsMap];
// }

