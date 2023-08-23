// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import * as React from 'react';
import Button from '@mui/material/Button';
import { DataGrid, GridToolbar } from '@mui/x-data-grid';
import { Link } from "@mui/material";

export default class TaskReportTable extends React.Component {
    state = {
        reportTableFile: this.props.reportTableFile,
        reportTable: null,
        testTask: this.props.testTask,
    };

    RenderDate(props) {
        if (props == null || props.value == null) {
            return null;
        }
        const [linkText, linkUrl] = props.value.split("@@");
        if (linkUrl == null) {
            return null;
        }
        return (
            <Link href={linkUrl} target="_blank" rel="noopener noreferrer">
                {linkText}
            </Link>
        );
    };

    render() {
        let reportTable = {
            columns: [
                { field: 'id', headerName: 'ID', width: 70 },
                { field: 'firstName', headerName: 'First name', width: 130 },
                { field: 'lastName', headerName: 'Last name', width: 130 },
                {
                    field: 'age',
                    headerName: 'Age',
                    type: 'number',
                    width: 90,
                },
                {
                    field: 'fullName',
                    headerName: 'Full name',
                    description: 'This column has a value getter and is not sortable.',
                    sortable: false,
                    width: 160,
                    valueGetter: (params) =>
                        `${params.row.firstName || ''} ${params.row.lastName || ''}`,
                },
                {
                    field: 'download',
                    headerName: 'Download',
                    renderCell: this.RenderDate,
                }
            ],
            rows: [
                { id: 1, lastName: 'Snow', firstName: 'Jon', age: 35, download: "Baidu@@https://www.baidu.com" },
                { id: 2, lastName: 'Lannister', firstName: 'Cersei', age: 42 },
                { id: 3, lastName: 'Lannister', firstName: 'Jaime', age: 45 },
                { id: 4, lastName: 'Stark', firstName: 'Arya', age: 16 },
                { id: 5, lastName: 'Targaryen', firstName: 'Daenerys', age: null },
                { id: 6, lastName: 'Melisandre', firstName: null, age: 150 },
                { id: 7, lastName: 'Clifford', firstName: 'Ferrara', age: 44 },
                { id: 8, lastName: 'Frances', firstName: 'Rossini', age: 36 },
                { id: 9, lastName: 'Roxie', firstName: 'Harvey', age: 65 },
            ],
        };

        reportTable.columns.forEach((column) => {
            if (column.hyperlink) {
                column.renderCell = hyperlinkRender;
            }
        });

        return (
            <div style={{ width: '100%' }}>
                <div style={{ backgroundColor: '#2F5496', color: 'white', padding: '10px', fontSize: 'medium', fontWeight: 'bold', marginBottom: '20px' }}>
                    Task Report Table
                </div>
                <DataGrid
                    rows={reportTable.rows}
                    columns={reportTable.columns}
                    initialState={{
                        pagination: {
                            paginationModel: { page: 0, pageSize: 25 },
                        },
                    }}
                    slots={{
                        toolbar: GridToolbar,
                    }}
                />
            </div>
      
          );
    };
};
