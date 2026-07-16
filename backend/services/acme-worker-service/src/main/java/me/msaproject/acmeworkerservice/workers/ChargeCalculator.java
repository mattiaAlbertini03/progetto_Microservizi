package me.msaproject.acmeworkerservice.workers;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ChargeCalculator {

    private static final BigDecimal UNLOCK_FEE = BigDecimal.valueOf(1.00);
    private static final BigDecimal COST_PER_MINUTE = BigDecimal.valueOf(0.20);
    private static final BigDecimal COST_PER_KILOMETER = BigDecimal.valueOf(0.50);
    private static final BigDecimal LOW_BATTERY_MULTIPLIER = BigDecimal.valueOf(1.10);

    private ChargeCalculator() {
    }

    static Result calculate(int minutesUsed, double kilometers, double batteryPct) {
        if (minutesUsed < 0) {
            throw new IllegalArgumentException("minutesUsed must be non-negative");
        }
        if (kilometers < 0) {
            throw new IllegalArgumentException("kilometers must be non-negative");
        }
        if (batteryPct < 0 || batteryPct > 100) {
            throw new IllegalArgumentException("batteryPct must be in [0, 100]");
        }

        BigDecimal baseAmount = UNLOCK_FEE
                .add(BigDecimal.valueOf(minutesUsed).multiply(COST_PER_MINUTE))
                .add(BigDecimal.valueOf(kilometers).multiply(COST_PER_KILOMETER));

        boolean penaltyApplied = batteryPct < 15;
        BigDecimal finalAmount = penaltyApplied
                ? baseAmount.multiply(LOW_BATTERY_MULTIPLIER)
                : baseAmount;

        return new Result(
                baseAmount.setScale(2, RoundingMode.HALF_UP),
                finalAmount.setScale(2, RoundingMode.HALF_UP),
                batteryPct,
                penaltyApplied);
    }

    record Result(
            BigDecimal baseAmount,
            BigDecimal finalAmount,
            double batteryPct,
            boolean batteryPenaltyApplied) {
    }
}
