package me.stiller.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Konsumen;
import me.stiller.repository.DataRepository;
import me.stiller.repository.DataRepositoryImpl;

@Module
public class DataModule {
    private ObservableList<Barang> barangList;
    private ObservableList<Konsumen> konsumenList;

    public DataModule() {
        barangList = FXCollections.observableArrayList();
        konsumenList = FXCollections.observableArrayList();
    }

    @Provides
    @Singleton
    public DataRepository provideBarangRepository() {
        return new DataRepositoryImpl();
    }

    @Provides
    @Singleton
    public ObservableList<Barang> provideBarangList() {
        return barangList;
    }

    @Provides
    @Singleton
    public ObservableList<Konsumen> provideKonsumenList() {
        return konsumenList;
    }
}
