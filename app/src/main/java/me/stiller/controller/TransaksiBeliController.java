package me.stiller.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.*;
import me.stiller.repository.DataRepository;
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
    private Label pageCount, valTotal;

    @FXML
    private HBox fTotal;

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
        fTotal.setVisible(false);
        list.addListener((ListChangeListener<DetailPembelian>) c -> {
            if (!c.getList().isEmpty()) iItemPrice.setText(Strings.EMPTY);
            changeTableView(0);
        });

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

        bSave.disableProperty().bind(Bindings.isEmpty(list));
        bDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        bPrint.disableProperty().bind(Bindings.isEmpty(list));
        fTotal.visibleProperty().bind(Bindings.isNotEmpty(list));
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

        double total = 0;
        table.setRoot(null);
        TreeItem<DetailPembelian> rootItem = new TreeItem<>();
        for (DetailPembelian penjualan : sortedData) {
            TreeItem<DetailPembelian> item = new TreeItem<>(penjualan);
            rootItem.getChildren().add(item);
            total += penjualan.getItemTotal();
        }
        table.setRoot(rootItem);
        valTotal.setText(formatPrice(total));
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
            pageCount.setText((pagination.getCurrentPageIndex() + 1) + " of " + pages);
            maxPageIndex.set(pages - 1);
        });

        bNext.setDisable(pagination.currentPageIndexProperty().getValue() == maxPageIndex.intValue());
        maxPageIndex.addListener((observable, oldValue, newValue) -> {
            bNext.setDisable(pagination.currentPageIndexProperty().intValue() == newValue.intValue());
        });

        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            pageCount.setText((index + 1) + " of " + pages);
            bNext.setDisable(newValue.intValue() >= maxPageIndex.intValue());
            log.info("Pages : " + pages);
            log.info("CURRENT " + newValue);
        });
        pageCount.setText((pagination.getCurrentPageIndex() + 1) + " of " + pages);
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
            BaseFont nLight = BaseFont.createFont(Main.class.getResource("fonts/NexaLight.otf").getPath(), "UTF-8", BaseFont.EMBEDDED);
            BaseFont musticabf = BaseFont.createFont(Main.class.getResource("fonts/MusticaproSemibold-2OG5o.otf").getPath(), "UTF-8", BaseFont.EMBEDDED);
            BaseFont kionabf = BaseFont.createFont(Main.class.getResource("fonts/Kiona-Regular.ttf").getPath(), "UTF-8", BaseFont.EMBEDDED);
            Font nexaLight = new Font(nLight, 42f);
            Font mustica = new Font(musticabf, 14f);
            Font mustica12 = new Font(musticabf, 12f);
            Font kiona = new Font(kionabf, 24f);

            Font header = new Font(musticabf, 12f, 0, Color.WHITE);

            File tempFile = Files.createTempFile("temp", ".pdf").toFile();
            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(doc, new FileOutputStream(tempFile));

            doc.open();

            PdfPTable info = new PdfPTable(1);
            info.setWidthPercentage(100);

            PdfPCell invoiceText = new PdfPCell(new Phrase("I N V O I C E", nexaLight));
            invoiceText.setPaddingBottom(12);
            invoiceText.setBorder(0);
            info.addCell(invoiceText);

            String id = String.format("%04d", Integer.parseInt(iOrderId.getText()));
            LocalDate date = LocalDate.parse(iOrderDate.getText());
            String invoice = "INV-" + date.getYear() + date.getMonthValue() + date.getDayOfMonth() + "-S" + id;
            PdfPCell invoiceNumber = new PdfPCell(new Phrase(invoice, kiona));
            invoiceNumber.setPaddingBottom(6);
            invoiceNumber.setBorder(0);
            info.addCell(invoiceNumber);

            PdfPCell invoiceDate = new PdfPCell(new Phrase("PEMBELIAN | " +
                    date.getDayOfMonth() + " " + date.getMonth() + " " + date.getYear(), mustica));
            invoiceDate.setPaddingBottom(16);
            invoiceDate.setBorder(0);
            info.addCell(invoiceDate);
            doc.add(info);

            PdfPTable mainTable = new PdfPTable(8);
            mainTable.setWidthPercentage(97);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11);

            String[] headerTexts = {"#", "Supplier ID", "Supplier Name", "Item ID", "Item Name", "Item Price", "Quantity", "Item Total"};
            int loop = 0;
            for (String headerText : headerTexts) {
                PdfPCell headerCell = new PdfPCell(new Phrase(headerText, header));
                if (loop == 0) headerCell.setPaddingLeft(4);
                if (loop == 7) headerCell.setPaddingRight(4);
                headerCell.setBorder(0);
                headerCell.setPaddingTop(6);
                headerCell.setPaddingBottom(6);
                headerCell.setBackgroundColor(Color.decode("#384450"));
                headerCell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
                if (loop >= 5) {
                    headerCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                }
                mainTable.addCell(headerCell);
                loop++;
            }

            int num = 1;
            double total = 0;
            for (DetailPembelian dp : list) {
                total += dp.getItemTotal();
                PdfPCell number = new PdfPCell(new Phrase(String.valueOf(num++), fontBody));
                PdfPCell cell1 = new PdfPCell(new Phrase(dp.getSupplierId(), fontBody));
                PdfPCell cell2 = new PdfPCell(new Phrase(dp.getSupplierName(), fontBody));
                PdfPCell cell3 = new PdfPCell(new Phrase(dp.getItemId(), fontBody));
                PdfPCell cell4 = new PdfPCell(new Phrase(dp.getItemName(), fontBody));
                PdfPCell cell5 = new PdfPCell(new Phrase(dp.getItemPriceFormatted(), fontBody));
                PdfPCell cell6 = new PdfPCell(new Phrase(String.valueOf(dp.getItemQuantity()), fontBody));
                PdfPCell cell7 = new PdfPCell(new Phrase(dp.getItemTotalFormatted(), fontBody));
                ArrayList<PdfPCell> cells = new ArrayList<>(List.of(number, cell1, cell2, cell3, cell4, cell5, cell6, cell7));
                number.setPaddingLeft(4);
                cell7.setPaddingRight(4);
                loop = 0;
                for (PdfPCell cell : cells) {
                    cell.setBorder(0);
                    cell.setPaddingTop(4);
                    cell.setPaddingBottom(4);
                    if (num % 2 == 0) cell.setBackgroundColor(Color.decode("#f0f0f0"));
                    else cell.setBackgroundColor(Color.decode("#e6e6e6"));
                    if (loop >= 5) {
                        cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                    }
                    mainTable.addCell(cell);
                    loop++;
                }
            }
            doc.add(mainTable);

            float[] width = {6, 1, 1};

            PdfPTable tTotal = new PdfPTable(width);
            tTotal.setWidthPercentage(97);


            PdfPCell blank = new PdfPCell(new Phrase());
            PdfPCell blank1 = new PdfPCell(new Phrase());
            PdfPCell blank2 = new PdfPCell(new Phrase());
            PdfPCell blank3 = new PdfPCell(new Phrase());

            ArrayList<PdfPCell> blanks = new ArrayList<>(List.of(blank, blank1, blank2, blank3));
            blanks.forEach(b -> {
                b.setPaddingTop(12);
                b.setBorder(0);
                tTotal.addCell(b);
            });

            PdfPCell textTotal = new PdfPCell(new Phrase("TOTAL", mustica12));
            textTotal.setPaddingTop(4);
            textTotal.setBorder(0);
            textTotal.setBorderWidthTop(2);
            tTotal.addCell(textTotal);

            PdfPCell valTotal = new PdfPCell(new Phrase(formatPrice(total), mustica12));
            valTotal.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            valTotal.setPaddingTop(4);
            valTotal.setPaddingRight(4);
            valTotal.setBorder(0);
            valTotal.setBorderWidthTop(2);
            tTotal.addCell(valTotal);

            doc.add(tTotal);
            doc.close();

            Desktop.getDesktop().open(tempFile);
            tempFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
