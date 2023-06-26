package me.stiller.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import me.stiller.Server;
import me.stiller.data.models.User;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.util.Strings;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static me.stiller.utils.Helper.concurentTask;

public class RegisterController implements Initializable {

    @FXML
    private HBox root, landing;

    @FXML
    private StackPane loginPane;

    @FXML
    private JFXTextField userField, emailField;

    @FXML
    private JFXPasswordField passField, cpassField;

    @FXML
    private JFXButton btnRegister, toLogin;

    @FXML
    private Label validUser, validEmail, validPass, validCPass;

    private Server server = new Server();
    private ArrayList<TextField> textFields = new ArrayList<>();
    private ArrayList<Label> eLabels = new ArrayList<>();
    private boolean isValid = true;

    private AuthController authController;

    public void setParentController(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textFields = new ArrayList<>(List.of(userField, emailField, passField, cpassField));
        eLabels = new ArrayList<>(List.of(validUser, validEmail, validPass, validCPass));
        btnRegister.setOnMouseClicked(event -> concurentTask(this::validateField));

        toLogin.setOnMouseClicked(event -> authController.changeView("login"));
    }

    private void validateField() {
        isValid = true;
        if (!userField.getText().isEmpty() && !emailField.getText().isEmpty()) {
            int existCode = server.isExist(userField.getText(), emailField.getText());
            setError(validUser, "This username is already used*", existCode == 1 || existCode == 3);
            setError(validEmail, "This email address is already registered*", existCode == 2 || existCode == 3);
        }

        textFields.forEach(tf ->
                setError(eLabels.get(textFields.indexOf(tf)), "This field is required*", tf.getText().isEmpty()));

        if (!emailField.getText().isEmpty()) {
            setError(validEmail, "Invalid email address*", !EmailValidator.getInstance().isValid(emailField.getText()));
        }

        LogManager.getLogger().info(!EmailValidator.getInstance().isValid(emailField.getText()));
        if (passField.getLength() > 0 && passField.getLength() < 8)
            setError(validPass, "Password must contain 8 character*", true);
        if (cpassField.getLength() >= 8)
            setError(validCPass, "Password does not match", !passField.getText().equals(cpassField.getText()) && cpassField.getLength() >= 8);


        if (isValid) {
            User user = new User(
                    Strings.EMPTY,
                    userField.getText(),
                    emailField.getText(),
                    passField.getText()
            );

            if (server.registerUser(user)) {
                authController.changeView("login");
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
            isValid = false;
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
