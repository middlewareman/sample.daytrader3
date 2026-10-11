package baseline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP/session/profile contracts complement the reusable trading scenarios.
 */
class SessionContractTest {
    private TradeClient client;

    @BeforeEach
    void setup() throws Exception {
        client = new TradeClient();
        client.configure(0, 0);
    }

    @Test
    void validLoginShowsTheCorrectAccountAndCash() throws Exception {
        TradeClient.Response login = client.login(0);
        assertEquals(200, login.status);
        assertTrue(login.text().contains("Welcome test:0,"));
        TradingContractTest.moneyEquals(TradingContractTest.initialCash(0), TradeClient.cash(client.account()));
    }

    @Test
    void wrongPasswordCannotAuthenticate() throws Exception {
        // Legacy EJB exception handling returns HTTP 500 rather than a friendly authentication error.
        assertEquals(500, client.action("action", "login", "uid", "test:1", "passwd", "wrong").status);
        assertTrue(client.action("action", "portfolio").text().contains("User Not Logged in"));
    }

    @Test
    void unauthenticatedRequestsCannotReachPortfolio() throws Exception {
        TradeClient.Response response = client.action("action", "portfolio");
        assertEquals(200, response.status);
        assertTrue(response.text().contains("User Not Logged in"));
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        assertEquals(200, client.login(2).status);
        assertEquals(200, client.logout().status);
        assertTrue(client.action("action", "account").text().contains("User Not Logged in"));
    }

    @Test
    void separateCookieJarsKeepAccountsIsolated() throws Exception {
        TradeClient other = new TradeClient();
        assertEquals(200, client.login(7).status);
        assertEquals(200, other.login(8).status);
        TradingContractTest.moneyEquals(TradingContractTest.initialCash(7), TradeClient.cash(client.account()));
        TradingContractTest.moneyEquals(TradingContractTest.initialCash(8), TradeClient.cash(other.account()));
        client.logout();
        TradingContractTest.moneyEquals(TradingContractTest.initialCash(8), TradeClient.cash(other.account()));
    }

    private TradeClient.Response profile(String password, String confirmation, String fullname) throws Exception {
        return client.action("action", "update_profile", "password", password, "cpassword", confirmation,
                "fullname", fullname, "address", "Updated Street", "creditcard", "4111111111111111", "email", "updated@example.invalid");
    }

    @Test
    void profileUpdateRejectsMismatchedPasswordsWithoutChangingThePassword() throws Exception {
        assertEquals(200, client.login(6).status);
        TradeClient.Response result = profile("changed", "different", "Rejected Name");
        assertEquals(200, result.status);
        assertTrue(result.text().contains("passwords do not match"));
        client.logout();
        assertEquals(200, client.login(6).status);
    }

    @Test
    void profileUpdatePersistsAndTheNewPasswordAuthenticates() throws Exception {
        assertEquals(200, client.login(9).status);
        TradeClient.Response result = profile("changed", "changed", "Updated Name");
        assertEquals(200, result.status);
        assertTrue(result.text().contains("Account profile update successful"));
        assertEquals("Updated Name", result.page.select("input[name=fullname]").val());
        client.logout();
        client = new TradeClient();
        TradeClient.Response login = client.action("action", "login", "uid", "test:9", "passwd", "changed");
        assertEquals(200, login.status);
        assertTrue(login.text().contains("Welcome test:9,"));
    }

    @Test
    void registrationCreatesAnAccountWithTheRequestedOpeningBalance() throws Exception {
        TradeClient.Response response = client.action("action", "register", "user id", "registered:test", "passwd", "xxx",
                "confirm passwd", "xxx", "Full Name", "Registered User", "Credit Card Number", "4111111111111111",
                "money", "5000.00", "email", "registered@example.invalid", "snail mail", "Fixture Street");
        assertEquals(200, response.status);
        assertTrue(response.text().contains("Welcome registered:test,"), response.text());
        TradingContractTest.moneyEquals(new java.math.BigDecimal("5000.00"), TradeClient.cash(client.account()));
    }
}
