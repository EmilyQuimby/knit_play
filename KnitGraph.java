import java.util.ArrayList;

/**
 * Represents a knitting pattern as a graph of stitches and their connections.
 *
 * Processes KnitSpeak commands to construct the graph, supports unpicking
 * and dropping stitches, and associates sounds with knitting operations.
 */
public class KnitGraph {
    private final ArrayList<Stitch> STITCHES;
    private final ArrayList<Edge> EDGES;
    private final ArrayList<SoundPlayer> SOUNDS; // stores all instances of SoundPlayer allows us to close clips remotely
    private final ArrayList<Stitch> DROPPED_STITCHES;
    private final String[] COMMANDS;
    private final KnitPlay KNIT_PLAY;           // this is required so that we can update the GUI 
    private int stitchesInRowBelow;
    private int workedStitches;
    private int workingStitch;                  // Index of the stitch being worked in the row below.
    private int increases;
    private int decreases;
    private int currentCommandNo;
    private int stitchNumber;
    private int turningStitchNo;
    private int edgeLength;
    private boolean castingOff;
    private boolean finishedKnitting;

    /**
     * Creates an empty knitting graph for the supplied pattern.
     *
     * Initialises the collections and state used to construct,
     * modify and play the graph.
     *
     * @param knitFile the KnitSpeak commands defining the pattern
     * @param knitPlay the application used to display knitting progress
     */
    public KnitGraph(String[] knitFile, KnitPlay knitPlay) {
        this.KNIT_PLAY = knitPlay;
        this.COMMANDS = knitFile;
        STITCHES = new ArrayList<Stitch>();
        EDGES = new ArrayList<Edge>();
        SOUNDS = new ArrayList<SoundPlayer>();
        DROPPED_STITCHES = new ArrayList<Stitch>();
        stitchesInRowBelow = 0;
        workedStitches = 0;
        workingStitch = 0;
        increases = 0;
        decreases = 0;
        currentCommandNo = 0;
        stitchNumber = 0;
        castingOff = false;
        finishedKnitting = false;
        turningStitchNo = 0;
        edgeLength = 0;
    }

    // KNITTING

    /**
     * Builds the knitting graph by processing KnitSpeak commands in sequence.
     *
     * Each command creates or modifies stitches and edges, while the graph
     * tracks row boundaries, stitch counts, increases and decreases.
     *
     * The current command index is retained so knitting can resume after
     * an interruption. This method is synchronized to prevent concurrent
     * execution with other synchronized graph operations.
     *
     * @throws Exception if command processing or sound playback fails
     */
    synchronized void doKnitting() throws Exception {
        KNIT_PLAY.updateStitchesMade("let the knitting begin");
        Thread.sleep(250);       // pause to allow GUI update to be read.
        for (int i = currentCommandNo; i < COMMANDS.length; i++) {
            currentCommandNo = i;
            KNIT_PLAY.updateKnittingCommands(COMMANDS[i]);
            if (COMMANDS[i].contains("co")) {
                edgeLength = 0;
                castOn(extractInt(COMMANDS[i]));
            }
            if (COMMANDS[i].matches("purl|knit")) {
                edgeLength = 2;
                for (int j = 0; j < stitchesInRowBelow; j++) {
                    knitOrPurl();
                }
            }
            if (COMMANDS[i].contains("tog") || COMMANDS[i].contains("ssk") || COMMANDS[i].contains("ssp")) {
                edgeLength = 1;
                if (COMMANDS[i].contains("ssk") || COMMANDS[i].contains("ssp")) {
                    int count = 0;
                    for (int k = 0; k < COMMANDS[i].length(); k++) {
                        if ((COMMANDS[i]).charAt(k) == 's') {
                            count++;
                        }
                    }
                    decrease(count - 1);
                } else if (COMMANDS[i].contains("psso")) {
                    decrease(2);
                } else {
                    decrease(extractInt(COMMANDS[i]) - 1);
                }
            }
            if (COMMANDS[i].contains("yo")) {
                edgeLength = 4;
                yarnOver();
            }
            if (COMMANDS[i].contains("kfb")) {
                edgeLength = 3;
                increase();
            }
            if (COMMANDS[i].matches("k\\d|p\\d|k\\d\\stbl|p\\d\\stbl|\\*.*k\\d|\\*.*p\\d")) {
                edgeLength = 2;
                int noOfStitches = extractInt(COMMANDS[i]);
                for (int n = 0; n < noOfStitches; n++) {
                    knitOrPurl();
                }
            }
            if (COMMANDS[i].contains("cast off")) {
                castOff();
            }
        }
    }

