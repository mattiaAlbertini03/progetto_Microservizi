package me.msaproject.acmemobilityservice.dto.bank;

public record BankAccountResponse(
        boolean connected,
        String userId,
        String username,
        String accountId,
        Double balance,
        String bankSessionId
) {
}
