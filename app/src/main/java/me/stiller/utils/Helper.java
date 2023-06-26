package me.stiller.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.util.Strings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;


public class Helper {

    public static String formatPrice(Double number) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(number);
    }

    public static void concurentTask(Runnable task) {
        Thread backgroundThread = new Thread(() -> {
            try {
                Thread.sleep(80);
                Platform.runLater(task);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        backgroundThread.start();
    }

    public static void delayRun(Runnable r, int delayMilis) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMilis));
        pause.setOnFinished(event -> r.run());
        pause.play();
    }

    public static SVGPath getIcon(String icon) {
        InputStream is = getFileFromResourceAsStream("me/stiller/icons/" + icon + ".svg");
        String svgContent = printInputStream(is).replace("\"></path><path d=\"", " ");
        int startIndex = svgContent.indexOf(" d=\"") + 4;
        int endIndex = svgContent.indexOf("\"", startIndex + 4);
        String pathString = svgContent.substring(startIndex, endIndex);

        SVGPath path = new SVGPath();
        path.setContent(pathString);

        path.getStyleClass().add("icon");
        return path;
    }

    private static InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = Helper.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    private static String printInputStream(InputStream is) {
        try {
            InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Strings.EMPTY;
    }

    public static String getExportPath(Node root) {
        Stage stage = (Stage) root.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XLSX Worksheet", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setTitle("Specify A File to Save");
        String filename = "report.xlsx";

        fileChooser.setInitialFileName(filename);

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) return file.getAbsolutePath();
        else return Strings.EMPTY;
    }
}
