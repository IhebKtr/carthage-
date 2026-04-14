package com.carthage.controllers.admin;

import com.carthage.entity.Reclamation;
import com.carthage.services.ReclamationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReclamationManagementController {

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private LineChart<String, Number> activityLineChart;
    
    @FXML private Label totalLabel;
    @FXML private Label pendingLabel;
    @FXML private Label urgentLabel;
    
    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colCategorie;
    @FXML private TableColumn<Reclamation, String> colPriorite;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, String> colDate;
    @FXML private TableColumn<Reclamation, Void> colActions;

    private final ReclamationService service = new ReclamationService();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colSujet.setCellValueFactory(cell -> {
            Reclamation r = cell.getValue();
            String auteur = (r.getAuthor() != null && r.getAuthor().getEmail() != null) ? r.getAuthor().getEmail() : "Inconnu";
            return new SimpleStringProperty(r.getSubject() + "\n" + auteur);
        });

        colCategorie.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: -carthage-text-muted; -fx-font-size: 13px;");
                }
            }
        });
        colCategorie.setCellValueFactory(cell -> {
            String cat = cell.getValue().getCategory() != null ? cell.getValue().getCategory().name() : "N/A";
            return new SimpleStringProperty(cat);
        });
            
        colPriorite.setCellValueFactory(cell -> {
            String prio = cell.getValue().getPriority() != null ? cell.getValue().getPriority().name() : "MEDIUM";
            return new SimpleStringProperty(prio);
        });
        colPriorite.setCellFactory(param -> new TableCell<>() {
             @Override
             protected void updateItem(String item, boolean empty) {
                 super.updateItem(item, empty);
                 if (empty || item == null) {
                     setGraphic(null);
                 } else {
                     Label badge = new Label(item);
                     badge.setStyle("-fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                     if (item.equalsIgnoreCase("HIGH") || item.equalsIgnoreCase("URGENT")) {
                         badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(229, 9, 20, 0.1); -fx-text-fill: #FF4D4D;");
                     } else if (item.equalsIgnoreCase("MEDIUM")) {
                         badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(251, 191, 36, 0.1); -fx-text-fill: #FBBF24;");
                     } else {
                         badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(74, 222, 128, 0.1); -fx-text-fill: #4ADE80;");
                     }
                     setGraphic(badge);
                 }
             }
        });

        colStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        colStatut.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label("• " + item);
                    badge.getStyleClass().add("status-badge");
                    if (item.equalsIgnoreCase("RESOLVED") || item.equalsIgnoreCase("RESOLU")) {
                        badge.getStyleClass().add("status-actif");
                    } else if (item.equalsIgnoreCase("PENDING") || item.equalsIgnoreCase("EN_ATTENTE")) {
                         badge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.1); -fx-text-fill: #3B82F6; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                         badge.setStyle("-fx-background-color: rgba(251, 191, 36, 0.1); -fx-text-fill: #FBBF24; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                }
            }
        });

        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm");
                return new SimpleStringProperty(cell.getValue().getCreatedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("👁");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(8, viewBtn, deleteBtn);
            {
                viewBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.setStyle("-fx-text-fill: #FF4D4D;");
                
                viewBtn.setOnAction(e -> handleView(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void loadData() {
        List<Reclamation> data = service.getAllReclamations();
        reclamationTable.setItems(FXCollections.observableArrayList(data));
        
        updateCharts(data);
    }

    private void updateCharts(List<Reclamation> data) {
        statusPieChart.getData().clear();
        categoryBarChart.getData().clear();
        
        long pending = data.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().name().contains("PENDING"))
                .count();
        long urgent = data.stream()
                .filter(r -> r.getPriority() != null && (r.getPriority().name().contains("HIGH") || r.getPriority().name().contains("URGENT")))
                .count();
        long resolved = data.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().name().contains("RESOLVED"))
                .count();
        long other = data.size() - pending - resolved;
        
        totalLabel.setText(String.valueOf(data.size()));
        pendingLabel.setText(String.valueOf(pending));
        urgentLabel.setText(String.valueOf(urgent));
        
        if (pending > 0) statusPieChart.getData().add(new PieChart.Data("En Attente", pending));
        if (resolved > 0) statusPieChart.getData().add(new PieChart.Data("Résolu", resolved));
        if (other > 0) statusPieChart.getData().add(new PieChart.Data("En Cours", other));

        // Simplified Category Chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tickets");
        data.stream()
            .filter(r -> r.getCategory() != null)
            .map(r -> r.getCategory().name())
            .distinct()
            .forEach(cat -> {
                long count = data.stream().filter(r -> r.getCategory() != null && r.getCategory().name().equals(cat)).count();
                series.getData().add(new XYChart.Data<>(cat, count));
            });
        categoryBarChart.getData().add(series);
    }

    private void handleView(Reclamation r) {
        if (r == null) return;
        try {
            AdminMainLayoutController main = (AdminMainLayoutController) reclamationTable.getScene().lookup("#contentArea").getUserData();
            if (main != null) {
                ReclamationDetailsController controller = main.loadViewAndGetController("/com/carthage/view/admin/reclamation-details-view.fxml");
                if (controller != null) {
                    controller.setReclamationAndMainController(r.getId(), main);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Reclamation r) {
        if (r == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la réclamation ?");
        confirm.showAndWait().ifPresent(response -> {
             if (response == ButtonType.OK) {
                 try {
                     service.delete(r.getId());
                     loadData();
                 } catch (Exception ex) {
                     ex.printStackTrace();
                 }
             }
        });
    }

}
