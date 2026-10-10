package baseline;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class RestartPersistenceTest {
    private static final Properties expected = new Properties();
    @BeforeAll static void load() throws Exception {
        try (InputStream input = Files.newInputStream(Paths.get(System.getProperty("daytrader.expectedState")))) { expected.load(input); }
        // These benchmark flags are JVM-local, not persisted with the database.
        // Restore the functional profile after restart: default long-run mode
        // intentionally suppresses order history on the account page.
        new TradeClient().configure(0, 0);
    }
    @ParameterizedTest(name = "durable trading state after restart: account {0}")
    @ValueSource(ints = {4, 5, 10, 11, 12, 13})
    void cashAndHoldingsSurviveRestart(int account) throws Exception {
        TradeClient client = new TradeClient();
        TradeClient.Response login = client.login(account);
        assertEquals(200, login.status); assertTrue(login.text().contains("Welcome test:" + account + ","));
        TradingContractTest.moneyEquals(new BigDecimal(expected.getProperty(account + ".cash")), TradeClient.cash(client.account()));
        assertEquals(Integer.parseInt(expected.getProperty(account + ".holdings")), client.holdings().size());
        client.logout();
    }
    @ParameterizedTest(name = "order history survives restart: account {0}")
    @ValueSource(ints = {4, 5, 10, 11, 12, 13})
    void orderHistorySurvivesRestart(int account) throws Exception {
        TradeClient client = new TradeClient();
        assertEquals(200, client.login(account).status);
        assertEquals(Integer.parseInt(expected.getProperty(account + ".orders")), client.orders().size());
        client.logout();
    }
}
