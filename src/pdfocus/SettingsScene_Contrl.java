package pdfocus;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class SettingsScene_Contrl implements Initializable{

    @FXML
    TextField folderPath;
    @FXML
    CheckBox darkModeCheckBox;
    @FXML
    Text applySettingsAlert;

    DirectoryChooser dirChooser;
    File dir;
    Stage srcStage;
    Parent root;
    Scene scene;

    //Settings
    PDFocusSettings settings;
    ObjectMapper objMapper;
    File jsonSettingsFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String appData = System.getenv("APPDATA");
        applySettingsAlert.setVisible(false);
        objMapper = new ObjectMapper(); //Used for JSOn parsing and generation
        jsonSettingsFile = new File(appData,"PDFocus/settings.json");
        try {
            settings = objMapper.readValue(jsonSettingsFile, PDFocusSettings.class); //Parses JSON into Java structures
        } catch (IOException e) {
            System.out.println("Unable to load settings");
        }
        folderPath.setText(settings.getFolderDirectory());
        darkModeCheckBox.setSelected(settings.getDarkMode());
    }

    public void changeDir(ActionEvent event){
        dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose Directory");
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        dir = dirChooser.showDialog(srcStage);
        if(dir != null){
            applySettingsAlert.setVisible(true);
            folderPath.setText(dir.getAbsolutePath() + "\\");
        }
    }

    public void checkBoxSelection(ActionEvent event){
        applySettingsAlert.setVisible(true);
    }

    public void applySettings(ActionEvent event){
        applySettingsAlert.setVisible(false);
        settings.setFolderDirectory(folderPath.getText());
        settings.setDarkMode(darkModeCheckBox.isSelected());
        try {
            objMapper.writerWithDefaultPrettyPrinter().writeValue(jsonSettingsFile, settings);
        } catch (IOException e) {
            System.out.println("Unable to write to JSON file");
        }
        
    }

    //-------------------------------------------------------------------------------------Menus--------------------------------------------------------------------------
    
    //Bionify Menu
    public void bionifyMenu(ActionEvent event) throws IOException{
        FXMLLoader bionifyLoader = new FXMLLoader(getClass().getResource("/resources/FXMLSource/UploadScene.fxml"));
        root = bionifyLoader.load();
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        srcStage.setScene(scene);
        srcStage.show();
    }

    //Documents Menu
    public void documentsMenu(ActionEvent event) throws IOException{
        FXMLLoader documentsLoader = new FXMLLoader(getClass().getResource("/resources/FXMLSource/DocumentsScene.fxml"));
        root = documentsLoader.load();
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        srcStage.setScene(scene);
        srcStage.show();
    }
}
