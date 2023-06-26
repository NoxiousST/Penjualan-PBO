package me.stiller.controller;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
import javafx.scene.control.Label;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import me.stiller.Main;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Jual;
import me.stiller.repository.DataRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static me.stiller.utils.Helper.*;

public class PenjualanController implements Initializable {


    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Jual> table;

    @FXML
    private JFXTreeTableColumn<Jual, String> cOrderId, cOrderDate, cCustomerName, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal;

    @FXML
    private JFXButton btnPref, btnNext, btnPrint, btnExport;

    @FXML
    private JFXTextField search;

    @FXML
    private DatePicker fdate, ldate;

    @FXML
    private Label pageCount;

    @FXML
    private Pagination pagination;

    private MainController mainController;
    private ArrayList<JFXTreeTableColumn<Jual, String>> columns = new ArrayList<>();
    private ObservableList<Jual> list;
    private FilteredList<Jual> filteredData;
    private int pages;

    Logger log = LogManager.getLogger(PenjualanController.class.getName());

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getBarangComponent().inject(this);

        list = dataRepository.getPenjualanList();
        list.forEach(i -> {
            i.setCustomerName(dataRepository.getKonsumen(i.getCustomerId()).getCustomerName());
            i.getItems().forEach(it -> {
                Barang b = dataRepository.getBarang(it.getItemId());
                it.setItemName(b.getItemName());
                it.setItemTotal(it.getItemQuantity() * it.getItemPrice());
            });
        });
        columns.addAll(List.of(cOrderId, cOrderDate, cCustomerName, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal));
        table.setShowRoot(false);
        initializeColumn();

