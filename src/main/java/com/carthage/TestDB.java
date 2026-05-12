package com.carthage;

import com.carthage.utils.DatabaseConnection;
import java.sql.ResultSet;

public class TestDB {
    public static void main(String[] args) throws Exception {
        ResultSet rs = DatabaseConnection.getInstance().getConnection().createStatement().executeQuery("DESCRIBE skin");
        while(rs.next()) {
            System.out.println(rs.getString("Field") + " - " + rs.getString("Type"));
        }
    }
}
