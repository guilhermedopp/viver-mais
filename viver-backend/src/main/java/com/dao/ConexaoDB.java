package com.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoDB {
    // Configurações padrão para MySQL local
    private static final String URL = "jdbc:mysql://localhost:3306/viver_db?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String USER = "root";
    private static final String PASS = System.getenv("DB_PASSWORD");

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}