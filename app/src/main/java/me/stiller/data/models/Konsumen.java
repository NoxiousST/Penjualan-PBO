package me.stiller.data.models;

import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Konsumen extends RecursiveTreeObject<Konsumen> {
    StringProperty customerId = new SimpleStringProperty();
    StringProperty customerName = new SimpleStringProperty();
    StringProperty customerAddress = new SimpleStringProperty();
    StringProperty customerCity = new SimpleStringProperty();
    StringProperty customerPostal = new SimpleStringProperty();
    StringProperty customerPhone = new SimpleStringProperty();
    StringProperty customerEmail = new SimpleStringProperty();

    public Konsumen() {}

    public Konsumen(String customerId, String customerName, String customerAddress, String customerCity, String customerPostal, String customerPhone, String customerEmail) {
        this.customerId = new SimpleStringProperty(customerId);
        this.customerName = new SimpleStringProperty(customerName);
        this.customerAddress = new SimpleStringProperty(customerAddress);
        this.customerCity = new SimpleStringProperty(customerCity);
        this.customerPostal = new SimpleStringProperty(customerPostal);
        this.customerPhone = new SimpleStringProperty(customerPhone);
        this.customerEmail = new SimpleStringProperty(customerEmail);
    }

    public String getCustomerId() {
        return customerId.get();
    }

    public void setCustomerId(String customerId) {
        this.customerId.set(customerId);
    }

    public String getCustomerName() {
        return customerName.get();
    }

    public void setCustomerName(String customerName) {
        this.customerName.set(customerName);
    }

    public String getCustomerAddress() {
        return customerAddress.get();
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress.set(customerAddress);
    }

    public String getCustomerCity() {
        return customerCity.get();
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity.set(customerCity);
    }

    public String getCustomerPostal() {
        return customerPostal.get();
    }

    public void setCustomerPostal(String customerPostal) {
        this.customerPostal.set(customerPostal);
    }

    public String getCustomerPhone() {
        return customerPhone.get();
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone.set(customerPhone);
    }

    public String getCustomerEmail() {
        return customerEmail.get();
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail.set(customerEmail);
    }


    public StringProperty returnData(JFXTreeTableColumn<Konsumen, String> column) {
        return switch (column.getId()) {
            case "cname" -> customerName;
            case "caddress" -> customerAddress;
            case "ccity" -> customerCity;
            case "cpostal" -> customerPostal;
            case "cphone" -> customerPhone;
            case "cemail" -> customerEmail;
            default -> customerId;
        };
    }
}
