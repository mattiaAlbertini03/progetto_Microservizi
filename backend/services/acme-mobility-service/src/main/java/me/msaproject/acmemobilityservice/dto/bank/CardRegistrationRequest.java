package me.msaproject.acmemobilityservice.dto.bank;

public record CardRegistrationRequest(
        String cardNumber,
        String cardHolder,
        String expiry,
        String cvv,
        String reservationRequestId
) {
}
