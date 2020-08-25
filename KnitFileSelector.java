import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
//returns an array of size 3 with randomly selected file paths to KnitSpeak files. This allows the flexibility to
//add more KnitSpeak files as the program evolves. A text file containing all the possible file path names is
//passed via the constructor. The only currently existing file is one for lce knitting patterns. Mor files could be
//added for cable patterns, textured patterns, plain patterns and so on.
public class KnitFileSelector {
    private final String[] knitFileSelection;
    private final ArrayList<String> allFiles;
    public KnitFileSelector(String selection) {
        allFiles = new ArrayList<String>();
        this.knitFileSelection = new String[3];
        FileReader fr = null;
        try {
            fr = new FileReader(selection);
            Scanner s = new Scanner(fr);
            while (s.hasNext()) {
                String knitFileName = s.nextLine();
                allFiles.add("knitSpeak/" + knitFileName);
            }
            s.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }finally {
            if(fr!=null) {
                try {
                    fr.close();
                }catch (IOException eIO) {
                    eIO.printStackTrace();
                }
            }
        }
    }
    //randomly selects 3 file paths from all whilst ensuring there are no duplicates.
    String[] getKnitFileSelection() {
        Random r = new Random();
        int first = r.nextInt(allFiles.size());
        int second = r.nextInt(allFiles.size());
        while(second == first) {
            second = r.nextInt(allFiles.size());
        }
        int third = r.nextInt(allFiles.size());
        while(third == first || third == second) {
            third = r.nextInt(allFiles.size());
        }
        knitFileSelection[0] = allFiles.get(first);
        knitFileSelection[1] = allFiles.get(second);
        knitFileSelection[2] = allFiles.get(third);
        return knitFileSelection;
    }
}
