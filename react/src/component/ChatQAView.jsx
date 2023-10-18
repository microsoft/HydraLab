// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React, { useState } from 'react'
import styles from '@chatscope/chat-ui-kit-styles/dist/default/styles.min.css'
import chatStyle from '../css/chat.module.css';
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
const taskStartMsgPrefix = "The task has queued or started and the taskID is ";

export default class ChatQAView extends BaseView {
    constructor(props) {
        super(props)
        this.state = {
            uploadedFile: null,
            uploading: false,
            fileUploading: new Map(), // {msgId: 1, state: true}
            sessionId: null,
            messages: sessionStorage.getItem('chatSessionHistory') ? JSON.parse(sessionStorage.getItem('chatSessionHistory')) : [
                {
                    id: 0,
                    message: "Hi, this is Hyda Lab Chat Bot, what can I do for you?",
                    sentTime: new Date().toDateString(),
                    sender: "Bot",
                    direction: "incoming",
                    type: "text",
                }
            ],
            waitingForResponse: false,
        }
        this.interval = null;
        this.inputReference = React.createRef();
    }

    downloadFile = () => {
        console.log("FILE DOWNLOADING")
    }

    // sendInputMessage = () => {
    //     console.log(this.inputReference.current);
    //     let content = "0";
    //     if (content) {
    //         this.sendMessage(content)
    //     }
    // }

    sendMessage = (content) => {
        console.log("OnSend message:", content)
        let msg = {
            id: this.state.messages.length,
            message: content,
            sentTime: new Date().toDateString(),
            sender: "User",
            direction: "outgoing",
            type: "text",
            uploadedFileName: null,
        }
        if (this.state.uploadedFile) {
            msg.type = "custom";
            msg.completingUpload = false;
            msg.uploadedFileName = this.state.uploadedFile?.name;
        }

        this.handleNewMessage(msg)
        this.askGPT(msg.message).then(res => {
            if (res.data && res.data.code === 200) {
                // any incoming message after clearing history shouldn't have any effect on state
                if (this.state.sessionId !== res.data.content.sessionId) {
                    return;
                }

                content = res.data.content.message;
                this.handleIncomingMessage(content);

                if (content.includes(taskStartMsgPrefix)) {
                    console.log("Http polling starts. Interval is 30 seconds");
                    let taskId = content.replace(taskStartMsgPrefix, "");
                    let intervalId = setInterval(() => {
                        this.queryTestStatus(taskId);
                    }, 30000);
                    this.interval = intervalId;
                }
                this.setState({
                    waitingForResponse: false,
                    fileUploading: this.completeUploading(msg.id),
                    uploading: false,
                })
            } else {
                console.log("Response code is not 200, res: ", res);
                this.snackBarFail(res)
                this.setState({
                    waitingForResponse: false,
                    fileUploading: this.completeUploading(msg.id),
                    uploading: false
                })
            }
        }).catch((error) => {
            console.log("Exception occurred, error: ", error);
            this.snackBarError(error)
            this.setState({
                waitingForResponse: false,
                fileUploading: this.completeUploading(msg.id),
                uploading: false
            })
        });
        this.setState({
            waitingForResponse: true,
            fileUploading: this.startUploading(msg),
            uploading: true,
            uploadedFile: null,
        })
    }

    handleIncomingMessage = (content) => {
        let responseMsg = {
            id: this.state.messages.length,
            message: content,
            sentTime: new Date().toDateString(),
            sender: "Bot",
            direction: "incoming",
            type: "text",
        }
        this.handleNewMessage(responseMsg);
    }

    handleNewMessage = (msg) => {
        let msgs = this.state.messages;
        msgs.push({
            id: msg.id,
            message: msg.message,
            sentTime: msg.sentTime,
            sender: msg.sender,
            direction: msg.direction,
            type: msg.type,
            uploadedFileName: msg.uploadedFileName,
        });
        sessionStorage.setItem("chatSessionHistory", JSON.stringify(msgs))
        this.setState({
            messages: msgs,
        });
    }

    queryTestStatus = (testTaskId) => {
        let question = "How about the test task " + testTaskId
        this.askGPT(question).then(res => {
            if (res.data && res.data.code === 200
                && res.data.content.success) {
                let content = res.data.content.message
                let responseMsg = {
                    id: this.state.messages.length,
                    message: content,
                    sentTime: new Date().toDateString(),
                    sender: "Bot",
                    direction: "incoming",
                    type: "text",
                }
                this.handleNewMessage(responseMsg)
                if (this.interval) {
                    clearInterval(this.interval)
                    this.interval = null;
                }
                console.log("Interval for task query is cleared.")
            }

        }).catch((error) => {
            this.snackBarError(error)
            this.setState({
                waitingForResponse: false,
                uploading: false
            })
        });
    }

    createOrReuseSession = () => {
        let chatSessionId = sessionStorage.getItem("chatSessionId")
        if (chatSessionId) {
            this.setState({
                sessionId: chatSessionId
            })
        }
        else {
            axios.get('/api/chat/qa/session').then(res => {
                if (res.data && res.data.code === 200) {
                    let chatQASessionId = res.data.content.sessionId;
                    this.setState({
                        sessionId: chatQASessionId
                    })
                    sessionStorage.setItem("chatSessionId", chatQASessionId)
                    console.log("current session id for chat QA: " + chatQASessionId)
                } else {
                    this.snackBarFail(res)
                }
            }).catch((error) => {
                this.snackBarError(error)
            })
        }
    }

    getMsgUploadingState = (id) => {
        return this.state.fileUploading.get(id);
    }

