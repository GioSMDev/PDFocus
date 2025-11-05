package pdfocus;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class UploadScene_Contrl implements Initializable{
    @FXML
    Button browseFileButton;
    @FXML
    Text inptFileTxtField;
    @FXML
    ImageView pageFlipIcon;
    @FXML
    Button cancelButton;
    @FXML
    Button generateButton;
    @FXML
    Text fileNameText;
    @FXML
    ImageView pdfCover;
    @FXML
    ProgressBar progressBar;
    @FXML
    Text progressMessage;
    @FXML
    Button terminateButton;
    @FXML
    ImageView genCycleIcon;
    @FXML
    Button pdfIconButton;
    @FXML
    Text BRFileNameText;
    @FXML
    StackPane pdfDropPane;
    @FXML
    CheckBox flipText;

    FileChooser fileChooser;
    Stage srcStage;
    Scene scene;
    File selectedFile;
    ExtensionFilter docFilter;
    Parent root;

    String folderDir;
    String selectedFileName;
    Task<Void> BRTask;
    File file_BR;
    static Alert loadError = new Alert(AlertType.ERROR);

    ObjectMapper objMapper;
    File jsonSettingsFile;
    PDFocusSettings settings;
    @Override
    public void initialize(URL location, ResourceBundle resources){
        //Load Json data for settings
        String appData = System.getenv("APPDATA");
        objMapper = new ObjectMapper();
        jsonSettingsFile = new File(appData,"PDFocus/settings.json");
        //Read Json data from file and mapp to PDFocusSettings.java Class
        try {
            settings = objMapper.readValue(jsonSettingsFile, PDFocusSettings.class);
            folderDir = settings.getFolderDirectory() + "\\";
        } catch (StreamReadException e) {
            System.out.println("Unable to read JSON file");
        } catch (DatabindException e) {
            System.out.println("Unable to bind JSON file to obj");
        } catch (IOException e) {
            e.printStackTrace();
        }

        cancelButton.setVisible(false);
        generateButton.setDisable(true);
        fileNameText.setVisible(false);
        terminateButton.setVisible(false);
        flipText.setSelected(false);

        pdfCover.setVisible(false);
        docFilter = new ExtensionFilter("PDF Files", "*.pdf");
        progressMessage.setText("");
        BRFileNameText.setText("");

    }

    public void dragPdf(DragEvent dEvent){
        Dragboard dragBoard = dEvent.getDragboard();
        if(dragBoard.hasFiles()){
            File dragedFile = dragBoard.getFiles().get(0);
            if(dragedFile.getName().substring(dragedFile.getName().length() - 4).equals(".pdf")){
                dEvent.acceptTransferModes(TransferMode.COPY); //REQUIRED: accepts the drag and copies the content (only if it is a pdf)
            }
        }
        dEvent.consume(); //Labels the event as "handeled" to prevent it from propigating to possible children ndoes listenign to the event (in our case we have no children)
    }
    public void dropPdf(DragEvent dEvent){
        srcStage = (Stage)((Node)dEvent.getSource()).getScene().getWindow();
        Dragboard dragBoard = dEvent.getDragboard();
        if(dragBoard.hasFiles()){
            File dragedFile = dragBoard.getFiles().get(0);
            System.out.println("Accepted");
            selectedFile = dragedFile;
            System.out.println(selectedFile.getAbsolutePath());
            this.displayCover(selectedFile);
        }
        dEvent.consume();
    }
    public void brosweFiles(ActionEvent event){
        fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF");
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();

        //Set Filter for PDF's only
        fileChooser.getExtensionFilters().add(docFilter);
        selectedFile = fileChooser.showOpenDialog(srcStage);
        this.displayCover(selectedFile);

    }

    private void displayCover(File file){
        if(file != null){
            try(PDDocument srcDoc = Loader.loadPDF(file)){
                browseFileButton.setVisible(false);
                inptFileTxtField.setVisible(false);
                pageFlipIcon.setVisible(false);

                cancelButton.setVisible(true);
                generateButton.setDisable(false);
                fileNameText.setVisible(true);
                fileNameText.setText(file.getName());

                //Grab image of src PDF's cover
                PDFRenderer coverRend = new PDFRenderer(srcDoc);
                BufferedImage cover = coverRend.renderImage(0);
                Image coverImg = SwingFXUtils.toFXImage(cover, null);
                pdfCover.setImage(coverImg);
                pdfCover.setVisible(true);
            }
            catch(Exception e){
                errorPopUp("File Load Error", "Unable to load " + file.getName() + "\nPath: " + file.getAbsolutePath());
            }

        }
    }

    public void cancelFile(ActionEvent event){
        flipText.setSelected(false);
        progressBar.setProgress(0);
        progressMessage.setText("");

        cancelButton.setVisible(false);
        selectedFile = null;
        browseFileButton.setVisible(true);
        inptFileTxtField.setVisible(true);
        pageFlipIcon.setVisible(true);
        generateButton.setDisable(true);
        fileNameText.setVisible(false);
        pdfCover.setVisible(false);

    }

    public void generateBRFile(ActionEvent event){
        File folderDirObj = new File(folderDir);
        if(!folderDirObj.exists()){
            errorPopUp("Missing or Inaccessible Folder Directory", folderDir + " is not accessible or does not exist.\n Address issue or change folder path in settings menu.");
            return;
        }

        BRFileNameText.setText("");
        genCycleIcon.setVisible(true);
        pdfIconButton.setVisible(false);
        progressBar.setProgress(-1);
        progressMessage.setText("Generating File...");
        cancelButton.setVisible(false);
        generateButton.setVisible(false);
        terminateButton.setVisible(true);
        terminateButton.setDisable(false);

        selectedFileName = selectedFile.getName();
        //Remove file type in name (*.pdf);
        selectedFileName = selectedFileName.substring(0, selectedFileName.length() - 4);

        //Run's Bonic reading generating in a new Thread class called "Task", simialr to "Callable" but allows Thread communciation with UI
        // creating a subclass of Task<Void>, overriding its methods, and assigning it to BRTask. (Called Anonymous inner class)
        BRTask = new Task<>(){
            //NOTE: You cannot update/touch any UI elements within a Task thread, they must all be update in the main JAVAFX UI thread, otherwise JAVAFx crashes
            @Override
            protected Void call(){
                System.out.println(selectedFileName);
                file_BR = BR_Generate_Struct.generate(folderDir, selectedFileName, selectedFile, flipText.isSelected());
                return null;
            }

            @Override
            protected void succeeded(){
                genCycleIcon.setVisible(false);
                pdfIconButton.setVisible(true);

                progressBar.setProgress(1);
                progressMessage.setText("Completed");

                BRFileNameText.setText(file_BR.getName());
                cancelButton.setVisible(true);
                terminateButton.setVisible(false);
                generateButton.setVisible(true);
                generateButton.setDisable(false);
                BRTask.isDone();
            }

            @Override
            protected void failed() {
                canclFailReset();
            }
            @Override
            protected void cancelled(){
                canclFailReset();
            }

            private void canclFailReset(){
                progressBar.setProgress(0);
                progressMessage.setText("");
                cancelButton.setVisible(true);
                generateButton.setVisible(true);
                generateButton.setDisable(false);
                BRTask.isDone();
            }
        };
        srcStage.setOnCloseRequest(closeEvent -> {BRTask.cancel();}); // Closes Task when Application closes
        new Thread(BRTask).start();
    }

    //----------------------------------------------------------------------------Source Document Input feild/Panel UI--------------------------------------------------------
    public void cancelBRGen(ActionEvent event){
        System.out.println("Before: " + BRTask.getState());
        BRTask.cancel();
        System.out.println("After: " + BRTask.getState());
        terminateButton.setVisible(false);
    }

    public void displayPdf(ActionEvent event){
        try {
            Desktop.getDesktop().open(file_BR);
        } catch (Exception e) {
            errorPopUp("File Load Error", "Unable to load " + file_BR.getName() + "\nPath: " + file_BR.getAbsolutePath());
        }
    }

    //--------------------------------------------------------------------------------Error Popup------------------------------------------------------------------------------
    private static void errorPopUp(String title, String cntText){
        loadError.setTitle(title);
        loadError.setContentText(cntText);
        loadError.showAndWait();
    }

    //-----------------------------------------------------------------------------------Menus---------------------------------------------------------------------------------
    //Settigns Menu
    public void settingsMenu(ActionEvent event) throws IOException{
        FXMLLoader settingsLoader = new FXMLLoader(getClass().getResource("/resources/FXMLSource/SettingsScene.fxml"));
        root = settingsLoader.load(); // Parent root is the base class for all JavaFX nodes: Builds the JavaFX hirarchy for the scene
        scene = new Scene(root);
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        srcStage.setScene(scene);
        srcStage.show();
    }

    //Documents Menu
    public void documentsMenu(ActionEvent event) throws IOException{
        FXMLLoader documentLoader = new FXMLLoader(getClass().getResource("/resources/FXMLSource/DocumentsScene.fxml"));
        root = documentLoader.load();
        scene = new Scene(root);
        srcStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        srcStage.setScene(scene);
        srcStage.show();
    }
}
