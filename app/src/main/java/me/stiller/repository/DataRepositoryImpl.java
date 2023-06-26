package me.stiller.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.stiller.data.models.*;

import javax.inject.Inject;
import java.util.ArrayList;

public class DataRepositoryImpl implements DataRepository {
    private final ObservableList<Barang> barangList;
    private final ObservableList<Konsumen> konsumenList;
    private final ObservableList<Jual> penjualanList;
    private final ObservableList<Jual.DJual> transactionList;
    private Barang barang;
    private User user;

    @Inject
    public DataRepositoryImpl() {
        barangList = FXCollections.observableArrayList();
        konsumenList = FXCollections.observableArrayList();
        penjualanList = FXCollections.observableArrayList();
        transactionList = FXCollections.observableArrayList();
    }

    @Override
    public ObservableList<Barang> getBarangList() {
        return barangList;
    }

    @Override
    public void setBarangList(ArrayList<Barang> list) {
        barangList.setAll(list);
    }

    @Override
    public Barang getBarang(int position) {
        return barangList.get(position);
    }

    @Override
    public Barang getBarang(String id) {
        for (Barang b : barangList)
            if (b.getItemId().equals(id)) return b;
        return new Barang();
    }

    @Override
    public Barang getSelectedBarang() {
        return barang;
    }

    @Override
    public void setSelectedBarang(Barang barang) {
        this.barang = barang;
    }

    @Override
    public ObservableList<Konsumen> getKonsumenList() {
        return konsumenList;
    }

    @Override
    public void setKonsumenList(ArrayList<Konsumen> list) {
        konsumenList.setAll(list);
    }

    @Override
    public Konsumen getKonsumen(int position) {
        return konsumenList.get(position);
    }

    @Override
    public Konsumen getKonsumen(String id) {
        for (Konsumen k : konsumenList)
            if (k.getCustomerId().equals(id)) return k;
        return new Konsumen();
    }

    @Override
    public ArrayList<String> getBarangIds() {
        ArrayList<String> ids = new ArrayList<>();
        barangList.forEach(barang -> ids.add(barang.getItemId()));
        return ids;
    }

    @Override
    public ArrayList<String> getKonsumenIds() {
        ArrayList<String> ids = new ArrayList<>();
        konsumenList.forEach(konsumen -> ids.add(konsumen.getCustomerId()));
        return ids;
    }

    @Override
    public ObservableList<Jual> getPenjualanList() {
        return penjualanList;
    }

    @Override
    public void setPenjualanList(ArrayList<Jual> list) {
        penjualanList.setAll(list);
    }

    @Override
    public ObservableList<Jual.DJual> getTrancationList() {
        return transactionList;
    }

    @Override
    public void setTransactionList(ArrayList<Jual.DJual> list) {
        transactionList.setAll(list);
    }

    @Override
    public User getLoginUser() {
        return user;
    }

    @Override
    public void setLoginUser(User user) {
        this.user = user;
    }

    @Override
    public void logout() {
        this.user = new User();
    }
}