package org.apereo.cas.consent;

import org.apereo.cas.config.CasConsentRedisConfiguration;
import org.apereo.cas.util.junit.EnabledIfListeningOnPort;

import lombok.Getter;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link RedisConsentRepositoryTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@SpringBootTest(classes = {
    CasConsentRedisConfiguration.class,
    BaseConsentRepositoryTests.SharedTestConfiguration.class
},
    properties = {
        "cas.consent.redis.host=localhost",
        "cas.consent.redis.port=6379",
        "cas.consent.redis.pool.max-active=20",
        "cas.consent.redis.pool.enabled=true"
    })
@Tag("Redis")
@Getter
@EnabledIfListeningOnPort(port = 6379)
public class RedisConsentRepositoryTests extends BaseConsentRepositoryTests {

    @Autowired
    @Qualifier(ConsentRepository.BEAN_NAME)
    protected ConsentRepository repository;

    @Test
    public void storeBadDecision() throws Exception {
        val repo = getRepository();
        assertNull(repo.storeConsentDecision(null));
    }
    
    @Test
    public void verifyDeleteFails() {
        val repo = getRepository();
        assertFalse(repo.deleteConsentDecision(-1, UUID.randomUUID().toString()));
    }


}
