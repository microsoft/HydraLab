// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import React from 'react'
import Chat, { useMessages, Bubble, Button, Card, CardActions, CardMedia, CardTitle, CardText } from '@chatui/core';
import '@chatui/core/dist/index.css';
import '../css/chatbot.css';
import BaseView from "@/component/BaseView";
import { components } from 'react-select';

// import 'bootstrap/dist/css/bootstrap.css'


const defaultQuickReplies = [
    {
        icon: 'message',
        name: 'Test my app',
        isNew: true,
        isHighlight: true,
    },
    {
        icon: 'message',
        name: 'My recent tests',
        isNew: true,
        isHighlight: true,
    },
];

function FC() {
    const { messages, appendMsg, setTyping } = useMessages([]);

    function handleQuickReplyClick(item) {
        console.log(item.name)
        handleSend('text', item.name);
    }
    
    function handleSend(type, val) {
        let actualVal = val.trim()
        if (type === 'text' && actualVal) {
            appendMsg({
                type: 'text',
                content: { text: val },
                position: 'right',
            });
    
            setTyping(true);
    
            setTimeout(() => {
                appendMsg({
                    type: 'text',
                    content: { text: 'Bala bala' },
                });
            }, 1000);
        }
        console.log("current message num: " + messages.length);
    }
    
    function uploadFile() {
        if (!state.selectedTeamName || !state.uploadAppInstallerFile) {
            this.snackBarMsg("Please upload APK/IPA/ZIP file and select a team")
            return
        }
        const formData = new FormData()
        formData.append("teamName", state.selectedTeamName)
        formData.append("appFile", state.uploadAppInstallerFile)
        formData.append("testAppFile", state.uploadTestPackageFile)
        formData.append("commitMessage", state.uploadTestDesc)
    
        axios.post('/api/package/add/', formData, {
            headers: {
                Accept: 'application/json',
                'content-type': 'multipart/form-data; ',
            }
        }).then(res => {
            if (res.data && res.data.code === 200) {
                // todo: append msg
            } else {
                // todo: alert
            }
        }).catch((error) => {
            // todo: alert
        });
    }

    function renderMessageContent(msg) {
        const { type, content } = msg;
        switch (type) {
            case 'text':
                return <Bubble content={content.text} />;
            case 'card':
                return <Card size="xl">
                    <CardMedia image="//gw.alicdn.com/tfs/TB1Xv5_vlr0gK0jSZFnXXbRRXXa-427-240.png" />
                    <CardTitle>Card title</CardTitle>
                    <CardText>{content.text}</CardText>
                    <CardText>content.text</CardText>
                    <CardActions>
                        <Button>
                            Upload app file
                            <input id="uploadAppInstallerFile"
                                type="file"
                                accept=".apk,.ipa,.zip"
                                onChange={uploadFile}
                            />
                        </Button>
                        <Button color="primary">Primary button</Button>
                    </CardActions>
                </Card>
            case 'image':
                return (
                    <Bubble type="image">
                        <img src={content.picUrl} alt="" />
                    </Bubble>
                );
            default:
                return null;
        }
    }


    return <Chat className='chatBot'
        navbar={{ title: 'QnA Bot' }}
        locale='en-US'
        messages={messages}
        quickReplies={defaultQuickReplies}
        onQuickReplyClick={handleQuickReplyClick}
        renderMessageContent={renderMessageContent}
        onSend={handleSend}>
    </Chat>
}




export default class BotChatView extends BaseView {
    state = {
        // messages: [
        //     {
        //         type: 'text',
        //         content : {
        //             text: 'nihao',
        //         },
        //         position: 'left'
        //     },
        //     {
        //         type: 'text',
        //         content : {
        //             text: 'nihao',
        //         },
        //         position: 'right'
        //     },
        //     {
        //         type: 'text',
        //         content : {
        //             text: 'nihao',
        //         },
        //         position: 'right'
        //     },
        //     {
        //         type: 'card',
        //         content : {
        //             text: 'nihao',
        //         },
        //         position: 'right'
        //     },
        // ]

    }

    render = () => {
        return (
            <FC />
        );
    }
}