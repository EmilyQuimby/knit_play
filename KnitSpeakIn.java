import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
//reads in a given KnitSpeak file and extracts the knitting commands in the correct order. Returns all the commands
//as an array. This can then be iterated over in the KnitGraph in order to create the graph.
public class KnitSpeakIn {
    private final String[] knitArray;
    private int asteriskStartLocation;
    public KnitSpeakIn (String filePath) {
        boolean asteriskFound = false;
        int reps = 0;
        ArrayList<String> knitFile = new ArrayList<String>();
        FileReader fr = null;
        try {
            fr = new FileReader(filePath);
            Scanner s = new Scanner(fr);
            while (s.hasNext()) {
                String[] knitLine = s.nextLine().split("[:,.]");
                for (String knit : knitLine) {
                    knit = knit.trim();
                    knit = knit.toLowerCase();
                    knitFile.add(knit);
                }
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
        //find commands with asterisks - denoting commands to be repeated
        for(int i=0; i<knitFile.size(); i++) {
            //the following statement can be removed if we want to see which row is being worked
            if (knitFile.get(i).contains("row")) {
                knitFile.remove(i);
            }
            if(knitFile.get(i).contains("*")) {
                if(!asteriskFound) {
                    asteriskFound = true;
                    asteriskStartLocation = i;
                    String here = knitFile.get(i).replace('*', ' ');
                    here = here.trim();
                    knitFile.set(i, here);
                }
                else {
                    asteriskFound = false;
                    reps = extractInt(knitFile.get(i));
                    knitFile.remove(i);
                    //insert repeated commands in order into knitFile
                    int count = 0;
                    while(count< reps) {
                        for (int sub = i -1; sub >= asteriskStartLocation; sub--) {
                            knitFile.add(i, knitFile.get(sub));
                        }
                        count++;
                    }

                }
            }
        }
        this.knitArray = new String[knitFile.size()];
        knitFile.toArray(knitArray);
    }
    static int extractInt(String str) {
        str = str.replaceAll("[^\\d]", "");
        str = str.trim();
        if(str.equals("")) {
            return 1;
        }
        return Integer.parseInt(str);
    }
    String[] getKnitFile() {
        return knitArray;
    }
}
