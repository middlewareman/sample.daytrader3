package baseline;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Legacy HTTP adapter. It deliberately has no enterprise API or application imports.
 */
final class TradeClient {
    private final String base = System.getProperty("daytrader.baseUrl");
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    static final class Response {
        final int status;
        final Document page;

        Response(int status, String html) {
            this.status = status;
            page = Jsoup.parse(html);
        }

        String text() {
            return page.text();
        }
    }

    static final class Holding {
        final int id;
        final String symbol;
        final BigDecimal quantity, price;

        Holding(Elements cells) {
            id = Integer.parseInt(cells.get(0).text());
            symbol = cells.get(2).text();
            quantity = money(cells.get(3).text());
            price = money(cells.get(4).text());
        }
    }

    static final class Order {
        final int id;
        final String status, type, symbol;
        final BigDecimal fee, quantity, price;

        Order(Elements cells) {
            id = Integer.parseInt(cells.get(0).text());
            status = cells.get(1).text();
            fee = money(cells.get(4).text());
            type = cells.get(5).text();
            symbol = cells.get(6).text();
            quantity = money(cells.get(7).text());
            price = money(cells.get(8).text());
        }
    }

    Response request(String path, String... parameters) throws IOException {
        URL url = new URL(base + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        for (Map.Entry<String, List<String>> header : cookies.get(URI.create(url.toString()), Collections.emptyMap()).entrySet())
            if (!header.getValue().isEmpty()) connection.setRequestProperty(header.getKey(), String.join("; ", header.getValue()));
        if (parameters.length > 0) {
            if (parameters.length % 2 != 0) throw new IllegalArgumentException("Expected name/value pairs");
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < parameters.length; i += 2) {
                if (body.length() > 0) body.append('&');
                body.append(URLEncoder.encode(parameters[i], "UTF-8")).append('=')
                        .append(URLEncoder.encode(parameters[i + 1], "UTF-8"));
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
        }
        try {
            int status = connection.getResponseCode();
            cookies.put(URI.create(url.toString()), connection.getHeaderFields());
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String html = "";
            if (input != null) try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) text.append(line).append('\n');
                html = text.toString();
            }
            return new Response(status, html);
        } finally {
            connection.disconnect();
        }
    }

    Response action(String... params) throws IOException {
        return request("/app", params);
    }

    Response login(int account) throws IOException {
        return action("action", "login", "uid", "test:" + account, "passwd", "xxx");
    }

    Response logout() throws IOException {
        return action("action", "logout");
    }

    void configure(int runtimeMode, int orderMode) throws IOException {
        // EnableLongRun is a checkbox: omitting it explicitly disables the
        // benchmark optimization that hides account-page order history.
        Response response = request("/config", "action", "updateConfig", "RunTimeMode", "" + runtimeMode,
                "OrderProcessingMode", "" + orderMode, "MaxUsers", "40", "MaxQuotes", "40",
                "EnablePublishQuotePriceChange", "on");
        if (response.status != 200 || !response.text().contains("DayTrader Configuration Updated"))
            throw new IOException("Configuration failed: " + response.status + " " + response.text());
    }

    Response account() throws IOException {
        return action("action", "account", "showAllOrders", "true");
    }

    static BigDecimal money(String text) {
        return new BigDecimal(text.replace("$", "").replace(",", "").trim());
    }

    static BigDecimal cash(Response response) {
        for (Element cell : response.page.select("td")) {
            if (cell.text().equalsIgnoreCase("cash balance:")) return money(cell.nextElementSibling().text());
        }
        throw new AssertionError("No cash balance in account page: " + response.text());
    }

    BigDecimal quote(String symbol) throws IOException {
        Response response = action("action", "quotes", "symbols", symbol);
        if (response.status != 200) throw new AssertionError("Quote response: " + response.status);
        for (Element row : response.page.select("tr")) {
            Elements cells = row.select("> td");
            if (cells.size() == 8 && cells.get(0).text().equals(symbol)) return money(cells.get(5).text());
        }
        throw new AssertionError("No quote row for " + symbol);
    }

    List<Holding> holdings() throws IOException {
        Response response = action("action", "portfolio");
        if (response.status != 200) throw new AssertionError("Portfolio response: " + response.status);
        List<Holding> result = new ArrayList<>();
        for (Element row : response.page.select("tr")) {
            Elements cells = row.select("> td");
            if (cells.size() >= 10 && !row.select("a[href*=holdingID]").isEmpty()) result.add(new Holding(cells));
        }
        return result;
    }

    List<Order> orders() throws IOException {
        Response response = account();
        if (response.status != 200) throw new AssertionError("Account response: " + response.status);
        List<Order> result = new ArrayList<>();
        for (Element row : response.page.select("tr")) {
            Elements cells = row.select("> td");
            if (cells.size() == 10 && cells.get(0).text().matches("\\d+") && !cells.get(6).select("a").isEmpty())
                result.add(new Order(cells));
        }
        return result;
    }
}
