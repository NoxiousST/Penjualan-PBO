package me.stiller.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import me.stiller.Main;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Pembelian;
import me.stiller.data.models.Pembelian;
import me.stiller.repository.DataRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import static me.stiller.utils.Helper.getExportPath;
import static me.stiller.utils.Helper.getIcon;

public class PembelianController implements Initializable {


    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Pembelian> table;

    @FXML
    private JFXTreeTableColumn<Pembelian, String> cOrderId, cOrderDate, cSupplierName, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal;

    @FXML
    private JFXButton btnPref, btnNext, btnExport;

    @FXML
    private JFXTextField search;

    @FXML
    private DatePicker fdate, ldate;

    @FXML
    private Label pageCount;

    @FXML
    private Pagination pagination;

    private ObjectMapper mapper = new ObjectMapper();
    private MainController mainController;
    private ArrayList<JFXTreeTableColumn<Pembelian, String>> columns = new ArrayList<>();
    private ObservableList<Pembelian> list;
    private FilteredList<Pembelian> filteredData;
    private int pages;

    Logger log = LogManager.getLogger(PembelianController.class.getName());

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        list = dataRepository.getPembelianList();

        columns.addAll(List.of(cOrderId, cOrderDate, cSupplierName, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal));
        table.setShowRoot(false);
        initializeColumn();

        setFilterPagination();
        setForm();
        list.addListener((ListChangeListener<Pembelian>) c -> changeTableView(0));


        LocalDate today = LocalDate.now();
        StringConverter<LocalDate> converter = new LocalDateStringConverter(DateTimeFormatter.ISO_DATE, null);
        fdate.setPrefHeight(28);
        ldate.setPrefHeight(28);

