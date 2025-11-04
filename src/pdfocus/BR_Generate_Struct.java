package pdfocus;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;


public class BR_Generate_Struct{
    public BR_Generate_Struct(){
        System.out.println("Running BR_Generate_Struct");
    }
    public static File generate(String srcFileDir, String srcFileName, File srcFile, Boolean flipText){
        File BR_genDir_obj = new File(genBRFileName(srcFileDir, srcFileName));
        try(PDDocument srcDoc = Loader.loadPDF(srcFile)){ //try-with-resources block automatically closes srcDoc after block is finished

            PDDocument BRDoc = Loader.loadPDF(srcFile); //create Copy of Source PDF
            int numPages = BRDoc.getNumberOfPages();
            boolean flippedText = removeTextFields(BRDoc, flipText); //remove all text elements
            for(int page = 0; page < numPages; ++page){
                BR_Generate_Text.generateBRText(page, srcDoc, BRDoc, flippedText); // generate Bionic Reading Formatted text for each page
            }

            //Save file
            try {
                BRDoc.save(BR_genDir_obj);
            } catch (Exception e) {
                BR_genDir_obj.delete();
                System.out.println("Unable to save " + BR_genDir_obj.getName()); // <-- Debugging
            }
            BRDoc.close();
        }
        catch(IOException e1){
            System.out.println(srcFile.getName() + " does not exist");
        }
        return BR_genDir_obj;
        
    }

    private static boolean removeTextFields(PDDocument docInpt, Boolean flipText) throws IOException{
        boolean insideTextBlock = false;
        List<Object> newTokens = new ArrayList<Object>();
        boolean isFlipped = false;
        //Generate new pdf file with same number of pages
        for(PDPage page: docInpt.getPages()){
            //Clears each page
            PDFStreamParser docParser = new PDFStreamParser(page);
            List<Object> tokens = docParser.parse();

            for(int i = 0; i < tokens.size(); i++){
                Object token = tokens.get(i);
                if(token instanceof Operator){
                    String name = ((Operator) token).getName();
                    
                    if(name.equals("BT")){ //Begining of textBlock
                        insideTextBlock = true;
                        continue;
                    }
                    else if(name.equals("ET")){ //End of textBlock
                        insideTextBlock = false;
                        continue;
                    }
                    else if (name.equals("cm") && i >= 6) { //Check if document is flipped (ex. in case pdf is from google Docs)
                        try {
                            float d = ((COSNumber) tokens.get(i - 3)).floatValue();  // a b c d e f: index -3 = 'd'
                            if (d < 0) {
                                isFlipped = true;
                            }
                        } catch (ClassCastException | IndexOutOfBoundsException e) {
                            System.out.println("Invalid cm operands at index " + i);
                        }
                    }

                    if(insideTextBlock){
                        //Skips over any text elements when inside a text block
                        continue;
                    }
                    else{
                        newTokens.add(token);
                    }
                }
                else {
                    // token is operand (COSBase subclass)
                    // skips operands inside text block
                    if (!insideTextBlock) {
                        newTokens.add(token);
                    }
                }
            }

            // Create new COSStream (Raw bianary Stream) for page content
            COSStream newStream = docInpt.getDocument().createCOSStream();
            //Wraps low level COSStream in higher level PDF stream, makign it usable with PDF pages
            PDStream pdStream = new PDStream(newStream);

            // Write tokens into the stream:
            try (OutputStream out = newStream.createOutputStream()) { //Writes raw bytes into the COSStream
                ContentStreamWriter writer = new ContentStreamWriter(out); //Serializes list of Operhands and Operators back into PDF format
                writer.writeTokens(newTokens); //writes list of new tokens into output stream as serialization
            }
            catch(IOException eArg){
                System.out.println("Unable to write tokens to COSStream");
            }
            
            page.setContents(pdStream); //Sets content of specified page to newly generated stream of tokens
            newTokens.clear();
        }
        newTokens = null; //Empties list for garbage removal
        if(flipText){
            return !isFlipped;
        }
        return isFlipped;
    }

    private static String genBRFileName(String srcFileDir, String srcFileName){
        String BRFileNameBase = srcFileDir + srcFileName + "_BR_v";
        String BRFileName = BRFileNameBase + "1";
        File tempFile = new File(BRFileName + ".pdf");
        int versionCounter = 1;

        while(tempFile.exists()){
            BRFileName = BRFileNameBase;
            ++versionCounter;
            BRFileName = BRFileNameBase + "" + versionCounter;
            tempFile = new File(BRFileName + ".pdf");
        }

        return BRFileName + ".pdf";
        
    }
}
