import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
//returns an array of size 3 with randomly selected file paths to KnitSpeak files. This allows the flexibility to
//add more KnitSpeak files as the program evolves. A text file containing all the possible file path names is
//passed via the constructor. The only currently existing file is one for lace knitting patterns. More files could be
//added for cable patterns, textured patterns, plain patterns and so on.
public class KnitFileSelector {
    private final String[] KNIT_FILE_SELECT;
    private final ArrayList<String> ALL_FILES;
    public KnitFileSelector(String selection) {
        ALL_FILES = new ArrayList<String>();
        this.KNIT_FILE_SELECT = new String[3];
        FileReader fr = null;
        try {
            fr = new FileReader(selection);
            Scanner s = new Scanner(fr);
            while (s.hasNext()) {
                String knitFileName = s.nextLine();
                ALL_FILES.add("knitSpeak/" + knitFileName);
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
        int first = r.nextInt(ALL_FILES.size());
        int second = r.nextInt(ALL_FILES.size());
        while(second == first) {
            second = r.nextInt(ALL_FILES.size());
        }
        int third = r.nextInt(ALL_FILES.size());
        while(third == first || third == second) {
            third = r.nextInt(ALL_FILES.size());
        }
        KNIT_FILE_SELECT[0] = ALL_FILES.get(first);
        KNIT_FILE_SELECT[1] = ALL_FILES.get(second);
        KNIT_FILE_SELECT[2] = ALL_FILES.get(third);
        return KNIT_FILE_SELECT;
    }
}
