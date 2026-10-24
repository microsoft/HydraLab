package com.microsoft.hydralab.center.openai.data;

import lombok.Data;

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
@Data
public class ChatMessage {
    private String role;
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    @SuppressWarnings("InterfaceIsType")
    public interface Role {
        String USER = "user";
        String SYSTEM = "system";
        String ASSISTANT = "assistant";
    }
}
