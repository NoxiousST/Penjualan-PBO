package me.stiller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.stiller.data.models.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;

public class Server {

    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "penjualan_pbo";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String DB_URL = "jdbc:mysql://localhost/" + DB_NAME;
    public Connection databaseLink;

    public Server() {
    }

    public Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            databaseLink = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        return databaseLink;
    }

    public ArrayList<Barang> readBarang() {
        ArrayList<Barang> list = new ArrayList<>();
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM barang");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Barang barang = new Barang(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getDouble(4),
                        resultSet.getInt(5),
                        resultSet.getInt(6)
                );
                list.add(barang);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public ArrayList<Konsumen> readKonsumen() {
        ArrayList<Konsumen> list = new ArrayList<>();
        Connection connection = new Server().getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM konsumen");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Konsumen konsumen = new Konsumen(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getString(6),
                        resultSet.getString(7)
                );
                list.add(konsumen);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public ArrayList<Supplier> readSupplier() {
        ArrayList<Supplier> list = new ArrayList<>();
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM supplier");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Supplier supplier = new Supplier(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5)
                );
                list.add(supplier);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }


    public ArrayList<Jual> retrieveJualData() {
        ArrayList<Jual> jualList = new ArrayList<>();

        try {
            PreparedStatement headerStatement = getConnection().prepareStatement("SELECT * FROM jual");
            ResultSet headerResultSet = headerStatement.executeQuery();

            while (headerResultSet.next()) {
                Jual jual = new Jual();
                jual.setOrderId(headerResultSet.getString(1));
                jual.setCustomerId(headerResultSet.getString(2));
                jual.setOrderDate(headerResultSet.getString(3));

                ObservableList<Jual.DJual> items = FXCollections.observableArrayList();

                PreparedStatement detailStatement = getConnection().prepareStatement("SELECT * FROM djual WHERE id_jual = ?");
                detailStatement.setString(1, jual.getOrderId());
                ResultSet detailResultSet = detailStatement.executeQuery();

                while (detailResultSet.next()) {
                    String itemId = detailResultSet.getString(3);
                    double itemPrice = detailResultSet.getDouble(4);
                    int itemQuantity = detailResultSet.getInt(5);

                    Jual.DJual item = new Jual.DJual();
                    item.setItemId(itemId);
                    item.setItemPrice(itemPrice);
                    item.setItemQuantity(itemQuantity);
                    items.add(item);
                }

                jual.setItems(items);
                jualList.add(jual);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Collections.reverse(jualList);
        return jualList;
    }

    public ArrayList<Pembelian> readPembelian() {
        ArrayList<Pembelian> list = new ArrayList<>();
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM pembelian");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Pembelian pembelian = new Pembelian(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3)
                );
                list.add(pembelian);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public User loginUser(String user, String password) {
        User rUser = new User();
        Connection connection = new Server().getConnection();

        String sql;
        String shaPass = DigestUtils.sha256Hex(password);
        if (EmailValidator.getInstance().isValid(user))
            sql = "SELECT * FROM user WHERE email = ? AND password = ?";
        else
            sql = "SELECT * FROM user WHERE username = ? AND password = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user);
            statement.setString(2, shaPass);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                rUser.setId(resultSet.getString(1));
                rUser.setUsername(resultSet.getString(2));
                rUser.setEmail(resultSet.getString(3));
                rUser.setPassword(resultSet.getString(4));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rUser;
    }

    public boolean checkUser(String user) {
        Connection connection = new Server().getConnection();
        String sql;
        if (EmailValidator.getInstance().isValid(user))
            sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        else
            sql = "SELECT COUNT(*) FROM user WHERE username = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return resultSet.getInt(1) > 0;

            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    public boolean insert(Barang barang) {
        String sql = "INSERT INTO barang (name, unit, price, stock, min_stock) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, barang.getItemName());
            statement.setString(2, barang.getItemUnit());
            statement.setDouble(3, barang.getItemPrice());
            statement.setInt(4, barang.getItemStock());
            statement.setInt(5, barang.getItemMinStock());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean insert(Konsumen konsumen) {
        String sql = "INSERT INTO konsumen (name, address, city, postal, phone, email) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, konsumen.getCustomerName());
            statement.setString(2, konsumen.getCustomerAddress());
            statement.setString(3, konsumen.getCustomerCity());
            statement.setString(4, konsumen.getCustomerPostal());
            statement.setString(5, konsumen.getCustomerPhone());
            statement.setString(6, konsumen.getCustomerEmail());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean insert(Supplier supplier) {
        LogManager.getLogger().debug(supplier.getSupplierName());
        LogManager.getLogger().debug(supplier.getSupplierEmail());
        LogManager.getLogger().debug(supplier.getSupplierAddress());
        LogManager.getLogger().debug(supplier.getItemId());

        String sql = "INSERT INTO supplier (name, email, address, itemId) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, supplier.getSupplierName());
            statement.setString(2, supplier.getSupplierEmail());
            statement.setString(3, supplier.getSupplierAddress());
            statement.setString(4, supplier.getItemId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean insert(Jual jual) {
        String sql = "INSERT INTO jual (id, id_konsumen, order_date) VALUES (?, ?, ?)";
        try {
            PreparedStatement headerStatement = getConnection().prepareStatement(sql);
            headerStatement.setString(1, jual.getOrderId());
            headerStatement.setString(2, jual.getCustomerId());
            headerStatement.setString(3, jual.getOrderDate());

            String sqldjual = "INSERT INTO djual (id_jual, id_barang, price, quantity) VALUES (?, ?, ?, ?)";
            for (Jual.DJual item : jual.getItems()) {
                PreparedStatement detailStatement = getConnection().prepareStatement(sqldjual);
                detailStatement.setString(1, jual.getOrderId());
                detailStatement.setString(2, item.getItemId());
                detailStatement.setDouble(3, item.getItemPrice());
                detailStatement.setInt(4, item.getItemQuantity());
                detailStatement.executeUpdate();
            }
            return headerStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean insert(Pembelian pembelian) {
        LogManager.getLogger().debug(pembelian.getOrderId());
        LogManager.getLogger().debug(pembelian.getOrderDate());
        LogManager.getLogger().debug(pembelian.getItems());

        String sql = "INSERT INTO pembelian (orderId, orderDate, items) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, pembelian.getOrderId());
            statement.setString(2, pembelian.getOrderDate());
            statement.setString(3, pembelian.getItems());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean registerUser(User user) {
        String sql = "INSERT INTO user (username, email, password) VALUES (?, ?, ?)";
        String shaPass = DigestUtils.sha256Hex(user.getPassword());

        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, shaPass);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public int isExist(String username, String email) {
        try {
            String query = "SELECT COUNT(*) FROM user WHERE username = ? OR email = ?";
            PreparedStatement statement = getConnection().prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, email);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    if (resultSet.getString("username").equals(username) && resultSet.getString("email").equals(email)) {
                        return 3; // Both username and email exist
                    } else if (resultSet.getString("email").equals(username)) {
                        return 2; // Email exists
                    } else {
                        return 1; // Username exists
                    }
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean update(Barang barang) {
        Connection connection = new Server().getConnection();
        String sql = "UPDATE barang SET name = ?, unit = ?, price = ?, stock = ?, min_stock = ? WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, barang.getItemName());
            statement.setString(2, barang.getItemUnit());
            statement.setDouble(3, barang.getItemPrice());
            statement.setInt(4, barang.getItemStock());
            statement.setInt(5, barang.getItemMinStock());
            statement.setString(6, barang.getItemId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean update(Konsumen konsumen) {
        Connection connection = new Server().getConnection();
        String sql = "UPDATE konsumen SET name = ?, address = ?, city = ?, postal = ?, phone = ?, email = ? WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, konsumen.getCustomerName());
            statement.setString(2, konsumen.getCustomerAddress());
            statement.setString(3, konsumen.getCustomerCity());
            statement.setString(4, konsumen.getCustomerPostal());
            statement.setString(5, konsumen.getCustomerPhone());
            statement.setString(6, konsumen.getCustomerEmail());
            statement.setString(7, konsumen.getCustomerId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean update(Supplier supplier) {
        Connection connection = new Server().getConnection();
        String sql = "UPDATE supplier SET name = ?, email = ?, address = ?, itemId = ? WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, supplier.getSupplierName());
            statement.setString(2, supplier.getSupplierEmail());
            statement.setString(3, supplier.getSupplierAddress());
            statement.setString(4, supplier.getItemId());
            statement.setString(5, supplier.getSupplierId());
            LogManager.getLogger().debug(statement.executeUpdate());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            LogManager.getLogger().debug(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(Barang barang) {
        LogManager.getLogger().debug(barang.getItemId());
        LogManager.getLogger().debug(barang.getItemName());
        LogManager.getLogger().debug(barang.getItemUnit());
        LogManager.getLogger().debug(barang.getItemPrice());
        LogManager.getLogger().debug(barang.getItemStock());
        LogManager.getLogger().debug(barang.getItemMinStock());
        String sql = "DELETE FROM barang WHERE id = ?";
        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, barang.getItemId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean delete(Konsumen konsumen) {
        Connection connection = new Server().getConnection();
        String sql = "DELETE FROM konsumen WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, konsumen.getCustomerId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean delete(Supplier supplier) {
        String sql = "DELETE FROM supplier WHERE id = ?";
        try {
            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, supplier.getSupplierId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getLast() {
        Connection connection = new Server().getConnection();
        String sql = "SELECT MAX(id) FROM jual";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int lastIndex = resultSet.getInt(1);
                return lastIndex + 1;
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getLastPembelian() {
        Connection connection = new Server().getConnection();
        String sql = "SELECT MAX(orderId) FROM pembelian";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int lastIndex = resultSet.getInt(1);
                return lastIndex + 1;
            }
            return 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void backupServer(String path) {
        Logger log = LogManager.getLogger();

        String[] command = {"mysqldump", "-h", HOST, "-P", PORT, "-u", DB_USER, DB_NAME};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(new File(path));

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) log.info("Backup created successfully.");
            else log.error("Backup creation failed. Exit code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
