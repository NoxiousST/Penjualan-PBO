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
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Jual;
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
import java.time.LocalDate;
import java.util.*;

import static me.stiller.utils.Helper.*;

public class BarangController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Barang> table;

    @FXML
    private JFXTreeTableColumn<Barang, String> cselect, cid, cname, cunit, cprice, cstock, cmin, caction;

    @FXML
    private Pagination pagination;

    @FXML
    private JFXButton btnCancel, btnConfirm, btnPref, btnNext, btnInsert, btnEdit, btnPrint, btnExport;

    @FXML
    private Label pageCount;

    @FXML
    private JFXTextField search, iid, iname, iprice, istock, imin;

    @FXML
    private JFXComboBox<String> iunit;

    @Inject
    DataRepository dataRepository;

    private final Server server = new Server();
    private final ArrayList<JFXTreeTableColumn<Barang, String>> columns = new ArrayList<>();
    private final ArrayList<JFXTextField> inputs = new ArrayList<>();
    private ObservableList<Barang> list;
    private FilteredList<Barang> filteredData;
    private boolean editSelected = false;
    private int pages;

    Logger log = LogManager.getLogger(BarangController.class.getName());
    private MainController mainController;

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getBarangComponent().inject(this);

        list = dataRepository.getBarangList();
        columns.addAll(List.of(cselect, cid, cname, cunit, cprice, cstock, cmin, caction));
        inputs.addAll(List.of(iid, iname, iprice, istock, imin));
        iunit.getItems().setAll("buah", "biji", "lembar", "liter", "gram", "other");
        initializeTable();
        setForm();

        root.setOnMousePressed(event -> removeFocus());
    }

    public void removeFocus() {
        root.getParent().requestFocus();
    }

    private void initializeTable() {
        cid.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemId"));
        cname.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemName"));
        cunit.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemUnit"));
        cprice.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemPriceFormatted"));
        cstock.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemStock"));
        cmin.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemMinStock"));

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
                    Barang barang = getTableRow().getItem();
                    if (newValue.equals(listMenu.getItems().get(0))) {
                        table.getSelectionModel().select(getIndex());
                        editSelected = true;
                        if (!table.getColumns().contains(cselect)) table.getColumns().add(0, cselect);
                        setFormEdit(barang);

                        btnEdit.getStyleClass().setAll("btn-edit-selected");
                        btnInsert.getStyleClass().setAll("btn-insert");

                    } else if (newValue.equals(listMenu.getItems().get(1))) {
                        if (server.delete(barang))
                            mainController.setDialog(true, "Item removed successfully");
                        else
                            mainController.setDialog(true, "There was an error while removing item");
                        inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
                        dataRepository.setBarangList(server.readBarang());
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

        TreeItem<Barang> root = new RecursiveTreeItem<>(dataRepository.getBarangList(), RecursiveTreeObject::getChildren);
        table.getColumns().setAll(columns);
        table.getColumns().remove(cselect);
        table.setRoot(root);
        table.setShowRoot(false);
        setFilterPagination();

    }

    private void setForm() {
        iunit.valueProperty().set(null);
        btnCancel.setGraphic(getIcon("close"));
        btnConfirm.setGraphic(getIcon("check"));
        btnPref.setGraphic(getIcon("left"));
        btnNext.setGraphic(getIcon("right"));

        btnInsert.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            iunit.valueProperty().set(null);

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
            Barang barang = new Barang();
            barang.setItemId(iid.getText());
            barang.setItemName(iname.getText());
            barang.setItemUnit(iunit.getValue());
            barang.setItemPrice(Double.parseDouble(iprice.getText()));
            barang.setItemStock(istock.getText());
            barang.setItemMinStock(imin.getText());
            if (!editSelected) {
                if (server.insert(barang)) mainController.setDialog(true, "Item added successfully");
                else mainController.setDialog(true, "There was an error while adding item");
            } else {
                if (server.update(barang)) mainController.setDialog(true, "Item updated successfully");
                else mainController.setDialog(true, "There was an error while updating the item");
            }
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            dataRepository.setBarangList(server.readBarang());
            changeTableView(0);
        });

        btnCancel.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            iunit.valueProperty().set(null);

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
            if (index < pagination.getPageCount()) pagination.setCurrentPageIndex(index + 1);
        });

        btnPrint.disableProperty().bind(Bindings.isEmpty(list));
        btnExport.disableProperty().bind(Bindings.isEmpty(list));
        btnConfirm.disableProperty().bind((iname.textProperty().isEmpty())
                .or(iunit.valueProperty().isNull())
                .or(iprice.textProperty().isEmpty())
                .or(istock.textProperty().isEmpty())
                .or(imin.textProperty().isEmpty()));
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, dataRepository.getBarangList().size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Barang> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        table.setRoot(null);
        TreeItem<Barang> rootItem = new TreeItem<>();
        for (Barang barang : sortedData) {
            TreeItem<Barang> item = new TreeItem<>(barang);
            rootItem.getChildren().add(item);
        }
        table.setRoot(rootItem);
    }

    private void setFilterPagination() {
        pages = (int) (Math.ceil(dataRepository.getBarangList().size() * 1.0 / 10));
        filteredData = new FilteredList<>(dataRepository.getBarangList(), p -> true);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.ceil(filteredData.size() * 1.0 / 10);
            if (pagination.getCurrentPageIndex() > pages)
                pagination.setCurrentPageIndex(pages);

            filteredData.setPredicate(
                    barang -> newValue == null || newValue.isEmpty() ||
                            barang.getItemName().toLowerCase().contains(newValue.toLowerCase()) ||
                            barang.getItemPrice().toString().contains(newValue));
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
        filteredData.addListener((ListChangeListener<Barang>) c -> {
            pages = (int) Math.ceil(c.getList().size() * 1.0 / 10);
            int currentIndex = pagination.getCurrentPageIndex();
            if (currentIndex > pages - 1) {
                pagination.setCurrentPageIndex(pages - 1);
            }
            pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
            maxPageIndex.set(pages - 1);
        });

        btnNext.setDisable(pagination.currentPageIndexProperty().getValue() == maxPageIndex.intValue());
        maxPageIndex.addListener((observable, oldValue, newValue) ->
                btnNext.setDisable(pagination.currentPageIndexProperty().intValue() == newValue.intValue()));

        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            pageCount.setText("Page " + (index + 1) + " of " + pages);
            btnNext.setDisable(newValue.intValue() >= maxPageIndex.intValue());
        });
        pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
    }

    private void setFormEdit(Barang barang) {
        iid.setText(barang.getItemId());
        iname.setText(barang.getItemName());
        iunit.setValue(barang.getItemUnit());
        iprice.setText(String.valueOf(barang.getItemPrice()));
        istock.setText(barang.getItemStock());
        imin.setText(barang.getItemMinStock());
    }

    private void print() {
        try {
            JRBeanCollectionDataSource itemDataSource = new JRBeanCollectionDataSource(list);
            Map<String, Object> param = new HashMap<>();

            param.put("title", "Laporan");
            param.put("itemDataSource", itemDataSource);
            JasperReport design = JasperCompileManager.compileReport(Objects.requireNonNull(
                    Main.class.getResource("jasper/barang.jrxml")).getPath());

            JasperPrint jasperPrint = JasperFillManager.fillReport(design, param, new JREmptyDataSource());

            JasperViewer.viewReport(jasperPrint, false);
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    private void export() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Barang");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID Barang");
        headerRow.createCell(1).setCellValue("Nama Barang");
        headerRow.createCell(2).setCellValue("Unit");
        headerRow.createCell(3).setCellValue("Harga");
        headerRow.createCell(4).setCellValue("Stok");
        headerRow.createCell(5).setCellValue("Minimum Stok");

        CellStyle priceStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        priceStyle.setDataFormat(dataFormat.getFormat("[$IDR] #,##0.00"));

        int rowNum = 1;
        for (Barang b : list) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(Integer.parseInt(b.getItemId()));
            dataRow.createCell(1).setCellValue(b.getItemName());
            dataRow.createCell(2).setCellValue(b.getItemUnit());
            dataRow.createCell(4).setCellValue(Integer.parseInt(b.getItemStock()));
            dataRow.createCell(5).setCellValue(Integer.parseInt(b.getItemMinStock()));
            XSSFCell priceCell = dataRow.createCell(3);
            priceCell.setCellValue(b.getItemPrice());
            priceCell.setCellStyle(priceStyle);
        }

        for (int i = 0; i <= 5; i++) sheet.autoSizeColumn(i);


        try (FileOutputStream fileOut = new FileOutputStream(getExportPath(root))) {
            workbook.write(fileOut);
            if (workbook.getSheet("Barang") != null)
                mainController.setDialog(true, "Report succesfully exported to WorkBook");
            else
                mainController.setDialog(false, "Failed to save file");

            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
