package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.UUID;

public class TournoisController {

    @FXML private FlowPane tournoisGrid;
    @FXML private TextField searchField;
    @FXML private ToggleButton btnAll, btnMine, btnDone;
    
    @FXML private AnchorPane gridPaneView;
    @FXML private VBox calendarPaneView;
    @FXML private GridPane calendarGrid;
    @FXML private Label monthYearLabel;
    @FXML private Button btnGridMode, btnCalendarMode;

    private Connection connection;
    private String activeFilter = "ALL"; // ALL | MINE | DONE
    private java.time.YearMonth currentMonth = java.time.YearMonth.now();

    private static class TournoiData {
        UUID id; String nom; Timestamp dateDebut;
        int maxTeams, currentTeams, prizePool;
        String type, status;
        boolean isMine;
        public TournoiData(UUID id, String nom, Timestamp dateDebut, int maxTeams, int currentTeams, int prizePool, String type, String status, boolean isMine) {
            this.id = id; this.nom = nom; this.dateDebut = dateDebut; this.maxTeams = maxTeams;
            this.currentTeams = currentTeams; this.prizePool = prizePool; this.type = type; this.status = status; this.isMine = isMine;
        }
    }

    @FXML
    public void initialize() {
        connection = DatabaseConnection.getInstance().getConnection();
        loadTournois();
        searchField.textProperty().addListener((obs, old, val) -> loadTournois());
    }

    @FXML public void onFilterAll()  { activeFilter = "ALL";  updateToggle(btnAll);  loadTournois(); }
    @FXML public void onFilterMine() { activeFilter = "MINE"; updateToggle(btnMine); loadTournois(); }
    @FXML public void onFilterDone() { activeFilter = "DONE"; updateToggle(btnDone); loadTournois(); }

    private void updateToggle(ToggleButton active) {
        for (ToggleButton b : new ToggleButton[]{btnAll, btnMine, btnDone}) {
            b.getStyleClass().removeAll("filter-toggle-active");
            if (!b.getStyleClass().contains("filter-toggle")) b.getStyleClass().add("filter-toggle");
        }
        active.getStyleClass().removeAll("filter-toggle");
        if (!active.getStyleClass().contains("filter-toggle-active")) active.getStyleClass().add("filter-toggle-active");
    }

    @FXML public void onModeGrid() {
        gridPaneView.setVisible(true);
        gridPaneView.setManaged(true);
        calendarPaneView.setVisible(false);
        calendarPaneView.setManaged(false);
        btnGridMode.getStyleClass().clear();
        btnGridMode.getStyleClass().add("button-solid-red");
        btnCalendarMode.getStyleClass().clear();
        btnCalendarMode.getStyleClass().add("filter-toggle");
    }

    @FXML public void onModeCalendar() {
        gridPaneView.setVisible(false);
        gridPaneView.setManaged(false);
        calendarPaneView.setVisible(true);
        calendarPaneView.setManaged(true);
        btnCalendarMode.getStyleClass().clear();
        btnCalendarMode.getStyleClass().add("button-solid-red");
        btnGridMode.getStyleClass().clear();
        btnGridMode.getStyleClass().add("filter-toggle");
    }

