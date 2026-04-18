package db;
import java.sql.*;


public class conect {

    private static final String URL = "jdbc:mysql://localhost:3306/reciclagem_db";
    private static final String USER = "user";
    private static final String PASSWORD = "user123";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
