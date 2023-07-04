package me.stiller.data.models;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.*;

import static me.stiller.utils.Helper.formatPrice;

public class DetailPembelian extends RecursiveTreeObject<DetailPembelian> {
    private final StringProperty supplierId = new SimpleStringProperty();
    private final StringProperty supplierName = new SimpleStringProperty();
    private final StringProperty itemId = new SimpleStringProperty();
    private final StringProperty itemName = new SimpleStringProperty();
    private final DoubleProperty itemPrice = new SimpleDoubleProperty();
    private final IntegerProperty itemQuantity = new SimpleIntegerProperty();
    private final DoubleProperty itemTotal = new SimpleDoubleProperty();

    StringProperty itemPriceFormatted = new SimpleStringProperty();
    StringProperty itemTotalFormatted = new SimpleStringProperty();

    public DetailPembelian() {
    }

    public DetailPembelian(String supplierId, String supplierName, String itemId, String itemName, Double itemPrice, Integer itemQuantity, Double itemTotal) {
        setSupplierId(supplierId);
        setSupplierName(supplierName);
        setItemId(itemId);
        setItemName(itemName);
        setItemPrice(itemPrice);
        setItemQuantity(itemQuantity);
        setItemTotal(itemTotal);
        setItemPriceFormatted();
        setItemTotalFormatted();
    }

    public String getSupplierId() {
        return supplierId.get();
    }

    public StringProperty supplierIdProperty() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId.set(supplierId);
    }

    public String getSupplierName() {
        return supplierName.get();
    }

    public StringProperty supplierNameProperty() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName.set(supplierName);
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
        setItemTotalFormatted();
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

    public String getItemTotalFormatted() {
        return formatPrice(getItemTotal());
    }

    public StringProperty itemTotalFormattedProperty() {
        return itemTotalFormatted;
    }

    public void setItemTotalFormatted() {
        this.itemTotalFormatted.set(formatPrice(getItemTotal()));
    }
}
