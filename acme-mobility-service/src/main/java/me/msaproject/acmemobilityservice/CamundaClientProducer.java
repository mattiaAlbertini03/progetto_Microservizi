package me.msaproject.acmemobilityservice;

import io.camunda.client.CamundaClient;
import io.camunda.client.CredentialsProvider;
import io.camunda.client.impl.basicauth.BasicAuthCredentialsProviderBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.net.URI;

@ApplicationScoped
public class CamundaClientProducer {

    @Produces
    @ApplicationScoped
    public CamundaClient camundaClient() {
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