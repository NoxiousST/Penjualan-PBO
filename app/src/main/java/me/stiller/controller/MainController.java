package me.stiller.controller;

import com.jfoenix.controls.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.User;
import me.stiller.repository.DataRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static me.stiller.utils.Helper.*;

public class MainController implements Initializable {

    @FXML
    private StackPane root;

    @FXML
    private VBox vbox;

    @FXML
    private VBox navbar;

    @FXML
    private JFXListView<HBox> nList;

    @FXML
    private JFXDialog dialog;

    @FXML
    private HBox titleBar, logoutLayout;

    @FXML
    private Pane iconStatus;

    @FXML
    private Label dialogMsg, username, email;

    @FXML
    private JFXButton dialogClose, btnClose, btnMax, btnMin, mdata, mtransact, mreport, mutil, mexit;

    @FXML
    private JFXDialogLayout dialogLayout;

    @FXML
    private Circle profile;

    private final Server server = new Server();
    private final ArrayList<String> navList = new ArrayList<>();
    private double offsetX;
    private double offsetY;
    private final ArrayList<JFXButton> menus = new ArrayList<>();

    public MainController() {
        navList.add("Barang");
        navList.add("Konsumen");
        navList.add("Transaksi");
        navList.add("Penjualan");
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        menus.addAll(List.of(mdata, mtransact, mreport, mutil, mexit));
        setTitleBar();
        setNavbar(navList);
        setPopupMenu(menus);
        navbar.setOnMouseEntered(event -> Platform.runLater(() -> navbarTransition(200, 32)));
        navbar.setOnMouseExited(event -> Platform.runLater(() -> navbarTransition(60, 16)));
        dialog.setDialogContainer(root);

        Server server = new Server();
        dataRepository.setBarangList(server.readBarang());
        dataRepository.setKonsumenList(server.readKonsumen());
        dataRepository.setPenjualanList(server.retrieveJualData());
        dataRepository.setSupplierList(server.readSupplier());

        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        changeNavbar("barang");
        nList.getSelectionModel().select(0);
        nList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            switch (index) {
                case 0 -> changeNavbar("barang");
                case 1 -> changeNavbar("konsumen");
                case 2 -> changeNavbar("transaksi");
                case 3 -> changeNavbar("penjualan");
            }
        });

        Image img= new Image(Objects.requireNonNull(
                Main.class.getResource("images/profile.png")).toString());
        profile.setFill(new ImagePattern(img));

        if (dataRepository.getLoginUser() != null) {
            User loginUser = dataRepository.getLoginUser();
            username.setText(loginUser.getUsername());
            email.setText(loginUser.getEmail());
            if (loginUser.getImage() != null) {
                profile.setFill(new ImagePattern(new Image(loginUser.getImage())));
            }
        }

        logoutLayout.setOnMouseClicked(event -> toLogout());

    }

    private void changeNavbar(String name) {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/" + name + ".fxml"));
        try {
            Parent childRoot = loader.load();
            Object controller = loader.getController();

            switch (controller.getClass().getSimpleName()) {
                case "BarangController" -> ((BarangController) controller).setParentController(this);
                case "KonsumenController" -> ((KonsumenController) controller).setParentController(this);
                case "SupplierController" -> ((SupplierController) controller).setParentController(this);
                case "TransaksiController" -> ((TransaksiController) controller).setParentController(this);
                case "PenjualanController" -> ((PenjualanController) controller).setParentController(this);
            }
            vbox.getChildren().setAll(childRoot);

            root.setOnMouseClicked(event -> {
                if (controller instanceof SupplierController) {
                    ((SupplierController) controller).removeFocus();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setNavbar(ArrayList<String> stringList) {
        nList.setExpanded(true);

        for (String s : stringList) {
            HBox h = new HBox();
            Label l = new Label(s);
            h.getChildren().add(getIcon(s));
            h.getChildren().add(l);
            nList.getItems().add(h);
        }
        nList.setFocusTraversable(false);

        logoutLayout.getChildren().add(0, getIcon("logout"));
    }

    private void navbarTransition(int width, int size) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(navbar.prefWidthProperty(), navbar.getWidth())),
                new KeyFrame(Duration.millis(320), new KeyValue(navbar.prefWidthProperty(), width, Interpolator.EASE_BOTH)),

                new KeyFrame(Duration.ZERO, new KeyValue(profile.radiusProperty(), profile.getRadius())),
                new KeyFrame(Duration.millis(320), new KeyValue(profile.radiusProperty(), size, Interpolator.EASE_BOTH))
        );

        t.play();
    }

    private void setTitleBar() {
        btnMin.setOnMouseClicked(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setIconified(true);
        });

        btnMax.setOnMouseClicked(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });

        btnClose.setOnMouseClicked(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        });

        titleBar.setOnMousePressed(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            offsetX = stage.getX() - event.getScreenX();
            offsetY = stage.getY() - event.getScreenY();
        });
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setX(event.getScreenX() + offsetX);
            stage.setY(event.getScreenY() + offsetY);
        });

        btnMin.setGraphic(getIcon("minimize"));
        btnMax.setGraphic(getIcon("maximize"));
        btnClose.setGraphic(getIcon("close"));
    }

    public void setDialog(boolean success, String message) {
        SVGPath ic;
        if (success) {
            dialogLayout.getStyleClass().add("dialog-success");
            ic = getIcon("checkmark");
        } else {
            dialogLayout.getStyleClass().add("dialog-failed");
            ic = getIcon("failed");
        }
        ic.setScaleX(1.3);
        ic.setScaleY(1.3);
        iconStatus.getChildren().add(ic);

        dialogMsg.setText(message);
        dialogMsg.setWrapText(true);
        dialogClose.setGraphic(getIcon("close"));
        dialogClose.setOnMouseClicked(event -> dialog.close());
        Platform.runLater(() -> dialog.show());
        delayRun(() -> dialog.close(), 8600);
    }

    private void setPopupMenu(ArrayList<JFXButton> menus) {
        ArrayList<Label> labels = new ArrayList<>();
        JFXListView<Label> listMenu = new JFXListView<>();
        JFXPopup popup = new JFXPopup(listMenu);
        listMenu.getStyleClass().add("popup-list");
        popup.getStyleClass().add("popup");

        listMenu.setOnMouseClicked(e -> {
            popup.hide();
            listMenu.getSelectionModel().clearSelection();
        });

        listMenu.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            switch (listMenu.getId()) {
                case "mdata" -> {
                    switch (index) {
                        case 0 -> changeNavbar("barang");
                        case 1 -> changeNavbar("konsumen");
                        case 2 -> changeNavbar("supplier");
                    }
                }
                case "mtransact" -> {
                    if (index == 0) changeNavbar("transaksi");
                }
                case "mreport" -> {
                    switch (index) {
                        case 0 -> changeNavbar("barang");
                        case 1 -> changeNavbar("konsumen");
                        case 2 -> changeNavbar("penjualan");
                    }
                }
                case "mutil" -> {
                    if (index == 0) backupPopup();
                }
            }
        });

        for (JFXButton menu : menus) {
            menu.setOnMouseClicked(event -> {
                switch (menu.getId()) {
                    case "mdata" -> labels.addAll(List.of(new Label("Data Barang"), new Label("Data Konsumen"), new Label("Data Supplier")));
                    case "mtransact" -> labels.addAll(List.of(new Label("Transaksi Jual"), new Label("Transaksi Beli")));
                    case "mreport" -> labels.addAll(List.of(new Label("Laporan Barang"), new Label("Laporan Konsumen"), new Label("Laporan Penjualan")));
                    case "mutil" -> labels.add(new Label("Backup"));
                    case "mexit" -> exitWindow();
                }
                listMenu.setId(menu.getId());
                listMenu.getItems().setAll(labels);

                popup.show(menu, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 1, 32);
                labels.clear();
            });
        }

    }

    private void backupPopup() {
        Stage stage = (Stage) root.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("BACKUP FILE (*.sql)", "*.sql");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setTitle("Specify A File to Save");
        String filename = "backup.sql";

        fileChooser.setInitialFileName(filename);

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            System.out.println("Save as file: " + file.getAbsolutePath());
            String savepath = file.getAbsolutePath();
            server.backupServer(savepath);
        }
    }

    public void toLogout() {
        try {
            exitWindow();

            FXMLLoader newLoader = new FXMLLoader(Main.class.getResource("views/auth.fxml"));
            Parent newRoot = newLoader.load();

            Scene scene = new Scene(newRoot, 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("css/styles.css")).toExternalForm());
            newRoot.requestFocus();

            Stage newStage = new Stage();
            newStage.initStyle(StageStyle.TRANSPARENT);
            newStage.setScene(scene);
            newStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exitWindow() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
        dataRepository.setLoginUser(new User());
    }
}


