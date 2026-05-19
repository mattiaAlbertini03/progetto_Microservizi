package me.msaproject.acmemobilityservice.camunda;

import io.camunda.client.CamundaClient;
import io.camunda.client.CredentialsProvider;
import io.camunda.client.impl.basicauth.BasicAuthCredentialsProviderBuilder;

import java.net.URI;

public final class CamundaClientFactory {

    private CamundaClientFactory() {}

    public static CamundaClient create() {
        CredentialsProvider credentialsProvider =
                new BasicAuthCredentialsProviderBuilder()
                        .username("demo")
                        .password("demo")
                        .build();

        return CamundaClient.newClientBuilder()
                .grpcAddress(URI.create("http://camunda-zeebe-gateway.orchestration:26500"))
                .restAddress(URI.create("http://camunda-zeebe-gateway.orchestration:8080"))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}