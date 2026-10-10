package baseline;

import com.ibm.websphere.samples.daytrader.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class FinancialCalculationsTest {
    private static BigDecimal decimal(String value) { return new BigDecimal(value); }
    @ParameterizedTest @CsvSource({"120.00,100.00,20.00", "80.00,100.00,-20.00", "100.00,100.00,0.00"})
    void gainUsesCurrentMinusOpeningBalance(String current, String opening, String gain) {
        assertEquals(decimal(gain), FinancialUtils.computeGain(decimal(current), decimal(opening)));
    }
    @ParameterizedTest @CsvSource({"120.00,100.00,20.00", "80.00,100.00,-20.00", "100.00,0.00,0.00"})
    void gainPercentIncludesLossAndZeroOpeningBalance(String current, String opening, String percent) {
        assertEquals(0, decimal(percent).compareTo(FinancialUtils.computeGainPercent(decimal(current), decimal(opening))));
    }
    @Test void noHoldingsHaveZeroPurchaseValue() {
        assertEquals(decimal("0.00"), FinancialUtils.computeHoldingsTotal(null));
        assertEquals(decimal("0.00"), FinancialUtils.computeHoldingsTotal(Collections.emptyList()));
    }
    @Test void holdingsValueUsesQuantityTimesPurchasePriceIncludingFractionalShares() {
        HoldingDataBean first = new HoldingDataBean(); first.setPurchasePrice(decimal("10.00")); first.setQuantity(2);
        HoldingDataBean second = new HoldingDataBean(); second.setPurchasePrice(decimal("12.00")); second.setQuantity(0.5);
        assertEquals(decimal("26.00"), FinancialUtils.computeHoldingsTotal(Arrays.asList(first, second)));
    }
}
