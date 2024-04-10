# Example SSI Spring Boot Application

## Description
Atlas is a project to provide a wrapper to use within the Gaia-X context.
This project is to show how to create a service, that will provide authentication via SSI for REST APIs.

## Preconditions to run this example service

 - JDK 17
 - [Localstack](https://docs.localstack.cloud/getting-started/installation/)
 - [AWS ClI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
 - S3 bucket `atlas-hkv-example` (Create bucket with `aws s3 mb s3://atlas-hkv-example --endpoint http://localhost:4566` - localstack needs s3 enabled listening on port 4566)

## ExampleApplication class
This is a normal spring boot application created by start.spring.io with some important modifications to its main class.

To enable autoconfiguration of atlas, you need to add the atlas package to the components to scan. Simply add the `@EnableAtlas` annotation to the [main class](src/main/java/com/chainstep/atlas/example2/ExampleApplication.java).
In addition, AtlasSelfDescriptionManager and AtlasCatalogue rely on scheduled spring.This is enabled by adding the `@EnableScheduling` annotation to the [main class](src/main/java/com/chainstep/atlas/example2/ExampleApplication.java).



## AtlasConfiguration

Use `atlas.config` properies within the [application.yml](src/main/resources/application.yml) or provide `AtlasConfiguration` config bean to configure atlas.

```yaml
atlas:
  config:
    vcGroup: default
    signatoryKeyAlias: signatory
    domain: atlas.cartrust.com
    didWebPath: gx/did
    nonce: todo
    templatesFolder: /vc-templates-runtime
    selfDescriptionTemplate: GaiaxCredentialSD
    selfDescriptionVcId: sd
    serviceBaseAddress: https://example.com
    apiDocsBasePath: /v3/api-docs
    selfSignSelfDescription: false
    returnEmptySelfDescription: true
    autostartSelfDescriptionSchedule: false
    autoCreateSignatoryDid: true
    selfDescriptionRefreshRate: 10
    catalogueRefreshRate: 60
```

### Property descriptions

 - **vcGroup**: The group which within your VCs should be stored
 - **signatoryKeyAlias**: the alias of your private key. This key will be used to create your did and sign all your VC/VPs
 - **domain**: Your did-web domain. This domain needs to point to your service instance
 - **didWebPath**: The domain suffix of your service. Should not be changed
 - **nonce**: Parameter of underlying waltid-ssikit configuration
 - **templatesFolder**: The location where your templates are stored. 
 - **selfDescriptionTemplate**: Template for GxSelfDescription. Either do not modify or provide new template in your templates folder.
 - **selfDescriptionVcId**: Identifier where to store the SelfDescriptionVC
 - **serviceBaseAddress**: Your web address of the service. This address is used for ServiceOfferings. It should match your domain property as https address.
 - **apiDocsBasePath**: No need to change if you don't want to reconfigure the OpenAPI 
 - **selfSignSelfDescription**: Set to true, if you want to sign your SelfDescription. 
 - **returnEmptySelfDescription**: Set to true, to unsigned SelfDescription
 - **autostartSelfDescriptionSchedule**: Set to true, to start scheduled process to send SelfDescription to authority found as ServiceOffering. Otherwise, the process can be started via SetupController. To use this feature, you need to enable scheduling for your project. This can be done by adding `@EnableScheduling` annotation ( [ExampleApplication.java](src/main/java/com/chainstep/atlas/example2/ExampleApplication.java) ).
 - **autoCreateSignatoryDid**: Set this to true, to create key and did document if not present. Otherwise, you need to invoke the setup controller. Usually `true` is the better option.
 - **selfDescriptionRefreshRate**: The time in seconds between the retries of asking the authority for the signed SelfDescription. No effect if scheduling not configured. 
 - **catalogueRefreshRate**: The time in seconds between refreshes of provided catalogue feature. Needs scheduling to be enabled.

## S3 data store

By default, atlas is configured to use a HKVStoreService to store everything. The actual implementation is the InMemoryHKVStore by waltid. 
There are other implementations available. We are using the S3HkvStore by Atlas for example. The AtlasAutoConfiguration is using HKVStore based implementations as VCStore and KeyStore.
Within the [ExampleAtlasConfiguration.java](src/main/java/com/chainstep/atlas/example2/ExampleAtlasConfiguration.java) we have the needed bean definitions and the relevant `atlas.hkv` configuration within the [application.yml](src/main/resources/application.yml)  

```java
    @Bean
    @ConfigurationProperties("atlas.hkv")
    public S3HKVStoreProperties s3HKVStoreProperties() {
        return new S3HKVStoreProperties();
    }
    @Bean
    public HKVStoreService hkvStore(S3HKVStoreProperties properties) {
        return new S3HkvStore(properties);
    }
```

```yaml
atlas:
  hkv:
    bucket: atlas-hkv-example # your bucked designated as hkv storage
    region: eu-central-1
    endpoint: http://localhost:4566 # endpoint needed for localstack
```

## Example Controller

To show the authentication interaction, we need two participants. The [ExampleSenderController.java](src/main/java/com/chainstep/atlas/example2/ExampleSenderController.java)
acts as participant/holder, that wants to authenticate via presentation of his SelfDescription VC. The [ExampleReceiverController.java](src/main/java/com/chainstep/atlas/example2/ExampleSenderController.java)
is offering a service, that needs authentication via SelfDescription and acts as verifier. 

On receiver side, we need to provide a bean of `AtlasJwtConfigBuilder` to configure paths that will need authentication.

```java
    @Bean
    public AtlasJwtConfigBuilder atlasJwtConfigBuilder() {
        return configurer -> {
            // add path pattern and the method it should be applied
            configurer.configure("/example/receiver.*").post();
            // you could also pass a list of VerificationPolicy objects
        };
    }
```

## ServiceOffering

 - We need at least one controller. In this case we want to offer the DistanceController as service.
 - A vc-template has to be provided. The templates filename has to start with *ServiceOffering*. For this example application we provide [ServiceOfferingDistance.json](./src/main/resources/vc-templates/ServiceOfferingDistance.json).
 - The service's api definition has to be grouped via a `GroupedOpenApi` with a group name matching the template
 - To activate service offer publishing, we need to create a bean of `AtlasServiceOfferingManager` [ExampleAtlasConfiguration.java](src/main/java/com/chainstep/atlas/example2/ExampleAtlasConfiguration.java)

ServiceOfferings will be published at `$baseAddress/gx/so` and a definition of a single service is visible as sub element of this address. In the example application, it is [/gx/so/ServiceOfferingDistance](http://localhost:9180/gx/so/ServiceOfferingDistance).

```java
    @Bean
    public GroupedOpenApi distanceGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("ServiceOfferingDistance") // This name has to match the ServiceOffering template name
                .packagesToScan("com.cartrust.atlas.example.distance")
                .pathsToMatch("/distance/**")
                .build();
    }

    @Bean
    public AtlasServiceOfferingManager serviceOfferingManager(
        AtlasConfiguration configuration,
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
```
