package pdfocus;

import java.io.InputStream;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

public class BR_Generate_Text extends PDFTextStripper {

    private static PDPageContentStream contentStream;
    private static PDDocument BrFile_ref = null;
    private static PDPage pageBR;
    private static int PageIndex_ref;
    private PDType0Font TNR_BOLD;
    private PDType0Font TNR;
    private PDType0Font TNR_Math;
    private static boolean isFlipped_ref;
    
    public BR_Generate_Text() throws IOException{
        try(InputStream fontStreamBld = getClass().getResourceAsStream("/resources/Fonts/timesbd.ttf"); InputStream fontStream = getClass().getResourceAsStream("/resources/Fonts/times.ttf"); InputStream fontStreamMath = getClass().getResourceAsStream("/resources/Fonts/XITSMath-Regular.ttf");){
            TNR_BOLD = PDType0Font.load(BrFile_ref, fontStreamBld, true); // true = embeded
            TNR = PDType0Font.load(BrFile_ref, fontStream, true);
            TNR_Math = PDType0Font.load(BrFile_ref, fontStreamMath, true);
        }
    }

    public static void generateBRText(int PageIndex, PDDocument srcFile, PDDocument BrFile, boolean isFlipped) throws IOException{
        BrFile_ref = BrFile;
        PageIndex_ref = PageIndex;
        isFlipped_ref = isFlipped;
        pageBR = BrFile.getPage(PageIndex);
        BR_Generate_Text pdfStripper = new BR_Generate_Text();
        pdfStripper.setSortByPosition(true);
        contentStream = new PDPageContentStream(BrFile, pageBR, PDPageContentStream.AppendMode.APPEND, false); //for export use: true, for debugging use: false

        pdfStripper.setStartPage(PageIndex + 1);
        pdfStripper.setEndPage(PageIndex + 1);

        try{
            @SuppressWarnings("unused") // <- supresses warning that "text" var is unused
            String text = pdfStripper.getText(srcFile);
            text = null; // Empties instance of text for garbage removal
        }
        catch(IOException e){
            System.out.println("ERROR: Unable to extract text from document");
        }
        contentStream.close();

    }

 
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException{
        //TextPosition contains data for position, font, ect.
        String character;
        char ch_base;
        int wordSize;
        int boldedIndexMax;
        float fontSize;
        float fontSize_offset = 0f;
        ArrayList<TextPosition> word = new ArrayList<TextPosition>();
        for(TextPosition text: textPositions){
            character = text.getUnicode(); //gives the actual letter
            ch_base = character.charAt(0);
            if(Character.UnicodeBlock.of(ch_base) == Character.UnicodeBlock.BASIC_LATIN && ((ch_base >= 'A' && ch_base <= 'Z') || (ch_base >= 'a' && ch_base <= 'z'))){

                //character is a letter
                word.add(text);

                //Checks if currents text obj is the last element in the list and if it isn't allows next itteration of loop, else indicates
                //the final word has been reached and goes on to write final word. Used in case no extra character defines end of list of characters (ex. titles)
                if(!(textPositions.get(textPositions.size() - 1) == text)){
                    continue;
                }
            }

            if(word.size() > 0)
            {
                wordSize = word.size() - 1;
                boldedIndexMax = wordSize/2;
                //Write bolded letters
                for(int i = 0; i <= boldedIndexMax; ++i){
                    fontSize = setFontSize(word.get(i).getFontSizeInPt());
                    contentStream.setFont(TNR_BOLD, fontSize - fontSize_offset);
                    writeToFile(word.get(i), true);
                }
                //Write Non Bolded letters
                for(int i = boldedIndexMax + 1; i < word.size(); ++i){
                    fontSize = setFontSize(word.get(i).getFontSizeInPt());
                    contentStream.setFont(TNR, fontSize - fontSize_offset);
                    writeToFile(word.get(i), true);
                }

            }
            word.clear();
            //Write characters that are not letters
            fontSize = setFontSize(text.getFontSizeInPt());
            contentStream.setFont(TNR_Math, fontSize - fontSize_offset);
            if(text.getUnicode().equals(" ")){
                writeToFile(text, false);
            }
            else{
                writeToFile(text, true);
            }
            
        }
        word = null;   
    }

    private static void writeToFile(TextPosition text, boolean drawRec) throws IOException{
        String character = text.getUnicode();
        float xPos = text.getXDirAdj(); // gives X position
        float yPos;
        float pageHeight = BrFile_ref.getPage(PageIndex_ref).getMediaBox().getHeight();

        if(isFlipped_ref){ //Google Docs PDF's
            yPos = text.getYDirAdj();
        }
        else{//All other PDF's
            // Flip Y axis back since page is inverted: (0,0) is in bottom left corner, not top left corner
            yPos = pageHeight - text.getYDirAdj();
        }
        //Draw white rectangle behind text: Rendered to make text more prominent and stand out from page in case page is photo copy of text
        if(drawRec){
            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.setStrokingColor(Color.WHITE);
            contentStream.addRect(xPos, yPos, text.getWidth(), text.getHeight() + 2.5f);
            contentStream.fillAndStroke();

            //Revert stroking and nonstroking colors to black for text rendering
            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.setStrokingColor(Color.BLACK);
        }
            
        
        //Write to BrFile
        contentStream.beginText();

        if (isFlipped_ref) {
            // Flipped (Google Docs, etc.)
            // Flip text upright by scaling Y by -1
            contentStream.setTextMatrix(new Matrix(1, 0, 0, -1, xPos, yPos));
        } else {
            // Normal (downloaded PDFs, Word exports)
            contentStream.setTextMatrix(Matrix.getTranslateInstance(xPos, yPos));
        }

        try{
            contentStream.showText(character);
        }
        catch(Exception eArg){ // If the font does not contain the symbol, replace it with blank space
            System.out.println("Exception Found:" + character);
            contentStream.showText(" ");
        }
        contentStream.endText();
        
    }

    //Preventative measure to ensure font is at a readable size
    private static float setFontSize(float rawFontSize){
        if(rawFontSize < 8){
            return 8;
        }
        else if(rawFontSize > 12){
            return rawFontSize - 5;
        }
        return rawFontSize;
    }
}
