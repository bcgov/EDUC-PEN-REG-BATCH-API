package ca.bc.gov.educ.penreg.api.rest;

import ca.bc.gov.educ.penreg.api.properties.ApplicationProperties;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * The type Rest web client.
 */
@Configuration
@Profile("!test")
public class RestWebClient {

    /**
     * The Props.
     */
    private final ApplicationProperties props;

    /**
     * Instantiates a new Rest web client.
     *
     * @param props the props
     */
    public RestWebClient(final ApplicationProperties props) {
        this.props = props;
    }

    /**
     * Web client web client.
     *
     * @return the web client
     */
    @Bean
    WebClient webClient() {
        val clientRegistryRepo = new InMemoryReactiveClientRegistrationRepository(ClientRegistration
                .withRegistrationId(this.props.getClientID())
                .tokenUri(this.props.getTokenURL())
                .clientId(this.props.getClientID())
                .clientSecret(this.props.getClientSecret())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build());
        val clientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistryRepo);
        val authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistryRepo, clientService);
        val oauthFilter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauthFilter.setDefaultClientRegistrationId(this.props.getClientID());
        final DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return WebClient.builder()
                .uriBuilderFactory(factory)
                .filter(oauthFilter)
                .build();
    }
}
