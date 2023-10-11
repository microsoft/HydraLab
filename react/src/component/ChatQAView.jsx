// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React, { useState } from 'react'
import styles from '@chatscope/chat-ui-kit-styles/dist/default/styles.min.css';
import { ConversationHeader, ChatContainer, InfoButton, Message, MessageList, TypingIndicator, MessageInput, MessageSeparator, InputToolbox, AttachmentButton, SendButton } from '@chatscope/chat-ui-kit-react'


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
    // todo: 
    //  1. 第二次上传时如果cancel会报错（clear state）
    //  2. uploading 状态无法单独控制，会同步动画
    //  3. 输入问题：
    //      disable 输入框改为 disable button & 回车发送
    //      自定义button设法拿到input的内容
    //      发送后清空input内容

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
                    type: "text",
                }
            ],
            waitingForRes: false,
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
            message: content,
            sentTime: new Date().toDateString(),
            sender: "User",
            direction: "outgoing",
            type: "text",
            completingUpload: null,
            uploadedFileName: null,
        }
        if (this.state.uploadedFile) {
            msg.type = "custom";
            msg.completingUpload = false;
            msg.uploadedFileName = this.state.uploadedFile?.name;
        }

        this.handleNewMessage(msg)
        this.askGPT(msg).then(res => {
            if (msg.type == "custom") {
                // todo: setState to rerender
                msg.completingUpload = true;
                console.log("[IN RESPONSE]msg.completingUpload: ", msg.completingUpload);
            }
            if (res.data && res.data.code === 200) {
                content = res.data.content.message
                let responseMsg = {
                    message: content,
                    sentTime: new Date().toDateString(),
                    sender: "Bot",
                    direction: "incoming",
                    type: "text",
                }
                this.handleNewMessage(responseMsg);

                if (content.includes(taskStartMsgPrefix)) {
                    console.log("Http polling starts. Interval is 30 seconds");
                    let taskId = content.replace(taskStartMsgPrefix, "");
                    let intervalId = setInterval(() => {
                        this.queryTestStatus(taskId)
                    }, 3000);
                    // }, 30000);
                    this.interval = intervalId;
                }

                this.setState({
                    waitingForRes: false,
                    uploading: false,
                })
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
            id: msgs.length,
            message: msg.message,
            sentTime: msg.sentTime,
            sender: msg.sender,
            direction: msg.direction,
            type: msg.type,
            completingUpload: msg.completingUpload,
            uploadedFileName: msg.uploadedFileName,
        });
        ls.set("chatSessionHistory", msgs)
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
                waitingForRes: false,
                uploading: false
            })
        });
    }

    createOrReuseSession() {
        // let chatSessionId = ls.get("chatSessionId")
        // if (chatSessionId) {
        //     this.setState({
        //         sessionId: chatSessionId
        //     })
        // }
        // else {
        axios.get('/api/qa/gpt/session').then(res => {
            if (res.data && res.data.code === 200) {
                let chatQASessionId = res.data.content.sessionId;
                this.setState({
                    sessionId: chatQASessionId
                })
                ls.set("chatSessionId", chatQASessionId)
                console.log("current session id for chat QA: " + chatQASessionId)
            } else {
                this.snackBarFail(res)
            }
        }).catch((error) => {
            this.snackBarError(error)
        })
        // }
    }

    askGPT = (msg) => {
        const formData = new FormData()
        formData.append("sessionId", this.state.sessionId)
        formData.append("question", msg.message)
        formData.append("appFile", this.state.uploadedFile)

        return axios.post('/api/qa/gpt/ask', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        })
    }

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

        messages.forEach((msg) => {
            let currentLoading = this.state.uploading && !msg.completingUpload
            switch (msg.type) {
                case 'custom':
                    displayedMessages.push(
                        <Message model={{
                            sentTime: msg.sentTime,
                            sender: msg.sender,
                            direction: msg.direction,
                            position: "single",
                            type: msg.type
                        }}>
                            <Message.CustomContent>
                                {msg.message}<br /><br />
                                <LoadingButton
                                    variant="contained"
                                    // className="pl-4 pr-4"
                                    loading={currentLoading}
                                    disabled
                                    loadingPosition="end">
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
                            position: "single",
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
            <Typography style={{ textAlign: 'center' }} variant="h4" className="m-3">
                Chat QA
            </Typography>
            <ChatContainer style={{ margin: 'auto', width: '70%', height: '850px', backgroundColor: '#fafafa' }}>
                <MessageList style={{ backgroundColor: '#fafafa', paddingTop: '10px' }}>
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
                                onChange={this.handleFileUpload}
                            />
                        </Button>
                    </FormControl>
                    {/* <LoadingButton style={{ margin: '10px 10px 10px auto', float: 'right' }}
                        variant="contained"
                        className="pl-4 pr-4"
                        disabled={false}
                        loading={false}
                        loadingPosition="end"
                        onClick={this.sendInputMessage}
                        endIcon={<span className="material-icons-outlined">send</span>}>
                        Run
                    </LoadingButton> */}
                    <MessageInput style={{ margin: '5px auto auto', backgroundColor: '#fafafa' }} ref={this.inputReference}
                        disabled={waitingForRes}
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