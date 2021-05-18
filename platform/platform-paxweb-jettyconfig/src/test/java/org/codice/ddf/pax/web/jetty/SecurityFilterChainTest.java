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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the proxy filter chain class. */
public class SecurityFilterChainTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilterChainTest.class);

  /**
   * Tests that all of the filters are properly called.
   *
   * @throws ServletException
   * @throws IOException
   */
  @Test
  public void testDoFilter() throws IOException, ServletException {
    SecurityFilterChain proxyChain = new SecurityFilterChain();
    SecurityFilter filter1 = createMockSecurityFilter("filter1");
    SecurityFilter filter2 = createMockSecurityFilter("filter2");
    SecurityFilter filter3 = createMockSecurityFilter("filter3");

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    proxyChain.addSecurityFilter(filter1);
    proxyChain.addSecurityFilter(filter2);
    proxyChain.addSecurityFilter(filter3);

    proxyChain.doFilter(request, response);

    // Verify that all of the filters were called once.
    verify(filter1).doFilter(request, response, proxyChain);
    verify(filter2).doFilter(request, response, proxyChain);
    verify(filter3).doFilter(request, response, proxyChain);
  }

  /**
   * Tests that an exception is thrown if a new filter is attempted to be added after the filter has
   * been run.
   *
   * @throws IOException
   * @throws ServletException
   */
  @Test(expected = IllegalStateException.class)
  public void testAddFilterAfterDo() throws IOException, ServletException {
    SecurityFilterChain proxyChain = new SecurityFilterChain();
    SecurityFilter filter1 = mock(SecurityFilter.class);
    proxyChain.doFilter(mock(HttpServletRequest.class), mock(HttpServletResponse.class));
    proxyChain.addSecurityFilter(filter1);
  }

  /**
   * Tests that an exception is thrown if more filters are attempted to be added after the filter
   * has been run.
   *
   * @throws IOException
   * @throws ServletException
   */
  @Test(expected = IllegalStateException.class)
  public void testAddFiltersAfterDo() throws IOException, ServletException {
    SecurityFilterChain proxyChain = new SecurityFilterChain();
    SecurityFilter filter2 = mock(SecurityFilter.class);
    SecurityFilter filter3 = mock(SecurityFilter.class);
    proxyChain.doFilter(mock(HttpServletRequest.class), mock(HttpServletResponse.class));
    proxyChain.addSecurityFilter(filter2);
    proxyChain.addSecurityFilter(filter3);
  }

  private SecurityFilter createMockSecurityFilter(final String name)
      throws IOException, ServletException {
    SecurityFilter mockFilter = mock(SecurityFilter.class);
    Mockito.when(mockFilter.toString()).thenReturn(name);
    Mockito.doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              LOGGER.debug("{} was called.", name);
              ((org.codice.ddf.platform.filter.SecurityFilterChain) args[2])
                  .doFilter(((HttpServletRequest) args[0]), ((HttpServletResponse) args[1]));
              return null;
            })
        .when(mockFilter)
        .doFilter(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(org.codice.ddf.platform.filter.SecurityFilterChain.class));

    return mockFilter;
  }
}
