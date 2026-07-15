type LoginRequest: void {
    .username: string
    .accountId: string
}

type LoginResponse: void {
    .sessionId: string
}

type SessionRequest: void {
    .sessionId: string
}

type MonetaryOperationRequest: void {
    .sessionId: string
    .amount: double
}

type OperationResult: void {
    .success: bool
}

type LogoutResponse: void {
    .username: string
    .accountId: string
    .balance: double
}

interface BankService {
    RequestResponse:
        login(LoginRequest)(LoginResponse),
        holdDeposit(MonetaryOperationRequest)(OperationResult),
        charge(MonetaryOperationRequest)(OperationResult),
        releaseDeposit(SessionRequest)(OperationResult),
        logout(SessionRequest)(LogoutResponse)
}