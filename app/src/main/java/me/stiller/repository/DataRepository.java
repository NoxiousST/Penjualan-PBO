package me.stiller.repository;

import javafx.collections.ObservableList;
import me.stiller.data.models.*;

import java.util.ArrayList;
import java.util.List;


public interface DataRepository {
    ObservableList<Barang> getBarangList();
    void setBarangList(ArrayList<Barang> list);
    Barang getBarang(int position);
    Barang getBarang(String id);
    Barang getSelectedBarang();
    void setSelectedBarang(Barang barang);

    ObservableList<Konsumen> getKonsumenList();
    void setKonsumenList(ArrayList<Konsumen> list);
    Konsumen getKonsumen(int position);
    Konsumen getKonsumen(String id);

    ObservableList<Supplier> getSupplierList();
    void setSupplierList(ArrayList<Supplier> list);
    Supplier getSupplier(int position);
    Supplier getSupplier(String id);
    Supplier getSelectedSupplier();
    void setSelectedSupplier(Supplier supplier);

    ObservableList<DetailPembelian> getDetailPembelian();
    void setDetailPembelian(List<DetailPembelian> list);


    ObservableList<Pembelian> getPembelianList();
    void setPembelianList(ArrayList<Pembelian> list);

    ArrayList<String> getBarangIds();
    ArrayList<String> getKonsumenIds();
    ArrayList<String> getSupplierIds();

    ObservableList<Jual> getPenjualanList();
    void setPenjualanList(ArrayList<Jual> list);

    ObservableList<Jual.DJual> getTransaksiJualList();
    void setTransaksiJualList(List<Jual.DJual> list);

    User getLoginUser();
    void setLoginUser(User user);
    void logout();
}