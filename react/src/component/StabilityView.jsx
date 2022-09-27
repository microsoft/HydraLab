import React from 'react'
import axios from '@/axios'
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Typography from '@material-ui/core/Typography';
import 'bootstrap/dist/css/bootstrap.css'
import {withStyles} from '@material-ui/core/styles';
import _ from 'lodash';
import {
    XAxis,
    Tooltip,
    PieChart,
    Pie,
    Cell,
    BarChart,
    Bar,
    LabelList
} from 'recharts';
import moment from 'moment';
import Skeleton from "@mui/material/Skeleton";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import BaseView from "@/component/BaseView";

const COLORS = ['#FF8042', '#e377c2', '#00C49F', '#EACC94', '#FFBB28', '#7fffd4']
/**
 * Palette
 * https://material-ui.com/customization/palette/
 */
const StyledTableCell = withStyles((theme) => ({
    head: {
        backgroundColor: theme.palette.primary.dark,
        color: theme.palette.common.white,
    },
    body: {
        fontSize: 14,
    },
}))(TableCell);

const StyledTableRow = withStyles((theme) => ({
    root: {
        '&:nth-of-type(odd)': {
            backgroundColor: theme.palette.action.selected,
        },
    },
}))(TableRow);
const RADIAN = Math.PI / 180;
const PieCustomizedLabel = ({cx, cy, midAngle, innerRadius, outerRadius, percent}) => {
    const radius = innerRadius + (outerRadius - innerRadius) * 1.1;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
        <text x={x} y={y} fontSize={10} textAnchor={x > cx ? 'start' : 'end'}
              dominantBaseline="central">
            {`${(percent * 100).toFixed(1)}%`}
        </text>
    );
};

// const getPath = (x, y, width, height) => `M${x},${y + height}
//           C${x + width / 3},${y + height} ${x + width / 2},${y + height / 3} ${x + width / 2}, ${y}
//           C${x + width / 2},${y + height / 3} ${x + (2 * width) / 3},${y + height} ${x + width}, ${y + height}
//           Z`;

// const TriangleBar = (props) => {
//     const { fill, x, y, width, height } = props;

//     return <path d={getPath(x, y, width, height)} stroke="none" fill={fill} />;
// };
// TriangleBar.propTypes = {
//     fill: PropTypes.string,
//     x: PropTypes.number,
//     y: PropTypes.number,
//     width: PropTypes.number,
//     height: PropTypes.number,
// };

export default class StabilityView extends BaseView {

    state = {
        units: null,

        hideSkeleton: true,
    }

    render() {
        const units = this.state.units
        const rows = []
        const heads = []
        let pie = null;
        let bar = null

        const {snackbarIsShown, snackbarSeverity, snackbarMessage} = this.state

        const headItems = ['Time', 'Test Method', 'Device', 'Link']
        if (units) {
            const byDevice = _.countBy(units, (u) => u.devicetaskDeviceName)
            console.log(byDevice)

            const onlyPixelUnits = units.filter((u) => u.devicetaskDeviceName.includes('pixel') && !u.title.includes('null'))
            const byName = _.countBy(onlyPixelUnits, (u) => u.title)
            console.log(byName)
            const pieChartData = _.sortBy(_.keys(byName).map((k) => ({
                name: k,
                value: byName[k]
            })), 'value')
            const barChartData = _.sortBy(_.keys(byDevice).map((k) => ({
                name: k.toUpperCase(),
                value: byDevice[k]
            })), 'value').reverse()

            console.log(pieChartData.length)

            bar = <div>
                <center>
                    <BarChart width={480} height={172} data={barChartData}
                              margin={{top: 15, right: 30, left: 20, bottom: 5}}>
                        <Tooltip/>
                        <Bar
                            labelLine={false}
                            fill="#8884d8"
                            dataKey="value">
                            {barChartData.map((entry, index) => (
                                <Cell key={`cellbar-${index}`}
                                      fill={COLORS[index % COLORS.length]}/>
                            ))}
                            <LabelList dataKey="value" position="top" offset={4}/>
                        </Bar>
                        <XAxis dataKey="name" height={0} tick={false} tickLine={false}
                               axisLine={false}/>
                    </BarChart>
                    <p className='badge' style={{fontSize: '0.8rem'}}>Failed cases by
                        devices</p>
                </center>
            </div>

            if (pieChartData.length) {
                pie = <div>
                    <center>
                        <PieChart width={300} height={172}>
                            <Pie
                                data={pieChartData}
                                labelLine={false}
                                label={PieCustomizedLabel}
                                dataKey="value">
                                {pieChartData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>
                                ))}
                            </Pie>
                            <Tooltip/>
                        </PieChart>
                        <p className='badge' style={{fontSize: '0.8rem'}}>Failed cases on
                            Pixel devices</p>
                    </center>
                </div>
            }

