/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.pki;

import ddf.security.audit.SecurityLogger;
import java.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.authc.AuthenticationToken;
import org.codice.ddf.platform.filter.SecurityFilterChain;
import org.codice.ddf.security.OcspService;
import org.codice.ddf.security.handler.AuthenticationTokenFactory;
import org.codice.ddf.security.handler.HandlerResultImpl;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PKIHandler implements AuthenticationHandler {

  private static final String AUTH_TYPE = "PKI";

  private static final String SOURCE = "PKIHandler";

  protected static final Logger LOGGER = LoggerFactory.getLogger(PKIHandler.class);

  protected AuthenticationTokenFactory tokenFactory;

  protected CrlChecker crlChecker;

  protected OcspService ocspService;

  private SecurityLogger securityLogger;

  public PKIHandler() {
    LOGGER.debug("Creating PKI handler.");
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  /**
   * Handler implementing PKI authentication. Returns the {@link
   * org.codice.ddf.security.handler.api.HandlerResult} containing a BinarySecurityToken if the
   * operation was successful.
   *
   * @param httpRequest http request to obtain attributes from and to pass into any local filter
   *     chains required
   * @param httpResponse http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return result of handling this request - status and optional tokens
   */
  @Override
  public HandlerResult getNormalizedToken(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      SecurityFilterChain chain,
      boolean resolve) {
    HandlerResult handlerResult = new HandlerResultImpl(HandlerResult.Status.NO_ACTION, null);
    handlerResult.setSource(SOURCE);

    String path = httpRequest.getServletPath();
    LOGGER.debug("Doing PKI authentication and authorization for path {}", path);

    // doesn't matter what the resolve flag is set to, we do the same action
    X509Certificate[] certs =
        (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
    AuthenticationToken token = tokenFactory.fromCertificates(certs, httpRequest.getRemoteAddr());

    // The httpResponse was null, return no action and try to process with other handlers
    if (httpResponse == null && resolve) {
      LOGGER.debug("HTTP Response was null for request {}", path);
      return handlerResult;
    }

    // No auth info was extracted, return NO_ACTION
    if (token == null) {
      return handlerResult;
    }

    // CRL was specified, check against CRL and return the result or throw a ServletException to the
    // WebSSOFilter
    if (crlChecker == null) {
      crlChecker = new CrlChecker(securityLogger);
    }
    if (crlChecker.passesCrlCheck(certs) && ocspService.passesOcspCheck(certs)) {
      handlerResult.setToken(token);
      handlerResult.setStatus(HandlerResult.Status.COMPLETED);
    } else {

      if (httpResponse == null) {
        LOGGER.error(
            "Error returning revoked certificate request because the HTTP response object is invalid.");
      } else {

        try {
          httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Your certificate is revoked.");
          httpResponse.flushBuffer();
          LOGGER.info("The certificate used to complete the request has been revoked.");
        } catch (Exception e) {
          LOGGER.error("Error returning revoked certificate request.");
        }
      }
      handlerResult.setStatus(HandlerResult.Status.REDIRECTED);
    }

    return handlerResult;
  }

  @Override
  public HandlerResult handleError(
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse,
      SecurityFilterChain chain) {
    HandlerResult result = new HandlerResultImpl(HandlerResult.Status.NO_ACTION, null);
    result.setSource(SOURCE);
    LOGGER.debug("In error handler for pki - no action taken.");
    return result;
  }

  public void setTokenFactory(AuthenticationTokenFactory factory) {
    tokenFactory = factory;
  }

  public void setOcspService(OcspService ocspService) {
    this.ocspService = ocspService;
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
