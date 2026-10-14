
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for knitting graph operations.
 *
 * Exercises knitting, unpicking and dropping stitches using
 * the MyrtleLeaf knitting pattern.
 *
 * These tests execute the knitting simulation and associated
 * sound operations, and may take approximately 10–15 minutes.
 */
class KnitGraphTest {

    /**
     * Verifies that knitting the complete pattern produces
     * the expected number of stitches.
     */
    @Test
    void knittingTest() throws Exception {
        KnitGraph actual = new KnitGraph(
            new KnitSpeakIn("knitSpeak/MyrtleLeaf.txt").getKnitFile(),
            new KnitPlay()
        );

        actual.doKnitting();

        assertEquals(442, actual.getNumberOfStitches());
    }

    /**
     * Verifies that two partially knitted graphs contain the
     * expected number of stitches and can be fully unpicked.
     *
     * Reuses the same graph between the two scenarios.
     */
    @Test
    void unpickingTest() throws Exception {
        String[] testCommands =
            new KnitSpeakIn("knitSpeak/MyrtleLeaf.txt").getKnitFile();

        KnitGraph actual = new KnitGraph(testCommands, new KnitPlay());

        actual.knitPartOfGraph(testCommands, 2);
        assertEquals(227, actual.getNumberOfStitches());

        actual.doUnpicking();
        assertEquals(0, actual.getNumberOfStitches());

        actual.knitPartOfGraph(testCommands, 3);
        assertEquals(155, actual.getNumberOfStitches());

        actual.doUnpicking();
        assertEquals(0, actual.getNumberOfStitches());
    }

    /**
     * Verifies the number and identity of stitches removed
     * when dropping a stitch from partially knitted graphs.
     *
     * Tests two different stages of the same knitting pattern.
     */
    @Test
    void dropAStitchTest() throws Exception {
        String[] testCommands =
            new KnitSpeakIn("knitSpeak/MyrtleLeaf.txt").getKnitFile();

        KnitGraph actual = new KnitGraph(testCommands, new KnitPlay());

        int[] firstExpected = {180, 159, 112, 91};
        int[] secondExpected = {115};

        actual.knitPartOfGraph(testCommands, 3);
        assertEquals(155, actual.getNumberOfStitches());

        actual.dropAStitch();
        assertEquals(154, actual.getNumberOfStitches());
        assertArrayEquals(secondExpected, actual.getDroppedStitches());

        actual.resetGraph();

        actual.knitPartOfGraph(testCommands, 2);
        assertEquals(227, actual.getNumberOfStitches());

        actual.dropAStitch();
        assertEquals(223, actual.getNumberOfStitches());
        assertArrayEquals(firstExpected, actual.getDroppedStitches());
    }
}
