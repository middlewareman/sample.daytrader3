package baseline;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Independent SQL assertions after Liberty stops, rather than attaching to live embedded Derby.
 */
class PersistedStateTest {
    private static Connection database;
    private static final Properties expected = new Properties();

    @BeforeAll
    static void open() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        try (InputStream in = Files.newInputStream(Paths.get(System.getProperty("daytrader.expectedState")))) {
            expected.load(in);
        }
        assertFalse(expected.isEmpty(), "HTTP suite must supply independently calculated expectations");
        database = DriverManager.getConnection("jdbc:derby:" + System.getProperty("daytrader.database"), "db_username", "db_password");
    }

    @AfterAll
    static void close() throws Exception {
        if (database != null) database.close();
    }

    private ResultSet query(String sql, Object... args) throws SQLException {
        PreparedStatement statement = database.prepareStatement(sql);
        statement.closeOnCompletion();
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
        return statement.executeQuery();
    }

    @ParameterizedTest(name = "persisted trading state for account {0}")
    @ValueSource(ints = {4, 5, 10, 11, 12, 13})
    void cashHoldingsAndOrderCountsMatchBusinessExpectations(int account) throws Exception {
        assertNotNull(expected.getProperty(account + ".cash"), "Missing scenario outcome");
        try (ResultSet row = query("SELECT BALANCE FROM ACCOUNTEJB WHERE ACCOUNTID=?", account)) {
            assertTrue(row.next());
            TradingContractTest.moneyEquals(new BigDecimal(expected.getProperty(account + ".cash")), row.getBigDecimal(1));
            assertFalse(row.next());
        }
        for (String kind : new String[]{"holdings", "orders"}) {
            String table = kind.equals("holdings") ? "HOLDINGEJB" : "ORDEREJB";
            try (ResultSet row = query("SELECT COUNT(*) FROM " + table + " WHERE ACCOUNT_ACCOUNTID=?", account)) {
                assertTrue(row.next());
                assertEquals(Integer.parseInt(expected.getProperty(account + "." + kind)), row.getInt(1));
            }
        }
    }

    @Test
    void persistedOrdersRetainExecutionPricesFeesQuantitiesAndCompletion() throws Exception {
        long expectedOrders = expected.stringPropertyNames().stream().filter(k -> k.startsWith("order.") && k.endsWith(".type")).count();
        assertEquals(9, expectedOrders, "Every trading profile must contribute its orders");
        int checked = 0;
        try (ResultSet rows = query("SELECT ORDERID,ORDERTYPE,ORDERSTATUS,PRICE,QUANTITY,ORDERFEE,QUOTE_SYMBOL,COMPLETIONDATE FROM ORDEREJB")) {
            while (rows.next()) {
                String prefix = "order." + rows.getInt(1);
                assertNotNull(expected.getProperty(prefix + ".type"), "Unexpected persisted order");
                assertEquals(expected.getProperty(prefix + ".type"), rows.getString(2));
                assertEquals(expected.getProperty(prefix + ".status"), rows.getString(3));
                TradingContractTest.moneyEquals(new BigDecimal(expected.getProperty(prefix + ".price")), rows.getBigDecimal(4));
                TradingContractTest.moneyEquals(new BigDecimal(expected.getProperty(prefix + ".quantity")), BigDecimal.valueOf(rows.getDouble(5)));
                TradingContractTest.moneyEquals(new BigDecimal("24.95"), rows.getBigDecimal(6));
                assertEquals(expected.getProperty(prefix + ".symbol"), rows.getString(7));
                assertNotNull(rows.getTimestamp(8));
                checked++;
            }
        }
        assertEquals(9, checked);
    }

    @Test
    void persistedHoldingRetainsPurchasePriceQuantityAndOwner() throws Exception {
        try (ResultSet row = query("SELECT HOLDINGID,ACCOUNT_ACCOUNTID,PURCHASEPRICE,QUANTITY,QUOTE_SYMBOL FROM HOLDINGEJB")) {
            assertTrue(row.next());
            String prefix = "holding." + row.getInt(1);
            assertEquals(Integer.parseInt(expected.getProperty(prefix + ".account")), row.getInt(2));
            TradingContractTest.moneyEquals(new BigDecimal(expected.getProperty(prefix + ".price")), row.getBigDecimal(3));
            TradingContractTest.moneyEquals(new BigDecimal(expected.getProperty(prefix + ".quantity")), BigDecimal.valueOf(row.getDouble(4)));
            assertEquals(expected.getProperty(prefix + ".symbol"), row.getString(5));
            assertFalse(row.next());
        }
    }

    @Test
    void successfulProfileAndRegistrationPersistAndRejectedProfileDoesNot() throws Exception {
        try (ResultSet row = query("SELECT FULLNAME,PASSWD,EMAIL FROM ACCOUNTPROFILEEJB WHERE USERID='test:9'")) {
            assertTrue(row.next());
            assertEquals("Updated Name", row.getString(1));
            assertEquals("changed", row.getString(2));
            assertEquals("updated@example.invalid", row.getString(3));
        }
        try (ResultSet row = query("SELECT FULLNAME,PASSWD FROM ACCOUNTPROFILEEJB WHERE USERID='test:6'")) {
            assertTrue(row.next());
            assertEquals("Test User 6", row.getString(1));
            assertEquals("xxx", row.getString(2));
        }
        try (ResultSet row = query("SELECT BALANCE,OPENBALANCE FROM ACCOUNTEJB WHERE PROFILE_USERID='registered:test'")) {
            assertTrue(row.next());
            TradingContractTest.moneyEquals(new BigDecimal("5000.00"), row.getBigDecimal(1));
            TradingContractTest.moneyEquals(new BigDecimal("5000.00"), row.getBigDecimal(2));
            assertFalse(row.next());
        }
    }
}
