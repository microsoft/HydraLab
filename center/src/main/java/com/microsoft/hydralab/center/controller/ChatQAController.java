// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.openai.ChatQAService;
import com.microsoft.hydralab.center.openai.data.ChatMessage;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.PkgUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

import static com.microsoft.hydralab.center.util.CenterConstant.CENTER_FILE_BASE_DIR;

/**
 * @author zhoule
 * @date 10/08/2023
 */

@RestController
@RequestMapping
public class ChatQAController {
    @Resource
    private ChatQAService chatQAService;
    @Resource
    private AttachmentService attachmentService;

    //session create
    @GetMapping("/api/chat/qa/session")
    public Result createSession() {
        String sessionId = chatQAService.createSession();
        JSONObject result = new JSONObject();
        result.put("sessionId", sessionId);
        result.put("message", "Session created successfully!");
        return Result.ok(result);
    }

    //session destroy
    @GetMapping("/api/chat/qa/session/destroy/{sessionId}")
    public Result destroySession(@PathVariable(value = "sessionId") String sessionId) {
        chatQAService.deleteSession(sessionId);
        return Result.ok("Session destroyed successfully!");
    }

    //ask question
    @PostMapping(value = {"/api/chat/qa/ask"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result askQuestion(@CurrentSecurityContext SysUser requestor,
                              @RequestHeader(HttpHeaders.HOST) String host,
                              @RequestParam(value = "sessionId") String sessionId,
                              @RequestParam(value = "question") String question,
                              @RequestParam(value = "appFile", required = false) MultipartFile appFile) {
        if (!chatQAService.isSessionExist(sessionId)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Session not exist!");
        }
        requestor.setHost(host);

        ChatQAService.ChatQAResult result = chatQAService.askQuestion(requestor, ChatMessage.Role.USER, sessionId, question);
        //Save test app file to server if exist
        if (appFile != null && !appFile.isEmpty()) {
            String relativeParent = FileUtil.getPathForToday();
            try {
                File tempAppFile = attachmentService.verifyAndSaveFile(appFile, CENTER_FILE_BASE_DIR + relativeParent, false, null,
                        PkgUtil.FILE_SUFFIX.APK_FILE, PkgUtil.FILE_SUFFIX.IPA_FILE);
                result = chatQAService.saveAppFile(requestor, sessionId, tempAppFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Result.ok(result);
    }

}