        setFilterPagination();
        setForm();
        list.addListener((ListChangeListener<Jual>) c -> changeTableView(0));
        btnPrint.disableProperty().bind(Bindings.isEmpty(list));


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
        cOrderId.setCellValueFactory(new TreeItemPropertyValueFactory<>("orderId"));
        cOrderDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("orderDate"));
        cCustomerName.setCellValueFactory(new TreeItemPropertyValueFactory<>("customerName"));

        columns.removeAll(List.of(cOrderId, cOrderDate, cCustomerName));
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
                            getTableRow().getItem().getItems().forEach(i ->
                                    labels.add(new Label(i.returnItem(col.getId()))));
                            container.getChildren().setAll(labels);
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

        btnPrint.setOnMouseClicked(event -> print());
        btnExport.setOnMouseClicked(event -> export());
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, list.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Jual> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        TreeItem<Jual> rootItem = new TreeItem<>();
        for (Jual penjualan : sortedData) {
            TreeItem<Jual> item = new TreeItem<>(penjualan);
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
            filteredData.setPredicate(jual -> {
                LocalDate date = LocalDate.parse(jual.getOrderDate());
                return date.isAfter(newValue.minusDays(1)) &&
                        date.isBefore(ldate.getValue().plusDays(1));
            });
            changeTableView(pagination.getCurrentPageIndex());
            table.refresh();
        });

        ldate.valueProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.ceil(filteredData.size() * 1.0 / 10);
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(jual -> {
                LocalDate date = LocalDate.parse(jual.getOrderDate());
                return date.isBefore(newValue.plusDays(1)) &&
                        date.isAfter(fdate.getValue().minusDays(1));
            });
            changeTableView(pagination.getCurrentPageIndex());
        });

        search.textProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.floor(filteredData.size() * 1.0 / 10) + 1;
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(jual -> {
                List<String> itemNames = jual.getItems().stream()
                        .map(Jual.DJual::getItemName)
                        .toList();
                List<String> itemIds = jual.getItems().stream()
                        .map(Jual.DJual::getItemId)
                        .toList();
                String lowerCaseNewValue = newValue.toLowerCase();
                return jual.getCustomerName().contains(lowerCaseNewValue) ||
                        jual.getOrderDate().contains(lowerCaseNewValue) ||
                        itemNames.stream().anyMatch(name -> name.toLowerCase().contains(lowerCaseNewValue)) ||
                        itemIds.stream().anyMatch(id -> id.toLowerCase().contains(lowerCaseNewValue));
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
        filteredData.addListener((ListChangeListener<Jual>) c -> {
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

    private void print() {
        try {
            File tempFile = Files.createTempFile("temp", ".pdf").toFile();
            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(doc, new FileOutputStream(tempFile));

            doc.open();

            PdfPTable mainTable = new PdfPTable(8);
            mainTable.setWidthPercentage(100);
            com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, java.awt.Color.WHITE);
            com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11);

            String[] headerTexts = {"Order ID", "Customer Name", "Order Date", "Item ID", "Item Name", "Item Price", "Quantity", "Item Total"};
            java.awt.Color headerColor = java.awt.Color.decode("#2979ff");

            for (String headerText : headerTexts) {
                PdfPCell headerCell = new PdfPCell(new Phrase(headerText, fontHeader));
                headerCell.setBackgroundColor(headerColor);
                mainTable.addCell(headerCell);
            }

            for (Jual j : list) {
                PdfPCell cId = new PdfPCell(new Phrase(j.getOrderId(), fontBody));
                PdfPCell cCustomer = new PdfPCell(new Phrase(j.getCustomerName(), fontBody));
                PdfPCell cDate = new PdfPCell(new Phrase(j.getOrderDate(), fontBody));

                cId.setRowspan(j.getItems().size());
                cCustomer.setRowspan(j.getItems().size());
                cDate.setRowspan(j.getItems().size());

                mainTable.addCell(cId);
                mainTable.addCell(cCustomer);
                mainTable.addCell(cDate);

                for (Jual.DJual d : j.getItems()) {
                    mainTable.addCell(new Phrase(d.getItemId(), fontBody));
                    mainTable.addCell(new Phrase(d.getItemName(), fontBody));
                    mainTable.addCell(new Phrase(d.getItemPriceFormatted(), fontBody));
                    mainTable.addCell(new Phrase(String.valueOf(d.getItemQuantity()), fontBody));
                    mainTable.addCell(new Phrase(d.getItemTotalFormatted(), fontBody));
                }
            }

            doc.add(mainTable);
            doc.close();

            Desktop.getDesktop().open(tempFile);
            tempFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void export() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Penjualan");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Order Id");
        headerRow.createCell(1).setCellValue("Order Date");
        headerRow.createCell(2).setCellValue("Customer Id");
        headerRow.createCell(3).setCellValue("Customer Name");
        headerRow.createCell(4).setCellValue("Item Id");
        headerRow.createCell(5).setCellValue("Item Name");
        headerRow.createCell(6).setCellValue("Item Price");
        headerRow.createCell(7).setCellValue("Item Quantity");
        headerRow.createCell(8).setCellValue("Item Total Price");

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setDataFormat((short) 14);

        int rowNum = 1;
        XSSFRow dataRow = sheet.createRow(rowNum);
        for (Jual jual : list) {
            dataRow.createCell(0).setCellValue(Integer.parseInt(jual.getOrderId()));
            dataRow.createCell(1).setCellValue(Integer.parseInt(jual.getCustomerId()));
            dataRow.createCell(2).setCellValue(jual.getCustomerName());
            XSSFCell dateCell = dataRow.createCell(3);
            dateCell.setCellValue(LocalDate.parse(jual.getOrderDate()));
            dateCell.setCellStyle(cellStyle);

            List<Jual.DJual> items = jual.getItems();
            for (Jual.DJual item : items) {
                dataRow.createCell(4).setCellValue(Integer.parseInt(item.getItemId()));
                dataRow.createCell(5).setCellValue(item.getItemName());
                dataRow.createCell(6).setCellValue(item.getItemPrice());
                dataRow.createCell(7).setCellValue(item.getItemQuantity());
                dataRow.createCell(8).setCellValue(item.getItemTotal());
                dataRow = sheet.createRow(rowNum++);
            }
        }

        for (int i = 0; i <= 8; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(getExportPath(root))) {
            workbook.write(fileOut);
            if (workbook.getSheet("Penjualan") != null) mainController.setDialog(true,
                    "Report succesfully exported to WorkBook");

            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
