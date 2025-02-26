package org.apereo.cas.services;

import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Misagh Moayyed
 * @since 4.1.0
 */
@Tag("RegisteredService")
@SpringBootTest(classes = {
    CasCoreUtilConfiguration.class,
    RefreshAutoConfiguration.class
})
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class PrincipalAttributeRegisteredServiceUsernameProviderTests {

    private static final File JSON_FILE = new File(FileUtils.getTempDirectoryPath(), "principalAttributeRegisteredServiceUsernameProvider.json");

    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(true).build().toObjectMapper();

    @Test
    public void verifyUsernameByPrincipalAttributeWithMapping() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider("email");

        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        val mappedAttribute = "urn:oid:0.9.2342.19200300.100.1.3";
        allowedAttributes.put("email", List.of(mappedAttribute));
        val policy = new ReturnMappedAttributeReleasePolicy()
            .setAllowedAttributes(CollectionUtils.wrap(allowedAttributes));
        val registeredService = RegisteredServiceTestUtils.getRegisteredService();
        registeredService.setAttributeReleasePolicy(policy);

        val principalAttributes = new HashMap<String, List<Object>>();
        principalAttributes.put("email", List.of("user@example.org"));
        val p = RegisteredServiceTestUtils.getPrincipal("person", principalAttributes);

        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(registeredService)
            .service(RegisteredServiceTestUtils.getService("verifyUsernameByPrincipalAttributeWithMapping"))
            .principal(p)
            .build();
        val id = provider.resolveUsername(usernameContext);
        assertEquals("user@example.org", id);
    }

    @Test
    public void verifyUsernameByPrincipalAttributeAsCollection() {
        val provider =
            new PrincipalAttributeRegisteredServiceUsernameProvider("cn");

        val attrs = new HashMap<String, List<Object>>();
        attrs.put("userid", CollectionUtils.wrap("u1"));
        attrs.put("cn", CollectionUtils.wrap("TheName"));

        val p = RegisteredServiceTestUtils.getPrincipal("person", attrs);

        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService"))
            .service(RegisteredServiceTestUtils.getService("usernameAttributeProviderService"))
            .principal(p)
            .build();
        val id = provider.resolveUsername(usernameContext);
        assertEquals("TheName", id);
    }

    @Test
    public void verifyUsernameByPrincipalAttribute() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");

        val attrs = new HashMap<String, List<Object>>();
        attrs.put("userid", List.of("u1"));
        attrs.put("cn", List.of("TheName"));

        val principal = RegisteredServiceTestUtils.getPrincipal("person", attrs);
        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService"))
            .service(RegisteredServiceTestUtils.getService("usernameAttributeProviderService"))
            .principal(principal)
            .build();
        val id = provider.resolveUsername(usernameContext);
        assertEquals("TheName", id);
    }

    @Test
    public void verifyNoAttrRelPolicy() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");

        val attrs = new HashMap<String, List<Object>>();
        attrs.put("userid", List.of("u1"));
        attrs.put("cn", List.of("TheName"));

        val p = RegisteredServiceTestUtils.getPrincipal("person", attrs);

        val service = RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService");
        service.setAttributeReleasePolicy(null);

        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService"))
            .service(RegisteredServiceTestUtils.getService("usernameAttributeProviderService"))
            .principal(p)
            .build();
        val id = provider.resolveUsername(usernameContext);
        assertEquals("TheName", id);
    }

    @Test
    public void verifyDisabledService() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");

        val attrs = new HashMap<String, List<Object>>();
        attrs.put("userid", List.of("u1"));
        attrs.put("cn", List.of("TheName"));

        val principal = RegisteredServiceTestUtils.getPrincipal("person", attrs);

        val registeredService = RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService");
        registeredService.setAccessStrategy(new DefaultRegisteredServiceAccessStrategy(false, false));
        registeredService.setAttributeReleasePolicy(null);

        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(registeredService)
            .service(RegisteredServiceTestUtils.getService("usernameAttributeProviderService"))
            .principal(principal)
            .build();
        assertThrows(UnauthorizedServiceException.class, () -> provider.resolveUsername(usernameContext));
    }

    @Test
    public void verifyUsernameByPrincipalAttributeNotFound() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");

        val attrs = new HashMap<String, List<Object>>();
        attrs.put("userid", List.of("u1"));

        val p = RegisteredServiceTestUtils.getPrincipal("person", attrs);

        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService"))
            .service(RegisteredServiceTestUtils.getService("usernameAttributeProviderService"))
            .principal(p)
            .build();
        val id = provider.resolveUsername(usernameContext);
        assertEquals(id, p.getId());
    }

    @Test
    public void verifyUsernameUndefined() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider();
        val p = RegisteredServiceTestUtils.getPrincipal("person");
        val usernameContext = RegisteredServiceUsernameProviderContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService("usernameAttributeProviderService"))
            .service(RegisteredServiceTestUtils.getService("usernameAttributeProviderService"))
            .principal(p)
            .build();
        val id = provider.resolveUsername(usernameContext);
        assertEquals(id, p.getId());
    }

    @Test
    public void verifyEquality() {
        val provider = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");
        val provider2 = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");
        assertEquals(provider, provider2);
    }

    @Test
    public void verifySerializeAPrincipalAttributeRegisteredServiceUsernameProviderToJson() throws Exception {
        val providerWritten = new PrincipalAttributeRegisteredServiceUsernameProvider("cn");
        MAPPER.writeValue(JSON_FILE, providerWritten);
        val providerRead = MAPPER.readValue(JSON_FILE, PrincipalAttributeRegisteredServiceUsernameProvider.class);
        assertEquals(providerWritten, providerRead);
    }
}
