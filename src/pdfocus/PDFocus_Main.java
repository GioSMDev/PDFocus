package pdfocus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class PDFocus_Main extends Application{

    private static void createJsonSettings() throws IOException{

        String appData = System.getenv("APPDATA");
        File tempDir = new File(appData,"PDFocus/settings.json");

        if(!tempDir.exists()){
            File configDir = new File(appData,"PDFocus");
            configDir.mkdir();

            File settingsFile = new File(configDir.getPath() + "/" + "settings.json");
            settingsFile.createNewFile();

            InputStream settingsConfigStream = PDFocus_Main.class.getResourceAsStream("/resources/defaultConfig/PDFocusSettings.json");
            OutputStream outputStream = new FileOutputStream(settingsFile.getAbsolutePath());
            settingsConfigStream.transferTo(outputStream);
            settingsConfigStream.close();
            outputStream.close();

            ObjectMapper objMapper = new ObjectMapper();
            PDFocusSettings settings = objMapper.readValue(settingsFile, PDFocusSettings.class);
            settings.setFolderDirectory(settings.getFolderDirectory().replace("${user.home}", System.getProperty("user.home")));
            objMapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile, settings);
        }
    }

    public static void main(String[] args) throws Exception {
        createJsonSettings();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Image logoIcon = new Image(getClass().getResourceAsStream("/resources/IconAssets/PDFocus_Icon.png"));
        
        Parent root = FXMLLoader.load(getClass().getResource("/resources/FXMLSource/UploadScene.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(logoIcon);
        primaryStage.setTitle("PDFocus");
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
