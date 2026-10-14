
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Selects knitting pattern files from a configurable list.
 *
 * Reads a text file containing available KnitSpeak pattern filenames
 * and randomly selects three distinct file paths.
 *
 * Keeping the pattern list separate from the code allows additional
 * knitting patterns, such as cable or textured patterns, to be added
 * without changing the selection logic.
 */
public class KnitFileSelector {

    private final String[] KNIT_FILE_SELECT;
    private final ArrayList<String> ALL_FILES;

    /**
     * Loads the available knitting pattern filenames from a text file.
     *
     * Each filename is prefixed with the KnitSpeak directory path
     * and stored for subsequent random selection.
     *
     * @param selection path to the text file listing available patterns
     */
    public KnitFileSelector(String selection) {
        ALL_FILES = new ArrayList<String>();
        this.KNIT_FILE_SELECT = new String[3];

        try (Scanner scanner = new Scanner(new FileReader(selection))) {
            while (scanner.hasNextLine()) {
                String knitFileName = scanner.nextLine();
                ALL_FILES.add("knitSpeak/" + knitFileName);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Randomly selects three distinct knitting pattern file paths.
     *
     * Generates three unique random indices into the available
     * file list and returns the corresponding paths.
     *
     * @return an array containing three distinct pattern file paths
     * @throws IllegalStateException if fewer than three files are available
     */
    String[] getKnitFileSelection() {
        if (ALL_FILES.size() < 3) {
            throw new IllegalStateException(
                "At least three knitting pattern files are required."
            );
        }

        Random r = new Random();

        int first = r.nextInt(ALL_FILES.size());

        int second = r.nextInt(ALL_FILES.size());
        while (second == first) {
            second = r.nextInt(ALL_FILES.size());
        }

        int third = r.nextInt(ALL_FILES.size());
        while (third == first || third == second) {
            third = r.nextInt(ALL_FILES.size());
        }

        KNIT_FILE_SELECT[0] = ALL_FILES.get(first);
        KNIT_FILE_SELECT[1] = ALL_FILES.get(second);
        KNIT_FILE_SELECT[2] = ALL_FILES.get(third);

        return KNIT_FILE_SELECT;
    }
}
