package org.apereo.cas.web;

import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.authentication.DefaultAuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.DefaultAuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.DefaultServicesManagerRegisteredServiceLocator;
import org.apereo.cas.services.InMemoryServiceRegistry;
import org.apereo.cas.services.RegisteredServiceAccessStrategyAuditableEnforcer;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.services.RegisteredServiceProperty.RegisteredServiceProperties;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.RegisteredServicesTemplatesManager;
import org.apereo.cas.services.ServicesManagerConfigurationContext;
import org.apereo.cas.services.mgmt.DefaultServicesManager;
import org.apereo.cas.services.web.support.RegisteredServiceResponseHeadersEnforcementFilter;
import org.apereo.cas.util.spring.DirectObjectProvider;
import org.apereo.cas.web.support.DefaultArgumentExtractor;
import org.apereo.cas.web.support.filters.ResponseHeadersEnforcementFilter;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link RegisteredServiceResponseHeadersEnforcementFilterTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("RegisteredService")
public class RegisteredServiceResponseHeadersEnforcementFilterTests {

    private static RegisteredServiceResponseHeadersEnforcementFilter getFilterForProperty(final RegisteredServiceProperties p) {
        return getFilterForProperty(Pair.of(p, "true"));
    }

    private static RegisteredServiceResponseHeadersEnforcementFilter getFilterForProperty(final Pair<RegisteredServiceProperties, String>... properties) {
        val appCtx = new StaticApplicationContext();
        appCtx.refresh();

        val context = ServicesManagerConfigurationContext.builder()
            .serviceRegistry(new InMemoryServiceRegistry(appCtx))
            .applicationContext(appCtx)
            .registeredServicesTemplatesManager(mock(RegisteredServicesTemplatesManager.class))
            .environments(new HashSet<>(0))
            .servicesCache(Caffeine.newBuilder().build())
            .registeredServiceLocators(List.of(new DefaultServicesManagerRegisteredServiceLocator()))
            .build();

        val servicesManager = new DefaultServicesManager(context);
        val argumentExtractor = new DefaultArgumentExtractor(new WebApplicationServiceFactory());

        val service = RegisteredServiceTestUtils.getRegisteredService("service-0");
        val props1 = new LinkedHashMap<String, RegisteredServiceProperty>();
        for (val p : properties) {
            val prop1 = new DefaultRegisteredServiceProperty();
            prop1.addValue(p.getValue());
            props1.put(p.getKey().getPropertyName(), prop1);
        }
        service.setProperties(props1);
        servicesManager.save(service);

        return new RegisteredServiceResponseHeadersEnforcementFilter(new DirectObjectProvider<>(servicesManager),
            new DirectObjectProvider<>(argumentExtractor),
            new DirectObjectProvider<>(new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy())),
            new DirectObjectProvider<>(new RegisteredServiceAccessStrategyAuditableEnforcer(appCtx)));
    }

    @Test
    public void verifyCacheControl() throws Exception {
        val filter = getFilterForProperty(RegisteredServiceProperties.HTTP_HEADER_ENABLE_CACHE_CONTROL);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        val servletContext = new MockServletContext();
        val filterConfig = new MockFilterConfig(servletContext);
        filterConfig.addInitParameter(ResponseHeadersEnforcementFilter.INIT_PARAM_CACHE_CONTROL_STATIC_RESOURCES, "css|js|png|txt|jpg|ico|jpeg|bmp|gif");
        filter.init(filterConfig);
        filter.doFilter(request, response, new MockFilterChain());
        assertNotNull(response.getHeader("Cache-Control"));
    }

    @Test
    public void verifyCacheControlDisabled() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_CACHE_CONTROL, "false"));
        filter.setEnableCacheControl(true);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("Cache-Control"));
    }

    @Test
    public void verifyContentSecurityPolicy() throws Exception {
        val filter = getFilterForProperty(RegisteredServiceProperties.HTTP_HEADER_ENABLE_CONTENT_SECURITY_POLICY);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        request.setRequestURI("/cas/login");
        filter.setContentSecurityPolicy("sample-policy");
        filter.doFilter(request, response, new MockFilterChain());
        assertNotNull(response.getHeader("Content-Security-Policy"));
    }

    @Test
    public void verifyContentSecurityPolicyDisabled() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_CONTENT_SECURITY_POLICY, "false"));
        filter.setContentSecurityPolicy(null);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        request.setRequestURI("/cas/login");
        filter.setContentSecurityPolicy("sample-policy");
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("Content-Security-Policy"));
    }

    @Test
    public void verifyStrictTransport() throws Exception {
        val filter = getFilterForProperty(RegisteredServiceProperties.HTTP_HEADER_ENABLE_STRICT_TRANSPORT_SECURITY);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        request.setSecure(true);
        filter.doFilter(request, response, new MockFilterChain());
        filter.doFilter(request, response, new MockFilterChain());
        assertNotNull(response.getHeader("Strict-Transport-Security"));
    }

    @Test
    public void verifyStrictTransportDisabled() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_STRICT_TRANSPORT_SECURITY, "false"));
        filter.setEnableStrictTransportSecurity(true);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        request.setSecure(true);
        filter.doFilter(request, response, new MockFilterChain());
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("Strict-Transport-Security"));
    }

    @Test
    public void verifyXContentOptions() throws Exception {
        val filter = getFilterForProperty(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XCONTENT_OPTIONS);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertNotNull(response.getHeader("X-Content-Type-Options"));
    }

    @Test
    public void verifyXContentOptionsDisabled() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XCONTENT_OPTIONS, "false"));
        filter.setEnableXContentTypeOptions(true);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("X-Content-Type-Options"));
    }

    @Test
    public void verifyOptionForUnknownService() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XCONTENT_OPTIONS, "false"));
        filter.setEnableXContentTypeOptions(true);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "unknown-123456");
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void verifyXframeOptions() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XFRAME_OPTIONS, "true"),
            Pair.of(RegisteredServiceProperties.HTTP_HEADER_XFRAME_OPTIONS, "sameorigin"));

        filter.setXframeOptions("some-other-value");
        filter.setEnableXFrameOptions(true);

        var response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals("sameorigin", response.getHeader("X-Frame-Options"));

        response = new MockHttpServletResponse();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-something-else");
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals("some-other-value", response.getHeader("X-Frame-Options"));
    }

    @Test
    public void verifyXframeOptionsDisabled() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XFRAME_OPTIONS, "false"));

        filter.setXframeOptions("some-other-value");
        filter.setEnableXFrameOptions(true);

        var response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("X-Frame-Options"));

        response = new MockHttpServletResponse();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-something-else");
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals("some-other-value", response.getHeader("X-Frame-Options"));
    }

    @Test
    public void verifyXssProtection() throws Exception {
        val filter = getFilterForProperty(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XSS_PROTECTION);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertNotNull(response.getHeader("X-XSS-Protection"));
    }

    @Test
    public void verifyXssProtectionDisabled() throws Exception {
        val filter = getFilterForProperty(Pair.of(RegisteredServiceProperties.HTTP_HEADER_ENABLE_XSS_PROTECTION, "false"));
        filter.setEnableXSSProtection(true);
        val response = new MockHttpServletResponse();
        val request = new MockHttpServletRequest();
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, "service-0");
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("X-XSS-Protection"));
    }
}
