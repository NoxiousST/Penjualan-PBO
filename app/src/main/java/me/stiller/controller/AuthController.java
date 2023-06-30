package me.stiller.controller;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import me.stiller.Main;
import me.stiller.repository.DataRepository;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static me.stiller.utils.Helper.getIcon;

public class AuthController implements Initializable {

    @FXML
    private HBox root, landing;

    @FXML
    private StackPane authPane;

    @FXML
    private JFXButton btnBack;

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        btnBack.setOnMouseClicked(event -> {
            if (authPane.getChildren().size() > 1)
                authPane.getChildren().remove(authPane.getChildren().size() - 1);
        });

        btnBack.setGraphic(getIcon("left"));
        replacePane("login");
    }

    public void add3Login(Node node) {
        authPane.getChildren().add(node);
    }

    public void replacePane(String name) {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/" + name + ".fxml"));
        try {
            Parent childRoot = loader.load();
            Object controller = loader.getController();
            switch (controller.getClass().getSimpleName()) {
                case "LoginController" -> ((LoginController) controller).setParentController(this);
                case "RegisterController" -> ((RegisterController) controller).setParentController(this);
            }
            authPane.getChildren().setAll(childRoot);
            authPane.requestFocus();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeScene() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }
}