    resetFileInput = (e) => {
        e.target.value = null;
        this.setState({
            uploadedFile: null,
        })
    }

    startUploading = (msg) => {
        let uploadingStateList = this.state.fileUploading;
        if (msg.type == "custom") {
            uploadingStateList.set(msg.id, true);
        }
        return uploadingStateList;
    }

    completeUploading = (id) => {
        let uploadingStateList = this.state.fileUploading;
        uploadingStateList.set(id, false);
        return uploadingStateList;
    }

    clearSessionHistory = () => {
        sessionStorage.removeItem("chatSessionId");
        sessionStorage.removeItem("chatSessionHistory");
        this.setState({
            uploading: false,
            fileUploading: new Map(),
            sessionId: null,
            messages: [
                {
                    id: 0,
                    message: "Hi, this is Hyda Lab Chat Bot, what can I do for you?",
                    sentTime: new Date().toDateString(),
                    sender: "Bot",
                    direction: "incoming",
                    type: "text",
                }
            ],
            waitingForResponse: false,
        });
        if (this.interval) {
            clearInterval(this.interval)
            this.interval = null;
        }
        this.createOrReuseSession();
    }

    askGPT = (msg) => {
        const formData = new FormData()
        formData.append("sessionId", this.state.sessionId)
        formData.append("question", msg)
        formData.append("appFile", this.state.uploadedFile)

        return axios.post('/api/chat/qa/ask', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        })
    }

    // remove HTML in display content
    getPlainText = (e) => {
        e.preventDefault();
        const selection = window.getSelection();
        if (selection) {
            const range = selection.getRangeAt(0);
            const text = e.clipboardData.getData("text/plain");
            range.deleteContents();
            // paste clean text at the cursor position
            range.insertNode(document.createTextNode(text));
            selection.collapseToEnd();

            const editor = document.querySelector(".cs-message-input__content-editor");
            if (editor) {
                // The value was inserted into the editor without changing the context state.
                // We don't want to change the state, because it causes problems with placing the cursor in the right place,
                // and if the input was empty the send button will remain disabled.
                // So we need to dispatch the "input" event manually.
                // This will trigger the onChange event, and the state will be updated
                editor.dispatchEvent(new Event("input", { bubbles: true }));
            }
        }
    }

    // handleInputKeyPress = (event) => {
    //     if (event.key == 'Enter') {        
    //         let content = event.target.textContent;
    //         console.log(event.target);
    //         if (content) {
    //             this.sendMessage(content);
    //             // this.inputReference.current.reset();
    //         }
    //     }
    // }

    componentDidMount() {
        this.createOrReuseSession();
    }

    componentDidUpdate() {
        this.inputReference.current.focus();
    }

    render() {
        const messages = this.state.messages
        const waitingForResponse = this.state.waitingForResponse
        const displayedMessages = []

        messages.forEach((msg) => {
            switch (msg.type) {
                case 'custom':
                    let currentLoading = this.state.uploading && this.getMsgUploadingState(msg.id)
                    displayedMessages.push(
                        <Message model={{
                            sentTime: msg.sentTime,
                            sender: msg.sender,
                            direction: msg.direction,
                            position: "single",
                            type: msg.type
                        }}>
                            {msg.sender == 'Bot' ?
                                (<Avatar src={"images/hydra_lab_logo.png"} name={"Bot"} />) :
                                (<Avatar src={"/api/auth/getUserPhoto"} name={"User"} />)}
                            < Message.CustomContent >
                                {msg.message} < br /><br />
                                <LoadingButton
                                    variant="contained"
                                    // className="pl-4 pr-4"
                                    loading={currentLoading}
                                    disabled
                                    loadingPosition="end">
                                    {currentLoading ? 'Uploading    ' : msg.uploadedFileName}
                                </LoadingButton>
                            </Message.CustomContent>
                        </Message >
                    );
                    break;
                case 'text':
                    displayedMessages.push(
                        <Message model={{
                            message: msg.message,
                            sentTime: msg.sentTime,
                            sender: msg.sender,
                            direction: msg.direction,
                            position: "single",
                            type: msg.type
                        }}>
                            {msg.sender == 'Bot' ?
                                (<Avatar src={"images/hydra_lab_logo.png"} name={"Bot"} />) :
                                (<Avatar src={"/api/auth/getUserPhoto"} name={"User"} />)}
                        </Message>
                    );
                    break;
                default:
                    break;
            }
        })

        return <div >
            <Typography style={{ textAlign: 'center' }} variant="h4" className="m-3">
                Chat QA
            </Typography>
            <ChatContainer style={{ margin: 'auto', width: '70%', height: '850px', backgroundColor: '#fafafa' }}>
                <MessageList style={{ backgroundColor: '#fafafa', paddingTop: '10px', fontSize: '1rem' }}>
                    {displayedMessages}
                </MessageList>

                <div as="InputToolbox">
                    <hr style={{ margin: 'auto auto 5px' }} />
                    <FormControl required>
                        <Button style={{ margin: '10px' }}
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
                                onClick={this.resetFileInput}
                                onChange={this.handleFileUpload}
                            />
                        </Button>
                    </FormControl>
                    <Button style={{ margin: '10px 10px 10px auto', float: 'right' }}
                        variant="contained"
                        className="pl-4 pr-4"
                        disabled={this.state.messages.length <= 1}
                        onClick={this.clearSessionHistory}>
                        Clear Chat
                    </Button>
                    <MessageInput className={chatStyle['chat-message-input']} ref={this.inputReference}
                        disabled={waitingForResponse}
                        onPaste={this.getPlainText}
                        // onKeyDown={this.handleInputKeyPress}
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