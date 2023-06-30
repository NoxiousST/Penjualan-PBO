package me.stiller.controller;


import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Supplier;
import me.stiller.data.models.Supplier;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.stiller.utils.Helper.getExportPath;
import static me.stiller.utils.Helper.getIcon;

public class SupplierController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Supplier> table;

    @FXML
    private JFXTreeTableColumn<Supplier, String> cselect, cid, cname, cemail, caddress, ccity, citem, caction;

    @FXML
    private Pagination pagination;

    @FXML
    private JFXButton btnCancel, btnConfirm, btnPref, btnNext, btnInsert, btnEdit, btnPrint, btnExport;

    @FXML
    private Label pageCount;

    @FXML
    private JFXTextField search, iId, iName, iEmail, iAddress, iCity;

    @FXML
    private JFXComboBox<String> iItem;

    @Inject
    DataRepository dataRepository;

    private final Server server = new Server();
    private final ArrayList<JFXTreeTableColumn<Supplier, String>> columns = new ArrayList<>();
    private final ArrayList<JFXTextField> inputs = new ArrayList<>();
    private ObservableList<Supplier> list;
    private FilteredList<Supplier> filteredData;
    private boolean editSelected = false;
    private int pages;

    Logger log = LogManager.getLogger(SupplierController.class.getName());
    private MainController mainController;

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        list = dataRepository.getSupplierList();
        columns.addAll(List.of(cselect, cid, cname, cemail, caddress, ccity, citem, caction));
        inputs.addAll(List.of(iId, iName, iEmail, iAddress, iCity));
        iItem.getItems().setAll(dataRepository.getBarangIds());
        initializeTable();
        setForm();

        root.setOnMousePressed(event -> removeFocus());
    }

    public void removeFocus() {
        root.getParent().requestFocus();
    }

    private void initializeTable() {
        cid.setCellValueFactory(new TreeItemPropertyValueFactory<>("supplierId"));
        cname.setCellValueFactory(new TreeItemPropertyValueFactory<>("supplierName"));
        cemail.setCellValueFactory(new TreeItemPropertyValueFactory<>("supplierEmail"));
        caddress.setCellValueFactory(new TreeItemPropertyValueFactory<>("supplierAddress"));
        ccity.setCellValueFactory(new TreeItemPropertyValueFactory<>("supplierCity"));

        citem.setCellFactory(param -> new TreeTableCell<>() {
            final VBox vbox = new VBox();
            final HBox hbox = new HBox();
            final Label label = new Label();
            final JFXButton action = new JFXButton();
            final JFXListView<Label> listMenu = new JFXListView<>();
            final JFXPopup popup = new JFXPopup(listMenu);
            final JFXComboBox<String> comboBox = new JFXComboBox<>();
            StringBuilder sb = new StringBuilder();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (getTableRow().getItem() != null) {
                    Supplier supplier = getTableRow().getItem();
                    sb = new StringBuilder(supplier.getItemId());
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
                    listMenu.getItems().setAll(new Label("Edit"), new Label("Delete"), new Label("Add Item"), new Label("Remove Item"));
                    listMenu.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    listMenu.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {

                        String[] splitArray = supplier.getItemId().split(",");
                        ObservableList<String> listId = FXCollections.observableArrayList(splitArray);
                        log.debug(listId.size());

                        JFXListView<Label> itemIds = new JFXListView<>();
                        itemIds.getStyleClass().add("popup-list");
                        itemIds.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

                        JFXPopup itemPopup = new JFXPopup(itemIds);
                        itemPopup.getStyleClass().add("popup");

                        if (newValue.intValue() == 0) {
                            table.getSelectionModel().select(getIndex());
                            editSelected = true;
                            if (!table.getColumns().contains(cselect)) table.getColumns().add(0, cselect);
                            setFormEdit(supplier);
                            btnEdit.getStyleClass().setAll("btn-edit-selected");
                            btnInsert.getStyleClass().setAll("btn-insert");
                        } else if (newValue.intValue() == 1) {
                            if (server.delete(supplier))
                                mainController.setDialog(true, "Item removed successfully");
                            else
                                mainController.setDialog(false, "There was an error while removing supplier");
                            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
                            dataRepository.setSupplierList(server.readSupplier());
                            changeTableView(0);

                        } else if (newValue.intValue() == 2) {
                            comboBox.setVisible(true);
                            comboBox.setManaged(true);
                            comboBox.show();
                        } else if (newValue.intValue() == 3) {
                            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
                            dataRepository.setSupplierList(server.readSupplier());
                            changeTableView(0);
                        }
                    });

                    comboBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
                        if (!newValue) {
                            comboBox.setVisible(false);
                            comboBox.setManaged(false);
                        }
                    });

                    hbox.getChildren().setAll(label, action);
                    HBox.setHgrow(label, Priority.ALWAYS);
                    widthProperty().addListener((observable, oldValue, newValue) -> {
                        label.setMaxWidth(newValue.intValue());
                    });
                    label.setAlignment(Pos.CENTER);
                    label.setText(sb.toString());

                    comboBox.setVisible(false);
                    comboBox.setManaged(false);

                    comboBox.getStyleClass().add("combobox");
                    dataRepository.getBarangList().forEach(barang -> {
                        String str = supplier.getItemId();
                        Pattern pattern = Pattern.compile("\\((\\d+)\\)");
                        ArrayList<String> numbersList = new ArrayList<>();
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            String id = matcher.group(1);
                            numbersList.add(id);
                        }

                        if (!numbersList.contains(barang.getItemId())) {
                            String s = barang.getItemName() + " (" + barang.getItemId() + ")";
                            comboBox.getItems().add(s);
                        }
                    });

                    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        sb.append(newValue).append(", ");
                        supplier.setItemId(sb.toString());
                        changeTableView(0);
                        comboBox.getSelectionModel().clearSelection();
                        comboBox.setVisible(false);
                        comboBox.setManaged(false);
                    });

                    comboBox.setOnMouseClicked(event -> {
                        comboBox.getSelectionModel().clearSelection();
                        comboBox.hide();
                    });

                    vbox.getChildren().addAll(hbox, comboBox);
                    vbox.setAlignment(Pos.CENTER);

                    listMenu.setOnMouseClicked(e -> Platform.runLater(() -> {
                        listMenu.getSelectionModel().clearSelection();
                        popup.hide();
                    }));

                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        setGraphic(vbox);
                        setText(null);

                    }
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

        TreeItem<Supplier> root = new RecursiveTreeItem<>(dataRepository.getSupplierList(), RecursiveTreeObject::getChildren);
        table.getColumns().setAll(columns);
        table.getColumns().remove(cselect);
        table.getColumns().remove(caction);
        table.setRoot(root);
        table.setShowRoot(false);
        setFilterPagination();

    }

    private void setForm() {
        iItem.valueProperty().set(null);
        btnCancel.setGraphic(getIcon("close"));
        btnConfirm.setGraphic(getIcon("check"));
        btnPref.setGraphic(getIcon("left"));
        btnNext.setGraphic(getIcon("right"));

        btnInsert.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            iItem.valueProperty().set(null);

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
            Supplier supplier = new Supplier();
            supplier.setSupplierId(iId.getText());
            supplier.setSupplierName(iName.getText());
            supplier.setSupplierEmail(iEmail.getText());
            supplier.setSupplierAddress(iAddress.getText());
            supplier.setSupplierCity(iCity.getText());

            Barang barang = dataRepository.getBarang(iItem.getValue());
            supplier.setItemId(barang.getItemName() + " (" + barang.getItemId() + ")");
            if (!editSelected) {
                if (server.insert(supplier)) mainController.setDialog(true, "Item added successfully");
                else mainController.setDialog(false, "There was an error while adding supplier");
            } else {
                if (server.update(supplier)) mainController.setDialog(true, "Item updated successfully");
                else mainController.setDialog(false, "There was an error while updating the data");
            }
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            dataRepository.setSupplierList(server.readSupplier());
            changeTableView(0);
        });

        btnCancel.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            iItem.valueProperty().set(null);

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
        btnConfirm.disableProperty().bind((iName.textProperty().isEmpty())
                .or(iEmail.textProperty().isNull())
                .or(iAddress.textProperty().isEmpty())
                .or(iCity.textProperty().isEmpty())
                .or(iItem.valueProperty().isNull()));
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, dataRepository.getSupplierList().size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Supplier> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        TreeItem<Supplier> rootItem = new TreeItem<>();
        for (Supplier supplier : sortedData) {
            TreeItem<Supplier> item = new TreeItem<>(supplier);
            rootItem.getChildren().add(item);
        }
        table.setRoot(rootItem);
    }

    private void setFilterPagination() {
        pages = (int) (Math.ceil(dataRepository.getSupplierList().size() * 1.0 / 10));
        filteredData = new FilteredList<>(dataRepository.getSupplierList(), p -> true);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.ceil(filteredData.size() * 1.0 / 10);
            if (pagination.getCurrentPageIndex() > pages)
                pagination.setCurrentPageIndex(pages);

            filteredData.setPredicate(
                    supplier -> newValue == null || newValue.isEmpty() ||
                            supplier.getSupplierName().toLowerCase().contains(newValue.toLowerCase()) ||
                            supplier.getSupplierEmail().toLowerCase().contains(newValue));
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
        filteredData.addListener((ListChangeListener<Supplier>) c -> {
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

    private void setFormEdit(Supplier supplier) {
        iId.setText(supplier.getSupplierId());
        iName.setText(supplier.getSupplierName());
        iEmail.setText(supplier.getSupplierEmail());
        iAddress.setText(supplier.getSupplierAddress());
        iCity.setText(supplier.getSupplierCity());
        iItem.setValue(supplier.getItemId());
    }

    private void print() {
        try {
            JRBeanCollectionDataSource itemDataSource = new JRBeanCollectionDataSource(list);
            Map<String, Object> param = new HashMap<>();

            param.put("title", "Laporan");
            param.put("itemDataSource", itemDataSource);
            JasperReport design = JasperCompileManager.compileReport(Objects.requireNonNull(
                    Main.class.getResource("jasper/supplier.jrxml")).getPath());

            JasperPrint jasperPrint = JasperFillManager.fillReport(design, param, new JREmptyDataSource());

            JasperViewer.viewReport(jasperPrint, false);
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    private void export() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Supplier");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID Supplier");
        headerRow.createCell(1).setCellValue("Nama");
        headerRow.createCell(2).setCellValue("Email");
        headerRow.createCell(3).setCellValue("Alamat");
        headerRow.createCell(4).setCellValue("Kota");
        headerRow.createCell(5).setCellValue("Item ID");

        int rowNum = 1;
        for (Supplier b : list) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(Integer.parseInt(b.getItemId()));
            dataRow.createCell(1).setCellValue(b.getSupplierName());
            dataRow.createCell(2).setCellValue(b.getSupplierEmail());
            dataRow.createCell(3).setCellValue(b.getSupplierAddress());
            dataRow.createCell(4).setCellValue(b.getSupplierCity());
            dataRow.createCell(5).setCellValue(Integer.parseInt(b.getItemId()));
        }

        for (int i = 0; i <= 5; i++) sheet.autoSizeColumn(i);


        try (FileOutputStream fileOut = new FileOutputStream(getExportPath(root))) {
            workbook.write(fileOut);
            if (workbook.getSheet("Supplier") != null)
                mainController.setDialog(true, "Report succesfully exported to WorkBook");
            else
                mainController.setDialog(false, "Failed to save file");

            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
