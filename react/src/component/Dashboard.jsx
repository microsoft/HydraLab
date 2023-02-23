// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React, {useEffect, useState} from 'react';
import clsx from 'clsx';
import {createMuiTheme, makeStyles} from '@material-ui/core/styles';
import CssBaseline from '@material-ui/core/CssBaseline';
import Drawer from '@material-ui/core/Drawer';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import List from '@material-ui/core/List';
import Divider from '@material-ui/core/Divider';
import Tooltip from "@mui/material/Tooltip";
import Grid from '@material-ui/core/Grid';
import Paper from '@material-ui/core/Paper';
import DeviceAgentsView from '@/component/DeviceAgentsView'
import DeviceGroupView from '@/component/DeviceGroupView'
import SearchView from '@/component/SearchView'
import RunnerView from '@/component/RunnerView'
import TestTaskView from '@/component/TasksView'
import AuthView from '@/component/AuthView'
import HeaderView from "@/component/HeaderView";
import {HashRouter as Router, Link, Route, Switch, useParams} from "react-router-dom";
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import {ThemeProvider} from '@material-ui/styles';
import Container from '@mui/material/Container';
import axios from "@/axios";
import TestJsonView from './TestJsonView';
import TeamManagement from "@/component/TeamManagement";

const drawerWidth = 240;

const grafanaWeb = "/grafana/dashboards"

let ls = require('local-storage');

