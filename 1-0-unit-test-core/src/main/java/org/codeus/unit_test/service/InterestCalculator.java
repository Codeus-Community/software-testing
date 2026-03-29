package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.model.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class InterestCalculator {

    private static final BigDecimal SAVINGS_ANNUAL_RATE = new BigDecimal("0.05");
    private static final BigDecimal CHECKING_ANNUAL_RATE = new BigDecimal("0.01");
    private static final BigDecimal BUSINESS_ANNUAL_RATE = new BigDecimal("0.03");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    /**
     * Calculates the simple interest for the given account over a specified period.
     *
     * @param account the account for which to calculate interest
     * @param fromDate the start date of the interest period
     * @param toDate the end date of the interest period
     * @return the interest amount accrued between fromDate and toDate
     * @throws IllegalArgumentException if account or dates are null, or if toDate is before fromDate
     */
    public BigDecimal calculateInterest(Account account, LocalDateTime fromDate, LocalDateTime toDate) {
        if (account == null || fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Account and dates cannot be null");
        }

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);

        if (daysBetween == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualRate = getAnnualRate(account.getType());
        BigDecimal dailyRate = annualRate.divide(DAYS_IN_YEAR, 10, RoundingMode.HALF_UP);

        return account.getBalance()
                .multiply(dailyRate)
                .multiply(new BigDecimal(daysBetween))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the compound interest for the given account over a specified period.
     * Interest is compounded based on the specified number of periods per year.
     *
     * @param account the account for which to calculate interest
     * @param fromDate the start date of the interest period
     * @param toDate the end date of the interest period
     * @param compoundingPeriodsPerYear number of compounding periods in a year
     * @return the compound interest accrued between fromDate and toDate
     * @throws IllegalArgumentException if account or dates are null, toDate is before fromDate,
     *                                  or compoundingPeriodsPerYear is non-positive
     */
    public BigDecimal calculateCompoundInterest(Account account, LocalDateTime fromDate, LocalDateTime toDate, int compoundingPeriodsPerYear) {
        if (account == null || fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Account and dates cannot be null");
        }

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        if (compoundingPeriodsPerYear <= 0) {
            throw new IllegalArgumentException("Compounding periods must be positive");
        }

        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);

        if (daysBetween == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualRate = getAnnualRate(account.getType());
        BigDecimal years = new BigDecimal(daysBetween).divide(DAYS_IN_YEAR, 10, RoundingMode.HALF_UP);

        BigDecimal ratePerPeriod = annualRate.divide(new BigDecimal(compoundingPeriodsPerYear), 10, RoundingMode.HALF_UP);
        BigDecimal periods = years.multiply(new BigDecimal(compoundingPeriodsPerYear));

        BigDecimal onePlusRate = BigDecimal.ONE.add(ratePerPeriod);
        BigDecimal compoundFactor = pow(onePlusRate, periods.intValue());

        BigDecimal finalAmount = account.getBalance().multiply(compoundFactor);
        return finalAmount.subtract(account.getBalance()).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAnnualRate(AccountType accountType) {
        if (accountType == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }

        return switch (accountType) {
            case SAVINGS -> SAVINGS_ANNUAL_RATE;
            case CHECKING -> CHECKING_ANNUAL_RATE;
            case BUSINESS -> BUSINESS_ANNUAL_RATE;
        };
    }

    private BigDecimal pow(BigDecimal base, int exponent) {
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base);
        }
        return result.setScale(10, RoundingMode.HALF_UP);
    }
}