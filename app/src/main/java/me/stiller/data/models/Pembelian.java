package me.stiller.data.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import static me.stiller.utils.Helper.formatPrice;

public class Pembelian extends RecursiveTreeObject<Pembelian> {
    StringProperty orderId = new SimpleStringProperty();
    StringProperty orderDate = new SimpleStringProperty();
    StringProperty items = new SimpleStringProperty();

    public Pembelian() {}

    public Pembelian(String orderId, String orderDate, String items) {
        setOrderId(orderId);
        setOrderDate(orderDate);
        setItems(items);
    }

    public String getOrderId() {
        return orderId.get();
    }

    public StringProperty orderIdProperty() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId.set(orderId);
    }

    public String getOrderDate() {
        return orderDate.get();
    }

    public StringProperty orderDateProperty() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate.set(orderDate);
    }


    public String getItems() {
        return items.get();
    }

    public StringProperty itemsProperty() {
        return items;
    }

    public void setItems(String items) {
        this.items.set(items);
    }

    public String returnItem(String column, JsonNode node) {
        return switch (column) {
            case "cSupplierName" -> node.get("supplierName").asText();
            case "cItemId" -> node.get("itemId").asText();
            case "cItemName" -> node.get("itemName").asText();
            case "cItemPrice" -> node.get("itemPriceFormatted").asText();
            case "cItemQuantity" -> node.get("itemQuantity").asText();
            case "cItemTotal" -> node.get("itemTotalFormatted").asText();
            default -> node.get("supplierId").asText();
        };
    }
}
