package me.msaproject.acmemobilityservice.reservation;

import io.camunda.client.CamundaClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ReservationService {

    @Inject
    CamundaClient camundaClient;

    public String createReservation(ReservationRequest request) {
        String reservationId = UUID.randomUUID().toString();

        camundaClient.newCreateInstanceCommand()
                .bpmnProcessId("reservation-process")
                .latestVersion()
                .variables(Map.of(
                        "reservationId", reservationId,
                        "userId", request.userId(),
                        "vehicleId", request.vehicleId(),
                        "stationId", request.stationId()
                ))
                .execute();

        return reservationId;
    }
}