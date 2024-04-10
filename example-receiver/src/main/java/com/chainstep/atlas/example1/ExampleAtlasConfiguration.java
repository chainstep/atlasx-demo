package com.chainstep.atlas.example1;

import com.cartrust.atlas.ssikit.AtlasCommunicator;
import com.cartrust.atlas.ssikit.catalogue.AtlasCatalogue;
import com.cartrust.atlas.ssikit.catalogue.AtlasCatalogueConfiguration;
import com.cartrust.atlas.ssikit.catalogue.CataloguePublisher;
import com.cartrust.atlas.ssikit.config.AtlasConfigProperties;
import com.cartrust.atlas.ssikit.config.AtlasInitializer;
import com.cartrust.atlas.ssikit.config.AtlasJwtConfigBuilder;
import com.cartrust.atlas.ssikit.config.OpenApiAccessor;
import com.cartrust.atlas.ssikit.gx.AtlasServiceOfferingManager;
import com.cartrust.atlas.ssikit.policies.GxCredentialSignedByAuthorityVerificationPolicy;
import com.cartrust.atlas.ssikit.policies.PresentedBySubjectVerificationPolicy;
import com.cartrust.atlas.ssikit.waltid.AtlasVcTemplateService;
import com.cartrust.atlas.ssikit.waltid.FSHKVStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.walt.services.hkvstore.HKVStoreService;
import id.walt.signatory.Signatory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ExampleAtlasConfiguration {
    @Bean
    public AtlasCatalogueConfiguration atlasCatalogueConfiguration() {
        return AtlasCatalogueConfiguration.builder()
                .peer("https://authority.atlas.cartrust.com/gx/catalogue")
                .build();
    }

    @Bean(name = "exampleGroupedOpenApi")
    public GroupedOpenApi exampleGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("ServiceOfferingExample1")
                .packagesToScan("com.chainstep.atlas.example1")
                .pathsToMatch("/example/**")
                .build();
    }

    @Bean
    public HKVStoreService hkvStore() {
        return new FSHKVStore("hkv-example");
    }

    @Bean
    public AtlasServiceOfferingManager serviceOfferingManager(
            AtlasConfigProperties configuration,
            AtlasInitializer atlasInitializer,
            Signatory signatoryService,
            AtlasVcTemplateService templateService,
            OpenApiAccessor openApiAccessor,
            ObjectMapper mapper
    ) {

        return new AtlasServiceOfferingManager(
                configuration,
                atlasInitializer,
                signatoryService,
                templateService,
                openApiAccessor,
                mapper
        );
    }

    @Bean
    public CataloguePublisher cataloguePublisher(
            AtlasCatalogue catalogue,
            AtlasConfigProperties configProperties,
            AtlasCommunicator communicator,
            ObjectMapper objectMapper
    ) {
        return new CataloguePublisher(
                catalogue,
                configProperties,
                communicator,
                objectMapper
        );
    }

    @Bean
    public AtlasJwtConfigBuilder atlasJwtConfigBuilder() {
        PresentedBySubjectVerificationPolicy presentedPolicy = new PresentedBySubjectVerificationPolicy();
        GxCredentialSignedByAuthorityVerificationPolicy authorityVerificationPolicy = new GxCredentialSignedByAuthorityVerificationPolicy();
        CustomPolicy customPolicy = new CustomPolicy();
        return configurer -> {
            configurer.configure("/example.*").all(List.of(presentedPolicy, authorityVerificationPolicy, customPolicy));
        };
    }
}