    /**
     * Creates the initial row of stitches and establishes its size.
     *
     * Once all stitches have been created, marks the row boundary
     * and prepares the graph for the next row.
     *
     * @param noOfStitches number of stitches to cast on
     * @throws Exception if stitch creation or row transition fails
     */
    private void castOn(int noOfStitches) throws Exception {
        stitchesInRowBelow = noOfStitches;      //establish the number of stitches in the first row
        while(noOfStitches > 0){
            makeStitch();
            noOfStitches--;
            Thread.sleep(125);  //pause between stitches
        }
        turn();
    }

    /**
     * Creates a stitch with an associated sound.
     *
     * Records the knitting command and stitch number, connects the
     * new stitch to the previous stitch with an edge, and adds it
     * to the graph.
     */
    private void makeStitch() {
        SoundPlayer soundPlayer = new SoundPlayer("knit", 2);
        Stitch stitch = new Stitch(soundPlayer);
        stitch.setCommandNo(currentCommandNo);
        stitch.setStitchNumber(stitchNumber);
        stitch.setCommand(COMMANDS[currentCommandNo]);
        if (!STITCHES.isEmpty()) {
            Stitch prev = getLastStitch();
            addEdge(prev, stitch, edgeLength);
        }
        STITCHES.add(stitch);
        SOUNDS.add(stitch.soundPlayer);
        KNIT_PLAY.updateStitchesMade("Made stitch no: " + stitchNumber);
        stitchNumber++;
    }

    /**
     * Adds a directed edge between two stitches.
     *
     * @param from the originating stitch
     * @param to the destination stitch
     * @param length the edge length used to determine playback sounds
     */
    private void addEdge(Stitch from, Stitch to, int length) {
        Edge edge = new Edge(from, to, length);
        EDGES.add(edge);
    }
    
    /**
     * Advances the working position through the current row.
     *
     * Moves to the next stitch in the row below, or turns the
     * knitting when all stitches in the row have been worked.
     *
     * @throws Exception if the operation is interrupted
     */
    private void workNextStitch() throws Exception {
        Thread.sleep(400);
        if (workedStitches < stitchesInRowBelow) {
            workingStitch--;
        }
        if (workedStitches == stitchesInRowBelow) {     //once all stitches in a row have been worked
            turn();
        }
    }

    /**
     * Marks the end of a row and prepares to work the next one.
     *
     * Records the turning stitch, resets the working position
     * and row counters, and adjusts the next row's stitch count
     * to account for increases and decreases.
     *
     * @throws InterruptedException if the row transition is interrupted
     */
    private void turn() throws InterruptedException {
        getLastStitch().setTurningStitch(true);
        getLastStitch().setTurningStitchNo(turningStitchNo);
        turningStitchNo++;
        workingStitch = STITCHES.indexOf(getLastStitch());
        workedStitches = 0;
        stitchesInRowBelow = stitchesInRowBelow + increases - decreases;
        increases = 0;
        decreases = 0;
        KNIT_PLAY.updateStitchesMade("next row!");
        Thread.sleep(450); //pause to allow the GUI update to be read.
    }

    /**
     * Creates a new stitch worked into the current stitch below.
     *
     * Records the relationship between the two stitches and
     * advances the working position.
     *
     * @throws Exception if stitch processing fails
     */
    private void knitOrPurl() throws Exception {
        makeStitch();
        STITCHES.get(workingStitch).stitchAbove.add(getLastStitch());   //works (adds) new stitch into stitch below
        workedStitches++;
        workNextStitch();
    }