const useStyles = makeStyles((theme) => ({
    root: {
        display: 'flex',
    },
    toolbar: {
        paddingRight: 24, // keep right padding when drawer closed
    },
    toolbarIcon: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        padding: '0 8px',
        ...theme.mixins.toolbar,
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
    },
    appBarShift: {
        marginLeft: drawerWidth,
        width: `calc(100% - ${drawerWidth}px)`,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    menuButton: {
        marginRight: 36,
    },
    menuButtonHidden: {
        display: 'none',
    },
    title: {
        flexGrow: 1,
    },
    drawerPaper: {
        position: 'relative',
        whiteSpace: 'nowrap',
        width: drawerWidth,
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    drawerPaperClose: {
        overflowX: 'hidden',
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        width: theme.spacing(7),
        [theme.breakpoints.up('sm')]: {
            width: theme.spacing(9),
        },
    },
    appBarSpacer: theme.mixins.toolbar,
    content: {
        flexGrow: 1,
        height: '100vh',
        overflow: 'auto',
    },
    container: {
        paddingTop: theme.spacing(4),
        paddingBottom: theme.spacing(4),
    },
    paper: {
        padding: theme.spacing(2),
        display: 'flex',
        overflow: 'auto',
        flexDirection: 'column',
    },
    fixedHeight: {
        height: 240,
    },
}));

export default function Dashboard() {
    const classes = useStyles();
    const [open, setOpen] = useState(true);
    const [centerVersion, setCenterVersion] = useState(0);

    const handleDrawer = () => {
        setOpen(!open);
    };

    const theme = createMuiTheme({
        palette: {
            primary: {
                main: "#0078d4",
                light: "#5ea6ff",
                dark: "#004ca1",
            },
            secondary: {
                main: "#1de9b6",
                light: "#6effe8",
                dark: "#00b686",
            },
        },
        typography: {
            fontFamily: ['Lato', 'Roboto'].join(','),
        }
    });

    useEffect(() => {
        axios.get('/api/center/info').then(res => {
            if (res.data && res.data.code === 200) {
                setCenterVersion(res.data.content.versionName)
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })

        axios.get('/api/package/getSAS').then(res => {
            if (res.data && res.data.code === 200) {
                ls.set("BlobSignature", res.data.content.signature);
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
    });

    return (
        <div id='main_root' className={classes.root}>
            <ThemeProvider theme={theme}>
                <CssBaseline/>
                <AppBar position="absolute"
                        className={clsx(classes.appBar)}>
                    <Toolbar className={classes.toolbar}>
                        <HeaderView/>
                    </Toolbar>
                </AppBar>
                <Router>
                    <Drawer
                        variant="permanent"
                        classes={{
                            paper: clsx(classes.drawerPaper, !open && classes.drawerPaperClose),
                        }}
                        open={open}>
                        <div className={classes.toolbarIcon}>
                        </div>
                        {/* search for an icon hosted by Google Fonts in https://fonts.google.com/icons?icon.set=Material+Icons */}
                        <List>
                            <ListItem component={Link} to={'/devices'} button
                                      sx={{flexGrow: 1}}>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">devices</span>
                                </ListItemIcon>
                                <ListItemText primary="Devices"/>
                            </ListItem>
                            <ListItem component={Link} to={'/manage'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">table_view</span>
                                </ListItemIcon>
                                <ListItemText primary="Device Group"/>
                            </ListItem>
                            <ListItem component={Link} to={'/tasks'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">task</span>
                                </ListItemIcon>
                                <ListItemText primary="Tasks"/>
                            </ListItem>
                            {/* <ListItem component={Link} to={'/stability'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">balance</span>
                                </ListItemIcon>
                                <ListItemText primary="Stability"/>
                            </ListItem> */}
                            <ListItem component={Link} to={'/runner'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">adb</span>
                                </ListItemIcon>
                                <ListItemText primary="Runner"/>
                            </ListItem>
                            <ListItem component={Link} to={'/info'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">search</span>
                                </ListItemIcon>
                                <ListItemText primary="Search"/>
                            </ListItem>
                            <ListItem component={Link} to={'/auth'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">manage_accounts</span>
                                </ListItemIcon>
                                <ListItemText primary="Authentication"/>
                            </ListItem>
                            <ListItem component="a" href={grafanaWeb} target="_blank"
                                      rel="noopener noreferrer" button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">analytics</span>
                                </ListItemIcon>
                                <ListItemText primary="Dashboard"/>
                            </ListItem>
                            {/* <ListItem component={Link} to={'/json'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">pending_actions</span>
                                </ListItemIcon>
                                <ListItemText primary="T2C Test Runner" />
                            </ListItem> */}
                            <ListItem component={Link} to={'/team'} button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">people</span>
                                </ListItemIcon>
                                <ListItemText primary="Team Management" />
                            </ListItem>
                            <ListItem component="a" href='https://github.com/microsoft/HydraLab/blob/main/README.md' target="_blank"
                                      rel="noopener noreferrer" button>
                                <ListItemIcon>
                                    <span className="material-icons-outlined">info</span>
                                </ListItemIcon>
                                <ListItemText primary="About"/>
                                <iframe style={{margin: '6px 0px 0px 0px'}}
                                        src="https://ghbtns.com/github-btn.html?user=microsoft&repo=HydraLab&type=star&count=true"
                                        frameBorder="0" scrolling="0" align="right" width="90" height="25" title="GitHub"></iframe>
                            </ListItem>
                            <ListItem
                                style={{
                                    height: "calc(100vh - 561px)",
                                    pointerEvents: "none"
                                }}>
                            </ListItem>
                            <Divider/>
                            <ListItem onClick={handleDrawer} button>
                                <ListItemIcon>
                                    {open ? <span className="material-icons">chevron_left</span>
                                        : <span className="material-icons">chevron_right</span>
                                    }
                                </ListItemIcon>
                                <ListItemText primary={centerVersion} style={{
                                    textAlign: "end"
                                }}/>
                            </ListItem>
                        </List>
                    </Drawer>
                    <main id='main_top' className={classes.content}>
                        <div id='main_sub' className={classes.appBarSpacer}/>
                        <Container id='main_container' maxWidth="xl" className={classes.container}>
                            <Grid container spacing={3}>
                                <Grid item xs={12}>
                                    <Paper className={classes.paper}>
                                        <Switch>
                                            <Route exact path="/">
                                                <DeviceAgentsView/>
                                            </Route>
                                            <Route exact path="/devices">
                                                <DeviceAgentsView/>
                                            </Route>
                                            <Route exact path="/manage">
                                                <DeviceGroupView/>
                                            </Route>
                                            <Route path="/runner">
                                                <RunnerView theme={theme}/>
                                            </Route>
                                            <Route path="/tasks/:current_page?">
                                                <TestTask/>
                                            </Route>
                                            {/* <Route exact path="/stability">
                                                <StabilityView/>
                                            </Route> */}
                                            <Route exact path="/info">
                                                <SearchView/>
                                            </Route>
                                            <Route exact path="/auth">
                                                <AuthView/>
                                            </Route>
                                            {/* <Route exact path="/json">
                                                <TestJsonView theme={theme} />
                                            </Route> */}
                                            <Route exact path="/team">
                                                <TeamManagement />
                                            </Route>
                                            <Route path="/info/:type/:id" children={<Child/>}/>
                                        </Switch>
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Container>
                    </main>
                </Router>
            </ThemeProvider>
        </div>
    )
}

function Child() {
    // We can use the `useParams` hook here to access
    // the dynamic pieces of the URL.
    let {type, id} = useParams();
    return <SearchView infoType={type} infoId={id}/>;
}

function TestTask() {
    // We can use the `useParams` hook here to access
    // the dynamic pieces of the URL.
    let {current_page} = useParams();
    current_page = /^\d+$/.test(current_page) ? parseInt(current_page, 10) : 1

    console.log(`TestTaskView, Page:  ${current_page}`)
    return <TestTaskView page={current_page}/>;
}