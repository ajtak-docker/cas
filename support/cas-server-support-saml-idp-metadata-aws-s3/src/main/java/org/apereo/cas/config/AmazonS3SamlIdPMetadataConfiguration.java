package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.saml.idp.metadata.AmazonS3SamlIdPMetadataCipherExecutor;
import org.apereo.cas.support.saml.idp.metadata.AmazonS3SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.AmazonS3SamlIdPMetadataLocator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataLocator;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.util.cipher.CipherExecutorUtils;
import org.apereo.cas.util.crypto.CipherExecutor;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

/**
 * This is {@link AmazonS3SamlIdPMetadataConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnProperty(prefix = "cas.authn.saml-idp.metadata.amazon-s3", name = "idp-metadata-bucket-name")
@Slf4j
@Configuration(value = "amazonS3SamlIdPMetadataConfiguration", proxyBeanMethods = false)
public class AmazonS3SamlIdPMetadataConfiguration {

    @Autowired
    @Qualifier("samlIdPMetadataCache")
    private ObjectProvider<Cache<String, SamlIdPMetadataDocument>> samlIdPMetadataCache;

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "samlIdPMetadataGeneratorCipherExecutor")
    @Autowired
    public CipherExecutor samlIdPMetadataGeneratorCipherExecutor(final CasConfigurationProperties casProperties) {
        val idp = casProperties.getAuthn().getSamlIdp();
        val crypto = idp.getMetadata().getAmazonS3().getCrypto();
        if (crypto.isEnabled()) {
            return CipherExecutorUtils.newStringCipherExecutor(crypto, AmazonS3SamlIdPMetadataCipherExecutor.class);
        }
        LOGGER.info("Amazon S3 SAML IdP metadata encryption/signing is turned off and " + "MAY NOT be safe in a production environment. " +
            "Consider using other choices to handle encryption, signing and verification of " + "metadata artifacts");
        return CipherExecutor.noOp();
    }

    @Bean
    @RefreshScope
    @Autowired
    public SamlIdPMetadataGenerator samlIdPMetadataGenerator(final CasConfigurationProperties casProperties,
                                                             @Qualifier("samlIdPMetadataGeneratorConfigurationContext")
                                                             final SamlIdPMetadataGeneratorConfigurationContext samlIdPMetadataGeneratorConfigurationContext,
                                                             @Qualifier("amazonS3Client")
                                                             final S3Client amazonS3Client) {
        val idp = casProperties.getAuthn().getSamlIdp();
        val generator = new AmazonS3SamlIdPMetadataGenerator(samlIdPMetadataGeneratorConfigurationContext, amazonS3Client, idp.getMetadata().getAmazonS3().getIdpMetadataBucketName());
        generator.generate(Optional.empty());
        return generator;
    }

    @Bean
    @RefreshScope
    @Autowired
    public SamlIdPMetadataLocator samlIdPMetadataLocator(final CasConfigurationProperties casProperties,
                                                         @Qualifier("samlIdPMetadataGeneratorCipherExecutor")
                                                         final CipherExecutor samlIdPMetadataGeneratorCipherExecutor,
                                                         @Qualifier("amazonS3Client")
                                                         final S3Client amazonS3Client) {
        val idp = casProperties.getAuthn().getSamlIdp();
        return new AmazonS3SamlIdPMetadataLocator(samlIdPMetadataGeneratorCipherExecutor, samlIdPMetadataCache.getObject(), idp.getMetadata().getAmazonS3().getIdpMetadataBucketName(),
            amazonS3Client);
    }
}
