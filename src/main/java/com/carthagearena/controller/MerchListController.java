package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Game;
import com.carthagearena.service.MerchService;
import com.carthagearena.util.MerchValidator;
import com.carthagearena.util.MerchValidator.ValidationResult;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur de la liste des produits Merch (vue principale)
 * Inclut : filtrage, tri, statistiques avancées, badges de statut
 */
public class MerchListController implements Initializable {

    // ─── Table ───────────────────────────────────────────────────────────────
    @FXML private TableView<Merch> tableMerch;
    @FXML private TableColumn<Merch, String> colName;
    @FXML private TableColumn<Merch, String> colType;
    @FXML private TableColumn<Merch, String> colPrice;
    @FXML private TableColumn<Merch, Integer> colStock;
    @FXML private TableColumn<Merch, Void> colStatus;
    @FXML private TableColumn<Merch, Void> colActions;

    // ─── Filtres ─────────────────────────────────────────────────────────────
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbTypeFilter;
    @FXML private ComboBox<String> cbSort;

    // ─── Stats ───────────────────────────────────────────────────────────────
    @FXML private Label lblTotalProducts;
    @FXML private Label lblTotalStock;
    @FXML private Label lblOutOfStock;
    @FXML private Label lblTotalValue;

    private final MerchService merchService = new MerchService();
    private ObservableList<Merch> masterList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        setupFilters();
        setupSort();
        loadData();
    }

    // ─── Configuration colonnes ───────────────────────────────────────────────

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Prix formaté
        colPrice.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getPriceFormatted()));

        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Colonne stock colorée (rouge si 0, jaune si < 5)
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(stock));
                    if (stock == 0) {
                        setStyle("-fx-text-fill: #FF4D4D; -fx-font-weight: bold;");
                    } else if (stock < 5) {
                        setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #4ADE80;");
                    }
                }
            }
        });

        // Colonne statut avec badges
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Merch merch = getTableView().getItems().get(getIndex());
                    Label badge = new Label();
                    badge.getStyleClass().add("status-badge");

                    if (merch.getStock() == 0) {
                        badge.setText("RUPTURE");
                        badge.getStyleClass().add("status-out-of-stock");
                    } else if (merch.getStock() < 5) {
                        badge.setText("STOCK BAS");
                        badge.getStyleClass().add("status-low-stock");
                    } else {
                        badge.setText("EN STOCK");
                        badge.getStyleClass().add("status-in-stock");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Colonne actions (Modifier / Supprimer)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏️ Modifier");
            private final Button btnDelete = new Button("🗑️ Supprimer");

            {
                btnEdit.getStyleClass().addAll("btn", "btn-warning");
                btnDelete.getStyleClass().addAll("btn", "btn-danger");

                btnEdit.setOnAction(e -> {
                    Merch merch = getTableView().getItems().get(getIndex());
                    openEditDialog(merch);
                });

                btnDelete.setOnAction(e -> {
                    Merch merch = getTableView().getItems().get(getIndex());
                    confirmDelete(merch);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8, btnEdit, btnDelete);
                    setGraphic(hbox);
                }
            }
        });
    }

    // ─── Filtres ─────────────────────────────────────────────────────────────

    private void setupFilters() {
        cbTypeFilter.setItems(FXCollections.observableArrayList(
                "Tous", "shirt", "cap", "jersey", "poster", "accessory"
        ));
        cbTypeFilter.setValue("Tous");

        tfSearch.textProperty().addListener((obs, old, newVal) -> applyFilterAndSort());
        cbTypeFilter.valueProperty().addListener((obs, old, newVal) -> applyFilterAndSort());
    }

    // ─── Tri ─────────────────────────────────────────────────────────────────

    private void setupSort() {
        cbSort.setItems(FXCollections.observableArrayList(
                "Date (récent)", "Date (ancien)",
                "Nom (A-Z)", "Nom (Z-A)",
                "Prix (croissant)", "Prix (décroissant)",
                "Stock (croissant)", "Stock (décroissant)"
        ));
        cbSort.setValue("Date (récent)");
        cbSort.valueProperty().addListener((obs, old, newVal) -> applyFilterAndSort());
    }

    private void applyFilterAndSort() {
        String keyword = tfSearch.getText() != null ? tfSearch.getText().toLowerCase().trim() : "";
        String type = cbTypeFilter.getValue();
        String sort = cbSort.getValue();

        // Filtrage
        List<Merch> filtered = masterList.stream()
                .filter(m -> {
                    boolean nameMatch = keyword.isEmpty() || m.getName().toLowerCase().contains(keyword);
                    boolean descMatch = keyword.isEmpty() ||
                            (m.getDescription() != null && m.getDescription().toLowerCase().contains(keyword));
                    boolean typeMatch = "Tous".equals(type) || type.equals(m.getType());
                    return (nameMatch || descMatch) && typeMatch;
                })
                .collect(Collectors.toList());

        // Tri
        Comparator<Merch> comparator = getComparator(sort);
        if (comparator != null) {
            filtered.sort(comparator);
        }

        tableMerch.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Merch> getComparator(String sortOption) {
        if (sortOption == null) return null;
        return switch (sortOption) {
            case "Date (récent)" -> Comparator.comparing(Merch::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case "Date (ancien)" -> Comparator.comparing(Merch::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Nom (A-Z)" -> Comparator.comparing(Merch::getName, String.CASE_INSENSITIVE_ORDER);
            case "Nom (Z-A)" -> Comparator.comparing(Merch::getName, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Prix (croissant)" -> Comparator.comparingInt(Merch::getPrice);
            case "Prix (décroissant)" -> Comparator.comparingInt(Merch::getPrice).reversed();
            case "Stock (croissant)" -> Comparator.comparingInt(Merch::getStock);
            case "Stock (décroissant)" -> Comparator.comparingInt(Merch::getStock).reversed();
            default -> null;
        };
    }

    // ─── Chargement données ───────────────────────────────────────────────────

    private void loadData() {
        try {
            List<Merch> merchs = merchService.findAll();
            masterList = FXCollections.observableArrayList(merchs);
            tableMerch.setItems(masterList);
            updateStats();
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void updateStats() {
        lblTotalProducts.setText("Produits : " + masterList.size());

        int totalStock = masterList.stream().mapToInt(Merch::getStock).sum();
        lblTotalStock.setText("Stock total : " + totalStock);

        long outOfStock = masterList.stream().filter(m -> m.getStock() == 0).count();
        lblOutOfStock.setText("Rupture : " + outOfStock);

        double totalValue = masterList.stream()
                .mapToDouble(m -> (m.getPrice() / 100.0) * m.getStock())
                .sum();
        lblTotalValue.setText(String.format("Valeur : %.2f DT", totalValue));
    }

    // ─── Boutons ─────────────────────────────────────────────────────────────

    @FXML
    private void onAddMerch() {
        openEditDialog(null);
    }

    @FXML
    private void onRefresh() {
        tfSearch.clear();
        cbTypeFilter.setValue("Tous");
        cbSort.setValue("Date (récent)");
        loadData();
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private void openEditDialog(Merch merch) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MerchFormView.fxml"));
            Parent root = loader.load();

            MerchFormController controller = loader.getController();
            controller.initData(merch, this::loadData);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(merch == null ? "➕ Ajouter un produit" : "✏️ Modifier le produit");
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.showAndWait();
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void confirmDelete(Merch merch) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer « " + merch.getName() + " » ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                merchService.delete(merch.getId());
                loadData();
                showInfo("Succès", "Produit supprimé avec succès ✅");
            } catch (SQLException e) {
                showError("Erreur de suppression", e.getMessage());
            }
        }
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
