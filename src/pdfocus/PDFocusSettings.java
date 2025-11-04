package pdfocus;

public class PDFocusSettings {
    private String folderDirectory;
    private Boolean darkMode;
    private Boolean genBRFileFolder;

    public String getFolderDirectory(){
        return folderDirectory;
    }
    public void setFolderDirectory(String dirPath){
        folderDirectory = dirPath;
    }


    public Boolean getGenBRFileFolder(){
        return genBRFileFolder;
    }
    public void setGenBRFileFolder(Boolean state){
        genBRFileFolder = state;
    }

    
    public Boolean getDarkMode(){
        return darkMode;
    }
    public void setDarkMode(Boolean state){
        darkMode = state;
    }
}