    /**
     * Creates a new stitch without working into a stitch below.
     *
     * Records an increase and adjusts the working position so
     * the next operation uses the correct underlying stitch.
     *
     * @throws Exception if stitch processing fails
     */
    private void yarnOver() throws Exception {
        makeStitch();
        increases++;
        workingStitch++;    //ensures workingStitch stays the same for workNextStitch() since "yo" doesn't work a
                            // stitch below.
        workNextStitch();
    }
    
    /**
     * Works multiple existing stitches together into one new stitch.
     *
     * Links the new stitch to each stitch being worked and records
     * the reduction in stitch count for the current row.
     *
     * @param noOfStitches number of stitches removed from the row count
     * @throws Exception if stitch processing fails
     */
    private void decrease(int noOfStitches) throws Exception {
        decreases = decreases + noOfStitches;
        makeStitch();
        while(noOfStitches>=0) {
            STITCHES.get(workingStitch).stitchAbove.add(getLastStitch());
            noOfStitches--;
            workedStitches++;
            workNextStitch();
        }
    }

    /**
     * Creates two new stitches worked into the same stitch below.
     *
     * Records the increase in stitch count and advances the
     * working position once both stitches have been created.
     *
     * @throws Exception if stitch processing fails
     */
    private void increase() throws Exception {
       //make a stitch and work it into stitch below twice (working the same stitch each time)
        makeStitch();
        STITCHES.get(workingStitch).stitchAbove.add(getLastStitch());
        makeStitch();
        STITCHES.get(workingStitch).stitchAbove.add(getLastStitch());
        increases++;
        workedStitches++;
        workNextStitch();
    }
    
    /**
     * Completes the knitting pattern by playing cast-off sounds.
     *
     * Generates sounds based on the number of stitches in the
     * final row and marks the knitting process as finished.
     *
     * @throws InterruptedException if sound generation is interrupted
     */
    private void castOff() throws InterruptedException {
        castingOff = true;
        KNIT_PLAY.updateStitchesMade("casting off");
        int count = 0;
        while(count<=stitchesInRowBelow) {
            SoundPlayer soundPlayer = new SoundPlayer("castoff", 1);
            SOUNDS.add(soundPlayer);
            Thread thread = new Thread(soundPlayer);
            thread.start();
            Thread.sleep(100);
            count++;
        }
        finishedKnitting = true;
    }

    /**
     * Extracts the numeric portion of a knitting command.
     *
     * Returns 1 when the command contains no digits.
     *
     * @param str the command to parse
     * @return the extracted integer, or 1 if no digits are present
     * @throws NumberFormatException if the extracted value cannot be parsed
     */
    private static int extractInt(String str) {
        str = str.replaceAll("[^\\d]", "");
        str = str.trim();
        if(str.equals("")) {
            return 1;
        }
        return Integer.parseInt(str);
    }

    // UNPICKING

    /**
     * Removes stitches from the graph in reverse order, playing an unpicking
     * sound and releasing the associated sound clip for each removed stitch.
     *
     * If unpicking is interrupted, attempts to restore a consistent state
     * at the previous row boundary using {@link #goBack()}.
     *
     * @throws Exception if unpicking or its recovery operations fail
     */
    synchronized void doUnpicking() throws Exception {
        KNIT_PLAY.updateStitchesMade("OK! let's unpick some stitches...");
        try {
            Thread.sleep(550);  //pause to allow GUI update to be seen
            //if casting off, do nothing.
            if(castingOff) {
                return;
            }
            //iterate backwards through stitches from the current stitch, remove stitches
            //and update variables as we go, also invokes "unpicking" soundscape
            for (int stitch = STITCHES.indexOf(getLastStitch()); stitch>=0; stitch--) {
                SoundPlayer soundPlayer = new SoundPlayer("unpick", 1);
                SOUNDS.add(soundPlayer);
                Thread thread = new Thread(soundPlayer);
                thread.start();

                currentCommandNo = getLastStitch().commandNo;
                STITCHES.get(stitch).soundPlayer.getClip().close();
                SOUNDS.remove(STITCHES.get(stitch).soundPlayer);
                KNIT_PLAY.updateStitchesMade("Unpicking stitch no. " + STITCHES.get(stitch).stitchNumber);
                STITCHES.remove(stitch);
                if(STITCHES.isEmpty()) {
                    resetGraph();
                }
                else {
                    //set to work the next stitch
                    stitchNumber = getLastStitch().stitchNumber + 1;
                }
                Thread.sleep(250);
            }
        }
        catch (InterruptedException e) {
            //when unpicking is interrupted by a call of "doKnitting()", remove remaining stitches in the current row,
            //unless the program is at the cast-on row, in which case the KnitGraph is reset.
            if (!STITCHES.isEmpty()) {
                goBack();
            }
        }
    }
    
