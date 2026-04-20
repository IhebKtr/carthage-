package com.carthage.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class PdfUtils {

    public static void exportTournamentToPdf(Window owner, String nom, java.sql.Timestamp dateDebut, int maxTeams, int currentTeams, int prizePool, String type, String status) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le tournoi en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName(nom.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        
        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // Add Title
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.RED);
                Paragraph title = new Paragraph("Détails du Tournoi : " + nom, titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Add Details
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);

                document.add(new Paragraph("Jeu / Type : ", boldFont));
                document.add(new Paragraph(type != null ? type : "Non spécifié", normalFont));
                document.add(Chunk.NEWLINE);

                String dateStr = dateDebut != null ? dateDebut.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "À définir";
                document.add(new Paragraph("Date de début : ", boldFont));
                document.add(new Paragraph(dateStr, normalFont));
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Prize Pool : ", boldFont));
                document.add(new Paragraph(prizePool + " DT", normalFont));
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Équipes inscrites : ", boldFont));
                document.add(new Paragraph(currentTeams + " sur " + maxTeams, normalFont));
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Statut : ", boldFont));
                document.add(new Paragraph(status != null ? status : "UPCOMING", normalFont));

                document.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
