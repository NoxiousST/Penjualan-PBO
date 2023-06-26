package me.stiller.repository;

import javafx.collections.ObservableList;
import me.stiller.data.models.*;

import java.util.ArrayList;


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

    ArrayList<String> getBarangIds();
    ArrayList<String> getKonsumenIds();

    ObservableList<Jual> getPenjualanList();
    void setPenjualanList(ArrayList<Jual> list);

    ObservableList<Jual.DJual> getTrancationList();
    void setTransactionList(ArrayList<Jual.DJual> list);

    User getLoginUser();
    void setLoginUser(User user);
    void logout();
}