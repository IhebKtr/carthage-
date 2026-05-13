package com.carthage.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class ICalUtils {

    public static void exportEvent(Window owner, String eventName, LocalDateTime start) {
        if (start == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'événement iCal");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar File (*.ics)", "*.ics"));
        fileChooser.setInitialFileName(eventName.replaceAll("[^a-zA-Z0-9]", "_") + ".ics");
        
        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                String dtStart = start.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                String dtEnd = start.plusHours(2).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                
                writer.write("BEGIN:VCALENDAR\n");
                writer.write("VERSION:2.0\n");
                writer.write("PRODID:-//Carthage Arena//FR\n");
                writer.write("BEGIN:VEVENT\n");
                writer.write("UID:" + java.util.UUID.randomUUID().toString() + "\n");
                writer.write("DTSTAMP:" + now + "\n");
                writer.write("DTSTART:" + dtStart + "\n");
                writer.write("DTEND:" + dtEnd + "\n");
                writer.write("SUMMARY:" + eventName + "\n");
                writer.write("END:VEVENT\n");
                writer.write("END:VCALENDAR\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