            units.forEach((t) => {
                rows.push(<StyledTableRow key={t.id} id={t.id} hover>
                    <TableCell id={t.id} align="center">
                        {moment(t.startTimeMillis).format('yyyy-MM-DD HH:mm:ss')}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.title}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.devicetaskDeviceName.toUpperCase()}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <a className='badge badge-info m-1' href={'#/info/case/' + t.id}
                           target='_blank' rel="noopener noreferrer">CASE</a>
                        <a className='badge badge-primary m-1' target='_blank'
                           href={t.devicetaskLogcatPath} download rel="noopener noreferrer">Device Log</a>
                        <a className='badge badge-warning m-1' target='_blank'
                           href={t.deviceTestTaskTestXmlReportPath} download rel="noopener noreferrer">XML</a>
                        <a className='badge badge-danger m-1' target='_blank'
                           href={t.deviceTestTaskInstrumentReportPath} download rel="noopener noreferrer">Agent Log</a>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        headItems.forEach((k) => heads.push(<StyledTableCell key={k}
                                                             align="center">{k}</StyledTableCell>))


        return <div>
            <center>
                <TableContainer style={{margin: "auto"}}>
                    <Table size="medium">
                        <TableHead>
                            <TableRow>
                                <TableCell colSpan="6">
                                    <Typography variant="h4" className="mt-2 mb-2">
                                        Test Unit Stability: Failed cases</Typography>
                                </TableCell>
                            </TableRow>

                            <TableRow align="center">
                                <TableCell hidden={this.state.hideSkeleton}/>
                                <TableCell colSpan="2" align="center"
                                           hidden={this.state.hideSkeleton}>
                                    <Skeleton variant="rectangular" width={480} height={172}/>
                                </TableCell>
                                <TableCell colSpan="2" align="center"
                                           hidden={this.state.hideSkeleton}>
                                    <Skeleton variant="circular" width={172} height={172}/>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell colSpan="2">{bar}</TableCell>
                                <TableCell colSpan="2">{pie}</TableCell>
                            </TableRow>
                            <TableRow>
                                {heads}
                            </TableRow>
                            <TableRow>
                                <TableCell colSpan="4" align="center"
                                           hidden={this.state.hideSkeleton}>
                                    <Skeleton variant="text" className="w-100 p-3"
                                              height={100}/>
                                    <Skeleton variant="text" className="w-100 p-3"
                                              height={100}/>
                                    <Skeleton variant="text" className="w-100 p-3"
                                              height={100}/>
                                </TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {rows}
                        </TableBody>
                    </Table>
                </TableContainer>
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
            </center>
        </div>
    }

    refreshListCases() {
        this.setState({
            hideSkeleton: false
        })
        axios.get('/api/test/case/unit/list').then(res => {
            if (res.data.code === 200) {
                const units = res.data.content;
                console.log(units)
                this.setState({
                    units: units,
                    hideSkeleton: true,
                })
            } else {
                this.snackBarFail(res)
                this.setState({
                    hideSkeleton: true
                })
            }
        }).catch((error) => {
            this.snackBarError(error)
            this.setState({
                hideSkeleton: false
            })
        })
    }

    componentDidMount() {
        this.refreshListCases()
    }

    componentWillUnmount() {
    }
}