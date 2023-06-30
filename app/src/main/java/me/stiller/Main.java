package me.stiller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import me.stiller.di.DaggerDataComponent;
import me.stiller.di.DataComponent;

import java.util.Objects;

public class Main extends Application {

    private static Main instance;
    public DataComponent dataComponent;

    public Main() {
        instance = this;
    }
    public static Main getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) throws Exception {
        dataComponent = DaggerDataComponent.create();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/auth.fxml"));
        Parent root = loader.load();

        loadExtFonts("RilenoSans-Regular.otf");
        loadExtFonts("RilenoSans-Bold.otf");
        loadExtFonts("RilenoSans-Medium.otf");
        loadExtFonts("MusticaproSemibold-2OG5o.otf");
        loadExtFonts("LouisGeorgeCafeBold.ttf");
        loadExtFonts("NexaBold.otf");

        //Scene scene = new Scene(root, 1080, 720);
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("css/styles.css")).toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        //stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("images/icon.png"))));
        stage.setScene(scene);
        stage.show();
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch();
    }

    private void loadExtFonts(String name) {
        Font.loadFont(Objects.requireNonNull(
                Main.class.getResource("fonts/" + name)).toExternalForm(), 10);
    }

    public DataComponent getComponent() {
        return dataComponent;
    }
}