    @FXML public void onPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        loadTournois();
    }

    @FXML public void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        loadTournois();
    }

    private void loadTournois() {
        tournoisGrid.getChildren().clear();
        User user = com.carthage.utils.SessionContext.getInstance().getCurrentUser();
        String search = searchField.getText();
        boolean hasSearch = search != null && !search.isBlank();

        // 1. Base Query
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT HEX(t.id) as hex_id, t.id, t.nom, t.date_debut, t.nb_equipes_max, t.prize_pool, t.status, t.type, " +
            "  (SELECT COUNT(*) FROM tournoi_team tt2 WHERE tt2.tournoi_id = t.id) AS current_teams "
        );
        if (user != null) {
            sql.append(", (SELECT COUNT(*) FROM tournoi_team tt3 JOIN team_membership tm3 ON tt3.team_id = tm3.team_id WHERE tt3.tournoi_id = t.id AND tm3.player_id = UNHEX(REPLACE(?, '-', ''))) > 0 AS is_mine ");
        } else {
            sql.append(", 0 AS is_mine ");
        }
        sql.append("FROM tournoi t ");

        // 2. Joins for filtering
        if ("MINE".equals(activeFilter)) {
            sql.append("JOIN tournoi_team tt ON t.id = tt.tournoi_id ");
            sql.append("JOIN team_membership tm ON tt.team_id = tm.team_id ");
        }

        sql.append("WHERE 1=1 ");

        // 3. Conditions
        if ("DONE".equals(activeFilter)) sql.append(" AND t.status = 'COMPLETED'");
        else if ("ALL".equals(activeFilter)) sql.append(" AND t.status != 'COMPLETED'");
        
        if ("MINE".equals(activeFilter) && user != null) {
            sql.append(" AND tm.player_id = UNHEX(REPLACE(?, '-', '')) ");
        }
        
        if (hasSearch) sql.append(" AND t.nom LIKE ?");
        
        sql.append(" ORDER BY t.date_debut DESC LIMIT 20");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (user != null) {
                ps.setString(paramIdx++, user.getId().toString()); // For the is_mine subquery
            }
            if ("MINE".equals(activeFilter) && user != null) {
                ps.setString(paramIdx++, user.getId().toString());
            }
            if (hasSearch) ps.setString(paramIdx++, "%" + search + "%");
            
            ResultSet rs = ps.executeQuery();
            java.util.List<TournoiData> tournois = new java.util.ArrayList<>();

            while (rs.next()) {
                UUID tid = com.carthage.utils.UUIDUtils.fromBytes(rs.getBytes("id"));
                tournois.add(new TournoiData(
                    tid, rs.getString("nom"), rs.getTimestamp("date_debut"),
                    rs.getInt("nb_equipes_max"), rs.getInt("current_teams"),
                    rs.getInt("prize_pool"), rs.getString("type"), rs.getString("status"),
                    rs.getBoolean("is_mine")
                ));
            }
            
            for (TournoiData t : tournois) {
                tournoisGrid.getChildren().add(buildCard(
                    t.id, t.nom, t.dateDebut, t.maxTeams, t.currentTeams, t.prizePool, t.type, t.status, null
                ));
            }
            
            if (tournoisGrid.getChildren().isEmpty()) {
                Label empty = new Label("Aucun tournoi trouvé.");
                empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20 0;");
                tournoisGrid.getChildren().add(empty);
            }
            
            buildCalendar(tournois);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur DB: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #FF4D4D; -fx-font-size: 13px;");
        tournoisGrid.getChildren().add(err);
    }

    private VBox buildCard(UUID id, String nom, Timestamp dateDebut, int maxTeams, int currentTeams,
                           int prizePool, String type, String status, String imageUrl) {
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E2633; -fx-background-radius: 12px; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;"));

        // ── Banner ── (image is the hero — keep it clear and visible)
        final double BANNER_W = 300;
        final double BANNER_H = 170;
        StackPane banner = new StackPane();
        banner.setPrefHeight(BANNER_H);
        banner.setMinHeight(BANNER_H);
        banner.setStyle("-fx-background-radius: 12px 12px 0 0; -fx-background-color: #1E2633;");

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(imageUrl, BANNER_W * 2, BANNER_H * 2, true, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(BANNER_W);
                iv.setFitHeight(BANNER_H);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                iv.setCache(true);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(BANNER_W, BANNER_H);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                iv.setClip(clip);
                banner.getChildren().add(iv);
            } catch (Exception e) {}
        }

        // Light bottom-only vignette: keeps the image clear while ensuring
        // badge & status text remain readable. Top 60% of the image is untouched.
        Region overlay = new Region();
        String accent = "VALORANT".equalsIgnoreCase(type)
            ? "rgba(229,9,20,0.55)"
            : "ONGOING".equalsIgnoreCase(status)
            ? "rgba(255,102,0,0.55)"
            : "rgba(20,26,35,0.75)";
        String bannerGrad = "linear-gradient(to bottom, transparent 0%, transparent 55%, " + accent + " 100%)";
        overlay.setStyle("-fx-background-color: " + bannerGrad + "; -fx-background-radius: 12px 12px 0 0;");
        overlay.setMouseTransparent(true);
        banner.getChildren().add(overlay);

        // Status badge color
        String statusColor = switch (status != null ? status.toUpperCase() : "UPCOMING") {
            case "ONGOING"   -> "#F59E0B";
            case "COMPLETED" -> "#6b7280";
            default          -> "#22C55E";
        };
        String statusText = switch (status != null ? status.toUpperCase() : "UPCOMING") {
            case "ONGOING"   -> "ONGOING";
            case "COMPLETED" -> "TERMINÉ";
            default          -> "UPCOMING";
        };
        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-background-color: " + statusColor +
            "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;" +
            " -fx-background-radius: 4px; -fx-padding: 3 8;" +
            " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 4, 0.2, 0, 1);");

        Label typeBadge = new Label(type != null ? type.toUpperCase() : "—");
        typeBadge.setStyle("-fx-background-color: rgba(20,26,35,0.85); -fx-text-fill: #E5E7EB;" +
            " -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3 8;" +
            " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 4, 0.2, 0, 1);");

        HBox badges = new HBox(6, statusBadge, typeBadge);
        badges.setAlignment(Pos.TOP_LEFT);
        badges.setPadding(new Insets(10));
        StackPane.setAlignment(badges, Pos.TOP_LEFT);
        banner.getChildren().add(badges);

        // ── Body ──
        VBox body = new VBox(7);
        body.setPadding(new Insets(2, 14, 14, 14));

        Label nameLabel = new Label(nom != null ? nom : "—");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        String dateStr = dateDebut != null
            ? dateDebut.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "TBD";
        Label info = new Label("📅 " + dateStr + "   👥 " + currentTeams + "/" + maxTeams + "   🏆 élimination");
        info.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        Label prizeTitle = new Label("PRIZE POOL");
        prizeTitle.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10px;");
        Label prizeVal = new Label(prizePool + " DT");
        prizeVal.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 16px;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btn = new Button("Voir Détails →");
        btn.setStyle("-fx-background-color: #1E2633; -fx-text-fill: white;" +
            " -fx-background-radius: 20px; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 12px;");
        btn.setOnAction(e -> {
            MainLayoutController mlc = (MainLayoutController) card.getScene().lookup("#contentArea").getUserData();
            mlc.loadTournoiDetail(id);
        });

        HBox bottom = new HBox(10, new VBox(2, prizeTitle, prizeVal), spacer, btn);
        bottom.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(nameLabel, info, bottom);
        card.getChildren().addAll(banner, body);
        return card;
    }

    private void buildCalendar(java.util.List<TournoiData> tournois) {
        calendarGrid.getChildren().clear();
        
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(days[i]);
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-background-color: #1A212D; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 10;");
            calendarGrid.add(l, i, 0);
        }

        java.time.LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); 
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 1;
        int col = dayOfWeek - 1;

        for (int day = 1; day <= daysInMonth; day++) {
            VBox dayCell = new VBox(5);
            dayCell.setStyle("-fx-background-color: #141A23; -fx-padding: 8;");
            dayCell.setMinHeight(100);
            
            Label dayLbl = new Label(String.valueOf(day));
            dayLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
            
            if (currentMonth.equals(java.time.YearMonth.now()) && day == java.time.LocalDate.now().getDayOfMonth()) {
                dayLbl.setStyle("-fx-text-fill: white; -fx-background-color: #E50914; -fx-background-radius: 10; -fx-padding: 2 6;");
            }
            
            dayCell.getChildren().add(dayLbl);
            
            java.time.LocalDate currentDay = currentMonth.atDay(day);
            
            // Conflict Detection Logic
            int myTournamentsOnThisDay = 0;
            for (TournoiData t : tournois) {
                if (t.dateDebut != null && t.dateDebut.toLocalDateTime().toLocalDate().equals(currentDay) && t.isMine) {
                    myTournamentsOnThisDay++;
                }
            }
            
            if (myTournamentsOnThisDay > 1) {
                dayCell.setStyle("-fx-background-color: rgba(229, 9, 20, 0.15); -fx-padding: 8; -fx-border-color: #E50914; -fx-border-width: 1; -fx-border-radius: 4;");
                Label conflictWarn = new Label("⚠️ Conflit d'Horaire");
                conflictWarn.setStyle("-fx-text-fill: #E50914; -fx-font-size: 9px; -fx-font-weight: bold;");
                dayCell.getChildren().add(conflictWarn);
            }
            
            for (TournoiData t : tournois) {
                if (t.dateDebut != null && t.dateDebut.toLocalDateTime().toLocalDate().equals(currentDay)) {
                    Label tLbl = new Label(t.nom);
                    tLbl.setWrapText(true);
                    tLbl.setMaxWidth(Double.MAX_VALUE);
                    
                    // Smart Tooltip
                    Tooltip tooltip = new Tooltip(
                        t.nom + "\n" +
                        "Jeu: " + (t.type != null ? t.type.toUpperCase() : "N/A") + "\n" +
                        "Prize Pool: " + t.prizePool + " DT\n" +
                        "Équipes: " + t.currentTeams + "/" + t.maxTeams + "\n" +
                        "Statut: " + (t.status != null ? t.status : "UPCOMING")
                    );
                    tooltip.setStyle("-fx-background-color: #1E2633; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 10px;");
                    Tooltip.install(tLbl, tooltip);
                    
                    // Smart Color Coding by Game Type & Status
                    String baseColor = "#22C55E"; // Green by default
                    if ("VALORANT".equalsIgnoreCase(t.type)) baseColor = "#E50914";
                    else if ("LEAGUE OF LEGENDS".equalsIgnoreCase(t.type) || "LOL".equalsIgnoreCase(t.type)) baseColor = "#0EA5E9";
                    else if ("CSGO".equalsIgnoreCase(t.type) || "CS:GO".equalsIgnoreCase(t.type)) baseColor = "#F59E0B";
                    
                    if ("COMPLETED".equalsIgnoreCase(t.status)) baseColor = "#6b7280";
                    else if ("ONGOING".equalsIgnoreCase(t.status)) baseColor = "#F59E0B";

                    // Highlight if user is participating
                    String borderStyle = t.isMine ? "; -fx-border-color: #FCD34D; -fx-border-width: 1.5px; -fx-border-radius: 4px;" : "";
                    
                    String normalStyle = "-fx-background-color: " + baseColor + "22; -fx-text-fill: " + baseColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 6; -fx-background-radius: 4; -fx-cursor: hand;" + borderStyle;
                    String hoverStyle = "-fx-background-color: " + baseColor + "44; -fx-text-fill: " + baseColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 6; -fx-background-radius: 4; -fx-cursor: hand;" + borderStyle;
                    
                    tLbl.setStyle(normalStyle);
                    tLbl.setOnMouseEntered(e -> tLbl.setStyle(hoverStyle));
                    tLbl.setOnMouseExited(e -> tLbl.setStyle(normalStyle));
                    
                    tLbl.setOnMouseClicked(e -> {
                        if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                            MainLayoutController mlc = (MainLayoutController) calendarGrid.getScene().lookup("#contentArea").getUserData();
                            mlc.loadTournoiDetail(t.id);
                        }
                    });
                    
                    // Smart Context Menu (Phase 3 - modified for PDF)
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem detailsItem = new MenuItem("Voir les Détails");
                    detailsItem.setOnAction(ev -> {
                        MainLayoutController mlc = (MainLayoutController) calendarGrid.getScene().lookup("#contentArea").getUserData();
                        mlc.loadTournoiDetail(t.id);
                    });
                    
                    MenuItem exportPdfItem = new MenuItem("Sauvegarder les infos (PDF)");
                    exportPdfItem.setOnAction(ev -> {
                        com.carthage.utils.PdfUtils.exportTournamentToPdf(
                            calendarGrid.getScene().getWindow(), 
                            t.nom, t.dateDebut, t.maxTeams, t.currentTeams, t.prizePool, t.type, t.status
                        );
                    });
                    
                    contextMenu.getItems().addAll(detailsItem, exportPdfItem);
                    tLbl.setOnContextMenuRequested(e -> contextMenu.show(tLbl, e.getScreenX(), e.getScreenY()));
                    
                    dayCell.getChildren().add(tLbl);
                }
            }
            
            calendarGrid.add(dayCell, col, row);
            
            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
        
        String[] months = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        monthYearLabel.setText(months[currentMonth.getMonthValue()-1] + " " + currentMonth.getYear());
    }
}
