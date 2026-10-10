package baseline;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

/** Loads a fixed fixture into the test-owned, stopped embedded database. */
public final class DerbyFixture {
    public static void main(String[] args) throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        String url = "jdbc:derby:" + args[0];
        try (Connection c = DriverManager.getConnection(url + ";create=true", "db_username", "db_password")) {
            c.setAutoCommit(false);
            StringBuilder ddl = new StringBuilder();
            for (String line : Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8)) {
                if (!line.trim().startsWith("#")) ddl.append(line).append('\n');
            }
            try (Statement sql = c.createStatement()) {
                // The Gradle task creates a new database: DROP statements are not needed.
                for (String statement : ddl.toString().split(";")) {
                    if (!statement.trim().isEmpty() && !statement.trim().toUpperCase().startsWith("DROP "))
                        sql.execute(statement);
                }
                sql.executeUpdate("INSERT INTO KEYGENEJB VALUES (10000, 'account'), (10000, 'order'), (10000, 'holding')");
                for (int i = 0; i < 40; i++) {
                    sql.executeUpdate("INSERT INTO ACCOUNTPROFILEEJB VALUES ('Fixture Street', 'xxx', 'test:" + i
                            + "', 'test" + i + "@example.invalid', '4111111111111111', 'Test User " + i + "')");
                    int cash = 10000 + i * 1000;
                    sql.executeUpdate("INSERT INTO ACCOUNTEJB VALUES (TIMESTAMP('2020-01-01 00:00:00'), " + cash
                            + ", 0, " + cash + ", " + i + ", TIMESTAMP('2020-01-01 00:00:00'), 0, 'test:" + i + "')");
                    int price = 50 + i;
                    sql.executeUpdate("INSERT INTO QUOTEEJB VALUES (" + price + ", " + price + ", 0, " + price
                            + ", " + price + ", 'Fixture Company " + i + "', 's:" + i + "', 0)");
                }
            }
            c.commit();
        }
        try { DriverManager.getConnection(url + ";shutdown=true"); }
        catch (SQLException stopped) { if (!"08006".equals(stopped.getSQLState())) throw stopped; }
        System.out.println("Deterministic fixture: 40 accounts, 40 quotes, zero holdings/orders; fixed timestamps and key seeds.");
    }
}
