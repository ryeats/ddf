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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.platform.filter.SecurityFilterChain;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.mock.MockServiceReference;

public class DelegatingHttpFilterHandlerTest {

  @Mock private BundleContext bundleContext;
  private Set<ServiceReference<SecurityFilter>> registeredSecurityFilterServiceReferences;

  @Before
  public void setup() throws InvalidSyntaxException {
    registeredSecurityFilterServiceReferences = new HashSet<>();
    bundleContext = mock(BundleContext.class);
    when(bundleContext.getServiceReferences(SecurityFilter.class, null))
        .thenReturn(registeredSecurityFilterServiceReferences);
  }

  @Test
  public void testDoFilterWithSecurityFilter()
      throws IOException, InvalidSyntaxException, ServletException {
    // given
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    final SecurityFilter securityFilter = registerSecurityFilter(new Hashtable());
    final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

    // when
    Handler handler = mock(Handler.class);

    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);

    underTest.handle("/", mockBaseRequest, servletRequest, servletResponse);

    // then
    final InOrder inOrder = Mockito.inOrder(securityFilter);
    inOrder
        .verify(securityFilter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
  }

  @Test
  public void testSecurityFiltersOnlyInitializedOnce()
      throws IOException, ServletException, InvalidSyntaxException {
    // given / when
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    final Dictionary dictionary1 = new Hashtable();
    dictionary1.put("osgi.http.whiteboard.filter.name", "filter1");
    final SecurityFilter securityFilter1 = registerSecurityFilter(dictionary1);
    Handler handler = mock(Handler.class);
    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);

    final Dictionary dictionary2 = new Hashtable();
    dictionary2.put("osgi.http.whiteboard.filter.name", "filter2");
    final SecurityFilter securityFilter2 = registerSecurityFilter(dictionary2);
    underTest.handle(
        "/", mockBaseRequest, mock(HttpServletRequest.class), mock(HttpServletResponse.class));
    underTest.handle(
        "/", mockBaseRequest, mock(HttpServletRequest.class), mock(HttpServletResponse.class));
    underTest.handle(
        "/", mockBaseRequest, mock(HttpServletRequest.class), mock(HttpServletResponse.class));
    final Dictionary dictionary3 = new Hashtable();
    dictionary3.put("osgi.http.whiteboard.filter.name", "filter3");
    final SecurityFilter securityFilter3 = registerSecurityFilter(dictionary3);
    underTest.handle(
        "/", mockBaseRequest, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

    // then
    verify(securityFilter1).init();
    verify(securityFilter2).init();
    verify(securityFilter3).init();
  }

  @Test
  public void testInitializeSecurityFilter()
      throws IOException, ServletException, InvalidSyntaxException {
    // given
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    final HttpSession httpSession = mock(HttpSession.class);
    final Request servletRequest = mock(Request.class);
    final ServletContext servletContext = mock(ServletContext.class);
    final Dictionary dictionary = new Hashtable();
    final String filterNameValue = "my-test-filter";
    dictionary.put("osgi.http.whiteboard.filter.name", filterNameValue);
    final String param1Key = "param1Key";
    final String param1Value = "param1Value";
    dictionary.put("init." + param1Key, param1Value);
    final String param2Key = "param2Key";
    final String param2Value = "param2Value";
    dictionary.put("init." + param2Key, param2Value);
    final SecurityFilter securityFilter = registerSecurityFilter(dictionary);

    // when
    Handler handler = mock(Handler.class);
    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);
    underTest.handle("/", mockBaseRequest, servletRequest, mock(HttpServletResponse.class));

