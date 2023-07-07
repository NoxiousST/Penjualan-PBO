package me.stiller.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.Barang;
import me.stiller.data.models.SuppliedItem;
import me.stiller.data.models.Supplier;
import me.stiller.repository.DataRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
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

import static me.stiller.utils.Helper.*;

public class SupplierController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Supplier> table;

    @FXML
    private JFXTreeTableColumn<Supplier, String> cselect, cid, cname, cemail, caddress, citem, caction;

    @FXML
    private Pagination pagination;

    @FXML
    private JFXButton btnCancel, btnConfirm, btnPref, btnNext, btnInsert, btnEdit, btnExport;

    @FXML
    private Label pageCount;

    @FXML
    private JFXTextField search, iId, iName, iEmail, iAddress, itemName, itemPrice;

    @FXML
    private JFXComboBox<String> itemId, itemIndex;

    @Inject
    DataRepository dataRepository;

    private ObjectMapper mapper = new ObjectMapper();
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
        columns.addAll(List.of(cselect, cid, cname, cemail, caddress, citem, caction));
        inputs.addAll(List.of(iId, iName, iEmail, iAddress, itemName, itemPrice));
        itemId.getItems().setAll(dataRepository.getBarangIds());
        itemIndex.getItems().add("New");
        initializeTable();
        setForm();

        removeClickFocus();
    }

    private void removeClickFocus() {
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

        citem.setCellFactory(param -> new TreeTableCell<>() {
            final HBox hbox = new HBox();
            final VBox vbox = new VBox();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (getTableRow().getItem() != null) {
                    try {
                        Supplier supplier = getTableRow().getItem();
                        JsonNode jsonNode = mapper.readTree(supplier.getItemId());
                        List<SuppliedItem> listSupplied = new ArrayList<>();
                        for (JsonNode node : jsonNode) {
                            String id = node.get("itemId").asText();
                            String name = node.get("itemName").asText();
                            double price = node.get("itemPrice").asDouble();

                            listSupplied.add(new SuppliedItem(id, name, price));
                        }

                        listSupplied.forEach(s -> {
                            Label l = new Label(s.getItemId() + "  ~  " + s.getItemName() + "  ~  " + formatPrice(s.getItemPrice()));
                            l.setAlignment(Pos.CENTER);
                            vbox.getChildren().add(l);
                        });

                        vbox.setSpacing(6);
                        hbox.getChildren().setAll(vbox);
                        hbox.setAlignment(Pos.CENTER);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        setGraphic(hbox);
                        setText(null);
                    }
                }
            }

            private ObservableList<String> intoList(String stringItem) {
                ObservableList<String> myList = FXCollections.observableArrayList();
                if (stringItem != null) {
                    if (stringItem.contains(", "))
                        myList.addAll(Arrays.asList(stringItem.split(", ")));
                    else
                        myList.add(stringItem);
                }
                return myList;
            }

            private String intoString(List<String> listItem) {
                StringBuilder sb = new StringBuilder();
                if (listItem != null && listItem.size() > 0) {
                    for (String item : listItem) {
                        sb.append(item).append(", ");
                    }
                    sb.setLength(sb.length() - 2);
                } else {
                    log.debug("WHAT");
                }
                return sb.toString();
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

        caction.setMaxWidth(72);
        caction.setMinWidth(72);
        caction.setCellFactory(param -> new JFXTreeTableCell<>() {
            final JFXButton action = new JFXButton();
            final JFXListView<Label> listMenu = new JFXListView<>();
            final JFXPopup popup = new JFXPopup(listMenu);

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (getTableRow().getItem() != null) {
                    Supplier supplier = getTableRow().getItem();

                    listMenu.getStyleClass().add("popup-list");
                    listMenu.getItems().setAll(new Label("Edit"), new Label("Delete"), new Label("Remove Item"));
                    listMenu.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

                    popup.getStyleClass().add("popup");
                    action.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    action.setRipplerFill(Paint.valueOf("white"));
                    action.setGraphic(getIcon("action"));
                    action.setOnMouseClicked(event -> {
                        int size = itemsToList(supplier).size();
                        listMenu.getItems().get(2).setDisable(size <= 1);
                        popup.show(action,
                                JFXPopup.PopupVPosition.TOP,
                                JFXPopup.PopupHPosition.LEFT,
                                event.getX(), event.getY());
                    });

                    listMenu.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
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
                        } else if (newValue.intValue() == 2 && !listMenu.getItems().get(2).isDisabled()) {
                            JFXListView<String> itemList = new JFXListView<>();
                            JFXPopup itemsPopup = new JFXPopup(itemList);
                            itemList.getStyleClass().add("popup-list");
                            itemsPopup.getStyleClass().add("popup");

                            List<SuppliedItem> sp = itemsToList(supplier);
                            for (int i = 0; i < sp.size(); i++) {
                                itemList.getItems().add(String.valueOf(i));
                            }

                            itemsPopup.show(action,
                                    JFXPopup.PopupVPosition.TOP,
                                    JFXPopup.PopupHPosition.LEFT);

                            itemList.getSelectionModel().selectedIndexProperty().addListener((obs, oVal, nVal) -> {
                                sp.remove(nVal.intValue());
                                String items = mapper.valueToTree(sp).toString();
                                supplier.setItemId(items);
                                if (server.update(supplier)) mainController.setDialog(true, "Data updated successfully");

                                inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
                                dataRepository.setSupplierList(server.readSupplier());
                                changeTableView(0);
                                table.refresh();
                            });

                            itemList.setOnMouseClicked(e -> Platform.runLater(() -> {
                                itemList.getSelectionModel().clearSelection();
                                itemsPopup.hide();
                            }));
                        }
                        table.refresh();
                    });

                    listMenu.setOnMouseClicked(e -> Platform.runLater(() -> {
                        listMenu.getSelectionModel().clearSelection();
                        popup.hide();
                    }));
                }

                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(action);
                    setText(null);
                }
            }
        });

        TreeItem<Supplier> root = new RecursiveTreeItem<>(dataRepository.getSupplierList(), RecursiveTreeObject::getChildren);
        table.getColumns().setAll(columns);
        table.getColumns().remove(cselect);
        table.setRoot(root);
        table.setShowRoot(false);
        setFilterPagination();
    }

    private void setForm() {
        itemId.valueProperty().set(null);
        btnCancel.setGraphic(getIcon("close"));
        btnConfirm.setGraphic(getIcon("check"));
        btnPref.setGraphic(getIcon("left"));
        btnNext.setGraphic(getIcon("right"));

        btnInsert.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            itemId.valueProperty().set(null);
            itemIndex.getItems().setAll("New");
            itemIndex.setValue("New");

            btnInsert.getStyleClass().setAll("btn-insert-selected");
            btnEdit.getStyleClass().setAll("btn-edit");
            editSelected = false;
            table.getColumns().remove(cselect);
            table.refresh();
        });

        btnEdit.setOnMouseClicked(event -> {
            btnEdit.getStyleClass().setAll("btn-edit-selected");
            btnInsert.getStyleClass().setAll("btn-insert");

            editSelected = true;
            if (!table.getColumns().contains(cselect))
                table.getColumns().add(0, cselect);
            table.refresh();
        });

        btnConfirm.setOnMouseClicked(event -> {
            try {
                Supplier supplier = new Supplier();
                supplier.setSupplierId(iId.getText());
                supplier.setSupplierName(iName.getText());
                supplier.setSupplierEmail(iEmail.getText());
                supplier.setSupplierAddress(iAddress.getText());

                String id = itemId.getValue();
                String name = itemName.getText();
                String price = itemPrice.getText();
                SuppliedItem newItem = new SuppliedItem(id, name, Double.parseDouble(price));

                if (!editSelected) {
                    List<SuppliedItem> newSuppliedItem = new ArrayList<>(List.of(newItem));
                    String newItems = mapper.valueToTree(newSuppliedItem).toString();
                    supplier.setItemId(newItems);
                    if (server.insert(supplier)) mainController.setDialog(true, "Item added successfully");
                    else mainController.setDialog(false, "There was an error while adding supplier");
                } else {
                    Supplier sp = dataRepository.getSelectedSupplier();
                    List<SuppliedItem> suppliedItem = itemsToList(sp);
                    if (itemIndex.getValue().equals("New")) suppliedItem.add(newItem);
                    else suppliedItem.set(Integer.parseInt(itemIndex.getValue()), newItem);

                    String items = mapper.valueToTree(suppliedItem).toString();
                    supplier.setItemId(items);

                    if (server.update(supplier)) mainController.setDialog(true, "Item updated successfully");
                    else mainController.setDialog(false, "There was an error while updating the data");
                }
                inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
                dataRepository.setSupplierList(server.readSupplier());
                changeTableView(0);
                table.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        btnCancel.setOnMouseClicked(event -> {
            inputs.forEach(input -> input.textProperty().set(Strings.EMPTY));
            itemId.valueProperty().set(null);

            btnInsert.getStyleClass().setAll("btn-insert-selected");
            btnEdit.getStyleClass().setAll("btn-edit");
        });

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

        itemId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Barang barang = dataRepository.getBarang(newValue.intValue());
            dataRepository.setSelectedBarang(barang);
            itemName.setText(barang.getItemName());
        });

        btnExport.disableProperty().bind(Bindings.isEmpty(list));
        btnConfirm.disableProperty().bind((iName.textProperty().isEmpty())
                .or(iEmail.textProperty().isNull())
                .or(iAddress.textProperty().isEmpty())
                .or(itemId.valueProperty().isNull()));
    }

    private void setFormEdit(Supplier supplier) {
        dataRepository.setSelectedSupplier(supplier);
        iId.setText(supplier.getSupplierId());
        iName.setText(supplier.getSupplierName());
        iEmail.setText(supplier.getSupplierEmail());
        iAddress.setText(supplier.getSupplierAddress());

        List<SuppliedItem> listSupplied = itemsToList(supplier);
        itemIndex.getItems().clear();
        for (int i = 0; i < listSupplied.size(); i++) {
            itemIndex.getItems().add(String.valueOf(i));
        }
        itemIndex.getItems().add("New");
        itemIndex.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals("New")) {

            } else {
                SuppliedItem sp = listSupplied.get(Integer.parseInt(newValue));
                itemId.setValue(sp.getItemId());
                itemPrice.setText(String.valueOf(sp.getItemPrice()));
            }
        });
    }

    private List<SuppliedItem> itemsToList(Supplier supplier) {
        try {
            JsonNode jsonNode = mapper.readTree(supplier.getItemId());
            List<SuppliedItem> listSupplied = new ArrayList<>();
            for (JsonNode node : jsonNode) {
                String id = node.get("itemId").asText();
                String name = node.get("itemName").asText();
                double price = node.get("itemPrice").asDouble();
                listSupplied.add(new SuppliedItem(id, name, price));
            }

            return listSupplied;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

    private void export() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Supplier");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Supplier ID");
        headerRow.createCell(1).setCellValue("Supplier Name");
        headerRow.createCell(2).setCellValue("Supplier Email");
        headerRow.createCell(3).setCellValue("Address");
        headerRow.createCell(4).setCellValue("Item ID");
        headerRow.createCell(5).setCellValue("Item Name");
        headerRow.createCell(6).setCellValue("Item Price");

        int rowNum = 1;
        for (Supplier supplier : filteredData) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(Integer.parseInt(supplier.getSupplierId()));
            dataRow.createCell(1).setCellValue(supplier.getSupplierName());
            dataRow.createCell(2).setCellValue(supplier.getSupplierEmail());
            dataRow.createCell(3).setCellValue(supplier.getSupplierAddress());

            try {
                JsonNode jsonNode = mapper.readTree(supplier.getItemId());
                int loop = 0;
                for (JsonNode node : jsonNode) {
                    dataRow.createCell(4).setCellValue(node.get("itemId").asInt());
                    dataRow.createCell(5).setCellValue(node.get("itemName").asText());
                    dataRow.createCell(6).setCellValue(node.get("itemPrice").asDouble());
                    loop++;
                    if (loop != jsonNode.size())
                        dataRow = sheet.createRow(rowNum++);
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
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
