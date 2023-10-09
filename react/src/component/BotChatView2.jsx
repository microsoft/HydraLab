// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React, { useState } from 'react'
import styles from '@chatscope/chat-ui-kit-styles/dist/default/styles.min.css';
import { Avatar, ConversationHeader, ChatContainer, InfoButton, Message, MessageList, TypingIndicator, MessageInput, MessageSeparator, InputToolbox, AttachmentButton, SendButton } from '@chatscope/chat-ui-kit-react'


import axios from '@/axios'
import FormControl from '@mui/material/FormControl';
import Typography from '@material-ui/core/Typography';
import LoadingButton from '@mui/lab/LoadingButton';
import UploadFileIcon from "@mui/icons-material/UploadFile";

import 'bootstrap/dist/css/bootstrap.css'
import Button from '@mui/material/Button';
import BaseView from "@/component/BaseView";

// window.sessionStorage
let ls = require('local-storage');

export default class BotChatView2 extends BaseView {
    // todo: 
    //  1. 第二次上传时如果cancel会报错（clear state）
    //  2. uploading 状态无法单独控制，会同步动画

    constructor(props) {
        super(props)
        this.state = {
            uploadDialogIsShown: false,
            uploading: false,
            uploadedFile: null,
            sessionId: null,
            messages: ls.get('chatSessionHistory') ? ls.get('chatSessionHistory') : [
                {
                    message: "Hi, this is Hyda Lab Chat Bot, what can I do for you?",
                    sentTime: new Date().toDateString(),
                    sender: "Bot",
                    direction: "incoming",
                    position: "first",
                    type: "text",
                }
            ],
            waitingForRes: false,
        }
        this.inputReference = React.createRef();
    }
    
