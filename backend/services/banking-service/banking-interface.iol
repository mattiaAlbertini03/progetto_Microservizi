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

type AccountRequest: void {
    .userId: string
}

type CardRegistrationRequest: void {
    .userId: string
    .username: string
    .cardNumber: string
    .cardHolder: string
    .expiry: string
    .cvv: string
}

type RechargeRequest: void {
    .userId: string
    .amount: double
}

type AccountResponse: void {
    .connected: bool
    .userId?: string
    .username?: string
    .accountId?: string
    .balance?: double
}

interface BankService {
    RequestResponse:
        login(LoginRequest)(LoginResponse),
        holdDeposit(MonetaryOperationRequest)(OperationResult),
        charge(MonetaryOperationRequest)(OperationResult),
        releaseDeposit(SessionRequest)(OperationResult),
        logout(SessionRequest)(LogoutResponse)
}

interface BankRestService {
    RequestResponse:
        getAccount(AccountRequest)(AccountResponse),
        registerAccount(CardRegistrationRequest)(AccountResponse),
        rechargeAccount(RechargeRequest)(AccountResponse),
        openBankSession(LoginRequest)(LoginResponse)
}
