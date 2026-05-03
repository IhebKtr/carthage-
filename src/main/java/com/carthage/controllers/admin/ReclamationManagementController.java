package com.carthage.controllers.admin;

import com.carthage.entity.Reclamation;
import com.carthage.entity.enums.ReclamationCategory;
import com.carthage.entity.enums.ReclamationStatus;
import com.carthage.services.ReclamationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;

public class ReclamationManagementController {

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private BarChart<String, Number> priorityBarChart;
    @FXML private LineChart<String, Number> activityLineChart;

    // ── KPI Labels ────────────────────────────────────────────────────────────
    @FXML private Label totalLabel;
    @FXML private Label pendingLabel;
    @FXML private Label resolvedLabel;
    @FXML private Label urgentLabel;

    // ── Advanced Stats ────────────────────────────────────────────────────────
    @FXML private Label resolutionRateLabel;
    @FXML private ProgressBar resolutionProgress;
    @FXML private Label topCategoryLabel;
    @FXML private Label topPriorityLabel;
    @FXML private Label avgPerDayLabel;
    @FXML private Label openLabel;

    // ── Filters ───────────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private Button filterAllBtn;
    @FXML private Button filterPendingBtn;
    @FXML private Button filterOpenBtn;
    @FXML private Button filterResolvedBtn;
    @FXML private Button filterUrgentBtn;

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colCategorie;
    @FXML private TableColumn<Reclamation, String> colPriorite;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, String> colDate;
    @FXML private TableColumn<Reclamation, Void>   colActions;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ReclamationService service = new ReclamationService();

    /** Master list — never replaced after first load. */
    private final ObservableList<Reclamation> masterList = FXCollections.observableArrayList();

    /** FilteredList wrapping the master list — predicate changes do NOT rebuild cells. */
    private FilteredList<Reclamation> filteredList;

    private ReclamationStatus currentStatusFilter = null;
    private boolean filterUrgentOnly = false;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Build filteredList once; bind it to the table once.
        filteredList = new FilteredList<>(masterList, r -> true);
        reclamationTable.setItems(filteredList);   // ← only called once, ever

        setupTable();
        setupCategoryCombo();
        loadData();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupCategoryCombo() {
        List<String> options = new ArrayList<>();
        options.add("Toutes");
        for (ReclamationCategory c : ReclamationCategory.values()) options.add(c.name());
        categoryFilterCombo.setItems(FXCollections.observableArrayList(options));
        categoryFilterCombo.setValue("Toutes");
    }

