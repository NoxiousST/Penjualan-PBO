package me.stiller.data.models;

import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

import static me.stiller.utils.Helper.formatPrice;

public class Jual extends RecursiveTreeObject<Jual> {
    StringProperty orderId = new SimpleStringProperty();
    StringProperty customerId= new SimpleStringProperty();
    StringProperty customerName = new SimpleStringProperty();
    StringProperty orderDate = new SimpleStringProperty();
    ObservableList<DJual> items = FXCollections.observableArrayList();

    public Jual() {}

    public Jual(String orderId, String customerId, String customerName, String orderDate, ObservableList<DJual> items) {
        setOrderId(orderId);
        setCustomerId(customerId);
        setCustomerName(customerName);
        setOrderDate(orderDate);
        this.items = items;
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

    public String getCustomerId() {
        return customerId.get();
    }

    public StringProperty customerIdProperty() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId.set(customerId);
    }

    public void setOrderDate(String orderDate) {
        this.orderDate.set(orderDate);
    }

    public String getCustomerName() {
        return customerName.get();
    }

    public StringProperty customerNameProperty() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName.set(customerName);
    }

    public ObservableList<DJual> getItems() {
        return items;
    }

    public void setItems(ObservableList<DJual> items) {
        this.items = items;
    }

    public static class DJual extends RecursiveTreeObject<DJual> {
        StringProperty itemId = new SimpleStringProperty();
        StringProperty itemName = new SimpleStringProperty();
        DoubleProperty itemPrice = new SimpleDoubleProperty();
        IntegerProperty itemQuantity = new SimpleIntegerProperty();
        DoubleProperty itemTotal = new SimpleDoubleProperty();

        StringProperty itemPriceFormatted = new SimpleStringProperty();
        StringProperty itemQuantityFormatted = new SimpleStringProperty();
        StringProperty itemTotalFormatted = new SimpleStringProperty();

        public DJual() {}

        public DJual(String itemId, String itemName, Double itemPrice, Integer itemQuantity, Double itemTotal) {
            setItemId(itemId);
            setItemName(itemName);
            setItemPrice(itemPrice);
            setItemQuantity(itemQuantity);
            setItemTotal(itemTotal);
            setItemPriceFormatted();
            setItemQuantityFormatted();
            setItemTotalFormatted();
        }

        public String getItemId() {
            return itemId.get();
        }

        public StringProperty itemIdProperty() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId.set(itemId);
        }

        public String getItemName() {
            return itemName.get();
        }

        public StringProperty itemNameProperty() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName.set(itemName);
        }

        public double getItemPrice() {
            return itemPrice.get();
        }

        public DoubleProperty itemPriceProperty() {
            return itemPrice;
        }

        public void setItemPrice(double itemPrice) {
            this.itemPrice.set(itemPrice);
        }

        public int getItemQuantity() {
            return itemQuantity.get();
        }

        public IntegerProperty itemQuantityProperty() {
            return itemQuantity;
        }

        public void setItemQuantity(int itemQuantity) {
            this.itemQuantity.set(itemQuantity);
        }

        public double getItemTotal() {
            return itemTotal.get();
        }

        public DoubleProperty itemTotalProperty() {
            return itemTotal;
        }

        public void setItemTotal(double itemTotal) {
            this.itemTotal.set(itemTotal);
        }

        public String getItemPriceFormatted() {
            return formatPrice(getItemPrice());
        }

        public StringProperty itemPriceFormattedProperty() {
            return itemPriceFormatted;
        }

        public void setItemPriceFormatted() {
            this.itemPriceFormatted.set(formatPrice(getItemPrice()));
        }

        public String getItemQuantityFormatted() {
            return String.valueOf(getItemQuantity());
        }

        public StringProperty itemQuantityFormattedProperty() {
            return itemQuantityFormatted;
        }

        public void setItemQuantityFormatted() {
            this.itemQuantityFormatted.set(String.valueOf(getItemQuantity()));
        }

        public String getItemTotalFormatted() {
            return formatPrice(getItemTotal());
        }

        public StringProperty itemTotalFormattedProperty() {
            return itemTotalFormatted;
        }

        public void setItemTotalFormatted() {
            this.itemTotalFormatted.set(formatPrice(getItemTotal()));
        }

        public String returnItem(String column) {
            return switch (column) {
                case "cItemName" -> getItemName();
                case "cItemPrice" -> getItemPriceFormatted();
                case "cItemQuantity" -> String.valueOf(getItemQuantity());
                case "cItemTotal" -> getItemTotalFormatted();
                default -> getItemId();
            };
        }
    }
}
