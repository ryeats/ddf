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
import java.io.IOException;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

public class DDFSessionHandler extends HandlerWrapper {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DDFSessionHandler.class);
  private final BundleContext context;

  private static BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(DelegatingHttpFilterHandler.class);
    Objects.requireNonNull(bundle, "Bundle cannot be null");
    return bundle.getBundleContext();
  }

  private static ContextPolicyManager getContextPolicyManager() {
    ServiceReference<ContextPolicyManager> serviceReference =
        getContext().getServiceReference(ContextPolicyManager.class);
    return getContext().getService(serviceReference);
  }

  private static SessionFactory getSessionFactory() {
    ServiceReference<SessionFactory> serviceReference =
        getContext().getServiceReference(SessionFactory.class);
    SessionFactory sessionFactory = getContext().getService(serviceReference);
    if (sessionFactory == null) {
      throw new SessionException("Unable to store user's session.");
    }
    return sessionFactory;
  }

  public DDFSessionHandler() throws InvalidSyntaxException {
    this(getContext());
  }

  public DDFSessionHandler(BundleContext context) throws InvalidSyntaxException {
    Objects.requireNonNull(context, "Bundle context cannot be null");
    this.context = context;
    ServiceReference<ContextPolicyManager> contextPolicyManagerServiceReference =
        this.context.getServiceReference(ContextPolicyManager.class);
  }

  @Override
  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Subject subject = (Subject) request.getAttribute(SecurityConstants.SECURITY_SUBJECT);

    if (subject != null) {
      if (getContextPolicyManager().getSessionAccess()) {
        addToSession(request, subject);
      }
    } else {
      LOGGER.debug("No java subject found to attach to thread.");
    }
    super.handle(target, baseRequest, request, response);
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
}
