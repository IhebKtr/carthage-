package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Order;
import com.carthagearena.service.MerchService;
import com.carthagearena.service.OrderService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StatsController implements Initializable {

    @FXML private Label lblTotalRevenue;
    @FXML private Label lblTotalOrders;
    @FXML private Label lblTotalProducts;

    @FXML private BarChart<String, Number> barChartSales;
    @FXML private PieChart pieChartTypes;
    @FXML private PieChart pieChartGender;
    @FXML private BarChart<String, Number> barChartAge;

    private final OrderService orderService = new OrderService();
    private final MerchService merchService = new MerchService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<Order> orders = orderService.findAll();
            List<Merch> merchs = merchService.findAll();

            // KPIs
            int revenue = orders.stream()
                    .filter(o -> o.getStatus() == Order.Status.PAID)
                    .mapToInt(Order::getTotalAmount)
                    .sum();
            lblTotalRevenue.setText(String.format("%.2f DT", revenue / 100.0));
            lblTotalOrders.setText(String.valueOf(orders.size()));
            lblTotalProducts.setText(String.valueOf(merchs.size()));

            // Graphique 1 : Top produits (Stock restant)
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Stock");
            // On prend les 5 produits avec le plus de stock
            merchs.stream()
                  .sorted((m1, m2) -> Integer.compare(m2.getStock(), m1.getStock()))
                  .limit(5)
                  .forEach(m -> series.getData().add(new XYChart.Data<>(
                          m.getName().length() > 15 ? m.getName().substring(0, 15) + "..." : m.getName(),
                          m.getStock())));
            
            barChartSales.getData().clear();
            barChartSales.getData().add(series);

            // Graphique 2 : Répartition par type de produit
            Map<String, Long> typesCount = merchs.stream()
                    .collect(Collectors.groupingBy(Merch::getType, Collectors.counting()));
            
            pieChartTypes.getData().clear();
            typesCount.forEach((type, count) -> {
                pieChartTypes.getData().add(new PieChart.Data(type + " (" + count + ")", count));
            });

            // Graphique 3 : Répartition par Genre
            Map<String, Integer> genderStats = orderService.getGenderStats();
            pieChartGender.getData().clear();
            genderStats.forEach((gender, count) -> {
                pieChartGender.getData().add(new PieChart.Data(gender, count));
            });

            // Graphique 4 : Répartition par Âge
            Map<String, Integer> ageStats = orderService.getAgeStats();
            XYChart.Series<String, Number> ageSeries = new XYChart.Series<>();
            ageSeries.setName("Utilisateurs");
            ageStats.forEach((group, count) -> {
                ageSeries.getData().add(new XYChart.Data<>(group, count));
            });
            barChartAge.getData().clear();
            barChartAge.getData().add(ageSeries);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des statistiques : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
