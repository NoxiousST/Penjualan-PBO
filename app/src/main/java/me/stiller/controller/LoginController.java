package me.stiller.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.api.*;
import me.stiller.data.models.User;
import me.stiller.interfaces.RetrieveLogin;
import me.stiller.repository.DataRepository;
import org.apache.commons.validator.routines.EmailValidator;

import javax.inject.Inject;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static me.stiller.utils.Helper.concurentTask;
import static me.stiller.utils.Helper.getIcon;

public class LoginController implements Initializable, RetrieveLogin {

    @FXML
    StackPane loginPane;

    @FXML
    JFXButton btnLogin, gLogin, fLogin, ghLogin, toRegister;

    @FXML
    HBox root;

    @FXML
    JFXTextField userField;

    @FXML
    JFXPasswordField passField;

    @FXML
    Label validUser, validPass;

    private final Server server = new Server();
    private AuthController authController;

    public void setParentController(AuthController authController) {
        this.authController = authController;
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        gLogin.setOnMouseClicked(e -> authController.add3Login(new AuthorizeGoogle(this).getWebView()));
        gLogin.setGraphic(getIcon("google"));

        fLogin.setOnMouseClicked(e -> authController.add3Login(new AuthorizeFacebook(this).getWebView()));
        fLogin.setGraphic(getIcon("facebook"));

        ghLogin.setOnMouseClicked(e -> authController.add3Login(new AuthorizeGithub(this).getWebView()));
        ghLogin.setGraphic(getIcon("github"));

        btnLogin.setOnMouseClicked(e -> concurentTask(this::validateField));
        toRegister.setOnMouseClicked(event -> authController.replacePane("register"));
    }

    private void validateField() {
        String username = userField.getText();
        String password = passField.getText();
        setError(validUser, "This field is required*", username.isEmpty());
        setError(validPass, "This field is required*", password.isEmpty());

        if (userField.getLength() > 0 && passField.getLength() > 0) {
            if (server.getConnection() == null) {
                setError(validPass, "Failed to connect to local server", true);
                return;
            }

            if (server.checkUser(username)) {
                User loginUser = server.loginUser(username, password);
                if (loginUser.getId() == null) setError(validPass, "Incorrect password*", true);
                else retrieveLogin(loginUser);
            } else {
                boolean isEmail = EmailValidator.getInstance().isValid(username);
                if (isEmail) setError(validUser, "We do not recognize this email address*", true);
                else setError(validUser, "We do not recognize this username*", true);
            }
        }
    }

    private void setError(Label errorLabel, String errorMessage, boolean showError) {
        if (showError) {
            MaterialIconView errorIcon = new MaterialIconView(MaterialIcon.ERROR_OUTLINE);
            errorIcon.getStyleClass().add("icon");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            errorLabel.setGraphic(errorIcon);
            errorLabel.setText(errorMessage);
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void authorize() {
        try {
            authController.closeScene();

            FXMLLoader newLoader = new FXMLLoader(Main.class.getResource("views/main.fxml"));
            Parent newRoot = newLoader.load();

            Scene scene = new Scene(newRoot, 1080, 720);
            scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("css/styles.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            Stage stage = new Stage();
            //stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
            newRoot.requestFocus();

            MainController mainController = newLoader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void retrieveLogin(User user) {
        dataRepository.setLoginUser(user);
        authorize();
    }
}