    /**
     * Rewinds the graph to a row boundary after unpicking is interrupted.
     *
     * Removes stitches until it encounters a turning stitch, then updates
     * the command index and stitch counters so knitting can resume.
     *
     * If the cast-on row is reached, resets the graph instead.
     *
     * @throws Exception if restoring the graph state fails
     */
    private void goBack() throws Exception {
        for (int s = STITCHES.indexOf(getLastStitch()); s >= 0; s--) {
            if (STITCHES.get(s).command.contains("co")) {
                KNIT_PLAY.updateStitchesMade("Let's start again!");
                resetGraph();
                return;
            }
            if (!STITCHES.get(s).turningStitch) {
                KNIT_PLAY.updateStitchesMade("Unpicking stitch no. " + STITCHES.get(s).stitchNumber);
                STITCHES.get(s).soundPlayer.getClip().close();
                SOUNDS.remove(STITCHES.get(s).soundPlayer);
                STITCHES.remove(STITCHES.get(s));
            
            }
            else {
                currentCommandNo = getLastStitch().commandNo + 1;
                stitchNumber = getLastStitch().stitchNumber + 1;
                turningStitchNo = getLastStitch().turningStitchNo;
                turn();
                // Determine the size of the row below using consecutive turning stitches.
                Stitch previousTurningStitch = null;
                for (Stitch found : STITCHES) {
                    if (found.turningStitch && found.turningStitchNo == getLastStitch().turningStitchNo - 1) {
                        previousTurningStitch = found;
                    }
                }
                stitchesInRowBelow = getLastStitch().stitchNumber - previousTurningStitch.stitchNumber;
                return;
            }
        }
    }

    // DROPPING STITCHES

    /**
     * Simulates dropping a stitch and the resulting laddering effect.
     *
     * Selects an eligible stitch from the current knitting row and
     * removes it along with the stitches that depend on it.
     * The graph's edges are then adjusted to reconnect the
     * remaining stitches.
     *
     * A stitch cannot be dropped once knitting has finished,
     * during casting off, or at certain row boundaries.
     *
     * @throws InterruptedException if sound playback is interrupted
     */
    synchronized void dropAStitch() throws InterruptedException {
        KNIT_PLAY.updateKnittingCommands("whoops - dropped a stitch!");

        if (castingOff || finishedKnitting) {
            KNIT_PLAY.updateStitchesMade("No - we'd already finished knitting!");
            return;
        }

        if (STITCHES.isEmpty()
                || workedStitches == stitchesInRowBelow
                || getLastStitch().commandNo == 1) {
            KNIT_PLAY.updateStitchesMade("No stitch has been dropped");
            return;
        }

        Stitch droppedStitch = findStitchToDrop();

        if (!canDropStitch(droppedStitch)) {
            KNIT_PLAY.updateStitchesMade("No stitch can be dropped yet");
            return;
        }

        doDropping(droppedStitch);
    }

    /**
     * Finds a stitch eligible to begin the dropping operation.
     *
     * Locates the first stitch without stitches worked into it,
     * excluding yarn-over stitches. Then searches forward to find
     * the last consecutive unworked stitch before encountering
     * one that has been worked.
     *
     * @return the stitch to drop, or null if no candidate is found
     */
    private Stitch findStitchToDrop() {
        Stitch firstEmptyStitch = null;
        Stitch droppedStitch = null;

        for (Stitch stitch : STITCHES) {
            if (stitch.stitchAbove.isEmpty()
                    && !COMMANDS[stitch.commandNo].contains("yo")) {
                firstEmptyStitch = stitch;
                break;
            }
        }

        if (firstEmptyStitch == null) {
            return null;
        }

        for (int drop = firstEmptyStitch.stitchNumber; drop < STITCHES.size(); drop++) {
            if (!STITCHES.get(drop).stitchAbove.isEmpty()) {
                break;
            }

            droppedStitch = STITCHES.get(drop);
        }

        return droppedStitch;
    }

