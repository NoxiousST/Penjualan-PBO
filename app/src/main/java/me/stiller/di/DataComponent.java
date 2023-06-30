package me.stiller.di;

import javax.inject.Singleton;

import dagger.Component;
import me.stiller.controller.*;

@Component(modules = {DataModule.class})
@Singleton
public interface DataComponent {
    void inject(AuthController controller);
    void inject(LoginController controller);
    void inject(MainController controller);
    void inject(BarangController controller);
    void inject(KonsumenController controller);
    void inject(SupplierController controller);
    void inject(TransaksiController controller);
    void inject(PenjualanController controller);
}