package me.stiller.controller;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.stiller.Main;
import me.stiller.Server;
import me.stiller.data.models.Barang;
import me.stiller.data.models.Jual;
import me.stiller.data.models.Konsumen;
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

public class TransaksiJualController implements Initializable {

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
    private JFXButton bConfirm, bCancel, bPref, bNext, bSave, bDelete, bPrint;

    @FXML
    private Label pageCount, valTotal;

    @FXML
    private HBox fTotal;

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

    Logger log = LogManager.getLogger(TransaksiJualController.class.getName());

    public void setParentController(MainController mainController) {
        this.mainController = mainController;
    }

    @Inject
    DataRepository dataRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Main mainApplication = Main.getInstance();
        mainApplication.getComponent().inject(this);

        list.setAll(dataRepository.getTransaksiJualList());

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
        cItemQuantity.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemQuantityFormatted"));
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
        list.addListener((ListChangeListener<Jual.DJual>) c -> {
            dataRepository.setTransaksiJualList(list);
            boolean isEmpty = c.getList().isEmpty();
            if (isEmpty) iItemPrice.setText(Strings.EMPTY);
            changeTableView(0);
        });

        iItemId.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                iItemPrice.setText(Strings.EMPTY);
            } else {
                Barang barang = dataRepository.getBarang(newValue);
                dataRepository.setSelectedBarang(barang);
                iItemName.setText(barang.getItemName());
                iItemPrice.setText(formatPrice(barang.getItemPrice()));
                boolean itemExist = false;
                for (Jual.DJual penjualan : list) {
                    if (Objects.equals(penjualan.getItemId(), barang.getItemId())) {
                        table.getSelectionModel().select(list.indexOf(penjualan));
                        itemExist = true;
                        break;
                    }
                }
                if (!itemExist) table.getSelectionModel().clearSelection();
                validateQuantity(iItemQuantity.getText());
            }
        });

        iItemQuantity.textProperty().addListener((observable, oldValue, newValue) -> {
            if (dataRepository.getSelectedBarang() != null) validateQuantity(newValue);
        });

        iCustomerId.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Konsumen konsumen = dataRepository.getKonsumen(newValue.intValue());
            iCustomerName.setText(konsumen.getCustomerName());
        });

        BooleanBinding disableButtonBinding = Bindings.createBooleanBinding(() -> {
            int quantity = Integer.parseInt(iItemQuantity.getText());
            int stock = dataRepository.getBarang(iItemId.getValue()).getItemStock();
            return quantity >= stock;
        }, iItemQuantity.textProperty(), iItemId.valueProperty());

        bConfirm.disableProperty().bind(disableButtonBinding
                .or(iItemName.textProperty().isEmpty())
                .or(iItemPrice.textProperty().isEmpty())
                .or(iItemTotal.textProperty().isEmpty()));

        bSave.disableProperty().bind(Bindings.isEmpty(list)
                .or(iCustomerId.valueProperty().isNull()));

        bDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        bPrint.disableProperty().bind(Bindings.isEmpty(list)
                .or(iCustomerName.textProperty().isEmpty())
                .or(iCustomerId.valueProperty().isNull()));
        fTotal.visibleProperty().bind(Bindings.isNotEmpty(list));
    }

    private void setForm() {
        iOrderId.setText(String.valueOf(server.getLast()));
        iOrderDate.setText(LocalDate.now().toString());

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

            dataRepository.setTransaksiJualList(list);
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
                dataRepository.setPenjualanList(server.retrieveJualData());
                iOrderId.setText(String.valueOf(server.getLast()));
                resetForm();

                list.forEach(item -> {
                    Barang barang = dataRepository.getBarang(item.getItemId());
                    barang.setItemStock(barang.getItemStock() - item.getItemQuantity());
                    server.update(barang);
                });
                list.clear();

            } else
                mainController.setDialog(false, "Failed to save data");

        });

        bPrint.setOnMouseClicked(event -> print());
    }

    private void changeTableView(int index) {
        int fromIndex = index * 10;
        int toIndex = Math.min(fromIndex + 10, list.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ArrayList<Jual.DJual> sortedData = new ArrayList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));

        double total = 0;
        table.setRoot(null);
        TreeItem<Jual.DJual> rootItem = new TreeItem<>();
        for (Jual.DJual penjualan : sortedData) {
            TreeItem<Jual.DJual> item = new TreeItem<>(penjualan);
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
        filteredData.addListener((ListChangeListener<Jual.DJual>) c -> {
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
            BaseFont nLight = BaseFont.createFont(Main.class.getResource("fonts/NexaLight.otf").getPath(), "UTF-8", BaseFont.EMBEDDED);
            BaseFont musticabf = BaseFont.createFont(Main.class.getResource("fonts/MusticaproSemibold-2OG5o.otf").getPath(), "UTF-8", BaseFont.EMBEDDED);
            BaseFont kionabf = BaseFont.createFont(Main.class.getResource("fonts/Kiona-Regular.ttf").getPath(), "UTF-8", BaseFont.EMBEDDED);
            com.lowagie.text.Font nexaLight = new com.lowagie.text.Font(nLight, 42f);
            com.lowagie.text.Font nexaLight12 = new com.lowagie.text.Font(nLight, 12f);
            com.lowagie.text.Font mustica = new com.lowagie.text.Font(musticabf, 14f);
            com.lowagie.text.Font mustica12 = new com.lowagie.text.Font(musticabf, 12f);
            com.lowagie.text.Font kiona = new com.lowagie.text.Font(kionabf, 24f);

            com.lowagie.text.Font header = new com.lowagie.text.Font(musticabf, 12f, 0, java.awt.Color.WHITE);

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

            PdfPCell invoiceDate = new PdfPCell(new Phrase("PENJUALAN | " +
                    date.getDayOfMonth() + " " + date.getMonth() + " " + date.getYear(), mustica));
            invoiceDate.setPaddingBottom(12);
            invoiceDate.setBorder(0);
            info.addCell(invoiceDate);

            PdfPCell customerName = new PdfPCell(new Phrase(iCustomerName.getText(), nexaLight12));
            customerName.setPaddingBottom(20);
            customerName.setBorder(0);
            info.addCell(customerName);

            doc.add(info);


            PdfPTable mainTable = new PdfPTable(6);
            mainTable.setWidthPercentage(97);
            com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11);

            String[] headerTexts = {"#", "Item ID", "Item Name", "Item Price", "Quantity", "Item Total"};
            int loop = 0;
            for (String headerText : headerTexts) {
                PdfPCell headerCell = new PdfPCell(new Phrase(headerText, header));
                if (loop == 0) headerCell.setPaddingLeft(4);
                if (loop == 5) headerCell.setPaddingRight(4);
                headerCell.setBorder(0);
                headerCell.setPaddingTop(6);
                headerCell.setPaddingBottom(6);
                headerCell.setBackgroundColor(java.awt.Color.decode("#384450"));
                headerCell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
                if (loop >= 3) {
                    headerCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                }
                mainTable.addCell(headerCell);
                loop++;
            }

            int num = 1;
            double total = 0;
            for (Jual.DJual dp : list) {
                total += dp.getItemTotal();
                PdfPCell number = new PdfPCell(new Phrase(String.valueOf(num++), fontBody));
                PdfPCell cell3 = new PdfPCell(new Phrase(dp.getItemId(), fontBody));
                PdfPCell cell4 = new PdfPCell(new Phrase(dp.getItemName(), fontBody));
                PdfPCell cell5 = new PdfPCell(new Phrase(dp.getItemPriceFormatted(), fontBody));
                PdfPCell cell6 = new PdfPCell(new Phrase(String.valueOf(dp.getItemQuantity()), fontBody));
                PdfPCell cell7 = new PdfPCell(new Phrase(dp.getItemTotalFormatted(), fontBody));
                ArrayList<PdfPCell> cells = new ArrayList<>(List.of(number, cell3, cell4, cell5, cell6, cell7));
                number.setPaddingLeft(4);
                cell7.setPaddingRight(4);
                loop = 0;
                for (PdfPCell cell : cells) {
                    cell.setBorder(0);
                    cell.setPaddingTop(4);
                    cell.setPaddingBottom(4);
                    if (num % 2 == 0) cell.setBackgroundColor(java.awt.Color.decode("#f0f0f0"));
                    else cell.setBackgroundColor(java.awt.Color.decode("#e6e6e6"));
                    if (loop >= 3) cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);

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
