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
import { withStyles } from '@material-ui/core/styles';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import TextField from "@mui/material/TextField";
import DialogActions from "@mui/material/DialogActions";
import DialogContentText from "@mui/material/DialogContentText";
import Alert from "@mui/material/Alert";
import Snackbar from "@mui/material/Snackbar";
import LoadingButton from '@mui/lab/LoadingButton';
import Skeleton from "@mui/material/Skeleton";
import IconButton from "@mui/material/IconButton";
import BaseView from "@/component/BaseView";
import FormControl from "@mui/material/FormControl";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import {FormHelperText} from "@mui/material";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
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

export default class TestJsonView extends BaseView {


    state = {
        testJsonList: null,
        hideSkeleton: true,
        uploading: false,
        uploadDialogIsShow: false,
        historyList: null,
        historyIsShow: false,
        hideHistorySkeleton: true,
        uploadJsonFile: null,
        uploadPackageName: "",
        uploadCaseName: "",
        teamList: null,
        selectedTeamName: null

    }

    render() {
        const { snackbarIsShown, snackbarSeverity, snackbarMessage } = this.state;
        const { teamList } = this.state;
        const jsonHeadItems = ['Package name', 'Case name', 'Last update time', 'Latest Json path', 'Operation']
        const jsonHeads = []
        const jsonList = this.state.testJsonList;
        const jsonRows = []
        const historyHeadItems = ['Update time', 'Json path']
        const historyHeads = []
        const historyList = this.state.historyList;
        const historyRows = []

        if (jsonList) {
            jsonList.forEach((t) => {
                jsonRows.push(<StyledTableRow key={t.id} id={t.id} hover>
                    <TableCell id={t.id} align="center">
                        {t.packageName}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.caseName}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        {t.displayIngestTime}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <a href={t.blobUrl}> Download URL</a>
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <IconButton onClick={() => this.getJsonHistory(t.packageName, t.caseName)}>
                            <span className="material-icons-outlined">history</span>
                        </IconButton>
                        <IconButton onClick={() => this.showUploadDialog(t.packageName, t.caseName)}>
                            <span className="material-icons-outlined">upgrade</span>
                        </IconButton>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        jsonHeadItems.forEach((k) => jsonHeads.push(<StyledTableCell key={k} align="center">
            {k}
        </StyledTableCell>))

        if (historyList) {
            historyList.forEach((t) => {
                historyRows.push(<StyledTableRow key={t.id} id={t.id} hover>
                    <TableCell id={t.id} align="center">
                        {t.displayIngestTime}
                    </TableCell>
                    <TableCell id={t.id} align="center">
                        <a href={t.blobUrl}> Download URL</a>
                    </TableCell>
                </StyledTableRow>)
            })
        }

        historyHeadItems.forEach((m) => historyHeads.push(<StyledTableCell key={m} align="center">
            {m}
        </StyledTableCell>))

        return <div>
            <center>
                <TableContainer style={{ margin: "auto" }}>
                    <Table size="medium">
                        <TableHead>
                            <TableRow>
                                <TableCell colSpan="3">
                                    <Typography variant="h4" className="mt-2 mb-2">
                                        T2C Test Runner</Typography>
                                </TableCell>
                                <TableCell colSpan="3">
                                    <Stack direction="row" spacing={2}
                                        justifyContent="flex-end">
                                        <LoadingButton
                                            variant="contained"
                                            loading={this.state.uploading}
                                            loadingPosition="end"
                                            onClick={() => this.showUploadDialog("", "")}
                                            endIcon={<span
                                                className="material-icons-outlined">add</span>}>
                                            Add T2C Json
                                        </LoadingButton>
                                    </Stack>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                {jsonHeads}
                            </TableRow>
                        </TableHead>
                        <TableCell colSpan="5" align="center" hidden={this.state.hideSkeleton}>
                            <Skeleton variant="text" className="w-100 p-3"
                                height={100} />
                            <Skeleton variant="text" className="w-100 p-3"
                                height={100} />
                        </TableCell>
                        <TableBody>
                            {jsonRows}
                        </TableBody>
                    </Table>
                </TableContainer>
                <Dialog open={this.state.historyIsShow}
                    fullWidth={true}
                    onClose={() => this.handleStatus("historyIsShow", false)}>
                    <DialogTitle>History</DialogTitle>
                    <DialogContent>
                        <TableContainer style={{ margin: "auto" }}>
                            <Table size="medium">
                                <TableHead>
                                    <TableRow>
                                        {historyHeads}
                                    </TableRow>
                                </TableHead>
                                <TableCell colSpan="3" align="center" hidden={this.state.hideHistorySkeleton}>
                                    <Skeleton variant="text" className="w-100 p-3"
                                        height={100} />
                                    <Skeleton variant="text" className="w-100 p-3"
                                        height={100} />
                                    <Skeleton variant="text" className="w-100 p-3"
                                        height={100} />
                                </TableCell>
                                <TableBody>
                                    {historyRows}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => this.handleStatus("historyIsShow", false)}>Close</Button>
                    </DialogActions>
                </Dialog>
                <Dialog open={this.state.uploadDialogIsShow}
                    fullWidth={true}
                    onClose={() => this.handleStatus("uploadDialogIsShow", false)}>
                    <DialogTitle>Upload test Json</DialogTitle>
                    <DialogContent>
                        {/*<DialogContentText id="alert-dialog-upload">*/}
                        {/*    JSON file: <label htmlFor="json-file">*/}
                        {/*        <input required*/}
                        {/*            accept=".json" id="uploadJsonFile" type="file"*/}
                        {/*            onChange={(e) => this.setState({ uploadJsonFile: e.target.files[0] })} />*/}
                        {/*    </label>*/}
                        {/*</DialogContentText>*/}
                        <FormControl required fullWidth={true}>
                            <Button
                                component="label"
                                variant="outlined"
                                startIcon={<UploadFileIcon />}
                            >
                                {this.state.uploadJsonFile ? this.state.uploadJsonFile.name : 'JSON file'}
                                <input id="uploadJsonFile"
                                       type="file"
                                       accept=".json"
                                       hidden
                                       onChange={this.handleFileUpload}
                                />
                            </Button>
                            <FormHelperText> </FormHelperText>
                        </FormControl> <br/>
                        <FormControl required fullWidth={true}>
                            <InputLabel id="agent-team-select-label" >Team</InputLabel>
                            <Select
                                labelId="agent-team-select-label"
                                id="agent-team-select"
                                label="Team"
                                size="small"
                                value={teamList ? this.state.selectedTeamName : 'None_Team'}
                                onChange={(select) => this.handleStatus('selectedTeamName', select.target.value)}
                            >
                                {teamList ? null : <MenuItem value={'None_Team'}>No team available</MenuItem>}
                                {teamList ? teamList.map((team, index) => (
                                    <MenuItem value={team.teamName} key={team.teamName}>{team.teamName}</MenuItem>
                                )) : null}
                            </Select>
                            <FormHelperText> </FormHelperText>
                        </FormControl> <br/>
                        <FormControl required fullWidth={true}>
                            <TextField
                                name="uploadPackageName"
                                value={this.state.uploadPackageName}
                                label="Package name"
                                size="small"
                                margin="dense"
                                onChange={this.handleValueChange}
                                fullWidth
                            />
                        </FormControl> <br/>
                        <FormControl required fullWidth={true}>
                            <TextField
                                name="uploadCaseName"
                                value={this.state.uploadCaseName}
                                label="Case name"
                                size="small"
                                margin="dense"
                                onChange={this.handleValueChange}
                                fullWidth
                            />
                        </FormControl> <br/>
                    </DialogContent>
                    <DialogActions>
                        <Button
                            onClick={() => this.handleStatus("uploadDialogIsShow", false)}>Cancel</Button>
                        <Button
                            variant="contained"
                            endIcon={<span className="material-icons-outlined">file_upload</span>}
                            onClick={() => this.uploadJson()}>Upload</Button>
                    </DialogActions>
                </Dialog>
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
            </center>
        </div>
    }

    refreshJsonList() {
        this.setState({
            hideSkeleton: false,
            testJsonList: null,
        })
        axios.get('/api/package/testJsonList').then(res => {
            const jsonList = res.data.content;
            console.log(jsonList)
            this.setState({
                testJsonList: jsonList,
                hideSkeleton: true
            })
        })
    }

    getJsonHistory(packageName, caseName) {
        this.setState({
            historyIsShow: true,
            historyList: null,
            hideHistorySkeleton: false,
        })
        axios.get('/api/package/testJsonHistory/' + packageName + '/' + caseName).then(res => {
            const resList = res.data.content;
            console.log(resList)
            this.setState({
                historyList: resList,
                hideHistorySkeleton: true,
            })
        })
    }

    showUploadDialog(defPackageName, defCaseName) {
        this.setState({
            uploadDialogIsShow: true,
            uploadPackageName: defPackageName,
            uploadCaseName: defCaseName,
        })
    }

    uploadJson() {
        const formData = new FormData();
        formData.append("teamName", this.state.selectedTeamName);
        formData.append("testJsonFile", this.state.uploadJsonFile);
        formData.append("packageName", this.state.uploadPackageName);
        formData.append("caseName", this.state.uploadCaseName);
        axios.post('api/package/uploadJson', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        }).then(res => {
            console.log(res);
            if (res.data && res.data.code === 200) {
                this.setState({
                    snackbarSeverity: "success",
                    snackbarMessage: "Json successfully uploaded",
                    snackbarIsShown: true,
                    uploading: false
                })
                this.refreshJsonList()
            } else {
                this.snackBarFail(res)
                this.setState({
                    uploading: false
                })
            }
        }).catch((error) => {
            this.snackBarError(error)
            this.setState({
                uploading: false
            })
        });
        this.setState({
            uploading: true,
            uploadDialogIsShow: false,
            uploadJsonFile: null,
            uploadPackageName: null,
            uploadCaseName: null,
            selectedTeamName: null
        })
    }

    componentDidMount() {
        this.refreshJsonList();
        this.refreshTeamList()
    }

    componentWillUnmount() {
        // cancel requests
    }

}