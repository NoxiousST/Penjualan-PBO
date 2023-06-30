package me.stiller.data.models;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.*;

public class Supplier extends RecursiveTreeObject<Supplier> {

    StringProperty supplierId = new SimpleStringProperty();
    StringProperty supplierName = new SimpleStringProperty();
    StringProperty supplierEmail = new SimpleStringProperty();
    StringProperty supplierAddress = new SimpleStringProperty();
    StringProperty supplierCity = new SimpleStringProperty();
    StringProperty itemId = new SimpleStringProperty();

    public Supplier() {}

    public Supplier(String supplierId, String supplierName, String supplierEmail, String supplierAddress, String supplierCity, String itemId) {
        setSupplierId(supplierId);
        setSupplierName(supplierName);
        setSupplierEmail(supplierEmail);
        setSupplierAddress(supplierAddress);
        setSupplierCity(supplierCity);
        setItemId(itemId);
    }

    public String getSupplierId() {
        return supplierId.get();
    }

    public void setSupplierId(String supplierId) {
        this.supplierId.set(supplierId);
    }

    public String getSupplierName() {
        return supplierName.get();
    }

    public void setSupplierName(String supplierName) {
        this.supplierName.setValue(supplierName);
    }

    public String getSupplierEmail() {
        return supplierEmail.get();
    }

    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail.set(supplierEmail);
    }

    public String getSupplierAddress() {
        return supplierAddress.get();
    }

    public void setSupplierAddress(String supplierAddress) {
        this.supplierAddress.set(supplierAddress);
    }

    public String getSupplierCity() {
        return supplierCity.get();
    }

    public void setSupplierCity(String supplierCity) {
        this.supplierCity.set(supplierCity);
    }

    public String getItemId() {
        return itemId.get();
    }

    public void setItemId(String itemId) {
        this.itemId.set(itemId);
    }


}
