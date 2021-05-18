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
package org.codice.ddf.pax.web.jetty;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.common.PrincipalHolder;
import ddf.security.http.SessionFactory;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nullable;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

public class JettyAuthenticator extends LoginAuthenticator {

  public static final String DDF_AUTH_METHOD = "DDF";
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JettyAuthenticator.class);
  private CopyOnWriteArraySet<String> keysOfInitializedSecurityFilters;

  public JettyAuthenticator() {
    super();
    keysOfInitializedSecurityFilters = new CopyOnWriteArraySet<>();
    _loginService = new DummyLoginService();
    _identityService = _loginService.getIdentityService();
  }

  @Override
  public void setConfiguration(AuthConfiguration configuration) {
    keysOfInitializedSecurityFilters.clear();
    if (configuration instanceof ConstraintSecurityHandler) {
      ((ConstraintSecurityHandler) configuration).setLoginService(_loginService);
      ((ConstraintSecurityHandler) configuration).setIdentityService(_identityService);
    }
  }

  @Override
  public String getAuthMethod() {
    return DDF_AUTH_METHOD;
  }

  @Override
  public Authentication validateRequest(
      ServletRequest servletRequest, ServletResponse servletResponse, boolean mandatory)
      throws ServerAuthException {
    Subject subject = (Subject) servletRequest.getAttribute(SecurityConstants.SECURITY_SUBJECT);
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    if (subject != null) {
      if (getContextPolicyManager().getSessionAccess()) {
        addToSession(httpServletRequest, subject);
      }
    } else {
      LOGGER.debug("No java subject found to attach to thread.");
      return Authentication.UNAUTHENTICATED;
    }

    UserIdentity userIdentity = new JettyUserIdentity(getSecuritySubject(subject));
    return new JettyAuthenticatedUser(userIdentity);
  }

  @Nullable
  private javax.security.auth.Subject getSecuritySubject(@Nullable Subject subject) {
    if (subject == null) {
      return null;
    }
    HashSet emptySet = new HashSet();
    HashSet subjectPrincipal = new HashSet();
    subjectPrincipal.add(subject);
    return new javax.security.auth.Subject(true, subjectPrincipal, emptySet, emptySet);
  }

  @Override
  public boolean secureResponse(
      ServletRequest req,
      ServletResponse res,
      boolean mandatory,
      Authentication.User validatedUser) {
    return true;
  }

  protected BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(DelegatingHttpFilterHandler.class);
    Objects.requireNonNull(bundle, "Bundle cannot be null");
    return bundle.getBundleContext();
  }

  private ContextPolicyManager getContextPolicyManager() {
    ServiceReference<ContextPolicyManager> serviceReference =
        getContext().getServiceReference(ContextPolicyManager.class);
    return getContext().getService(serviceReference);
  }

  private SessionFactory getSessionFactory() {
    ServiceReference<SessionFactory> serviceReference =
        getContext().getServiceReference(SessionFactory.class);
    SessionFactory sessionFactory = getContext().getService(serviceReference);
    if (sessionFactory == null) {
      throw new SessionException("Unable to store user's session.");
    }
    return sessionFactory;
  }

  /**
   * Attaches a subject to the HttpSession associated with an HttpRequest. If a session does not
   * already exist, one will be created.
   *
   * @param httpRequest HttpRequest associated with an HttpSession to attach the Subject to
   * @param subject Subject to attach to request
   */
  private void addToSession(HttpServletRequest httpRequest, Subject subject) {
    PrincipalCollection principals = subject.getPrincipals();
    HttpSession session = getSessionFactory().getOrCreateSession(httpRequest);
    PrincipalHolder principalHolder =
        (PrincipalHolder) session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
    PrincipalCollection oldPrincipals = principalHolder.getPrincipals();
    if (!principals.equals(oldPrincipals)) {
      principalHolder.setPrincipals(principals);
    }
  }

  private class DummyLoginService implements org.eclipse.jetty.security.LoginService {

    private final JettyIdentityService jettyIdentityService = new JettyIdentityService();

    @Override
    public String getName() {
      return null;
    }

    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
      return null;
    }

    @Override
    public boolean validate(UserIdentity user) {
      return false;
    }

    @Override
    public IdentityService getIdentityService() {
      return jettyIdentityService;
    }

    @Override
    public void setIdentityService(IdentityService service) {
      // not needed
    }

    @Override
    public void logout(UserIdentity user) {
      // not needed
    }
  }
}
