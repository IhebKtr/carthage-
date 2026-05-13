package com.carthage.services;

import com.carthage.utils.EnvConfig;
import com.carthage.utils.DatabaseConnection;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * EmailService — hybrid design:
 *  - Instance-based OOP (Aymen) for clean reusable send()
 *  - Reads config from .env via EnvConfig (Aymen)
 *  - Uses javax.mail (master's dependency, no extra lib needed)
 *  - Preserves match-reminder scheduler (master)
 */
public class EmailService {

    // ── Instance config (Aymen style) ─────────────────────────────────────────
    private final Session session;
    private final String fromAddress;
    private final String fromName;

    public EmailService() {
        EnvConfig config = EnvConfig.getInstance();

        String host     = config.getRequired("MAIL_HOST");
        int    port     = config.getIntRequired("MAIL_PORT");
        String username = config.getRequired("MAIL_USERNAME");
        String password = config.getRequired("MAIL_PASSWORD");

        this.fromAddress = username;
        this.fromName    = config.getRequired("MAIL_FROM_NAME");

        Properties props = new Properties();
        props.put("mail.smtp.host",             host);
        props.put("mail.smtp.port",             String.valueOf(port));
        props.put("mail.smtp.starttls.enable",  "true");
        props.put("mail.smtp.auth",             "true");

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /**
     * Generic send — usable from anywhere in the codebase.
     */
    public void send(String to, String subject, String body) throws EmailException {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setContent(body, "text/html; charset=utf-8");
            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailException("Échec de l'envoi de l'email à " + to + " : " + e.getMessage(), e);
        }
    }

    public static class EmailException extends Exception {
        public EmailException(String message, Throwable cause) { super(message, cause); }
    }

    // ── Static match-reminder helpers (master style) ───────────────────────────

    /**
     * Sends a styled HTML match-reminder email.
     * Uses EnvConfig for credentials — no hardcoded values.
     */
    public static void sendMatchReminder(String toEmail, String teamName,
                                         String opponentName, String matchTime) {
        try {
            new EmailService().send(
                toEmail,
                "Rappel de Match: " + teamName + " vs " + opponentName,
                "<div style=\"font-family: Arial, sans-serif; background-color: #141A23; color: white; padding: 20px;\">"
                    + "<h2 style=\"color: #E50914;\">Rappel de votre match à venir !</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Ceci est un rappel automatique pour votre match organisé par Carthage Arena.</p>"
                    + "<div style=\"background-color: #1A212D; padding: 15px; border-radius: 8px; margin: 20px 0;\">"
                    + "<h3>" + teamName + " VS " + opponentName + "</h3>"
                    + "<p><strong>Heure prévue:</strong> " + matchTime + "</p>"
                    + "</div>"
                    + "<p>Soyez prêts 15 minutes à l'avance.</p>"
                    + "<br><p>L'équipe Carthage Arena</p>"
                    + "</div>"
            );
            System.out.println("Reminder email sent successfully to " + toEmail);
        } catch (EmailException e) {
            e.printStackTrace();
        }
    }

    // ── Scheduler (master) ────────────────────────────────────────────────────
    private static ScheduledExecutorService scheduler;

    /** Starts the hourly scheduler that checks for matches within 24 h. */
    public static void startScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
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

    public static void testSendRemindersNow() {
        System.out.println("==== TEST MANUEL : Vérification des matchs dans 24h ====");
        checkAndSendReminders(24);
    }

    private static void checkAndSendReminders(int hoursThreshold) {
        String sql =
            "SELECT u.email, t1.name AS team_name, t2.name AS opponent_name, m.scheduled_at " +
            "FROM match_game m " +
            "JOIN team t1 ON m.team1_id = t1.id " +
            "JOIN team t2 ON m.team2_id = t2.id " +
            "JOIN team_membership tm ON t1.id = tm.team_id " +
            "JOIN user u ON tm.player_id = u.id " +
            "WHERE tm.role = 'CAPTAIN' AND m.status = 'SCHEDULED' " +
            "  AND m.scheduled_at > NOW() AND m.scheduled_at <= DATE_ADD(NOW(), INTERVAL ? HOUR) " +
            "UNION " +
            "SELECT u.email, t2.name AS team_name, t1.name AS opponent_name, m.scheduled_at " +
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
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

            while (rs.next()) {
                String    email       = rs.getString("email");
                String    teamName    = rs.getString("team_name");
                String    opponent    = rs.getString("opponent_name");
                Timestamp matchTimeTs = rs.getTimestamp("scheduled_at");

                if (email != null && !email.isEmpty() && matchTimeTs != null) {
                    String matchTimeStr = matchTimeTs.toLocalDateTime().format(fmt);
                    System.out.println("Envoi d'un rappel à " + email + " (" + teamName + " vs " + opponent + ")");
                    sendMatchReminder(email, teamName, opponent, matchTimeStr);
                    count++;
                }
            }

            System.out.println(count == 0
                ? "Aucun match trouvé dans les prochaines " + hoursThreshold + " heures."
                : count + " rappels envoyés.");

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification des rappels : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
