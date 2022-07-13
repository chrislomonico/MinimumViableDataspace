package org.eclipse.dataspaceconnector.system.tests.identityhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityHubIntegrationTest {

    private static final String HUB_URL_FORMAT = "http://localhost:%s/api/identity-hub";

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private IdentityHubClientImpl client;

    @BeforeEach
    void setUp() {
        client = new IdentityHubClientImpl(OK_HTTP_CLIENT, OBJECT_MAPPER);
    }

    @ParameterizedTest
    @ValueSource(ints = {8181, 8182, 8183})
    void retrieveVerifiableCredentials_empty(int hubPort) throws IOException {
        var hubUrl = String.format(HUB_URL_FORMAT, hubPort);

        var vcs = client.getVerifiableCredentials(hubUrl);

        assertThat(vcs.succeeded()).isTrue();
        assertThat(vcs.getContent()).isEmpty();
    }
}
