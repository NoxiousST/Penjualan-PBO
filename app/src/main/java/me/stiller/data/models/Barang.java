package me.stiller.data.models;

import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.*;

import java.util.Objects;

import static me.stiller.utils.Helper.formatPrice;

public class Barang extends RecursiveTreeObject<Barang> {

    StringProperty itemId = new SimpleStringProperty();
    StringProperty itemName = new SimpleStringProperty();
    StringProperty itemUnit = new SimpleStringProperty();
    DoubleProperty itemPrice = new SimpleDoubleProperty();
    StringProperty itemStock = new SimpleStringProperty();
    StringProperty itemMinStock = new SimpleStringProperty();
    StringProperty itemPriceFormatted = new SimpleStringProperty();

    public Barang() {
    }

    public Barang(String itemId, String itemName, String itemUnit, Double itemPrice, String itemStock, String itemMinStock) {
        setItemId(itemId);
        setItemName(itemName);
        setItemUnit(itemUnit);
        setItemPrice(itemPrice);
        setItemStock(itemStock);
        setItemMinStock(itemMinStock);
        setItemPriceFormatted();
    }

    public String getItemId() {
        return itemId.get();
    }

    public void setItemId(String itemId) {
        this.itemId.set(itemId);
    }

    public String getItemName() {
        return itemName.get();
    }

    public void setItemName(String itemName) {
        this.itemName.setValue(itemName);
    }

    public String getItemUnit() {
        return itemUnit.get();
    }

    public void setItemUnit(String itemUnit) {
        this.itemUnit.set(itemUnit);
    }

    public Double getItemPrice() {
        return itemPrice.get();
    }

    public void setItemPrice(Double itemPrice) {
        this.itemPrice.set(itemPrice);
    }

    public String getItemStock() {
        return itemStock.get();
    }

    public void setItemStock(String itemStock) {
        this.itemStock.set(itemStock);
    }

    public String getItemMinStock() {
        return itemMinStock.get();
    }

    public void setItemMinStock(String itemMinStock) {
        this.itemMinStock.set(itemMinStock);
    }

    public String getItemPriceFormatted() {
        return itemPriceFormatted.get();
    }

    public void setItemPriceFormatted() {
        this.itemPriceFormatted.set(formatPrice(getItemPrice()));
    }

}
