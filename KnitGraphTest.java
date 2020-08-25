import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.Test;

class KnitGraphTest {
	
//***PLEASE COMMENT-OUT CALL OF clearDroppedStitches() IN KnitGraph LINE 437 BEFORE RUNNING THIS TEST***//

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
		assertEquals(442, actual.getnumberOfStitches());
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
			assertEquals(227, actual.getnumberOfStitches());
			actual.doUnpicking();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(0, actual.getnumberOfStitches());
		
		try {
			actual.knitPartOfGraph(testCommands, 3);
			assertEquals(155, actual.getnumberOfStitches());
			actual.doUnpicking();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(0, actual.getnumberOfStitches());
		
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
			assertEquals(155, actual.getnumberOfStitches());
			actual.dropAStitch();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(154, actual.getnumberOfStitches());
		assertArrayEquals(secondExpected, actual.getDroppedStitches());
		
		actual.resetGraph();
		actual.clearDroppedStitches();
		try {
			actual.knitPartOfGraph(testCommands, 2);
			assertEquals(227, actual.getnumberOfStitches());
			actual.dropAStitch();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(223, actual.getnumberOfStitches());
		assertArrayEquals(firstExpected, actual.getDroppedStitches());
		
	}
}
