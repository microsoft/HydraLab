// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSON;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class GraphAPIUtils {

    private GraphAPIUtils() {

    }

    public static Group createSecurityGroup(IAuthenticationProvider authProvider) {
        GraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();

        Group group = new Group();
        group.description = "Your Phone App Selfhost Canary V2 user group";
        group.displayName = "Your Phone App Selfhost Canary test 2";
        LinkedList<String> groupTypesList = new LinkedList<String>();
        groupTypesList.add("Unified");
//        groupTypesList.add("DynamicMembership");
        group.groupTypes = groupTypesList;
        group.mailEnabled = true;
        group.mailNickname = "yourphonecanarytestv22";
        group.securityEnabled = true;
//        group.additionalDataManager().put("members@odata.bind",
//        new JsonPrimitive("[  \"https://graph.microsoft.com/v1.0/users/881881d0-7286-4f7e-9212-cc1c29b99a4b\",
//        \"https://graph.microsoft.com/v1.0/users/839b6578-080e-4cfc-b76c-b54771628c0a\"]"));

        return graphClient.groups()
                .buildRequest()
                .post(group);
    }

    public static User getMeUser(IAuthenticationProvider authProvider) {
        GraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();

        return graphClient.me()
                .buildRequest()
                .get();
    }

    public static UserCollectionPage getUserByAlias(IAuthenticationProvider authProvider, String alias) {
        GraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();

        return graphClient.users()
                .buildRequest()
                .filter(String.format("userPrincipalName eq '%s@microsoft.com'", alias))
                .select("id,displayName,mail,userPrincipalName")
                .get();
    }

    public static String getAsString(Object user) {
        return JSON.toJSONString(user);
    }

    public static IAuthenticationProvider getTokenStringProvider(String token) {
        return new IAuthenticationProvider() {
            @NotNull
            @Override
            public CompletableFuture<String> getAuthorizationTokenAsync(@NotNull URL requestUrl) {
                return new CompletableFuture<String>() {
                    @Override
                    public String get() throws InterruptedException, ExecutionException {
                        return token;
                    }
                };
            }
        };

    }
}
