
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Reads and parses knitting instructions written in KnitSpeak.
 *
 * Converts a pattern file into an ordered array of knitting commands
 * that can be processed by KnitGraph.
 *
 * Commands are separated using punctuation, normalised to lowercase,
 * and expanded where asterisk markers indicate repeated instructions.
 */
public class KnitSpeakIn {

    private final String[] knitArray;

    /**
     * Reads a KnitSpeak pattern file and prepares its commands.
     *
     * Splits instructions at colons, commas and full stops,
     * removes row labels, and expands repeated command sequences
     * enclosed by asterisks.
     *
     * @param filePath path to the KnitSpeak pattern file
     */
    public KnitSpeakIn(String filePath) {
        boolean asteriskFound = false;
        int asteriskStartLocation = 0;
        int reps = 0;

        ArrayList<String> knitFile = new ArrayList<>();

        // Read and normalise the knitting instructions.
        try (Scanner scanner = new Scanner(new FileReader(filePath))) {
            while (scanner.hasNextLine()) {
                String[] knitLine = scanner.nextLine().split("[:,.]");

                for (String knit : knitLine) {
                    knit = knit.trim().toLowerCase();
                    knitFile.add(knit);
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        // Remove row labels before processing repeated commands.
        knitFile.removeIf(command -> command.contains("row"));

        // Expand command sequences enclosed by asterisks.
        for (int i = 0; i < knitFile.size(); i++) {
            if (knitFile.get(i).contains("*")) {
                if (!asteriskFound) {
                    asteriskFound = true;
                    asteriskStartLocation = i;

                    String command = knitFile.get(i).replace('*', ' ').trim();
                    knitFile.set(i, command);
                } else {
                    asteriskFound = false;
                    reps = extractInt(knitFile.get(i));
                    knitFile.remove(i);

                    // Insert repeated commands in their original order.
                    int count = 0;
                    while (count < reps) {
                        for (int sub = i - 1; sub >= asteriskStartLocation; sub--) {
                            knitFile.add(i, knitFile.get(sub));
                        }
                        count++;
                    }
                }
            }
        }

        this.knitArray = knitFile.toArray(new String[0]);
    }

    /**
     * Extracts the numeric repetition count from a command.
     *
     * Removes non-numeric characters and returns 1 when
     * no number is present.
     *
     * @param str command containing a possible repetition count
     * @return the extracted integer, or 1 if no digits are found
     */
    private static int extractInt(String str) {
        str = str.replaceAll("[^\\d]", "").trim();

        if (str.isEmpty()) {
            return 1;
        }

        return Integer.parseInt(str);
    }

    /**
     * Returns the parsed knitting commands in execution order.
     *
     * @return an array of KnitSpeak commands
     */
    String[] getKnitFile() {
        return knitArray;
    }
}
