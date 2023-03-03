// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.util;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.config.RestTemplateConfig;
import com.microsoft.hydralab.common.util.FileUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthUtil {

    @Value("${spring.security.oauth2.client.provider.azure-ad.token-uri:}")
    String tokenUrl;
    @Value("${spring.security.oauth2.client.provider.azure-ad.photo-uri:}")
    String photoUrl;
    @Value("${spring.security.oauth2.client.provider.azure-ad.authorization-uri:}")
    String authorizationUri;

    @Value("${spring.security.oauth2.client.registration.azure-client.client-id:}")
    String clientId;
    @Value("${spring.security.oauth2.client.registration.azure-client.client-secret:}")
    String clientSecret;
    @Value("${spring.security.oauth2.client.registration.azure-client.redirect-uri:}")
    String redirectUri;
    @Value("${spring.security.oauth2.client.registration.azure-client.ignore-uri:}")
    String ignoreUri;
    @Value("${spring.security.oauth2.client.registration.azure-client.scope:}")
    String scope;

    Map<String, Boolean> urlMapping = null;

    /**
     * check the uri is need verify auth
     *
     * @param requestUrl
     * @return
     */
    public boolean isIgnore(String requestUrl) {
        if (requestUrl == null) {
            return false;
        }
        if (urlMapping == null) {
            urlMapping = new HashMap<>();
            String[] ignoreUrls = ignoreUri.split(",");
            for (String tempUrl : ignoreUrls) {
                urlMapping.put(tempUrl, true);
            }
        }
        if (urlMapping.get(requestUrl) == null) {
            return false;
        }
        return true;
    }

    /**
     * check the token is gengrate by this client
     *
     * @param token
     * @return
     */
    public boolean verifyToken(String token) {
        JSONObject userInfo = decodeAccessToken(token);
        if (clientId != null && userInfo != null && clientId.equals(userInfo.getString("appid"))) {
            return true;
        }

        return false;
    }

    /**
     * decode accesstoken
     *
     * @param accessToken
     * @return
     */
    public JSONObject decodeAccessToken(String accessToken) {
        JSONObject userInfo = null;
        try {
            String[] pieces = accessToken.split("\\.");
            String b64payload = pieces[1];
            String jsonString = new String(Base64.decodeBase64(b64payload), FileUtil.UTF_8);
            userInfo = JSONObject.parseObject(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userInfo;
    }

    public String getLoginUserName(String accessToken) {
        String username = "";
        JSONObject userInfo = decodeAccessToken(accessToken);
        if (userInfo != null) {
            username = userInfo.getString("unique_name");
        }
        return username;
    }

    public String getLoginUserDisplayName(String accessToken) {
        String name = "";
        JSONObject userInfo = decodeAccessToken(accessToken);
        if (userInfo != null) {
            name = userInfo.getString("name");
        }
        return name;
    }

    /**
     * generate the oauth2 login url
     *
     * @return
     */
    public String getLoginUrl() {
        String loginUrl = authorizationUri + "?client_id=" + clientId + "&response_type=code&redirect_uri=" + redirectUri + "&response_mode=query&scope=" + scope;
        return loginUrl;
    }

    public String getLoginUrl(String originUrl, String queryString) {
        if (originUrl == null) {
            return getLoginUrl();
        }
        String url = originUrl;
        if (queryString != null) {
            url = url + "?" + queryString;
        }
        try {
            url = URLEncoder.encode(url, FileUtil.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String loginUrl =
                authorizationUri + "?client_id=" + clientId + "&response_type=code&redirect_uri=" + redirectUri + "&response_mode=query&scope=" + scope + "&state=" + url;
        return loginUrl;
    }

    /**
     * get accessToken by authcode
     *
     * @param code
     * @return
     */
    public String verifyCode(String code) {
        String accessToken = null;
        try {
            RestTemplate restTemplateHttps = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");

            LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("grant_type", "authorization_code");
            body.add("client_secret", clientSecret);
            HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JSONObject> json = restTemplateHttps.exchange(tokenUrl, HttpMethod.POST, entity, JSONObject.class);
            accessToken = json.getBody().getString("access_token");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    /**
     * get accessToken by authcode
     *
     * @param accessToken
     * @return
     */
    public InputStream requestPhoto(String accessToken) throws Exception {

        RestTemplate restTemplateHttps = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);

        ResponseEntity<Resource> result = restTemplateHttps.exchange(photoUrl, HttpMethod.GET, entity, Resource.class);
        Resource body = result.getBody();
        return body.getInputStream();
    }
}
