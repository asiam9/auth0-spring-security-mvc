package com.auth0.spring.security.mvc;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.auth0.web.Auth0User;
import com.auth0.web.SessionUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;

/**
 * Class that verifies the JWT token and when valid, it will set
 * the userdetails in the authentication object
 */
public class Auth0AuthenticationProvider implements AuthenticationProvider,
        InitializingBean {

    private static final AuthenticationException AUTH_ERROR =
            new Auth0TokenException("Authentication Error");

    private JWTVerifier jwtVerifier = null;
    private String clientSecret = null;
    private String clientId = null;
    private String securedRoute = null;
    private final Log logger = LogFactory.getLog(getClass());

    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {

        final String token = ((Auth0JWTToken) authentication).getJwt();
        logger.info("Trying to authenticate with token: " + token);

        try {
            final Auth0JWTToken tokenAuth = ((Auth0JWTToken) authentication);
            final Map<String, Object> decoded = jwtVerifier.verify(token);
            logger.debug("Decoded JWT token" + decoded);
            tokenAuth.setAuthenticated(true);
            // Retrieve our Auth0User object from session
            final ServletRequestAttributes servletReqAttr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            final HttpServletRequest req = servletReqAttr.getRequest();
            final Auth0User auth0User = SessionUtils.getAuth0User(req);
            // precaution, the flow would ordinarily ensure this has been setup
            if (auth0User == null) {
                throw new IllegalStateException("No Auth0User object found in session - unexpected state.");
            }
            tokenAuth.setPrincipal(new Auth0UserDetails(auth0User));
            tokenAuth.setDetails(decoded);
            return authentication;
        } catch (InvalidKeyException e) {
            logger.debug("InvalidKeyException thrown while decoding JWT token "
                    + e.getLocalizedMessage());
            throw AUTH_ERROR;
        } catch (NoSuchAlgorithmException e) {
            logger.debug("NoSuchAlgorithmException thrown while decoding JWT token "
                    + e.getLocalizedMessage());
            throw AUTH_ERROR;
        } catch (IllegalStateException e) {
            logger.debug("IllegalStateException thrown while decoding JWT token "
                    + e.getLocalizedMessage());
            throw AUTH_ERROR;
        } catch (SignatureException e) {
            logger.debug("SignatureException thrown while decoding JWT token "
                    + e.getLocalizedMessage());
            throw AUTH_ERROR;
        } catch (IOException e) {
            logger.debug("IOException thrown while decoding JWT token "
                    + e.getLocalizedMessage());
            throw AUTH_ERROR;
        } catch (JWTVerifyException e) {
            logger.debug("JWTVerifyException thrown while decoding JWT token "
                    + e.getLocalizedMessage());
            throw AUTH_ERROR;
        }
    }

    public boolean supports(Class<?> authentication) {
        return Auth0JWTToken.class.isAssignableFrom(authentication);
    }

    public void afterPropertiesSet() throws Exception {
        if ((clientSecret == null) || (clientId == null)) {
            throw new RuntimeException(
                    "client secret and client id are not set for Auth0AuthenticationProvider");
        }
        if (securedRoute == null) {
            throw new RuntimeException(
                    "You must set which route pattern is used to check for users so that they must be authenticated");
        }
        jwtVerifier = new JWTVerifier(new Base64(true).decodeBase64(clientSecret), clientId);
    }

    public String getSecuredRoute() {
        return securedRoute;
    }

    public void setSecuredRoute(String securedRoute) {
        this.securedRoute = securedRoute;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
