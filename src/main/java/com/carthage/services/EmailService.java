package com.carthage.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.carthage.utils.DatabaseConnection;

public class EmailService {

    private static final String USERNAME = "real.iheb2@gmail.com";
    private static final String PASSWORD = "xyujpzaykqmhvnxr"; // To be configured

    public static void sendMatchReminder(String toEmail, String teamName, String opponentName, String matchTime) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Rappel de Match: " + teamName + " vs " + opponentName);

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; background-color: #141A23; color: white; padding: 20px;\">"
                    + "<h2 style=\"color: #E50914;\">Rappel de votre match à venir !</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Ceci est un rappel automatique pour votre match organisé par Carthage Arena.</p>"
                    + "<div style=\"background-color: #1A212D; padding: 15px; border-radius: 8px; margin: 20px 0;\">"
                    + "<h3>" + teamName + " VS " + opponentName + "</h3>"
                    + "<p><strong>Heure prévue:</strong> " + matchTime + "</p>"
                    + "</div>"
                    + "<p>Soyez prêts 15 minutes à l'avance.</p>"
                    + "<br><p>L'équipe Carthage Arena</p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Reminder email sent successfully to " + toEmail);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // --- SCHEDULER ---
    private static ScheduledExecutorService scheduler;

    /**
     * Démarre le planificateur qui vérifie toutes les heures s'il y a des matchs prévus dans 24h.
     * À appeler idéalement au démarrage de l'application (ex: Main.java ou Initializable).
     */
    public static void startScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            // Exécute la vérification tout de suite, puis toutes les heures
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("Exécution automatique : Vérification des matchs dans 24h...");
                checkAndSendReminders(24);
            }, 0, 1, TimeUnit.HOURS);
            System.out.println("Email scheduler démarré.");
        }
    }

    public static void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("Email scheduler arrêté.");
        }
    }

    /**
     * Méthode pour tester immédiatement l'envoi d'e-mails pour les matchs prévus dans les prochaines 24h.
     */
    public static void testSendRemindersNow() {
        System.out.println("==== TEST MANUEL : Vérification des matchs dans 24h ====");
        checkAndSendReminders(24);
    }

    /**
     * Vérifie les matchs prévus dans un délai donné (ex: 24h) et envoie un e-mail aux capitaines.
     */
    private static void checkAndSendReminders(int hoursThreshold) {
        String sql = "SELECT u.email, t1.name as team_name, t2.name as opponent_name, m.scheduled_at " +
                     "FROM match_game m " +
                     "JOIN team t1 ON m.team1_id = t1.id " +
                     "JOIN team t2 ON m.team2_id = t2.id " +
                     "JOIN team_membership tm ON t1.id = tm.team_id " +
                     "JOIN user u ON tm.player_id = u.id " +
                     "WHERE tm.role = 'CAPTAIN' AND m.status = 'SCHEDULED' " +
                     "  AND m.scheduled_at > NOW() AND m.scheduled_at <= DATE_ADD(NOW(), INTERVAL ? HOUR) " +
                     "UNION " +
                     "SELECT u.email, t2.name as team_name, t1.name as opponent_name, m.scheduled_at " +
                     "FROM match_game m " +
                     "JOIN team t1 ON m.team1_id = t1.id " +
                     "JOIN team t2 ON m.team2_id = t2.id " +
                     "JOIN team_membership tm ON t2.id = tm.team_id " +
                     "JOIN user u ON tm.player_id = u.id " +
                     "WHERE tm.role = 'CAPTAIN' AND m.status = 'SCHEDULED' " +
                     "  AND m.scheduled_at > NOW() AND m.scheduled_at <= DATE_ADD(NOW(), INTERVAL ? HOUR)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hoursThreshold);
            ps.setInt(2, hoursThreshold);

            ResultSet rs = ps.executeQuery();
            int count = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

            while (rs.next()) {
                String email = rs.getString("email");
                String teamName = rs.getString("team_name");
                String opponentName = rs.getString("opponent_name");
                Timestamp matchTimeTs = rs.getTimestamp("scheduled_at");

                if (email != null && !email.isEmpty() && matchTimeTs != null) {
                    String matchTimeStr = matchTimeTs.toLocalDateTime().format(formatter);
                    System.out.println("Envoi d'un rappel à " + email + " (" + teamName + " vs " + opponentName + ")");
                    
                    // Envoi de l'e-mail
                    sendMatchReminder(email, teamName, opponentName, matchTimeStr);
                    count++;
                }
            }
            
            if (count == 0) {
                System.out.println("Aucun match trouvé dans les prochaines " + hoursThreshold + " heures.");
            } else {
                System.out.println(count + " rappels envoyés.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification des rappels de matchs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Test de l'envoi d'un email
        System.out.println("Début du test d'envoi d'email...");
        
        // Remplacez par votre email pour tester la réception
        String testEmail = "sr24csgo@gmail.com"; 
        
        sendMatchReminder(
                testEmail, 
                "Équipe Alpha", 
                "Équipe Beta", 
                "25 Octobre 2026 à 20:00"
        );
        
        System.out.println("Fin du test. Vérifiez la console pour d'éventuelles erreurs et votre boîte de réception.");
        System.out.println("NOTE: Si vous obtenez une AuthenticationFailedException, assurez-vous d'avoir mis un mot de passe d'application valide à la ligne 11.");
    }
}
