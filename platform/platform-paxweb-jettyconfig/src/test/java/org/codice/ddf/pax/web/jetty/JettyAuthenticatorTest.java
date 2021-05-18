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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.ServerAuthException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class JettyAuthenticatorTest {

  private JettyAuthenticator jettyAuthenticator;

  @Mock private BundleContext bundleContext;

  private Set<ServiceReference<SecurityFilter>> registeredSecurityFilterServiceReferences;

  @Before
  public void setup() throws InvalidSyntaxException {
    registeredSecurityFilterServiceReferences = new HashSet<>();
    bundleContext = mock(BundleContext.class);

    jettyAuthenticator =
        new JettyAuthenticator() {
          @Override
          protected BundleContext getContext() {
            return bundleContext;
          }
        };
  }

  /**
   * The {@link javax.servlet.Filter#init(FilterConfig)} javadoc does not specify that the {@link
   * FilterConfig} argument may not be null. This test confirms that there are no errors when
   * initializing the {@link SecurityFilter}s and filtering if this situation occurs.
   */
  @Test
  public void testDoFilterAfterInitDelegateServletFilterWithNullFilterConfig()
      throws ServerAuthException {
    // given
    jettyAuthenticator.setConfiguration(null);
    final ServletRequest servletRequest = mock(HttpServletRequest.class);
    final ServletResponse servletResponse = mock(HttpServletResponse.class);

    // when
    jettyAuthenticator.validateRequest(servletRequest, servletResponse, false);

    // then
  }

  @Test
  public void testSetConfiguration() {
    // given
    final ConstraintSecurityHandler constraintSecurityHandler =
        mock(ConstraintSecurityHandler.class);
    // when
    jettyAuthenticator.setConfiguration(constraintSecurityHandler);
    // then
    verify(constraintSecurityHandler).setLoginService(jettyAuthenticator.getLoginService());
    verify(constraintSecurityHandler)
        .setIdentityService(jettyAuthenticator.getLoginService().getIdentityService());
  }
}