    /**
     * Checks whether a stitch has the connections needed for dropping.
     *
     * A stitch must have both an incoming and an outgoing edge
     * so that the surrounding stitches can be reconnected.
     *
     * @param stitch the candidate stitch
     * @return true if the stitch has both connections; otherwise false
     */
    private boolean canDropStitch(Stitch stitch) {
        if (stitch == null) {
            return false;
        }

        boolean hasIncomingEdge = false;
        boolean hasOutgoingEdge = false;

        for (Edge edge : EDGES) {
            if (edge.to == stitch) {
                hasIncomingEdge = true;
            }

            if (edge.from == stitch) {
                hasOutgoingEdge = true;
            }

            if (hasIncomingEdge && hasOutgoingEdge) {
                return true;
            }
        }

        return false;
    }

    /**
     * Recursively identifies stitches affected by a dropped stitch.
     *
     * Finds stitches whose stitchAbove collection contains the
     * dropped stitch, adds them to DROPPED_STITCHES, and continues
     * following the dependency chain.
     *
     * @param dropped the stitch whose dependent stitches are being found
     */
    private void addDroppedStitch(Stitch dropped) {
        boolean droppedStitchFound = false;
        Stitch nextDrop = null;
        for(Stitch s : STITCHES) {
            if(s.stitchAbove.contains(dropped)) {
                DROPPED_STITCHES.add(s);
                nextDrop = s;
                droppedStitchFound = true;
            }
        }
        if (droppedStitchFound) {
            addDroppedStitch(nextDrop);
        }
    }
    
    /**
     * Removes the affected stitches and reconnects the graph.
     *
     * Plays a sound for each dropped stitch, replaces the affected
     * edges with longer edges connecting the surviving stitches,
     * and removes the dropped stitches from the graph.
     *
     * The new edge lengths represent the gaps created by laddering
     * and are used during subsequent sound playback.
     *
     * @param droppedStitch the initial stitch to drop
     * @throws InterruptedException if sound playback is interrupted
     */
    private void doDropping(Stitch droppedStitch) throws InterruptedException {
        DROPPED_STITCHES.add(droppedStitch);
        addDroppedStitch(droppedStitch);
        for(Stitch s : DROPPED_STITCHES) {
            SoundPlayer soundPlayer = new SoundPlayer("drop", 4);
            SOUNDS.add(soundPlayer);
            Thread thread = new Thread(soundPlayer);
            thread.start();
            Thread.sleep(100);
            thread.join();
            KNIT_PLAY.updateStitchesMade("Dropped Stitch: " + s.stitchNumber);
            Thread.sleep(450);
        }
        Stitch tempFrom = null;
        Stitch tempTo = null;
        Edge newEdge;
        Edge fromEdge = null;
        Edge toEdge = null;
        int edgeLocation = 0;

        for (Stitch s : DROPPED_STITCHES) {
            for (Edge e : EDGES) {
                if(e.from.equals(s)) {
                    tempTo = e.to;
                    toEdge = e;
                }
                if(e.to.equals(s)) {
                    tempFrom = e.from;
                    fromEdge = e;
                    edgeLocation = EDGES.indexOf(e);
                }
            }
            // Use a length greater than ordinary knitting edges to represent
            // the gap left by dropped stitches and distinguish it during playback.
            edgeLength = 5 * (tempTo.stitchNumber-tempFrom.stitchNumber-1);
            newEdge = new Edge(tempFrom, tempTo, edgeLength);
            EDGES.add(edgeLocation, newEdge);
            EDGES.remove(toEdge);
            EDGES.remove(fromEdge);
        }
        for (Stitch s : DROPPED_STITCHES) {
            STITCHES.remove(s);
        }
    }

    // PLAYBACK