    // then
    verify(securityFilter).init();
  }

  /**
   * This method tests some edge cases of adding {@link SecurityFilter}s through {@link
   * ServiceReference}s. Filter names must have service-property key="{@link
   * org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_FILTER_NAME}", so the
   * service-property with key="filter-name" will not be used to init the {@link
   * javax.servlet.Filter}. The init param service-property prefix may be defined using the {@link
   * org.ops4j.pax.web.extender.whiteboard.ExtenderConstants#PROPERTY_INIT_PREFIX} service-property
   * key.
   */
  @Test
  public void testInitializeSecurityFilterWithComplicatedInitParams()
      throws IOException, ServletException, InvalidSyntaxException {
    // given
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    final HttpSession httpSession = mock(HttpSession.class);
    final Request servletRequest = mock(Request.class);
    final ServletContext servletContext = mock(ServletContext.class);

    final Dictionary dictionary = new Hashtable();
    dictionary.put("filter-name", "my-test-filter");
    final String incorrectlyFormattedInitParamKey = "incorrectlyFormattedInitParamKey";
    dictionary.put(
        "init." + incorrectlyFormattedInitParamKey, "incorrectlyFormattedInitParamValue");
    final String customInitPrefix = "myInitPrefix.";
    dictionary.put("init-prefix", customInitPrefix);
    final String initParamKey = "initParamKey";
    final String initParamValue = "initParamValue";
    dictionary.put(customInitPrefix + initParamKey, initParamValue);
    final String servletInitParamKey = "servletInitParamKey";
    final String servletInitParamValue = "servletInitParamValue";
    dictionary.put("servlet.init." + servletInitParamKey, servletInitParamValue);
    final String anotherServiceProperty = "anotherServiceProperty";
    dictionary.put(anotherServiceProperty, "anotherServicePropertyValue");
    final SecurityFilter securityFilter = registerSecurityFilter(dictionary);

    // when
    Handler handler = mock(Handler.class);
    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);
    underTest.handle("/", mockBaseRequest, servletRequest, mock(HttpServletResponse.class));

    // then
    verify(securityFilter).init();
  }

  /**
   * The {@link javax.servlet.Filter#init(FilterConfig)} javadoc does not specify that the {@link
   * FilterConfig} argument may not be null. This test confirms that there are no errors when
   * initializing the {@link SecurityFilter}s and filtering if this situation occurs.
   */
  @Test
  public void testDoFilterAfterInitDelegateServletFilterWithNullFilterConfig()
      throws IOException, ServletException, InvalidSyntaxException {
    // given
    //    jettyAuthenticator.setConfiguration(null);
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    final SecurityFilter securityFilter = registerSecurityFilter(new Hashtable());
    final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

    // when
    Handler handler = mock(Handler.class);
    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);
    underTest.handle("/", mockBaseRequest, servletRequest, servletResponse);

    // then
    final InOrder inOrder = Mockito.inOrder(securityFilter);
    inOrder.verify(securityFilter).init();
    inOrder
        .verify(securityFilter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
  }

  private HttpFilter mockHttpFilter() throws Exception {
    HttpFilter mockFilter = mock(HttpFilter.class);
    doAnswer(
            invocationOnMock -> {
              HttpServletRequest request = invocationOnMock.getArgument(0);
              HttpServletResponse response = invocationOnMock.getArgument(1);
              HttpFilterChain filterChain = invocationOnMock.getArgument(2);
              filterChain.doFilter(request, response);
              return null;
            })
        .when(mockFilter)
        .doFilter(any(), any(), any());
    return mockFilter;
  }

  @Test
  public void testDoFilterWithSecurityFiltersInCorrectOrder()
      throws IOException, ServletException, InvalidSyntaxException {
    // given
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    final Dictionary dictionary0 = new Hashtable();
    dictionary0.put(Constants.SERVICE_RANKING, 0);
    final SecurityFilter securityFilter0 = registerSecurityFilter(dictionary0);
    final Dictionary dictionary100 = new Hashtable();
    dictionary100.put(Constants.SERVICE_RANKING, 100);
    final SecurityFilter securityFilter100 = registerSecurityFilter(dictionary100);
    final Dictionary dictionary1 = new Hashtable();
    dictionary1.put(Constants.SERVICE_RANKING, 1);
    final SecurityFilter securityFilter1 = registerSecurityFilter(dictionary1);
    final SecurityFilter securityFilter = registerSecurityFilter(new Hashtable());
    final Dictionary dictionary99 = new Hashtable();
    dictionary99.put(Constants.SERVICE_RANKING, 99);
    final SecurityFilter securityFilter99 = registerSecurityFilter(dictionary99);
    final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

    // when
    Handler handler = mock(Handler.class);
    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);
    underTest.handle("/", mockBaseRequest, servletRequest, servletResponse);

    // then
    final InOrder inOrder =
        Mockito.inOrder(
            securityFilter0, securityFilter100, securityFilter1, securityFilter, securityFilter99);
    inOrder
        .verify(securityFilter100)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
    inOrder
        .verify(securityFilter99)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
    inOrder
        .verify(securityFilter1)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
    inOrder
        .verify(securityFilter0)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
    inOrder
        .verify(securityFilter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(SecurityFilterChain.class));
  }

  /**
   * Rank services in the order they are passed in: First reference = highest rank Last reference =
   * lowest rank
   *
   * @param references
   */
  private void rankServiceReferences(ServiceReference<?>... references) {
    for (int i = 0; i < references.length; i++) {
      for (int j = i + 1; j < references.length; j++) {
        when(references[i].compareTo(references[j])).thenReturn(1);
        when(references[j].compareTo(references[i])).thenReturn(-1);
      }
    }
  }

  @Test
  public void testDelegatingHttpFilterHandler() throws Exception {
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    HttpFilter mockFilter = mockHttpFilter();
    ServiceReference<HttpFilter> mockServiceReference = mock(ServiceReference.class);

    when(bundleContext.getServiceReferences(any(Class.class), anyString()))
        .thenReturn(Collections.singletonList(mockServiceReference));
    when(bundleContext.getService(mockServiceReference)).thenReturn(mockFilter);

    Handler handler = mock(Handler.class);

    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);

    underTest.handle("/", mockBaseRequest, mockRequest, mockResponse);

    verify(mockFilter).doFilter(any(), any(), any());
    verify(handler).handle("/", mockBaseRequest, mockRequest, mockResponse);
  }

  @Test
  public void testDelegatingHttpFilterHandlerWithServiceRanking() throws Exception {
    Request mockBaseRequest = mock(Request.class);
    when(mockBaseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    HttpFilter filter1 = mockHttpFilter();
    HttpFilter filter2 = mockHttpFilter();
    HttpFilter filter3 = mockHttpFilter();

    ServiceReference<HttpFilter> reference1 = mock(ServiceReference.class);
    ServiceReference<HttpFilter> reference2 = mock(ServiceReference.class);
    ServiceReference<HttpFilter> reference3 = mock(ServiceReference.class);
    rankServiceReferences(reference1, reference2, reference3);

    when(bundleContext.getService(reference1)).thenReturn(filter1);
    when(bundleContext.getService(reference2)).thenReturn(filter2);
    when(bundleContext.getService(reference3)).thenReturn(filter3);
    when(bundleContext.getServiceReferences(any(Class.class), anyString()))
        .thenReturn(Arrays.asList(reference2, reference3, reference1));

    Handler handler = mock(Handler.class);

    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(bundleContext);
    underTest.setHandler(handler);

    underTest.handle("/", mockBaseRequest, mockRequest, mockResponse);

    InOrder inOrder = Mockito.inOrder(filter1, filter2, filter3);
    inOrder.verify(filter1).doFilter(any(), any(), any());
    inOrder.verify(filter2).doFilter(any(), any(), any());
    inOrder.verify(filter3).doFilter(any(), any(), any());
    inOrder.verifyNoMoreInteractions();
  }

  private SecurityFilter registerSecurityFilter(Dictionary serviceProperties)
      throws IOException, ServletException {
    final SecurityFilter securityFilter = mock(SecurityFilter.class);
    Mockito.doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              ((org.codice.ddf.platform.filter.SecurityFilterChain) args[2])
                  .doFilter(((HttpServletRequest) args[0]), ((HttpServletResponse) args[1]));
              return null;
            })
        .when(securityFilter)
        .doFilter(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(SecurityFilterChain.class));

    final MockServiceReference securityFilterServiceReference = new MockServiceReference();
    securityFilterServiceReference.setProperties(serviceProperties);
    when(bundleContext.getService(securityFilterServiceReference)).thenReturn(securityFilter);
    registeredSecurityFilterServiceReferences.add(securityFilterServiceReference);
    return securityFilter;
  }
}
