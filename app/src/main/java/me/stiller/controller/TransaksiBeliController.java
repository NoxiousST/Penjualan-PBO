package me.stiller.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.VBox;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.*;
import me.stiller.repository.DataRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

import static me.stiller.utils.Helper.*;

public class TransaksiBeliController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXTreeTableView<DetailPembelian> table;

    @FXML
    private JFXTreeTableColumn<DetailPembelian, String> cSelect, cSupplier, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal;

    @FXML
    private JFXComboBox<String> iItemId, iSupplierId;

    @FXML
    private JFXTextField iOrderId, iOrderDate, iSupplierName, iItemName, iItemPrice, iItemQuantity, iItemTotal, iSearch;

    @FXML
    private JFXButton bConfirm, bCancel, bPref, bNext, bSave, bDelete, bPrint;

    @FXML
    private Label pageCount;

    @FXML
    private Pagination pagination;

    private ObjectMapper mapper = new ObjectMapper();
    private MainController mainController;
    private final Server server = new Server();
    private final ObservableList<DetailPembelian> list = FXCollections.observableArrayList();
    private final ArrayList<JFXTreeTableColumn<DetailPembelian, String>> columns = new ArrayList<>();
    private final ArrayList<JFXTextField> itemInputs = new ArrayList<>();
    private SuppliedItem selectedSupplied = new SuppliedItem();
    private FilteredList<DetailPembelian> filteredData;
    private boolean itemExist = false;
    private int pages;

    Logger log = LogManager.getLogger(TransaksiBeliController.class.getName());

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        list.setAll(dataRepository.getDetailPembelian());
        iSupplierId.getItems().setAll(dataRepository.getSupplierIds());

        columns.addAll(List.of(cSelect, cSupplier, cItemId, cItemName, cItemPrice, cItemQuantity, cItemTotal));
        itemInputs.addAll(List.of(iItemName, iItemPrice, iItemQuantity, iItemTotal));
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
        cSupplier.setCellValueFactory(new TreeItemPropertyValueFactory<>("supplierName"));
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
        list.addListener((ListChangeListener<DetailPembelian>) c -> changeTableView(0));

        iItemQuantity.textProperty().addListener((observable, oldValue, newValue) -> {
            if (selectedSupplied != null) validateQuantity(newValue);
        });

        iSupplierId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            try {
                itemInputs.forEach(i -> i.setText(Strings.EMPTY));
                Supplier supplier = dataRepository.getSupplier(newValue.intValue());
                dataRepository.setSelectedSupplier(supplier);
                iSupplierName.setText(supplier.getSupplierName());

                JsonNode jsonNode = mapper.readTree(supplier.getItemId());
                iItemId.getItems().clear();
                for (JsonNode node : jsonNode) {
                    iItemId.getItems().add(node.get("itemId").asText());
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        iItemId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Supplier supplier = dataRepository.getSelectedSupplier();
            try {
                JsonNode jsonNode = mapper.readTree(supplier.getItemId());
                JsonNode indexNode = jsonNode.get(newValue.intValue());
                selectedSupplied = mapper.treeToValue(indexNode, SuppliedItem.class);
                iItemName.setText(selectedSupplied.getItemName());
                iItemPrice.setText(formatPrice(selectedSupplied.getItemPrice()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            validateQuantity(iItemQuantity.getText());
        });

        bConfirm.disableProperty().bind(iItemName.textProperty().isEmpty()
                .or(iItemPrice.textProperty().isEmpty())
                .or(iItemQuantity.textProperty().isEmpty())
                .or(iItemTotal.textProperty().isEmpty()));

        bSave.disableProperty().bind(Bindings.isEmpty(list)
                .or(iSupplierId.valueProperty().isNull()));

        bDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        bPrint.disableProperty().bind(Bindings.isEmpty(list));
    }

    private void setForm() {
        iOrderId.setText(String.valueOf(server.getLastPembelian()));
        iOrderDate.setText(LocalDate.now().toString());

        bConfirm.setOnMouseClicked(event -> {
            int quantity = Integer.parseInt(iItemQuantity.getText());
            DetailPembelian detailPembelian = new DetailPembelian(
                    iSupplierId.getValue(),
                    iSupplierName.getText(),
                    iItemId.getValue(),
                    selectedSupplied.getItemName(),
                    selectedSupplied.getItemPrice(),
                    quantity,
                    selectedSupplied.getItemPrice() * quantity
            );

            for (DetailPembelian dp : list) {
                if (detailPembelian.getItemId().equals(dp.getItemId()) &&
                        detailPembelian.getSupplierId().equals(dp.getSupplierId())) {
                    dp.setItemQuantity(quantity);
                    dp.setItemTotal(dp.getItemPrice() * quantity);
                    itemExist = true;
                    log.debug(itemExist);
                    break;
                }
            }

            if (!itemExist) list.add(0, detailPembelian);
            dataRepository.setDetailPembelian(list);
            itemExist = false;
            table.getSelectionModel().clearSelection();
            resetForm();
        });

        bCancel.setOnMouseClicked(event -> {
            table.getSelectionModel().clearSelection();
            resetForm();
        });

        bDelete.setOnMouseClicked(event -> {
            DetailPembelian penjualan = list.get(table.getSelectionModel().getSelectedIndex());
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
            Pembelian pembelian = new Pembelian();
            pembelian.setOrderId(iOrderId.getText());
            pembelian.setOrderDate(iOrderDate.getText());
            pembelian.setItems(mapper.valueToTree(list).toString());

            if (server.insert(pembelian)) {
                mainController.setDialog(true, "Data successfully saved");
                list.clear();
                dataRepository.setPenjualanList(server.retrieveJualData());
                iOrderId.setText(String.valueOf(server.getLastPembelian()));
                resetForm();

                try {
                    JsonNode jsonNode = mapper.readTree(pembelian.getItems());
                    for (JsonNode node : jsonNode) {
                        Barang barang = dataRepository.getBarang(node.get("itemId").asText());
                        barang.setItemStock(barang.getItemStock() + node.get("itemQuantity").asInt());
                        server.update(barang);
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            } else
                mainController.setDialog(false, "Failed to save data");

        });

        bPrint.setOnMouseClicked(event -> print());
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, list.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<DetailPembelian> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        table.setRoot(null);
        TreeItem<DetailPembelian> rootItem = new TreeItem<>();
        for (DetailPembelian penjualan : sortedData) {
            TreeItem<DetailPembelian> item = new TreeItem<>(penjualan);
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
        filteredData.addListener((ListChangeListener<DetailPembelian>) c -> {
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

    private void setFormEdit(DetailPembelian detailPembelian) {
        Double total = detailPembelian.getItemPrice() * detailPembelian.getItemQuantity();
        iSupplierId.setValue(detailPembelian.getSupplierId());
        iSupplierName.setText(detailPembelian.getSupplierName());
        iItemId.setValue(detailPembelian.getItemId());
        iItemName.setText(detailPembelian.getItemName());
        iItemPrice.setText(formatPrice(detailPembelian.getItemPrice()));
        iItemQuantity.setText(String.valueOf(detailPembelian.getItemQuantity()));
        iItemTotal.setText(formatPrice(total));
    }

    private void resetForm() {
        itemInputs.forEach(input -> input.setText(Strings.EMPTY));
        if (iItemId.getValue() != null) iItemId.setValue(null);
    }

    private void validateQuantity(String text) {
        if (text.matches("\\d+")) {
            Double total = selectedSupplied.getItemPrice() * Integer.parseInt(iItemQuantity.getText());
            iItemTotal.setText(formatPrice(total));
        } else {
            iItemTotal.setText(Strings.EMPTY);
        }
    }

    private void print() {
        try {
            File tempFile = Files.createTempFile("temp", ".pdf").toFile();
            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(doc, new FileOutputStream(tempFile));

            doc.open();

            PdfPTable info = new PdfPTable(2);
            PdfPTable invoice = new PdfPTable(1);

            PdfPCell txtInvoiceNumber = new PdfPCell(new Phrase("Invoice Number"));
            txtInvoiceNumber.setBorder(0);
            invoice.addCell(txtInvoiceNumber);

            PdfPCell invoiceNumber = new PdfPCell(new Phrase("1234567890"));
            txtInvoiceNumber.setBorder(0);
            invoice.addCell(txtInvoiceNumber);
            invoice.addCell(invoiceNumber);

            info.addCell(invoice);

            doc.add(info);

            PdfPTable mainTable = new PdfPTable(8);
            mainTable.setWidthPercentage(100);
            com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, java.awt.Color.WHITE);
            com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11);

            String[] headerTexts = {"#", "Supplier ID", "Supplier Name", "Item ID", "Item Name", "Item Price", "Quantity", "Item Total"};
            java.awt.Color headerColor = java.awt.Color.decode("#2979ff");

            for (String headerText : headerTexts) {
                PdfPCell headerCell = new PdfPCell(new Phrase(headerText, fontHeader));
                headerCell.setBackgroundColor(headerColor);
                mainTable.addCell(headerCell);
            }

            int num = 1;
            for (DetailPembelian dp : list) {
                mainTable.addCell(new Phrase(String.valueOf(num++), fontBody));
                mainTable.addCell(new Phrase(dp.getSupplierId(), fontBody));
                mainTable.addCell(new Phrase(dp.getSupplierName(), fontBody));
                mainTable.addCell(new Phrase(dp.getItemId(), fontBody));
                mainTable.addCell(new Phrase(dp.getItemName(), fontBody));
                mainTable.addCell(new Phrase(dp.getItemPriceFormatted(), fontBody));
                mainTable.addCell(new Phrase(String.valueOf(dp.getItemQuantity()), fontBody));
                mainTable.addCell(new Phrase(dp.getItemTotalFormatted(), fontBody));
            }

            doc.add(mainTable);
            doc.close();

            Desktop.getDesktop().open(tempFile);
            tempFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
