import com.carthage.utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckTournoiSchema {
    public static void main(String[] args) {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("DESCRIBE tournoi");
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " : " + rs.getString("Type"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
