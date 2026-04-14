import com.carthage.utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckReclamationSchema {
    public static void main(String[] args) {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            Statement stmt = conn.createStatement();
            System.out.println("--- reclamation ---");
            ResultSet rs = stmt.executeQuery("DESCRIBE reclamation");
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " : " + rs.getString("Type"));
            }
            
            System.out.println("\n--- reclamation_response ---");
            try {
                rs = stmt.executeQuery("DESCRIBE reclamation_response");
                while (rs.next()) {
                    System.out.println(rs.getString("Field") + " : " + rs.getString("Type"));
                }
            } catch (Exception e) {
                System.out.println("reclamation_response table not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
