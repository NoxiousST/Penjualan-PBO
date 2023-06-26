package me.stiller.controller;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Konsumen;

import me.stiller.repository.DataRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static me.stiller.utils.Helper.*;

public class KonsumenController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Konsumen> table;

    @FXML
    private JFXTreeTableColumn<Konsumen, String> cselect, cid, cname, caddress, ccity, cpostal, cphone, cemail, caction;

    @FXML
    private Pagination pagination;

    @FXML
    private JFXButton btnCancel, btnConfirm, btnPref, btnNext, btnInsert, btnEdit, btnPrint, btnExport;

    @FXML
    private Label pageCount;

    @FXML
    private JFXTextField search, iid, iname, iaddress, icity, ipostal, iphone, iemail;

    @Inject
    DataRepository dataRepository;

    Server server = new Server();
    private MainController mainController;
    private final ArrayList<JFXTreeTableColumn<Konsumen, String>> columns = new ArrayList<>();
    private final ArrayList<JFXTextField> inputs = new ArrayList<>();
    private ObservableList<Konsumen> list;
    private FilteredList<Konsumen> filteredData;
    private boolean editSelected = false;
    private int pages;

    Logger log = LogManager.getLogger(KonsumenController.class.getName());

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getBarangComponent().inject(this);

        list = dataRepository.getKonsumenList();
        columns.addAll(List.of(cselect, cid, cname, caddress, ccity, cpostal, cphone, cemail, caction));
        inputs.addAll(List.of(iid, iname, iaddress, icity, ipostal, iphone, iemail));
        initializeTable();
        setForm();

        btnCancel.setGraphic(getIcon("close"));
        btnConfirm.setGraphic(getIcon("check"));
        btnPref.setGraphic(getIcon("left"));
        btnNext.setGraphic(getIcon("right"));

        root.setOnMousePressed(event -> removeFocus());
    }

    public void removeFocus() {
        root.getParent().requestFocus();
    }

    private void initializeTable() {
        cid.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerId"));
        cname.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerName"));
        caddress.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerAddress"));
        ccity.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerCity"));
        cpostal.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerPostal"));
        cphone.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerPhone"));

        cemail.setCellFactory(param -> new JFXTreeTableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String email = getTableRow().getItem().getCustomerEmail();
                    setTooltip(new Tooltip(email));

                    setGraphic(null);
                    setText(email);
                }
            }
        });

        caction.setCellFactory(param -> new JFXTreeTableCell<>() {
            final JFXButton action = new JFXButton();
            final JFXListView<Label> listMenu = new JFXListView<>();
            final JFXPopup popup = new JFXPopup(listMenu);

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                caction.setMaxWidth(60);

                popup.getStyleClass().add("popup");
                action.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                action.setRipplerFill(Paint.valueOf("white"));
                action.setGraphic(getIcon("action"));
                action.setOnMouseClicked(event -> popup.show(action,
                        JFXPopup.PopupVPosition.TOP,
                        JFXPopup.PopupHPosition.LEFT,
                        event.getX(), event.getY())
                );
                listMenu.getStyleClass().add("popup-list");
                listMenu.getItems().setAll(new Label("Edit"), new Label("Delete"));
                listMenu.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                listMenu.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    Konsumen konsumen = getTableRow().getItem();
                    if (newValue.equals(listMenu.getItems().get(0))) {
                        table.getSelectionModel().select(getIndex());
                        editSelected = true;
                        if (!table.getColumns().contains(cselect)) table.getColumns().add(0, cselect);
                        setFormEdit(konsumen);
                        btnEdit.getStyleClass().setAll("btn-edit-selected");
                        btnInsert.getStyleClass().setAll("btn-insert");
                    } else if (newValue.equals(listMenu.getItems().get(1))) {
                        if (server.delete(konsumen))
                            mainController.setDialog(true, "Customer removed successfully");
                        else
                            mainController.setDialog(true, "There was an error while removing cutomer");
                        dataRepository.setKonsumenList(server.readKonsumen());
                        changeTableView(0);
                        table.getSelectionModel().clearSelection();
                    }
                });

                listMenu.setOnMouseClicked(event -> {
                    popup.hide();
                    listMenu.getSelectionModel().clearSelection();
                });

                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(action);
                    setText(null);
                }
            }
        });

        cselect.setMaxWidth(72);
        cselect.setMinWidth(72);
        cselect.setCellFactory(param -> new JFXTreeTableCell<>() {
            final JFXCheckBox checkBox = new JFXCheckBox();

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                checkBox.setDisable(true);
                table.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) ->
                        checkBox.setSelected(newIndex.intValue() == getTableRow().getIndex()));
                if (editSelected) getTableRow().getStyleClass().add("row");

                getTableRow().setOnMouseClicked(event -> {
                    if (editSelected) setFormEdit(getTableRow().getItem());
                });

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    getTableRow().getStyleClass().remove("row");
                } else {
                    setGraphic(checkBox);
                    setText(null);
                }
            }
        });

        TreeItem<Konsumen> root = new RecursiveTreeItem<>(list, RecursiveTreeObject::getChildren);
        table.getColumns().setAll(columns);
        table.getColumns().remove(cselect);
        table.setRoot(root);
        table.setShowRoot(false);
        setFilterPagination();
    }

    private void setForm() {
        btnInsert.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));

            btnInsert.getStyleClass().setAll("btn-insert-selected");
            btnEdit.getStyleClass().setAll("btn-edit");
            editSelected = false;
            table.getColumns().remove(cselect);
        });

        btnEdit.setOnMouseClicked(event -> {
            btnEdit.getStyleClass().setAll("btn-edit-selected");
            btnInsert.getStyleClass().setAll("btn-insert");

            editSelected = true;
            if (!table.getColumns().contains(cselect))
                table.getColumns().add(0, cselect);
        });

        btnConfirm.setOnMouseClicked(event -> {
            Konsumen konsumen = new Konsumen();
            konsumen.setCustomerId(iid.getText());
            konsumen.setCustomerName(iname.getText());
            konsumen.setCustomerAddress(iaddress.getText());
            konsumen.setCustomerCity(icity.getText());
            konsumen.setCustomerPostal(ipostal.getText());
            konsumen.setCustomerPhone(iphone.getText());
            konsumen.setCustomerEmail(iemail.getText());
            if (!editSelected) {
                if (server.insert(konsumen)) mainController.setDialog(true, "Customer added successfully");
                else mainController.setDialog(true, "There was an error while adding cutomer");
            } else {
                if (server.update(konsumen)) mainController.setDialog(true, "Customer updated successfully");
                else mainController.setDialog(true, "There was an error while updating the data");
            }
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            dataRepository.setKonsumenList(server.readKonsumen());
            changeTableView(0);
        });

        btnCancel.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            btnInsert.getStyleClass().setAll("btn-insert-selected");
            btnEdit.getStyleClass().setAll("btn-edit");
        });

        btnPrint.setOnMouseClicked(event -> print());
        btnExport.setOnMouseClicked(event -> export());

        btnPref.setOnAction(event -> {
            int index = pagination.getCurrentPageIndex();
            if (index > 0) pagination.setCurrentPageIndex(index - 1);
            removeFocus();
        });

        btnNext.setOnAction(event -> {
            int index = pagination.getCurrentPageIndex();
            if (index < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(index + 1);
            }
            log.info(pagination.getPageCount());
        });

        btnPrint.disableProperty().bind(Bindings.isEmpty(list));
        btnExport.disableProperty().bind(Bindings.isEmpty(list));
        btnConfirm.disableProperty().bind(((
                iname.textProperty().isEmpty())
                .or(iaddress.textProperty().isEmpty())
                .or(icity.textProperty().isEmpty())
                .or(ipostal.textProperty().isEmpty())
                .or(iphone.textProperty().isEmpty())
                .or(iemail.textProperty().isEmpty())));
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, list.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Konsumen> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        table.setRoot(null);
        TreeItem<Konsumen> rootItem = new TreeItem<>();
        for (Konsumen konsumen : sortedData) {
            TreeItem<Konsumen> item = new TreeItem<>(konsumen);
            rootItem.getChildren().add(item);
        }
        table.setRoot(rootItem);
    }

    private void setFilterPagination() {
        pages = (int) (Math.ceil(list.size() * 1.0 / 10));
        filteredData = new FilteredList<>(list, p -> true);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.ceil(filteredData.size() * 1.0 / 10);
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(
                    konsumen -> newValue == null || newValue.isEmpty() ||
                            konsumen.getCustomerName().toLowerCase().contains(newValue.toLowerCase()) ||
                            konsumen.getCustomerEmail().toLowerCase().contains(newValue.toLowerCase()));
            changeTableView(pagination.getCurrentPageIndex());
        });

        pagination.setPageCount(pages);
        pagination.setCurrentPageIndex(0);
        changeTableView(0);
        pagination.currentPageIndexProperty().addListener(
                (observable, oldValue, newValue) -> changeTableView(newValue.intValue()));

        IntegerProperty maxPageIndex = new SimpleIntegerProperty();
        maxPageIndex.set(pages - 1);
        btnPref.disableProperty().bind(pagination.currentPageIndexProperty().isEqualTo(0));
        filteredData.addListener((ListChangeListener<Konsumen>) c -> {
            pages = (int) Math.ceil(c.getList().size() * 1.0 / 10);
            int currentIndex = pagination.getCurrentPageIndex();
            if (currentIndex > pages - 1) {
                pagination.setCurrentPageIndex(pages - 1);
            }
            pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
            maxPageIndex.set(pages - 1);
        });

        btnNext.setDisable(pagination.currentPageIndexProperty().getValue() == maxPageIndex.intValue());
        maxPageIndex.addListener((observable, oldValue, newValue) -> {
            btnNext.setDisable(pagination.currentPageIndexProperty().intValue() == newValue.intValue());
        });

        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            pageCount.setText("Page " + (index + 1) + " of " + pages);
            btnNext.setDisable(newValue.intValue() >= maxPageIndex.intValue());
        });
        pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
    }

    private void setFormEdit(Konsumen konsumen) {
        iid.setText(konsumen.getCustomerId());
        iname.setText(konsumen.getCustomerName());
        iaddress.setText(konsumen.getCustomerAddress());
        icity.setText(konsumen.getCustomerCity());
        ipostal.setText(konsumen.getCustomerPostal());
        iphone.setText(konsumen.getCustomerPhone());
        iemail.setText(konsumen.getCustomerEmail());
    }

    private void print() {
        try {
            JRBeanCollectionDataSource customerDataSource = new JRBeanCollectionDataSource(list);
            Map<String, Object> param = new HashMap<>();

            param.put("title", "Laporan");
            param.put("customerDataSource", customerDataSource);
            JasperReport design = JasperCompileManager.compileReport(Objects.requireNonNull(
                    Main.class.getResource("jasper/konsumen.jrxml")).getPath());

            JasperPrint jasperPrint = JasperFillManager.fillReport(design, param, new JREmptyDataSource());

            JasperViewer.viewReport(jasperPrint, false);
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    private void export() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Konsumen");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID Konsumen");
        headerRow.createCell(1).setCellValue("Nama Konsumen");
        headerRow.createCell(2).setCellValue("Alamat");
        headerRow.createCell(3).setCellValue("Kota");
        headerRow.createCell(4).setCellValue("Kode Pos");
        headerRow.createCell(5).setCellValue("Telepon");
        headerRow.createCell(6).setCellValue("Email");

        CellStyle priceStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        priceStyle.setDataFormat(dataFormat.getFormat("[$IDR] #,##0.00"));

        int rowNum = 1;
        for (Konsumen k : list) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(Integer.parseInt(k.getCustomerId()));
            dataRow.createCell(1).setCellValue(k.getCustomerName());
            dataRow.createCell(2).setCellValue(k.getCustomerAddress());
            dataRow.createCell(3).setCellValue(k.getCustomerCity());
            dataRow.createCell(4).setCellValue(k.getCustomerPostal());
            dataRow.createCell(5).setCellValue(k.getCustomerPhone());
            dataRow.createCell(6).setCellValue(k.getCustomerEmail());
        }

        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(getExportPath(root))) {
            workbook.write(fileOut);
            if (workbook.getSheet("Konsumen") != null)
                mainController.setDialog(true, "Report succesfully exported to WorkBook");
            else
                mainController.setDialog(false, "Failed to save file");

            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
