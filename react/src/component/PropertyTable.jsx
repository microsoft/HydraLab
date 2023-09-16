// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React from 'react'
import 'bootstrap/dist/css/bootstrap.css'
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import { withStyles } from '@material-ui/core/styles';
import _ from 'lodash';

/**
 * Palette
 * https://material-ui.com/customization/palette/
 */
const StyledTableCell = withStyles((theme) => ({
    head: {
        backgroundColor: theme.palette.primary.main,
        color: theme.palette.common.white,
    },
    body: {
        fontSize: 14,
    },
}))(TableCell);

const StyledTableRow = withStyles((theme) => ({
    root: {
        '&:nth-of-type(odd)': {
            backgroundColor: theme.palette.action.hover,
        },
    },
}))(TableRow);

export default class AdaptivePropertyTable extends React.Component {

    render() {
        let properties = this.props.properties;
        const title = this.props.title ? this.props.title : null
        const lineTextCount = this.props.lineTextCount ? this.props.lineTextCount : 40
        const propertyValueProcessor = this.props.propertyValueProcessor
        const pList = []
        const mergeList = [];
        const colSpan = 4;

        const keyConverter = (k) => k.replace(/\b\w/g, l => l.toUpperCase()) + ":"

        properties = _.sortBy(properties, (item) => item.v ? item.v.toString().length : 0)

        properties.forEach((entry) => {
            var key = keyConverter(entry.k)
            var value = entry.v
            if (!value) {
                return
            }

            var measure = key + value.toString()
            var showedValueElement = value
            if (propertyValueProcessor) {
                showedValueElement = propertyValueProcessor(entry.k, value)
            }
            if (showedValueElement === 'SKIP') {
                return
            }

            if (measure.length > lineTextCount) {
                pList.push(
                    <StyledTableRow key={key} >
                        <TableCell key={key} style={{ verticalAlign: 'top' }}><b>{key}</b></TableCell>
                        <TableCell colSpan={colSpan - 1} style={{ maxWidth: "640px" }}>
                            { showedValueElement ? showedValueElement : value.toString() }
                        </TableCell>
                    </StyledTableRow>)
            } else {
                mergeList.push({ k: key, v: (showedValueElement ? showedValueElement : value.toString()) })
                if (mergeList.length === 2) {
                    pList.push(
                        <StyledTableRow key={key} >
                            {mergeList.map((item) => [<TableCell key={item.k}><b>{item.k}</b></TableCell>, <TableCell key={item.k + "v"}>{item.v}</TableCell>])}
                        </StyledTableRow>)
                    mergeList.length = 0
                }
            }
        })

        if (mergeList.length > 0) {
            var key = mergeList[0].k
            var value = mergeList[0].v
            pList.unshift(
                <StyledTableRow key={key} >
                    <TableCell key={key}><b>{key}</b></TableCell>
                    <TableCell colSpan={colSpan - 1}>{value.toString()}</TableCell>
                </StyledTableRow>)
        }

        var titlePart = null
        if (title) {
            titlePart = <TableHead>
                <TableRow key="title-1">
                    <StyledTableCell colSpan={colSpan}><b>{title}</b></StyledTableCell>
                </TableRow>
            </TableHead>
        }

        return <center>
            <TableContainer component={Paper}>
                <Table aria-label="simple table" size="small">
                    {titlePart}
                    <TableBody>
                        {pList}
                    </TableBody>
                </Table>
            </TableContainer>
            <table className="table table-borderless">
                <tbody>
                    <tr>
                        <td align='center'>
                        </td>
                    </tr>
                </tbody>
            </table>
        </center>
    }

    componentDidMount() {
        console.log("componentDidMount")
    }

}