        fdate.setConverter(converter);
        ldate.setConverter(converter);
        fdate.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isAfter(today)) setDisable(true);
                if (ldate.getValue() != null && date.isAfter(ldate.getValue())) setDisable(true);
                if (date.equals(ldate.getValue())) getStyleClass().add("selected");
            }
        });

        ldate.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isAfter(today)) setDisable(true);
                if (fdate.getValue() != null && date.isBefore(fdate.getValue())) setDisable(true);
                if (date.equals(fdate.getValue())) getStyleClass().add("selected");
            }
        });

        btnPref.setGraphic(getIcon("left"));
        btnNext.setGraphic(getIcon("right"));

        root.setOnMousePressed(event -> removeFocus());
    }

    public void removeFocus() {
        root.getParent().requestFocus();
    }

    private void initializeColumn() {
        table.getColumns().setAll(columns);
        log.debug(cOrderId.getStyleClass().get(0));
        cOrderId.setCellValueFactory(new TreeItemPropertyValueFactory<>("orderId"));
        cOrderDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("orderDate"));
        cSupplierName.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerName"));

        columns.removeAll(List.of(cOrderId, cOrderDate));
        columns.forEach(col -> {
            col.setCellFactory(param -> new JFXTreeTableCell<>() {
                final VBox container = new VBox();
                final List<Label> labels = new ArrayList<>();

                @Override
                public void updateItem(String item, boolean empty) {
                    container.setSpacing(5);
                    container.setAlignment(Pos.TOP_CENTER);

                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        if (getTableRow().getItem() != null) {
                            try {
                                Pembelian pembelian = getTableRow().getItem();
                                JsonNode jsonNode = mapper.readTree(pembelian.getItems());
                                for (JsonNode node : jsonNode) {
                                    labels.add(new Label(pembelian.returnItem(col.getId(), node)));
                                }

                                container.getChildren().setAll(labels);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }

                        }
                        setGraphic(container);
                        setText(null);
                    }
                    labels.clear();
                    table.refresh();
                }
            });
        });
    }

    private void setForm() {
        btnPref.setOnAction(event -> {
            int index = pagination.getCurrentPageIndex();
            if (index > 0) {
                pagination.setCurrentPageIndex(index - 1);
            }
            removeFocus();
        });

        btnNext.setOnAction(event -> {
            int index = pagination.getCurrentPageIndex();
            if (index < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(index + 1);
            }
            log.info(pagination.getPageCount());
        });

        btnExport.setOnMouseClicked(event -> export());
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, list.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Pembelian> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        TreeItem<Pembelian> rootItem = new TreeItem<>();
        for (Pembelian penjualan : sortedData) {
            TreeItem<Pembelian> item = new TreeItem<>(penjualan);
            rootItem.getChildren().add(item);
        }
        table.setRoot(rootItem);
        table.refresh();
    }

    private void setFilterPagination() {
        pages = (int) (Math.ceil(list.size() * 1.0 / 10));
        filteredData = new FilteredList<>(list, p -> true);

        fdate.valueProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.ceil(filteredData.size() * 1.0 / 10);
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(pembelian -> {
                LocalDate date = LocalDate.parse(pembelian.getOrderDate());
                List<String> supplierName = stringToListNode(pembelian.getItems(), "supplierName");
                List<String> itemNames = stringToListNode(pembelian.getItems(), "itemName");
                List<String> itemIds = stringToListNode(pembelian.getItems(), "itemId");

                String lowerCaseNewValue = search.getText().toLowerCase();

                boolean isMatch = supplierName.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemNames.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemIds.stream().anyMatch(id -> id.toLowerCase().contains(lowerCaseNewValue));

                if (newValue != null && ldate.getValue() != null) {
                    isMatch = isMatch && (date.isBefore(ldate.getValue().plusDays(1)) &&
                            date.isAfter(newValue.minusDays(1)));
                } else if (newValue != null) {
                    isMatch = isMatch && date.isAfter(newValue.minusDays(1));
                } else if (ldate.getValue() != null) {
                    isMatch = isMatch && date.isBefore(ldate.getValue().plusDays(1));
                }

                return isMatch;
            });
            changeTableView(pagination.getCurrentPageIndex());
            table.refresh();
        });

        ldate.valueProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.ceil(filteredData.size() * 1.0 / 10);
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(pembelian -> {
                LocalDate date = LocalDate.parse(pembelian.getOrderDate());
                List<String> supplierName = stringToListNode(pembelian.getItems(), "supplierName");
                List<String> itemNames = stringToListNode(pembelian.getItems(), "itemName");
                List<String> itemIds = stringToListNode(pembelian.getItems(), "itemId");
                String lowerCaseNewValue = search.getText().toLowerCase();

                boolean isMatch = supplierName.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemNames.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemIds.stream().anyMatch(id -> id.toLowerCase().contains(lowerCaseNewValue));

                if (fdate.getValue() != null && newValue != null) {
                    isMatch = isMatch && (date.isBefore(newValue.plusDays(1)) &&
                            date.isAfter(fdate.getValue().minusDays(1)));
                } else if (fdate.getValue() != null) {
                    isMatch = isMatch && date.isAfter(fdate.getValue().minusDays(1));
                } else if (newValue != null) {
                    isMatch = isMatch && date.isBefore(newValue.plusDays(1));
                }

                return isMatch;
            });
            changeTableView(pagination.getCurrentPageIndex());
        });

        search.textProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.floor(filteredData.size() * 1.0 / 10) + 1;
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(pembelian -> {
                LocalDate date = LocalDate.parse(pembelian.getOrderDate());
                List<String> supplierName = stringToListNode(pembelian.getItems(), "supplierName");
                List<String> itemNames = stringToListNode(pembelian.getItems(), "itemName");
                List<String> itemIds = stringToListNode(pembelian.getItems(), "itemId");
                String lowerCaseNewValue = newValue.toLowerCase();

                boolean isMatch = supplierName.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemNames.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemIds.stream().anyMatch(id -> id.toLowerCase().contains(lowerCaseNewValue));

                if (fdate.getValue() != null && ldate.getValue() != null) {
                    isMatch = isMatch && (date.isBefore(ldate.getValue().plusDays(1)) &&
                            date.isAfter(fdate.getValue().minusDays(1)));
                } else if (fdate.getValue() != null) {
                    isMatch = isMatch && date.isAfter(fdate.getValue().minusDays(1));
                } else if (ldate.getValue() != null) {
                    isMatch = isMatch && date.isBefore(ldate.getValue().plusDays(1));
                }

                return isMatch;

            });
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
        filteredData.addListener((ListChangeListener<Pembelian>) c -> {
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
            log.info("Pages : " + pages);
            log.info("CURRENT " + newValue);
        });
        pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
    }

    private void export() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Pembelian");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Order Id");
        headerRow.createCell(1).setCellValue("Order Date");
        headerRow.createCell(2).setCellValue("Supplier Id");
        headerRow.createCell(3).setCellValue("Supplier Name");
        headerRow.createCell(4).setCellValue("Item Id");
        headerRow.createCell(5).setCellValue("Item Name");
        headerRow.createCell(6).setCellValue("Item Price");
        headerRow.createCell(7).setCellValue("Item Quantity");
        headerRow.createCell(8).setCellValue("Item Total Price");

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setDataFormat((short) 14);

        int rowNum = 1;
        for (Pembelian pembelian : list) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(Integer.parseInt(pembelian.getOrderId()));
            XSSFCell dateCell = dataRow.createCell(1);
            dateCell.setCellValue(LocalDate.parse(pembelian.getOrderDate()));
            dateCell.setCellStyle(cellStyle);

            try {
                JsonNode jsonNode = mapper.readTree(pembelian.getItems());
                for (JsonNode node : jsonNode) {
                    int supplierId = node.get("supplierId").asInt();
                    String supplierName = node.get("supplierName").asText();
                    int itemId = node.get("itemId").asInt();
                    String itemName = node.get("itemName").asText();
                    double itemPrice = node.get("itemPrice").asDouble();
                    int itemQuantity = node.get("itemQuantity").asInt();
                    double itemTotal = node.get("itemTotal").asDouble();
                    dataRow.createCell(2).setCellValue(supplierId);
                    dataRow.createCell(3).setCellValue(supplierName);
                    dataRow.createCell(4).setCellValue(itemId);
                    dataRow.createCell(5).setCellValue(itemName);
                    dataRow.createCell(6).setCellValue(itemPrice);
                    dataRow.createCell(7).setCellValue(itemQuantity);
                    dataRow.createCell(8).setCellValue(itemTotal);
                    dataRow = sheet.createRow(rowNum++);
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i <= 8; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(getExportPath(root))) {
                workbook.write(fileOut);
                if (workbook.getSheet("Pembelian") != null) {
                    mainController.setDialog(true, "Report succesfully exported to WorkBook");
                }
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> stringToListNode(String items, String field) {
        ArrayList<String> stringList = new ArrayList<>();
        try {
            JsonNode jsonNode = mapper.readTree(items);
            for (JsonNode node : jsonNode) {
                stringList.add(node.get(field).asText());
            }
            return stringList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