    /**
     * Plays the completed knitting graph as a sequence of sounds.
     *
     * Traverses the graph's edges, using their lengths to determine
     * the sound associated with each destination stitch.
     *
     * The initial cast-on stitch is played separately because it
     * is not the destination of an edge. Longer edges created by
     * dropped stitches also trigger dropping sounds.
     *
     * Resets the graph after playback has finished.
     *
     * @throws InterruptedException if playback is interrupted
     */
    synchronized void playGraph() throws InterruptedException{
        KNIT_PLAY.updateKnittingCommands("");
        KNIT_PLAY.updateStitchesMade("Playing the knit graph");
        String command = "clapping";
        int loops = 1;
        EDGES.get(0).from.soundPlayer = new SoundPlayer(command, loops);
        SOUNDS.add(EDGES.get(0).from.soundPlayer);
        EDGES.get(0).from.thread = new Thread(EDGES.get(0).from.soundPlayer);
        EDGES.get(0).from.thread.start();
        Thread.sleep(250);
        EDGES.get(0).from.thread = null;
        // Edge lengths encode knitting operations as sound choices:
        // 0 = cast on, 1 = decrease, 2 = knit/purl,
        // 3 = increase, 4 = yarn over, >4 = dropped stitches.
        for (Edge e : EDGES) {
            if (e.length == 0) {
                command = "clapping";
                loops = 1;
            }
            if (e.length == 1 || e.length == 4) {
                command = "calling";
                loops = 1;
            }
            if (e.length == 2) {
                command = "highlands";
                loops = 1;
            }
            if (e.length == 3) {
                command = "response";
                loops = 2;
            }
            else if (e.length > 4){     // these will be a result of dropped stitches
                for (int i = 0; i<e.length; i++) {
                    SoundPlayer droppingSound = new SoundPlayer("drop", 1);
                    SOUNDS.add(droppingSound);
                    Thread droppedThread = new Thread(droppingSound);
                    droppedThread.start();
                }
            }
            e.to.soundPlayer = new SoundPlayer(command, loops);
            SOUNDS.add(e.to.soundPlayer);
            e.to.thread = new Thread(e.to.soundPlayer);
            e.to.thread.start();
            Thread.sleep(350);
            e.to.thread = null;
        }
        KNIT_PLAY.updateKnittingCommands("Program resetting...");
        KNIT_PLAY.updateStitchesMade("");
        resetGraph();
    }

    // GENERAL HELPERS

    /**
     * Returns the most recently added stitch.
     *
     * @return the last stitch in the graph
     * @throws IndexOutOfBoundsException if the graph contains no stitches
     */
    private Stitch getLastStitch() {
        Stitch last  = STITCHES.get(STITCHES.size()-1);
        return last;
    }

    /**
     * Restores the graph to its initial state.
     *
     * Closes tracked sound clips, removes stitches and edges,
     * and resets the counters and flags used during knitting.
     *
     * The original knitting commands are retained, allowing
     * the pattern to be processed again from the beginning.
     */
    void resetGraph() {
        for(SoundPlayer s : SOUNDS) {
            s.getClip().close();
        }
        STITCHES.clear();
        EDGES.clear();
        SOUNDS.clear();
        DROPPED_STITCHES.clear();
        stitchesInRowBelow = 0;
        workedStitches = 0;
        workingStitch = 0;
        increases = 0;
        decreases = 0;
        stitchNumber = 0;
        currentCommandNo = 0;
        castingOff = false;
        finishedKnitting = false;
        turningStitchNo = 0;
        edgeLength = 0;
    }

    ///////////////////////////STITCH CLASS///////////////////////
    /**
     * Represents an individual stitch in the knitting graph.
     *
     * Tracks the command that created the stitch, its position
     * in the pattern, and any stitches subsequently worked into it.
     *
     * Also stores its associated sound player and information
     * about whether it marks a row boundary.
     */
    private static class Stitch {
        protected SoundPlayer soundPlayer;
        protected Thread thread;
        protected ArrayList<Stitch> stitchAbove;    // Stitches created by working into this stitch in subsequent rows.
        protected int commandNo;                    // stores index of command that created stitch
        protected String command;
        protected int stitchNumber;                 // stores stitch number
        protected boolean turningStitch;            // is this a turning stitch?
        protected int turningStitchNo;              // if so, which one?
        public Stitch(SoundPlayer soundPlayer) {
            stitchAbove = new ArrayList<Stitch>();
            this.soundPlayer = soundPlayer;
            this.thread = new Thread(soundPlayer);
            thread.start();
            thread = null;
            turningStitch = false;
            turningStitchNo = -1;
        }
        void setCommandNo(int i) {
            commandNo = i;
        }
        void setCommand(String command) {
            this.command = command;
        }
        void setStitchNumber(int j) {
            stitchNumber = j;
        }
        void setTurningStitch (boolean b) {
            turningStitch = b;
        }
        void setTurningStitchNo (int k) {
            turningStitchNo = k;
        }
    }
    ////////////////////////////////////////////////////////////

