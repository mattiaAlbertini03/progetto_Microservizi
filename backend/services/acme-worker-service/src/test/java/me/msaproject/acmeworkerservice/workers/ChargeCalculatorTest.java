package me.msaproject.acmeworkerservice.workers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargeCalculatorTest {

    @Test
    void calculatesBaseChargeWithoutPenaltyAtFifteenPercent() {
        ChargeCalculator.Result result = ChargeCalculator.calculate(10, 2.0, 15.0);

        assertEquals(new BigDecimal("4.00"), result.baseAmount());
        assertEquals(new BigDecimal("4.00"), result.finalAmount());
        assertFalse(result.batteryPenaltyApplied());
    }

    @Test
    void addsTenPercentPenaltyBelowFifteenPercent() {
        ChargeCalculator.Result result = ChargeCalculator.calculate(10, 2.0, 14.9);

        assertEquals(new BigDecimal("4.00"), result.baseAmount());
        assertEquals(new BigDecimal("4.40"), result.finalAmount());
        assertTrue(result.batteryPenaltyApplied());
    }

    @Test
    void rejectsInvalidTripMeasurements() {
        assertThrows(IllegalArgumentException.class,
                () -> ChargeCalculator.calculate(-1, 1, 50));
        assertThrows(IllegalArgumentException.class,
                () -> ChargeCalculator.calculate(1, -1, 50));
        assertThrows(IllegalArgumentException.class,
                () -> ChargeCalculator.calculate(1, 1, 101));
    }
}
