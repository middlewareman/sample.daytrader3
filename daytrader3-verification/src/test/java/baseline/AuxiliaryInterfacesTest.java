package baseline;

import org.junit.jupiter.api.Test;
import org.jsoup.nodes.Element;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Representative external checks for retained Faces and REST modules.
 */
class AuxiliaryInterfacesTest {
    @Test
    void facesRendersAQuoteAndRetainsViewStateOnPostback() throws Exception {
        TradeClient client = new TradeClient();
        client.configure(0, 0);
        TradeClient.Response page = client.request("/PingFaceletSmall.jsf");
        assertEquals(200, page.status, page.text());
        assertTrue(page.text().contains("Stock Information"));
        assertTrue(page.page.select("td").stream().anyMatch(e -> e.text().matches("s:\\d+")), page.text());
        Element state = page.page.selectFirst("input[type=hidden][name$=ViewState]");
        assertNotNull(state, "Faces must render a server view-state token");
        TradeClient.Response postback = client.request("/PingFaceletSmall.jsf", "form1", "form1", state.attr("name"), state.val());
        assertEquals(200, postback.status, postback.text());
        assertTrue(postback.text().contains("Stock Information"));
        assertTrue(postback.page.select("td").stream().anyMatch(e -> e.text().matches("s:\\d+")), postback.text());
    }

    private String rest(String path, String accept) throws Exception {
        URL url = new URL(System.getProperty("daytrader.baseUrl").replace("/daytrader", "/rest") + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", accept);
        try {
            assertEquals(200, connection.getResponseCode(), "REST " + path);
            assertTrue(connection.getContentType().startsWith(accept), connection.getContentType());
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream in = connection.getInputStream()) {
                byte[] block = new byte[4096];
                int count;
                while ((count = in.read(block)) >= 0) bytes.write(block, 0, count);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    @Test
    void restSearchReturnsTheFixedAddress() throws Exception {
        String json = rest("/addresses/search/Entry1", "application/json");
        assertTrue(json.matches("(?s).*\"entryName\"\\s*:\\s*\"Entry1\".*"), json);
        assertTrue(json.matches("(?s).*\"zipCode\"\\s*:\\s*\"12345\".*"), json);
        assertFalse(json.contains("Entry2"), "Search must exclude other entries");
    }

    @Test
    void restSingleAddressRejectsGetWithoutContentType() throws Exception {
        URL url = new URL(System.getProperty("daytrader.baseUrl").replace("/daytrader", "/rest") + "/addresses/Entry1");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "text/xml");
        try {
            assertEquals(415, connection.getResponseCode(), "Preserve characterized legacy REST response");
        } finally {
            connection.disconnect();
        }
    }
}
