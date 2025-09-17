// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.RestTemplateConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
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
import java.net.URL;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthUtil {

    @Value("${spring.security.oauth2.client.provider.azure-ad.token-uri:}")
    String tokenUrl;
    @Value("${spring.security.oauth2.client.provider.azure-ad.photo-uri:}")
    String photoUrl;
    @Value("${spring.security.oauth2.client.provider.azure-ad.authorization-uri:}")
    String authorizationUri;
    @Value("${spring.security.oauth2.client.provider.azure-ad.tenant-id:}")
    String tenantId;
    @Value("${spring.security.oauth2.client.provider.azure-ad.audience:}")
    String audience;
    @Value("${spring.security.oauth2.client.provider.azure-ad.instance-uri:}")
    String instanceUri;
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
    @Value("${spring.security.oauth2.client.provider.azure-ad.mise-enabled:false}")
    boolean miseEnabled;

    Map<String, Boolean> urlMapping = null;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthUtil.class);
    private static Boolean isMiseLogInitialized = false;

    public boolean isValidToken(String token) {
        LOGGER.info("Starting token validation...");
        if (miseEnabled) {
            return validateTokenWithMISE(token);
        }
        return validateTokenWithPublicKey(token) && validateAudienceAndExpiredTime(token);
    }

    private boolean validateAudienceAndExpiredTime(String token) {
        try {
            JSONObject tokenInfo = decodeAccessToken(token);
            if (tokenInfo == null) {
                throw new IllegalArgumentException("Invalid token: unable to decode access token");
            }
            String aud = tokenInfo.getString("aud");
            String[] audiences = this.audience.split(",");
            if (aud == null || aud.isEmpty() || !Arrays.stream(audiences).anyMatch(a -> a.equals(aud))) {
                throw new IllegalArgumentException("Invalid token: audience does not match client ID");
            }
            // Additional checks for expiration can be added here
            Long exp = tokenInfo.getLong("exp");
            if (exp == null || exp < System.currentTimeMillis() / 1000) {
                throw new IllegalArgumentException("Invalid token: token has expired");
            }
            LOGGER.info("Token validation passed: audience and expiration time are valid");
            return true;
        } catch (Exception e) {
            LOGGER.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean validateTokenWithPublicKey(String token) {
        try {
            URL jwkSetURL = new URL("https://login.microsoftonline.com/" + tenantId + "/discovery/keys?appid=" + clientId);

            JWKSet jwkSet = JWKSet.load(jwkSetURL);

            JWSObject jwsObject = JWSObject.parse(token);

            PublicKey publicKey = getPublicKey(jwsObject, jwkSet);
            RSASSAVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);

            boolean isVerified = jwsObject.verify(verifier);
            return isVerified;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean validateTokenWithMISE(String token) {
        LOGGER.info("Starting MISE token validation...");

        try {
            // Mise mise = Mise.createClient();
            Class<?> miseClass = Class.forName("com.microsoft.identity.service.essentials.Mise");
            Object mise = miseClass.getMethod("createClient").invoke(null);

            if (!isMiseLogInitialized) {
                LOGGER.info("Initializing MISE...");
                // mise.assignLogMessageCallback(new Mise.ILogCallback() {...}, null);
                Class<?> iLogCallbackClass = Class.forName("com.microsoft.identity.service.essentials.Mise$ILogCallback");

                Object logCallback = java.lang.reflect.Proxy.newProxyInstance(
                        iLogCallbackClass.getClassLoader(),
                        new Class<?>[]{iLogCallbackClass},
                        (proxy, method, args) -> {
                            String methodName = method.getName();
                            if ("callback".equals(methodName)) {
                                Object level = args[0];
                                String message = (String) args[1];
                                // Print all log levels for simplicity
                                LOGGER.info(message);
                            }
                            return null;
                        }
                );

                miseClass.getMethod("assignLogMessageCallback", iLogCallbackClass, Object.class)
                        .invoke(mise, logCallback, null);
                isMiseLogInitialized = true;
            }

            // Configure MISE
            JSONObject config = new JSONObject();
            JSONObject azureAd = new JSONObject();
            azureAd.put("Instance", instanceUri);
            azureAd.put("ClientId", clientId);
            azureAd.put("TenantId", tenantId);
            String[] audiences = audience.split(",");
            azureAd.put("Audiences", audiences);
            JSONObject logging = new JSONObject();
            logging.put("logLevel", "Debug");
            azureAd.put("Logging", logging);
            config.put("AzureAd", azureAd);

            miseClass.getMethod("configure", String.class, String.class)
                    .invoke(mise, config.toString(), null);

            // MiseValidationInput miseValidationInput = new MiseValidationInput();
            Class<?> miseValidationInputClass = Class.forName("com.microsoft.identity.service.essentials.MiseValidationInput");
            Object miseValidationInput = miseValidationInputClass.getDeclaredConstructor().newInstance();

            miseValidationInputClass.getField("authorizationHeader").set(miseValidationInput, "Bearer " + token);
            miseValidationInputClass.getField("originalMethodHeader").set(miseValidationInput, "GET");
            miseValidationInputClass.getField("originalUriHeader").set(miseValidationInput, "https://myapi.com/api/values");

            // try (MiseValidationResult validationResult = mise.validate(miseValidationInput)) { ... }
            Object validationResult = miseClass.getMethod("validate", miseValidationInputClass)
                    .invoke(mise, miseValidationInput);

            Class<?> miseValidationResultClass = Class.forName("com.microsoft.identity.service.essentials.MiseValidationResult");
            int statusCode = (int) miseValidationResultClass.getMethod("getHttpResponseStatusCode").invoke(validationResult);
            LOGGER.info("Status code " + statusCode);

            String errorDescription = (String) miseValidationResultClass.getMethod("getErrorDescription").invoke(validationResult);
            if (errorDescription != null) {
                LOGGER.error("Error message " + errorDescription);
            }

            // Close validationResult if AutoCloseable
            if (validationResult instanceof AutoCloseable) {
                ((AutoCloseable) validationResult).close();
            }
            if (statusCode != 200) {
                LOGGER.error("MISE token validation failed with status code: " + statusCode);
                return false;
            }
            LOGGER.info("MISE token validation passed");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private PublicKey getPublicKey(JWSObject jwsObject, JWKSet jwkSet) throws JOSEException {
        JWSAlgorithm algorithm = jwsObject.getHeader().getAlgorithm();
        if (!algorithm.equals(JWSAlgorithm.RS256)) {
            throw new IllegalArgumentException("No RS256");
        }

        RSAKey rsaKey = null;
        for (JWK jwk : jwkSet.getKeys()) {
            if (jwk.getKeyID().equals(jwsObject.getHeader().getKeyID())) {
                rsaKey = (RSAKey) jwk;
                break;
            }
        }
        if (rsaKey == null) {
            throw new IllegalArgumentException("No publicKey");
        }

        PublicKey publicKey = rsaKey.toRSAPublicKey();
        return publicKey;
    }

    public String getAppClientId(String accessToken) {
        String clientId = "";
        JSONObject clientInfo = decodeAccessToken(accessToken);
        if (clientInfo != null) {
            clientId = clientInfo.getString("appid");
        }
        return clientId;
    }

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
        if (clientId != null && userInfo != null && clientId.equals(userInfo.getString("aud"))) {
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
            ParserConfig.getGlobalInstance().setSafeMode(true);
            userInfo = JSONObject.parseObject(jsonString);
            // reset safe mode to avoid the impact on other json parse
            ParserConfig.getGlobalInstance().setSafeMode(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userInfo;
    }

    public String getLoginUserName(String accessToken) {
        String username = "";
        JSONObject userInfo = decodeAccessToken(accessToken);
        if (userInfo != null) {
            username = userInfo.getString("email");
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
        String loginUrl = authorizationUri + "?client_id=" + clientId +
                "&response_type=code+id_token&redirect_uri=" + redirectUri +
                "&response_mode=form_post&nonce=" + UUID.randomUUID() + "&scope=" + scope;
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

    public String getClientId() {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalStateException("Client ID is not configured");
        }
        return clientId;
    }
}
