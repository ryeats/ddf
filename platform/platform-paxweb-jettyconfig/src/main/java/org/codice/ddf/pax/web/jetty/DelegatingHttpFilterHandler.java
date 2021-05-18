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

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.util.SortedServiceList;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code DelegatingHttpFilterHandler} provides a way to create global filters which will apply
 * to all requests. It finds any registered {@link HttpFilter} services and passes incoming requests
 * to them in order of service ranking.
 *
 * <p>As of OSGi R6, there is a proper way to define global servlets/filters/listeners/etc., defined
 * by the HTTP Whiteboard spec. However, pax-web does not yet implement that feature, so we're left
 * using this workaround.
 *
 * <p>When https://ops4j1.jira.com/browse/PAXWEB-1123 is resolved, this workaround should be
 * revisited.
 */
public class DelegatingHttpFilterHandler extends HandlerWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingHttpFilterHandler.class);

  private static final String FILTER = "(objectclass=" + HttpFilter.class.getName() + ")";

  private final HttpFilterServiceListener listener = new HttpFilterServiceListener();

  private final SortedServiceList<HttpFilter> httpFilters;

  private final BundleContext context;

  private CopyOnWriteArraySet<String> keysOfInitializedSecurityFilters;

  private static BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(DelegatingHttpFilterHandler.class);
    Objects.requireNonNull(bundle, "Bundle cannot be null");
    return bundle.getBundleContext();
  }

  public DelegatingHttpFilterHandler() throws InvalidSyntaxException {
    this(getContext());
  }

  public DelegatingHttpFilterHandler(BundleContext context) throws InvalidSyntaxException {
    Objects.requireNonNull(context, "Bundle context cannot be null");
    keysOfInitializedSecurityFilters = new CopyOnWriteArraySet<>();
    this.context = context;
    this.context.addServiceListener(listener, FILTER);
    this.httpFilters =
        new SortedServiceList<HttpFilter>() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };

    /*
     * The service listener won't pick up services that are already registered. Must manually
     * add them to the service list.
     */
    Collection<ServiceReference<HttpFilter>> serviceReferences =
        this.context.getServiceReferences(HttpFilter.class, FILTER);
    for (ServiceReference<HttpFilter> reference : serviceReferences) {
      this.listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
    }
  }

  @Override
  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    LOGGER.trace("Delegating to {} HttpFilters.", httpFilters.size());

    Handler handler = this.getHandler();
    if (handler != null) {
      if (!this.checkSecurity(baseRequest)) {
        handler.handle(target, baseRequest, request, response);
      } else {

        TreeSet<ServiceReference<SecurityFilter>> sortedSecurityFilterServiceReferences = null;

        if (context == null) {
          LOGGER.debug(
              "Unable to get BundleContext. No servlet SecurityFilters can be applied. Blocking the request processing.");
          return;
        }

        try {
          try {
            sortedSecurityFilterServiceReferences =
                new TreeSet<>(context.getServiceReferences(SecurityFilter.class, null));
          } catch (InvalidSyntaxException ise) {
            LOGGER.debug("Should never get this exception as there is no filter being passed.");
          }
          final SecurityFilterChain chain = new SecurityFilterChain();
          chain.addSecurityFilter(new ProxyFilters(target, baseRequest));
          if (!CollectionUtils.isEmpty(sortedSecurityFilterServiceReferences)) {
            LOGGER.debug(
                "Found {} filter(s), now filtering...",
                sortedSecurityFilterServiceReferences.size());

            // Insert the SecurityFilters into the chain one at a time (from lowest service ranking
            // to highest service ranking). The SecurityFilter with the highest service-ranking will
            // end up at index 0 in the FilterChain, which means that the SecurityFilters will be
            // run in order of highest to lowest service ranking.
            for (ServiceReference<SecurityFilter> securityFilterServiceReference :
                sortedSecurityFilterServiceReferences) {
              final SecurityFilter securityFilter =
                  context.getService(securityFilterServiceReference);

              if (!hasBeenInitialized(securityFilterServiceReference, context)) {
                initializeSecurityFilter(context, securityFilterServiceReference, securityFilter);
              }
              chain.addSecurityFilter(securityFilter);
            }
          }
          chain.doFilter(request, response);
        } catch (Exception e) {
          response.sendError(500, e.getMessage());
        }
      }
    }
  }

  private class ProxyFilters implements SecurityFilter {

    private final String target;
    private final Request baseRequest;

    public ProxyFilters(String target, Request baseRequest) {
      this.target = target;
      this.baseRequest = baseRequest;
    }

    @Override
    public void init() {}

    @Override
    public void doFilter(
        HttpServletRequest request,
        HttpServletResponse response,
        org.codice.ddf.platform.filter.SecurityFilterChain var3)
        throws IOException, ServletException {
      ProxyHttpFilterChain filterChain =
          new ProxyHttpFilterChain(httpFilters, getHandler(), target, baseRequest);
      filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
  }

  /**
   * This logic to get the filter name from a {@link ServiceReference<Filter>} is copied from {@link
   * org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker#createWebElement(ServiceReference,
   * javax.servlet.Servlet)}. See the pax-web Whiteboard documentation and {@link
   * org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_FILTER_NAME} for how
   * to configure {@link Filter} services with a filter name.
   */
  @Nonnull
  private static String getFilterName(
      ServiceReference<SecurityFilter> securityFilterServiceReference,
      BundleContext bundleContext) {
    final String HTTP_WHITEBOARD_FILTER_NAME = "osgi.http.whiteboard.filter.name";
    final String filterNameFromTheServiceProperty =
        getStringProperty(securityFilterServiceReference, HTTP_WHITEBOARD_FILTER_NAME);
    // If this service property is not specified, the fully qualified name of the service object's
    // class is used as the servlet filter name.
    if (StringUtils.isBlank(filterNameFromTheServiceProperty)) {
      return bundleContext.getService(securityFilterServiceReference).getClass().getCanonicalName();
    } else {
      return filterNameFromTheServiceProperty;
    }
  }

  private boolean hasBeenInitialized(
      final ServiceReference<SecurityFilter> securityFilterServiceReference,
      final BundleContext bundleContext) {
    return keysOfInitializedSecurityFilters.contains(
        getFilterKey(securityFilterServiceReference, bundleContext));
  }

  @Nonnull
  private static String getFilterKey(
      final ServiceReference<SecurityFilter> securityFilterServiceReference,
      final BundleContext bundleContext) {
    return getFilterName(securityFilterServiceReference, bundleContext);
  }

  private void initializeSecurityFilter(
      BundleContext bundleContext,
      ServiceReference<SecurityFilter> securityFilterServiceReference,
      SecurityFilter securityFilter) {
    final String filterName = getFilterName(securityFilterServiceReference, bundleContext);

    securityFilter.init();
    keysOfInitializedSecurityFilters.add(
        getFilterKey(securityFilterServiceReference, bundleContext));
    LOGGER.debug("Initialized SecurityFilter {}", filterName);
  }

  private static String getStringProperty(ServiceReference<?> serviceReference, String key) {
    Object value = serviceReference.getProperty(key);
    if (value != null && !(value instanceof String)) {
      LOGGER.warn("Service property [key={}] value must be a String", key);
      return null;
    } else {
      return (String) value;
    }
  }

  protected boolean checkSecurity(Request request) {
    switch (request.getDispatcherType()) {
      case REQUEST:
      case ASYNC:
        return true;
      case FORWARD:
        return false;
      default:
        return false;
    }
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
    LOGGER.debug("Started {}", DelegatingHttpFilterHandler.class.getSimpleName());
  }

  @Override
  protected void doStop() throws Exception {
    super.doStop();
    LOGGER.debug("Stopped {}", DelegatingHttpFilterHandler.class.getSimpleName());
  }

  private class HttpFilterServiceListener implements ServiceListener {

    @Override
    public void serviceChanged(ServiceEvent event) {
      ServiceReference<?> reference = event.getServiceReference();
      switch (event.getType()) {
        case ServiceEvent.REGISTERED:
          httpFilters.bindPlugin(reference);
          break;
        case ServiceEvent.UNREGISTERING:
          httpFilters.unbindPlugin(reference);
          break;
        default:
          /* only care when services are added or removed */
          break;
      }
    }
  }
}