    createOrReuseSession() {
        let chatSessionId = ls.get("chatSessionId")
        if (chatSessionId) {
            this.setState({
                sessionId: chatSessionId
            })
        }
        else {
            axios.get('/api/qa/gpt/session').then(res => {
                if (res.data && res.data.code === 200) {
                    this.setState({
                        sessionId: res.data.content.sessionId
                    })
                    ls.set("chatSessionId", res.data.content.sessionId)
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => {
                this.snackBarError(error)
            })
        }
    }

    downloadFile = () => {
        console.log("FILE DOWNLOADING")
    }

    sendMessage = (content) => {
        console.log("ONSEND MESSAGE & FILE:", content)
        let msg = {
            message: content,
            sentTime: new Date().toDateString(),
            sender: "User",
            direction: "outgoing",
            position: "normal",
            type: "text",
            completingUpload: null,
            uploadedFileName: null,
        }
        if (this.state.uploadedFile) {
            // console.log("Current state of uploading:", this.state.uploading)
            // msg.message = (<Message.CustomContent>
            //         {msg.message}<br/><br/>
            //         <LoadingButton
            //             variant="contained"
            //             className="pl-4 pr-4"
            //             loading={this.state.uploading}
            //             loadingPosition="end"
            //             onClick={() => {
            //                 this.downloadFile()
            //             }}
            //             endIcon={<span
            //                 className="material-icons-outlined">file_download</span>}>
            //             Uploading
            //         </LoadingButton>
            //     </Message.CustomContent>);
            msg.type = "custom";
            msg.completingUpload = false;
            msg.uploadedFileName = this.state.uploadedFile?.name;
        }

        this.handleNewMessage(msg, false)

        const formData = new FormData()
        formData.append("sessionId", this.state.sessionId)
        formData.append("question", msg.message)
        formData.append("appFile", this.state.uploadedFile)

        axios.post('/api/qa/gpt/ask', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        })
        .then(res => {
            if (msg.type == "custom") {
                // todo: setState to rerender
                msg.completingUpload = true;
                console.log("[IN RESPONSE]msg.completingUpload: ", msg.completingUpload);
            }
            if (res.data && res.data.code === 200) {
                let responseMsg = {
                    message: res.data.content,
                    sentTime: new Date().toDateString(),
                    sender: "Bot",
                    direction: "incoming",
                    position: "normal",
                    type: "text",
                }
                this.handleNewMessage(responseMsg)
                this.setState({
                    waitingForRes: false,
                    uploading: false
                })
                
                console.log(this.state)
            } else {
                this.snackBarFail(res)
                this.setState({
                    waitingForRes: false,
                    uploading: false
                })
            }
        }).catch((error) => {
            this.snackBarError(error)
            this.setState({
                waitingForRes: false,
                uploading: false
            })
        });
        this.setState({
            waitingForRes: true,
            uploading: true,
            uploadedFile: null,
        })
    }

    handleNewMessage = (msg) => {
        let msgs = this.state.messages;
        msgs.push({
            message: msg.message,
            sentTime: msg.sentTime,
            sender: msg.sender,
            direction: msg.direction,
            position: msg.position,
            type: msg.type,
            completingUpload: msg.completingUpload,
            uploadedFileName: msg.uploadedFileName,
        });
        ls.set("chatSessionHistory", msgs)
        this.setState({
            messages: msgs,
        });
    }

    componentDidMount() {
        this.createOrReuseSession();
    }

    componentDidUpdate() {
        this.inputReference.current.focus();
    }

    // shouldComponentUpdate(nextProps, nextState) {
    //     console.log(this.state)
    //     console.log(nextState)
    //     if (this.state !== nextState) {
    //         return true;
    //     }
    //     return false;
    // }

    render() {
        const messages = this.state.messages
        const waitingForRes = this.state.waitingForRes
        const displayedMessages = []

        let botIco = 'images/default_user.png'
        messages.forEach((msg) => {
            let currentLoading = this.state.uploading && !msg.completingUpload
            switch (msg.type) {
                case 'custom':
                    displayedMessages.push(
                        <Message model={{
                            sentTime: msg.sentTime,
                            sender: msg.sender,
                            direction: msg.direction,
                            position: msg.position ? msg.position : "normal",
                            type: msg.type
                        }}>
                            <Message.CustomContent>
                                {msg.message}<br/><br/>
                                <LoadingButton
                                    variant="contained"
                                    // className="pl-4 pr-4"
                                    loading={currentLoading}
                                    disabled
                                    loadingPosition="end"
                                    onClick={() => {
                                        this.downloadFile()
                                    }}>
                                    {currentLoading ? 'Uploading    ' : msg.uploadedFileName}
                                </LoadingButton>
                            </Message.CustomContent>
                        </Message>
                    );
                    break;
                case 'text':
                    displayedMessages.push(
                        <Message model={{
                            message: msg.message,
                            sentTime: msg.sentTime,
                            sender: msg.sender,
                            direction: msg.direction,
                            position: msg.position ? msg.position : "normal",
                            type: msg.type
                        }}
                        />
                    );
                    break;
                default:
                    break;
            }
        })

        return <div >
            <Typography variant="h4" className="m-3">
                Chat Bot</Typography>
            <ChatContainer style={{ height: '850px' }}>
                {/* <ConversationHeader>
                    <Avatar src={botIco} name="Chat Bot" />
                    <ConversationHeader.Content userName="Chat Bot" info="" />
                    <ConversationHeader.Actions />
                </ConversationHeader> */}
                <MessageList >
                    {displayedMessages}
                </MessageList>

                <div as="InputToolbox">
                    <hr style={{margin: 'auto auto 5px'}} />
                    <FormControl required>
                        <Button 
                            component="label"
                            variant="outlined"
                            endIcon={<span
                                className="material-icons-outlined">file_upload</span>}
                        >
                            {this.state.uploadedFile ? this.state.uploadedFile?.name : 'Upload File'}
                            <input id="uploadedFile"
                                type="file"
                                accept=".apk,.ipa"
                                hidden
                                onChange={this.handleFileUpload}
                            />
                        </Button>
                    </FormControl>
                    <MessageInput style={{margin: '5px auto auto'}} ref={this.inputReference}
                        disabled={waitingForRes}
                        placeholder="Type message here"
                        autoFocus={true}
                        sendButton={true}
                        onSend={this.sendMessage}
                        attachButton={false} />
                </div>
                
            </ChatContainer>
        </div>
    }
}