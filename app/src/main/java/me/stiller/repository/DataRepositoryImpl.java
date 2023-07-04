package me.stiller.repository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.stiller.data.models.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class DataRepositoryImpl implements DataRepository {
    private final ObservableList<Barang> barangList;
    private final ObservableList<Konsumen> konsumenList;
    private final ObservableList<Supplier> supplierList;
    private final ObservableList<Jual> penjualanList;
    private final ObservableList<Jual.DJual> transactionList;
    private final ObservableList<DetailPembelian> beliTransactionList;
    private final ObservableList<Pembelian> pembelianList;
    private Barang barang;
    private User user;
    private Supplier supplier;

    @Inject
    public DataRepositoryImpl() {
        barangList = FXCollections.observableArrayList();
        konsumenList = FXCollections.observableArrayList();
        supplierList = FXCollections.observableArrayList();
        penjualanList = FXCollections.observableArrayList();
        transactionList = FXCollections.observableArrayList();
        beliTransactionList = FXCollections.observableArrayList();
        pembelianList = FXCollections.observableArrayList();
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
    public ObservableList<Supplier> getSupplierList() {
        return supplierList;
    }

    @Override
    public void setSupplierList(ArrayList<Supplier> list) {
        supplierList.setAll(list);
    }

    @Override
    public Supplier getSupplier(int position) {
        return supplierList.get(position);
    }

    @Override
    public Supplier getSupplier(String id) {
        for (Supplier k : supplierList)
            if (k.getSupplierId().equals(id)) return k;
        return new Supplier();
    }

    @Override
    public Supplier getSelectedSupplier() {
        return this.supplier;
    }

    @Override
    public void setSelectedSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public ObservableList<DetailPembelian> getDetailPembelian() {
        return beliTransactionList;
    }

    @Override
    public void setDetailPembelian(List<DetailPembelian> list) {
        beliTransactionList.setAll(list);
    }

    @Override
    public ObservableList<Pembelian> getPembelianList() {
        return pembelianList;
    }

    @Override
    public void setPembelianList(ArrayList<Pembelian> list) {
        pembelianList.setAll(list);
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
    public ArrayList<String> getSupplierIds() {
        ArrayList<String> ids = new ArrayList<>();
        supplierList.forEach(konsumen -> ids.add(konsumen.getSupplierId()));
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