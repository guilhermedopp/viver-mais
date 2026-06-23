package com.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import io.github.cdimascio.dotenv.Dotenv;

public class ConexaoDB {

    public static Connection conectar() throws SQLException {
        // Carrega as variáveis do ficheiro .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        // Vai buscar os dados. Se não houver .env, usa os de emergência/padrão locais
        String url = dotenv.get("DB_URL", "jdbc:mysql://localhost:3306/viver_db?allowPublicKeyRetrieval=true&useSSL=false");
        String user = dotenv.get("DB_USER", "root");
        String pass = dotenv.get("DB_PASS", ""); 
        
        return DriverManager.getConnection(url, user, pass);
    }
}