    ///////////////////////////EDGE CLASS///////////////////////
    /**
     * Represents a connection between two stitches.
     *
     * The edge length encodes information about the knitting
     * operation and determines the sound used during playback.
     * Longer edges can also represent gaps caused by dropped stitches.
     */
    private static class Edge {
        protected Stitch from;
        protected Stitch to;
        protected int length;
        public Edge(Stitch from, Stitch to, int length) {
            this.from = from;
            this.to = to;
            this.length = length;
        }
    }
    ////////////////////////////////////////////////////////////
    
    
    // TEST HELPERS

    /**
     * Returns the number of stitches currently in the graph.
     *
     * @return the current stitch count
     */
    int getNumberOfStitches() {
    	return STITCHES.size();
    }
    /**
     * Returns the stitch numbers recorded during dropping operations.
     *
     * @return an array containing the recorded dropped stitch numbers
     */
    int[] getDroppedStitches() {
    	int[] stitches = new int[DROPPED_STITCHES.size()];
    	for(int i=0;i<stitches.length;i++) {
    		stitches[i] = DROPPED_STITCHES.get(i).stitchNumber;
    	}
    	return stitches;
    }

    /**
     * Builds part of a knitting graph for testing purposes.
     *
     * Processes a fraction of the supplied commands, allowing
     * tests to inspect the graph before the pattern is complete.
     *
     * @param testCommands the knitting commands to process
     * @param fraction divisor used to determine how many commands to process
     * @throws Exception if command processing fails
     */
    synchronized void knitPartOfGraph(String[] testCommands, int fraction) throws Exception {
        KNIT_PLAY.updateStitchesMade("let the knitting begin");
        Thread.sleep(250);       //pause to allow GUI update to be read.
         for (int i = 0; i < testCommands.length/fraction; i++) {
             currentCommandNo = i;
             KNIT_PLAY.updateKnittingCommands(testCommands[i]);
             if (testCommands[i].contains("co")) {
                 edgeLength = 0;
                 castOn(extractInt(testCommands[i]));
             }
             if (testCommands[i].matches("purl|knit")) {
                 edgeLength = 2;
                 for (int j = 0; j < stitchesInRowBelow; j++) {
                     knitOrPurl();
                 }
             }
             if (testCommands[i].contains("tog") || testCommands[i].contains("ssk") || testCommands[i].contains("ssp")) {
                 edgeLength = 1;
                 if (testCommands[i].contains("ssk") || testCommands[i].contains("ssp")) {
                     int count = 0;
                     for (int k = 0; k < testCommands[i].length(); k++) {
                         if ((testCommands[i]).charAt(k) == 's') {
                             count++;
                         }
                     }
                     decrease(count - 1);
                 } else if (testCommands[i].contains("psso")) {
                     decrease(2);
                 } else {
                     decrease(extractInt(testCommands[i]) - 1);
                 }
             }
             if (testCommands[i].contains("yo")) {
                 edgeLength = 4;
                 yarnOver();
             }
             if (testCommands[i].contains("kfb")) {
                 edgeLength = 3;
                 increase();
             }
             if (testCommands[i].matches("k\\d|p\\d|k\\d\\stbl|p\\d\\stbl|\\*.*k\\d|\\*.*p\\d")) {
                 edgeLength = 2;
                 int noOfStitches = extractInt(testCommands[i]);
                 for (int n = 0; n < noOfStitches; n++) {
                     knitOrPurl();
                 }
             }
             if (testCommands[i].contains("cast off")) {
                 castOff();
             }
         }
     }
}
