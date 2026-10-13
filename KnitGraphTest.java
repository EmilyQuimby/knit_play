import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.Test;

class KnitGraphTest {
	//*Please note this entire test takes approx 10-15 minutes to run*//

	@Test
	//knits a completed graph and checks that the correct number of stitches have been made
	void knittingTest() {
		KnitGraph actual = new KnitGraph(new KnitSpeakIn("knitSpeak/MyrtleLeaf.txt").getKnitFile(),
				new KnitPlay());
		try {
			actual.doKnitting();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(442, actual.getNumberOfStitches());
	}
	@Test
	//part-knits a graph, checks that the correct number of stitches have been made, unpicks the graph
	//and checks that all stitches have been removed. Checks for a couple of part-knitted graphs with varying sizes
	void unpickingTest() {
		String[] testCommands = new KnitSpeakIn("knitSpeak/MyrtleLeaf.txt").getKnitFile();
		KnitGraph actual = new KnitGraph(testCommands,
				new KnitPlay());
		try {
			actual.knitPartOfGraph(testCommands, 2);
			assertEquals(227, actual.getNumberOfStitches());
			actual.doUnpicking();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(0, actual.getNumberOfStitches());
		
		try {
			actual.knitPartOfGraph(testCommands, 3);
			assertEquals(155, actual.getNumberOfStitches());
			actual.doUnpicking();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(0, actual.getNumberOfStitches());
		
	}
	@Test
	//part-knits a graph, checks that the correct number of stitches have been made, drops a stitch
	//and checks that the correct number of stitches have been removed (dropped). Also checks that
	//the correct stitches have been removed (dropped).
	//Checks for a couple of part-knitted graphs with varying sizes
	void dropAStitchTest() {
		String[] testCommands = new KnitSpeakIn("knitSpeak/MyrtleLeaf.txt").getKnitFile();
		KnitGraph actual = new KnitGraph(testCommands,
				new KnitPlay());
		int[] firstExpected = new int[] {180,159,112,91};
		int[] secondExpected = new int[] {115};
		
		try {
			actual.knitPartOfGraph(testCommands, 3);
			assertEquals(155, actual.getNumberOfStitches());
			actual.dropAStitch();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(154, actual.getNumberOfStitches());
		assertArrayEquals(secondExpected, actual.getDroppedStitches());
		
		actual.resetGraph();
		try {
			actual.knitPartOfGraph(testCommands, 2);
			assertEquals(227, actual.getNumberOfStitches());
			actual.dropAStitch();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(223, actual.getNumberOfStitches());
		assertArrayEquals(firstExpected, actual.getDroppedStitches());
		
	}
}
