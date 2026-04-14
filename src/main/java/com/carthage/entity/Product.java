package com.carthage.entity;

import java.util.Objects;

public class Product {

    private Integer id;
    private String productType;
    private String name;
    private String description;
    private int pricePoints;
    private boolean available;

    public Product() {}

    public Product(Integer id, String productType, String name, String description, int pricePoints, boolean available) {
        this.id = id;
        this.productType = productType;
        this.name = name;
        this.description = description;
        this.pricePoints = pricePoints;
        this.available = available;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPricePoints() {
        return pricePoints;
    }

    public void setPricePoints(int pricePoints) {
        this.pricePoints = pricePoints;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product that = (Product) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(productType, that.productType) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                pricePoints == that.pricePoints &&
                available == that.available;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productType, name, description, pricePoints, available);
    }
}
