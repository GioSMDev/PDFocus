package pdfocus;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DocumentsScene_Contrl implements Initializable{
    @FXML
    ScrollPane documentScrollPane;
    @FXML
    VBox documentVBox;

    PDFocusSettings settings;
    Alert loadError = new Alert(AlertType.ERROR);
    File documentsFolder;

    Parent root;
    Scene scene;
    Stage srcStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateDocExplorer();
    }

    private void populateDocExplorer(){
        documentVBox.getChildren().clear();
        //documentVBox.setSpacing(4); // Create spacing between each pdf file button
        double prefWidth = documentScrollPane.getPrefWidth();
        ObjectMapper objMapper = new ObjectMapper();
        File jSonSettingsFile = new File(System.getenv("APPDATA"), "PDFocus/settings.json");
        try {
            settings = objMapper.readValue(jSonSettingsFile, PDFocusSettings.class);
            documentsFolder = new File(settings.getFolderDirectory());
            if(documentsFolder.exists() && documentsFolder.isDirectory()){
                for(File pdfFile: documentsFolder.listFiles()){
                    if(pdfFile.getName().substring(pdfFile.getName().length() - 4).equals(".pdf")){
                        Button currFile = new Button();
                        currFile.getStyleClass().add("pdfDocumentButton");
                        currFile.setAlignment(Pos.BASELINE_LEFT);
                        currFile.setText(pdfFile.getName());
                        currFile.setPrefWidth(prefWidth);
                        currFile.setOnAction(event -> {openPdf(pdfFile);}); // Lambda function: allwos for consice anonymous code
                        documentVBox.getChildren().add(currFile);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Unable to read from JSON file");
        }
        documentScrollPane.setContent(documentVBox);
    }

    private void openPdf(File pdf){
        try {
            Desktop.getDesktop().open(pdf);
        } catch (Exception e) {
            loadError.setTitle("File Load Error");
            loadError.setContentText("Unable to load " + pdf.getName() + "\nPath: " + pdf.getAbsolutePath());
            loadError.showAndWait();
        }
    }

    public void openFolderDir(ActionEvent event){
        if(Desktop.isDesktopSupported()){
            try {
                Desktop.getDesktop().open(documentsFolder);
            } catch (Exception e) {
                loadError.setTitle("Directory Load Error");
                loadError.setContentText("Unable to load " + documentsFolder.getName() + "\nPath: " + documentsFolder.getAbsolutePath());
                loadError.showAndWait();
            }
        }
        
    }

    public void reloadExplorer(ActionEvent event){
        populateDocExplorer();
    }
//--------------------------------------------------------------------Menus---------------------------------------------------------------------------
    //Bionify Menu
    public void bionifyMenu(ActionEvent event) throws IOException{
        FXMLLoader bionifyLoader = new FXMLLoader(getClass().getResource("/resources/FXMLSource/UploadScene.fxml"));
        root = bionifyLoader.load();
        scene = new Scene(root);
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        srcStage.setScene(scene);
        srcStage.show();
    }

    //Settings Menu
    public void settingsMenu(ActionEvent event) throws IOException{
        FXMLLoader settingsLoader = new FXMLLoader(getClass().getResource("/resources/FXMLSource/SettingsScene.fxml"));
        root = settingsLoader.load();
        scene = new Scene(root);
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        srcStage.setScene(scene);
        srcStage.show();
    }
    
}