    private void loadData() {
        try {
            List<Reclamation> fresh = service.getAllReclamations();
            masterList.setAll(fresh);          // updates in-place — cells are NOT recycled
            updateCharts(fresh);
        } catch (Exception e) {
            e.printStackTrace();
            masterList.clear();
            updateCharts(Collections.emptyList());
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Impossible de charger les réclamations : connexion à la base de données indisponible.");
                alert.setHeaderText("Erreur de connexion");
                alert.showAndWait();
            });
        }
        applyPredicate();                  // re-apply current predicate after reload
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    /**
     * Rebuilds the FilteredList predicate from current state.
     * This is the ONLY way we filter — we never call setItems() again.
     */
    private void applyPredicate() {
        String search    = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String catFilter = categoryFilterCombo.getValue();
        ReclamationStatus statusSnap = currentStatusFilter;
        boolean urgentSnap           = filterUrgentOnly;

        filteredList.setPredicate(r -> {
            // 1. Status / urgent gate
            if (urgentSnap) {
                if (r.getPriority() == null) return false;
                String p = r.getPriority().name();
                if (!p.equalsIgnoreCase("HIGH") && !p.equalsIgnoreCase("URGENT")) return false;
            } else if (statusSnap != null) {
                if (r.getStatus() != statusSnap) return false;
            }

            // 2. Full-text search
            if (!search.isEmpty()) {
                String subj  = r.getSubject() != null ? r.getSubject().toLowerCase() : "";
                String email = (r.getAuthor() != null && r.getAuthor().getEmail() != null)
                               ? r.getAuthor().getEmail().toLowerCase() : "";
                if (!subj.contains(search) && !email.contains(search)) return false;
            }

            // 3. Category combo
            if (catFilter != null && !catFilter.equals("Toutes")) {
                if (r.getCategory() == null || !r.getCategory().name().equals(catFilter)) return false;
            }

            return true;
        });
    }

    @FXML public void handleSearch()         { applyPredicate(); }
    @FXML public void handleCategoryFilter() { applyPredicate(); }

    @FXML public void filterAll() {
        currentStatusFilter = null;
        filterUrgentOnly = false;
        setActiveFilter(filterAllBtn);
        applyPredicate();
    }

    @FXML public void filterPending() {
        currentStatusFilter = ReclamationStatus.PENDING;
        filterUrgentOnly = false;
        setActiveFilter(filterPendingBtn);
        applyPredicate();
    }

    @FXML public void filterOpen() {
        currentStatusFilter = ReclamationStatus.OPEN;
        filterUrgentOnly = false;
        setActiveFilter(filterOpenBtn);
        applyPredicate();
    }

    @FXML public void filterResolved() {
        currentStatusFilter = ReclamationStatus.RESOLVED;
        filterUrgentOnly = false;
        setActiveFilter(filterResolvedBtn);
        applyPredicate();
    }

    @FXML public void filterUrgent() {
        currentStatusFilter = null;
        filterUrgentOnly = true;
        setActiveFilter(filterUrgentBtn);
        applyPredicate();
    }

    private void setActiveFilter(Button active) {
        List<Button> all = List.of(filterAllBtn, filterPendingBtn, filterOpenBtn,
                                   filterResolvedBtn, filterUrgentBtn);
        for (Button btn : all) {
            btn.getStyleClass().remove("filter-toggle-active");
            if (!btn.getStyleClass().contains("filter-toggle")) {
                btn.getStyleClass().add("filter-toggle");
            }
        }
        active.getStyleClass().remove("filter-toggle");
        if (!active.getStyleClass().contains("filter-toggle-active")) {
            active.getStyleClass().add("filter-toggle-active");
        }
    }

    // ── Charts & Stats ────────────────────────────────────────────────────────

    private void updateCharts(List<Reclamation> data) {
        statusPieChart.getData().clear();
        categoryBarChart.getData().clear();
        priorityBarChart.getData().clear();
        activityLineChart.getData().clear();

        long pending  = data.stream().filter(r -> r.getStatus() == ReclamationStatus.PENDING).count();
        long resolved = data.stream().filter(r -> r.getStatus() == ReclamationStatus.RESOLVED).count();
        long open     = data.stream().filter(r -> r.getStatus() == ReclamationStatus.OPEN).count();
        long closed   = data.stream().filter(r -> r.getStatus() == ReclamationStatus.CLOSED).count();
        long urgent   = data.stream()
                            .filter(r -> r.getPriority() != null &&
                                         (r.getPriority().name().equalsIgnoreCase("HIGH") ||
                                          r.getPriority().name().equalsIgnoreCase("URGENT")))
                            .count();

        totalLabel.setText(String.valueOf(data.size()));
        pendingLabel.setText(String.valueOf(pending));
        resolvedLabel.setText(String.valueOf(resolved));
        urgentLabel.setText(String.valueOf(urgent));
        openLabel.setText(String.valueOf(open));

        double rate = data.isEmpty() ? 0.0 : (resolved + closed) * 100.0 / data.size();
        resolutionRateLabel.setText(String.format("%.0f %%", rate));
        resolutionProgress.setProgress(rate / 100.0);

        data.stream()
            .filter(r -> r.getCategory() != null)
            .collect(Collectors.groupingBy(r -> r.getCategory().name(), Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresentOrElse(e -> topCategoryLabel.setText(e.getKey()),
                             () -> topCategoryLabel.setText("N/A"));

        data.stream()
            .filter(r -> r.getPriority() != null)
            .collect(Collectors.groupingBy(r -> r.getPriority().name(), Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresentOrElse(e -> topPriorityLabel.setText(e.getKey()),
                             () -> topPriorityLabel.setText("N/A"));

        long recent = data.stream()
            .filter(r -> r.getCreatedAt() != null &&
                         r.getCreatedAt().isAfter(LocalDate.now().minusDays(14).atStartOfDay()))
            .count();
        avgPerDayLabel.setText(String.format("%.1f", recent / 14.0));

        if (pending  > 0) statusPieChart.getData().add(new PieChart.Data("En Attente", pending));
        if (resolved > 0) statusPieChart.getData().add(new PieChart.Data("Résolu",     resolved));
        if (open     > 0) statusPieChart.getData().add(new PieChart.Data("En Cours",   open));
        if (closed   > 0) statusPieChart.getData().add(new PieChart.Data("Fermé",      closed));

        XYChart.Series<String, Number> catSeries = new XYChart.Series<>();
        catSeries.setName("Tickets");
        data.stream()
            .filter(r -> r.getCategory() != null)
            .collect(Collectors.groupingBy(r -> r.getCategory().name(), Collectors.counting()))
            .forEach((cat, count) -> catSeries.getData().add(new XYChart.Data<>(cat, count)));
        categoryBarChart.getData().add(catSeries);

        XYChart.Series<String, Number> prioSeries = new XYChart.Series<>();
        prioSeries.setName("Priorité");
        Map<String, Long> prioMap = data.stream()
            .filter(r -> r.getPriority() != null)
            .collect(Collectors.groupingBy(r -> r.getPriority().name(), Collectors.counting()));
        for (String p : new String[]{"LOW", "MEDIUM", "HIGH", "URGENT"}) {
            if (prioMap.containsKey(p)) prioSeries.getData().add(new XYChart.Data<>(p, prioMap.get(p)));
        }
        priorityBarChart.getData().add(prioSeries);

        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Réclamations");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 13; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            long count = data.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(day))
                .count();
            lineSeries.getData().add(new XYChart.Data<>(day.format(dayFmt), count));
        }
        activityLineChart.getData().add(lineSeries);
    }

    // ── Table Setup ───────────────────────────────────────────────────────────

    private void setupTable() {

        colSujet.setCellValueFactory(cell -> {
            Reclamation r = cell.getValue();
            String auteur = (r.getAuthor() != null && r.getAuthor().getEmail() != null)
                            ? r.getAuthor().getEmail() : "Inconnu";
            return new SimpleStringProperty(r.getSubject() + "\n" + auteur);
        });

        colCategorie.setCellValueFactory(cell -> {
            String cat = cell.getValue().getCategory() != null
                         ? cell.getValue().getCategory().name() : "N/A";
            return new SimpleStringProperty(cat);
        });
        colCategorie.setCellFactory(p -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle("-fx-text-fill: -carthage-text-muted; -fx-font-size: 13px;"); }
            }
        });

        colPriorite.setCellValueFactory(cell -> {
            String prio = cell.getValue().getPriority() != null
                          ? cell.getValue().getPriority().name() : "MEDIUM";
            return new SimpleStringProperty(prio);
        });
        colPriorite.setCellFactory(p -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setStyle("-fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                String color = item.equalsIgnoreCase("HIGH") || item.equalsIgnoreCase("URGENT")
                               ? "rgba(229,9,20,0.12); -fx-text-fill: #FF4D4D;"
                               : item.equalsIgnoreCase("MEDIUM")
                                 ? "rgba(251,191,36,0.12); -fx-text-fill: #FBBF24;"
                                 : "rgba(74,222,128,0.12); -fx-text-fill: #4ADE80;";
                badge.setStyle(badge.getStyle() + "-fx-background-color: " + color);
                setGraphic(badge);
            }
        });

        colStatut.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus().name()));
        colStatut.setCellFactory(p -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label("• " + item);
                badge.getStyleClass().add("status-badge");
                if (item.equalsIgnoreCase("RESOLVED") || item.equalsIgnoreCase("CLOSED")) {
                    badge.getStyleClass().add("status-actif");
                } else if (item.equalsIgnoreCase("PENDING")) {
                    badge.setStyle("-fx-background-color: rgba(59,130,246,0.1); -fx-text-fill: #3B82F6; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else {
                    badge.setStyle("-fx-background-color: rgba(251,191,36,0.1); -fx-text-fill: #FBBF24; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                }
                setGraphic(badge);
            }
        });

        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm");
                return new SimpleStringProperty(cell.getValue().getCreatedAt().format(fmt));
            }
            return new SimpleStringProperty("");
        });

        // ── Actions column ────────────────────────────────────────────────────
        // setCellValueFactory is REQUIRED so JavaFX knows when to call updateItem.
        // Without it, cells that get recycled during predicate changes may skip updateItem.
        colActions.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(null));

        colActions.setCellFactory(p -> new TableCell<>() {
            private final Button viewBtn   = new Button("👁");
            private final Button deleteBtn = new Button("🗑");
            private final HBox   box       = new HBox(8, viewBtn, deleteBtn);
            {
                viewBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.setStyle("-fx-text-fill: #FF4D4D;");

                // Always read the item from the ROW, not from getIndex().
                // getIndex() is unreliable during filter transitions.
                viewBtn.setOnAction(e -> {
                    Reclamation r = getTableRow() != null ? (Reclamation) getTableRow().getItem() : null;
                    if (r != null) handleView(r);
                });
                deleteBtn.setOnAction(e -> {
                    Reclamation r = getTableRow() != null ? (Reclamation) getTableRow().getItem() : null;
                    if (r != null) handleDelete(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // Use getTableRow().getItem() — not index — to decide visibility.
                boolean show = !empty && getTableRow() != null && getTableRow().getItem() != null;
                setGraphic(show ? box : null);
            }
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleView(Reclamation r) {
        if (r == null)
            return;
        try {
            AdminMainLayoutController main =
                (AdminMainLayoutController) reclamationTable.getScene()
                    .lookup("#contentArea").getUserData();
            if (main != null) {
                ReclamationDetailsController ctrl = main.loadViewAndGetController(
                    "/com/carthage/view/admin/reclamation-details-view.fxml");
                if (ctrl != null) ctrl.setReclamationAndMainController(r.getId(), main);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Reclamation r) {
        if (r == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la réclamation ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(r.getId());
                    loadData(); // masterList.setAll() — no setItems() call
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void handleDownloadPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("rapport_reclamations_" + LocalDate.now().toString() + ".pdf");
        
        File file = fileChooser.showSaveDialog(reclamationTable.getScene().getWindow());
        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                
                // --- Title ---
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.BLACK);
                Paragraph title = new Paragraph("Rapport des Réclamations - Carthage", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                title.setSpacingAfter(10);
                document.add(title);
                
                // --- Meta Info & Stats ---
                Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.DARK_GRAY);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
                
                Paragraph metaDate = new Paragraph("Date de génération : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), metaFont);
                metaDate.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                metaDate.setSpacingAfter(20);
                document.add(metaDate);
                
                Paragraph statsTitle = new Paragraph("Statistiques Générales :", boldFont);
                statsTitle.setSpacingAfter(10);
                document.add(statsTitle);
                
                // Add KPI values from the labels
                document.add(new Paragraph("• Total des réclamations : " + totalLabel.getText(), metaFont));
                document.add(new Paragraph("• En attente : " + pendingLabel.getText(), metaFont));
                document.add(new Paragraph("• Résolu : " + resolvedLabel.getText(), metaFont));
                document.add(new Paragraph("• Urgent : " + urgentLabel.getText(), metaFont));
                document.add(new Paragraph("• Taux de résolution : " + resolutionRateLabel.getText(), metaFont));
                
                Paragraph spacing = new Paragraph(" ");
                spacing.setSpacingAfter(15);
                document.add(spacing);
                
                // --- Table ---
                Paragraph tableTitle = new Paragraph("Détails des Réclamations (Vue Actuelle) :", boldFont);
                tableTitle.setSpacingAfter(10);
                document.add(tableTitle);
                
                PdfPTable table = new PdfPTable(6); // Changed from 5 to 6 for the Date column
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2.5f, 2f, 1.5f, 1.2f, 1.2f, 1.5f});
                
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
                String[] headers = {"Sujet", "Auteur", "Catégorie", "Priorité", "Statut", "Date"};
                for (String headerText : headers) {
                    PdfPCell header = new PdfPCell(new Phrase(headerText, headerFont));
                    header.setBackgroundColor(new BaseColor(229, 9, 20)); // Carthage Red
                    header.setPadding(8);
                    header.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    table.addCell(header);
                }
                
                Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
                
                List<Reclamation> itemsToExport = filteredList; // Export currently filtered items
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                
                for (Reclamation r : itemsToExport) {
                    String sujet = r.getSubject() != null ? r.getSubject() : "N/A";
                    String auteur = (r.getAuthor() != null && r.getAuthor().getEmail() != null) ? r.getAuthor().getEmail() : "Inconnu";
                    String cat = r.getCategory() != null ? r.getCategory().name() : "N/A";
                    String prio = r.getPriority() != null ? r.getPriority().name() : "N/A";
                    String statut = r.getStatus() != null ? r.getStatus().name() : "N/A";
                    String date = r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "N/A";
                    
                    table.addCell(new Phrase(sujet, cellFont));
                    table.addCell(new Phrase(auteur, cellFont));
                    table.addCell(new Phrase(cat, cellFont));
                    table.addCell(new Phrase(prio, cellFont));
                    table.addCell(new Phrase(statut, cellFont));
                    table.addCell(new Phrase(date, cellFont));
                }
                
                document.add(table);
                document.close();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le rapport PDF a été généré avec succès !");
                alert.setHeaderText(null);
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la création du PDF : " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
