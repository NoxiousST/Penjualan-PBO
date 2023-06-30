package me.stiller.controller;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.VBox;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Jual;
import me.stiller.data.models.Konsumen;
import me.stiller.repository.DataRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import net.sf.jasperreports.view.JasperViewer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
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

public class TransaksiController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<Jual.DJual> table;

    @FXML
    private JFXTreeTableColumn<Jual.DJual, String> cSelect, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal;

    @FXML
    private JFXComboBox<String> iItemId, iCustomerId;

    @FXML
    private JFXTextField iOrderId, iOrderDate, iCustomerName, iItemName, iItemPrice, iItemQuantity, iItemTotal, iSearch;

    @FXML
    private JFXButton bConfirm, bCancel, bPref, bNext, bSave, bDelete, bPrint, bExport;

    @FXML
    private Label pageCount;

    @FXML
    private Pagination pagination;

    private MainController mainController;
    private final Server server = new Server();
    private final ObservableList<Jual.DJual> list = FXCollections.observableArrayList();
    private final ArrayList<JFXTreeTableColumn<Jual.DJual, String>> columns = new ArrayList<>();
    private final ArrayList<JFXTextField> barangInputs = new ArrayList<>();
    private FilteredList<Jual.DJual> filteredData;
    private boolean itemExist = false;
    private int pages;

    Logger log = LogManager.getLogger(TransaksiController.class.getName());
    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        iItemId.getItems().addAll(dataRepository.getBarangIds());
        iCustomerId.getItems().setAll(dataRepository.getKonsumenIds());

        columns.addAll(List.of(cSelect, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal));
        barangInputs.addAll(List.of(iItemName, iItemPrice, iItemQuantity, iItemTotal));
        table.setShowRoot(false);

        initializeColumn();
        setFilterPagination();
        initializeListener();
        setForm();

        bCancel.setGraphic(getIcon("close"));
        bConfirm.setGraphic(getIcon("check"));
        bPref.setGraphic(getIcon("left"));
        bNext.setGraphic(getIcon("right"));

        root.setOnMousePressed(event -> removeFocus());
    }

    public void removeFocus() {
        root.getParent().requestFocus();
    }

    private void initializeColumn() {
        cItemId.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemId"));
        cItemName.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemName"));
        cItemPrice.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemPriceFormatted"));
        cItemQuantity.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemQuantity"));
        cItemTotal.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemTotalFormatted"));

        cSelect.setMaxWidth(72);
        cSelect.setMinWidth(72);
        cSelect.setCellFactory(param -> new JFXTreeTableCell<>() {
            final JFXCheckBox checkBox = new JFXCheckBox();

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                checkBox.setDisable(true);
                table.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) ->
                        checkBox.setSelected(newIndex.intValue() == getTableRow().getIndex()));
                getTableRow().setOnMouseClicked(event -> setFormEdit(getTableRow().getItem()));
                getTableRow().getStyleClass().add("row");
                setText(null);

                if (empty) {
                    setGraphic(null);
                    getTableRow().getStyleClass().remove("row");
                } else {
                    setGraphic(checkBox);
                }
            }
        });

        table.getColumns().setAll(columns);
    }

    private void initializeListener() {
        list.addListener((ListChangeListener<Jual.DJual>) c -> changeTableView(0));

        iItemId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Barang barang = dataRepository.getBarang(newValue.intValue());
            dataRepository.setSelectedBarang(barang);
            iItemName.setText(barang.getItemName());
            iItemPrice.setText(formatPrice(barang.getItemPrice()));
            validateQuantity(iItemQuantity.getText());
        });

        iItemQuantity.textProperty().addListener((observable, oldValue, newValue) -> {
            if (dataRepository.getSelectedBarang() != null) validateQuantity(newValue);
        });

        iCustomerId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Konsumen konsumen = dataRepository.getKonsumen(newValue.intValue());
            iCustomerName.setText(konsumen.getCustomerName());
        });

        bConfirm.disableProperty().bind(((iItemId.valueProperty().isNull())
                .or(iItemName.textProperty().isEmpty())
                .or(iItemPrice.textProperty().isEmpty())
                .or(iItemQuantity.textProperty().isEmpty())
                .or(iItemTotal.textProperty().isEmpty())));

        bSave.disableProperty().bind(Bindings.isEmpty(list)
                .or(iCustomerId.valueProperty().isNull()));

        bDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        bPrint.disableProperty().bind(Bindings.isEmpty(list));
        bExport.disableProperty().bind(Bindings.isEmpty(list));
    }

    private void setForm() {
        iOrderId.setText(String.valueOf(server.getLast()));
        iOrderDate.setText(LocalDate.now().toString());

        iItemId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Barang barang = dataRepository.getBarang(newValue.intValue());
            boolean itemExist = false;
            for (Jual.DJual penjualan : list) {
                if (Objects.equals(penjualan.getItemId(), barang.getItemId())) {
                    table.getSelectionModel().select(list.indexOf(penjualan));
                    itemExist = true;
                    break;
                }
            }
            if (!itemExist) table.getSelectionModel().clearSelection();
        });

        bConfirm.setOnMouseClicked(event -> {
            int qty = Integer.parseInt(iItemQuantity.getText());
            Barang b = dataRepository.getSelectedBarang();
            Jual.DJual penjualan = new Jual.DJual(b.getItemId(), b.getItemName(), b.getItemPrice(), qty, b.getItemPrice() * qty);

            for (Jual.DJual jual : list) {
                if (jual.getItemId().equals(penjualan.getItemId())) {
                    jual.setItemQuantity(qty);
                    itemExist = true;
                    break;
                }
            }

            if (!itemExist) list.add(0, penjualan);
            itemExist = false;
            table.getSelectionModel().clearSelection();
            resetForm();
        });

        bCancel.setOnMouseClicked(event -> {
            table.getSelectionModel().clearSelection();
            resetForm();
        });

        bDelete.setOnMouseClicked(event -> {
            Jual.DJual penjualan = list.get(table.getSelectionModel().getSelectedIndex());
            list.remove(penjualan);
            resetForm();
        });

        bPref.setOnAction(event -> {
            int index = pagination.getCurrentPageIndex();
            if (index > 0) {
                pagination.setCurrentPageIndex(index - 1);
            }
            removeFocus();
        });

        bNext.setOnAction(event -> {
            int index = pagination.getCurrentPageIndex();
            if (index < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(index + 1);
            }
        });

        bSave.setOnMouseClicked(event -> {
            Jual penjualan = new Jual();
            penjualan.setItems(list);
            penjualan.setOrderId(iOrderId.getText());
            penjualan.setOrderDate(iOrderDate.getText());
            penjualan.setCustomerId(iCustomerId.getValue());

            if (server.insert(penjualan)) {
                mainController.setDialog(true, "Data successfully saved");
                list.clear();
                dataRepository.setPenjualanList(server.retrieveJualData());
                iOrderId.setText(String.valueOf(server.getLast()));
                resetForm();
            } else
                mainController.setDialog(false, "Failed to save data");

        });

        bPrint.setOnMouseClicked(event -> print());
        bExport.setOnMouseClicked(event -> export());
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, list.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Jual.DJual> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        table.setRoot(null);
        TreeItem<Jual.DJual> rootItem = new TreeItem<>();
        for (Jual.DJual penjualan : sortedData) {
            TreeItem<Jual.DJual> item = new TreeItem<>(penjualan);
            rootItem.getChildren().add(item);
        }
        table.setRoot(rootItem);
    }

    private void setFilterPagination() {
        pages = (int) (Math.floor(list.size() * 1.0 / 10) + 1);
        filteredData = new FilteredList<>(list, p -> true);
        iSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            int pages = (int) Math.floor(filteredData.size() * 1.0 / 10) + 1;
            if (pagination.getCurrentPageIndex() > pages) {
                pagination.setCurrentPageIndex(pages);
            }
            filteredData.setPredicate(
                    penjualan -> newValue == null || newValue.isEmpty() ||
                            penjualan.getItemId().toLowerCase().contains(newValue.toLowerCase()) ||
                            penjualan.getItemName().toLowerCase().contains(newValue.toLowerCase()));
            changeTableView(pagination.getCurrentPageIndex());
        });

        pagination.setPageCount(pages);
        pagination.setCurrentPageIndex(0);
        changeTableView(0);
        pagination.currentPageIndexProperty().addListener(
                (observable, oldValue, newValue) -> changeTableView(newValue.intValue()));

        IntegerProperty maxPageIndex = new SimpleIntegerProperty();
        maxPageIndex.set(pages - 1);
        bPref.disableProperty().bind(pagination.currentPageIndexProperty().isEqualTo(0));
        filteredData.addListener((ListChangeListener<Jual.DJual>) c -> {
            pages = (int) Math.ceil(c.getList().size() * 1.0 / 10);
            int currentIndex = pagination.getCurrentPageIndex();
            if (currentIndex > pages - 1) {
                pagination.setCurrentPageIndex(pages - 1);
            }
            pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
            maxPageIndex.set(pages - 1);
        });

        bNext.setDisable(pagination.currentPageIndexProperty().getValue() == maxPageIndex.intValue());
        maxPageIndex.addListener((observable, oldValue, newValue) -> {
            bNext.setDisable(pagination.currentPageIndexProperty().intValue() == newValue.intValue());
        });

        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            pageCount.setText("Page " + (index + 1) + " of " + pages);
            bNext.setDisable(newValue.intValue() >= maxPageIndex.intValue());
            log.info("Pages : " + pages);
            log.info("CURRENT " + newValue);
        });
        pageCount.setText("Page " + (pagination.getCurrentPageIndex() + 1) + " of " + pages);
    }

    private void setFormEdit(Jual.DJual penjualan) {
        Double total = penjualan.getItemPrice() * penjualan.getItemQuantity();
        iItemId.setValue(penjualan.getItemId());
        iItemName.setText(penjualan.getItemName());
        iItemPrice.setText(formatPrice(penjualan.getItemPrice()));
        iItemQuantity.setText(String.valueOf(penjualan.getItemQuantity()));
        iItemTotal.setText(formatPrice(total));
    }

    private void resetForm() {
        barangInputs.forEach(input -> input.setText(Strings.EMPTY));
        if (iItemId.getValue() != null) iItemId.setValue(null);
    }

    private void validateQuantity(String text) {
        Barang barang = dataRepository.getSelectedBarang();
        if (text.matches("\\d+")) {
            Double total = barang.getItemPrice() * Integer.parseInt(iItemQuantity.getText());
            iItemTotal.setText(formatPrice(total));
        } else {
            iItemTotal.setText(Strings.EMPTY);
        }
    }

    private void print() {
        try {
            JRBeanCollectionDataSource transactionDataSource = new JRBeanCollectionDataSource(list);
            Map<String, Object> parameter = new HashMap<>();

            parameter.put("title", "Laporan");
            parameter.put("transactionDataSource", transactionDataSource);
            JasperReport jasperDesign =JasperCompileManager.compileReport(Objects.requireNonNull(
                    Main.class.getResource("jasper/transaksi.jrxml")).getPath());

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, parameter, new JREmptyDataSource());

            JasperViewer.viewReport(jasperPrint, false);
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    private void export() {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Transaksi");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID Barang");
        headerRow.createCell(1).setCellValue("Nama Barang");
        headerRow.createCell(2).setCellValue("Harga");
        headerRow.createCell(3).setCellValue("Kuantitas");
        headerRow.createCell(4).setCellValue("Total Harga");

        CellStyle priceStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        priceStyle.setDataFormat(dataFormat.getFormat("[$IDR] #,##0.00"));

        int rowNum = 1;
        for (Jual.DJual d : list) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(Integer.parseInt(d.getItemId()));
            dataRow.createCell(1).setCellValue(d.getItemName());
            dataRow.createCell(3).setCellValue(d.getItemQuantity());
            XSSFCell priceCell = dataRow.createCell(2);
            XSSFCell totalCell = dataRow.createCell(4);
            priceCell.setCellValue(d.getItemPrice());
            totalCell.setCellValue(d.getItemTotal());
            priceCell.setCellStyle(priceStyle);
            totalCell.setCellStyle(priceStyle);
        }

        for (int i = 0; i <= 4; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(getExportPath(root))) {
            workbook.write(fileOut);
            if (workbook.getSheet("Transaksi") != null)
                mainController.setDialog(true, "Report succesfully exported to WorkBook");
            else
                mainController.setDialog(false, "Failed to save file");
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
