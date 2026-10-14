import java.util.ArrayList;

public class KnitGraph {
    private final ArrayList<Stitch> STITCHES;   //stores all stitches
    private final ArrayList<Edge> EDGES;        //stores all edges
    private final ArrayList<SoundPlayer> SOUNDS;//stores all instances of SoundPlayer allows us to close clips remotely
    private final ArrayList<Stitch> DROPPED_STITCHES;
    private final String[] COMMANDS;
    private final KnitPlay KNIT_PLAY;           //this is required so that we can update the GUI 
    private int stitchesInRowBelow;             //keeps track of how many stitches to be worked in current row
    private int workedStitches;                 //keeps track of how many stitches have been worked per row
    private int workingStitch;                  //keeps a track of which stitch is being worked
    private int increases;                      //counts number of increases made in row
    private int decreases;                      //counts number of decreases made in row
    private int currentCommandNo;               //keeps track of command as we knit/unpick
    private int stitchNumber;                   //allocates numbers to stitches
    private int turningStitchNo;                //tracks turning stitches
    private int edgeLength;
    private boolean castingOff;
    private boolean finishedKnitting;
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

    ////////////////////////////////////KNITTING////////////////////////////////

    /**
     * Iterates over the commands array in order to build a KnitGraph
     * <p>
     * doKnitting() is synchronized with other methods doUnpicking() and dropAStitch(). If it is interrupted by either
     * of these methods, it stops processing. Each command tells the program what to do, and therefore an appropriate
     * helper method is accessed, and Edge length is set. The variable currentCommandNo is initially set to 0, however
     * this changes once KnitGraph processing begins. When doKnitting() is resumed it knows where in the list of
     * commands to start from by accessing this variable. As this process runs, the program keeps track of how many
     * stitches are in the last row and how many stitches have been increased/decreased. In this respect, the program
     * knows when a row of stitches has finished being worked, and the KnitGraph is able to maintain a consistent state.
     * Once all commands have been processed, the KnitGraph is complete and ready to be "played".
     * <p>
     * This method can be (manually) evolved as new KnitSpeak files are added. New knit commands can be included,
     * either by adding to the already existing ones, or creating a new if statement. For example, if a cabling knit
     * pattern were to be included, the program would need to know how to treat a command such as "C3B" which means to
     * cable across three stitches by pushing the first three working stitches to the back and working the following
     * three stitches before working the first three. This would be a straightforward operation.
     * @throws Exception
     */
   synchronized void doKnitting() throws Exception {
       KNIT_PLAY.stitchesMade.setText("let the knitting begin");
       Thread.sleep(250);       //pause to allow GUI update to be read.
        for (int i = currentCommandNo; i < COMMANDS.length; i++) {
            currentCommandNo = i;
            KNIT_PLAY.knittingCommands.setText(COMMANDS[i]);
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
    /////////////////////////KNITTING HELPER METHODS/////////////////////////////
    private void castOn(int noOfStitches) throws Exception {
        stitchesInRowBelow = noOfStitches;      //establish the number of stitches in the first row
        while(noOfStitches > 0){
            makeStitch();
            noOfStitches--;
            Thread.sleep(125);  //pause between stitches
        }
        turn();
    }
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
        KNIT_PLAY.stitchesMade.setText("Made stitch no: " + stitchNumber);
        stitchNumber++;
    }
    private void addEdge(Stitch from, Stitch to, int length) {
        Edge edge = new Edge(from, to, length);
        EDGES.add(edge);
    }
    //tracks where the working stitch is in relation to stitches in the row below
    private void workNextStitch() throws Exception {
        Thread.sleep(400);
        if (workedStitches < stitchesInRowBelow) {
            workingStitch--;
        }
        if (workedStitches == stitchesInRowBelow) {     //once all stitches in a row have been worked
            turn();
        }
    }
    //turn knitting to work the next row. Resets relevant variables and updates turning stitch.
    private void turn() throws InterruptedException {
        getLastStitch().setTurningStitch(true);
        getLastStitch().setTurningStitchNo(turningStitchNo);
        turningStitchNo++;
        workingStitch = STITCHES.indexOf(getLastStitch());
        workedStitches = 0;
        stitchesInRowBelow = stitchesInRowBelow + increases - decreases;
        increases = 0;
        decreases = 0;
        KNIT_PLAY.stitchesMade.setText("next row!");
        Thread.sleep(450); //pause to allow the GUI update to be read.
    }
    private void knitOrPurl() throws Exception {
        makeStitch();
        STITCHES.get(workingStitch).stitchAbove.add(getLastStitch());   //works (adds) new stitch into stitch below
        workedStitches++;
        workNextStitch();
    }
    //creates a new stitch without working it into a stitch below
    private void yarnOver() throws Exception {
        makeStitch();
        increases++;
        workingStitch++;    //ensures workingStitch stays the same for workNextStitch() since "yo" doesn't work a
                            // stitch below.
        workNextStitch();
    }
    //for use with ssk, k2t, p2t etc. may add complexity later to account for the different variations
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
    //ends the knitting process and creates new "castoff" sounds for each stitch in the row below.
    private void castOff() throws InterruptedException {
        castingOff = true;
        KNIT_PLAY.stitchesMade.setText("casting off");
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
    private static int extractInt(String str) {
        str = str.replaceAll("[^\\d]", "");
        str = str.trim();
        if(str.equals("")) {
            return 1;
        }
        return Integer.parseInt(str);
    }

    ////////////////////////////////////UNPICKING////////////////////////////////

    /**
     * Pauses knitting to unpick stitches.
     * <p>
     * Iterates backwards through all the stitches. For each stitch, a new "unpick" sound is generated. The existing
     * sound clip for each stitch is closed before removing both the sound and the stitch itself. If there are no
     * stitches to unpick i.e. they've all been removed, then the KnitGraph is reset.
     * <p>
     * If this process is interraupted by doKnitting(), then the helper method goBack() is called. This removes any
     * remaining stitches from the current row, and sets all the variables to reflect the current state of the
     * KnitGraph. In particular, the KnitGraph needs to know where to resume knitting from.
     * @throws Exception
     */
    synchronized void doUnpicking() throws Exception {
        KNIT_PLAY.stitchesMade.setText("OK! let's unpick some stitches...");
        Thread.sleep(550);  //pause to allow GUI update to be seen
        try {
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
                KNIT_PLAY.stitchesMade.setText("Unpicking stitch no. " + STITCHES.get(stitch).stitchNumber);
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
            goBack();
        }
    }
    //////////////////////////////UNPICKING HELPER METHODS///////////////////////////////
    private void goBack() throws Exception {
        //loop to remove any remaining stitches in that row
        for (int s = STITCHES.indexOf(getLastStitch()); s >= 0; s--) {
            //if we've cast off as far as the cast on row, or all stitches have been cast off, just start again.
            if (STITCHES.get(s).command.contains("co")) {
                KNIT_PLAY.stitchesMade.setText("Let's start again!");
                resetGraph();
                return;
            }
            //if it's not a turning stitch i.e. not the end of the row, keep removing it
            if (!STITCHES.get(s).turningStitch) {
                KNIT_PLAY.stitchesMade.setText("Unpicking stitch no. " + STITCHES.get(s).stitchNumber);
                STITCHES.get(s).soundPlayer.getClip().close();
                SOUNDS.remove(STITCHES.get(s).soundPlayer);
                STITCHES.remove(STITCHES.get(s));
            
            }
            //otherwise set variables and turn
            else {
                currentCommandNo = getLastStitch().commandNo + 1;
                stitchNumber = getLastStitch().stitchNumber + 1;
                turningStitchNo = getLastStitch().turningStitchNo;
                turn();
                //work out how many stitches below:
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

    //////////////////////////////////////////DROPPING STITCHES///////////////////////////////////////

    /**
     * Drops a stitch.
     * <p>
     * When a stitch is dropped, stitches which contain the dropped stitch also become dropped, and so on.
     * This results in a "ladder" in the KnitGraph where all related stitches are removed and replaced with a single
     * extra-long edge which joins the stitches on either side of the dropped stitch.
     * <p>
     * To find the first dropped stitch, the program iterates over all stitches: ignores all stitches which have
     * stitches contained within them (already worked stitches) until a stitch is found which does not contain another
     * stitch i.e. it hasn't been worked. The program continues to iterate until it finds a stitch which has been
     * worked. The first dropped stitch is the one before this stitch. This stitch is added to an array of stitches to
     * be dropped. The program then iterates through all the stitches again to find one/s that contain the last dropped
     * stitch, and these are also added to the array. This continues until there are no more stitches which contain the
     * last dropped stitch. The program then takes all edges related to the dropped stitches, and replaces them with a
     * new edge which joins the adjacent stitches back together. Finally, the program iterates through the array of
     * dropped stitches and removes them from the KnitGraph. The dropped stitch array is then cleared, and the resulting
     * "laddered" KnitGraph can be "played".
     * @throws InterruptedException
     */
    synchronized void dropAStitch() throws InterruptedException {
        KNIT_PLAY.knittingCommands.setText("whoops - dropped a stitch!");

        if (castingOff || finishedKnitting) {
            KNIT_PLAY.stitchesMade.setText("No - we'd already finished knitting!");
            return;
        }

        if (STITCHES.isEmpty()
                || workedStitches == stitchesInRowBelow
                || getLastStitch().commandNo == 1) {
            KNIT_PLAY.stitchesMade.setText("No stitch has been dropped");
            return;
        }

        Stitch droppedStitch = findStitchToDrop();

        if (!canDropStitch(droppedStitch)) {
            KNIT_PLAY.stitchesMade.setText("No stitch can be dropped yet");
            return;
        }

        doDropping(droppedStitch);
    }
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
    //method keeps adding dropped stitches to the DROPPED_STITCHES list until there are no more to be added.
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
    //method adjusts the edges to replace edges of stitches to be dropped, then removes the dropped stitches
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
            KNIT_PLAY.stitchesMade.setText("Dropped Stitch: " + s.stitchNumber);
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
            //sets new edge length depending on how many consecutive stitches have been dropped
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

    ////////////////////////////////////////PLAY THE COMPLETED GRAPH///////////////////////////////////////

    /**
     * Iterates over the KnitGraph edges, updates their "to" stitches sounds depending on the length of the edge. The
     * length of the edge is determined in the knitting process and depends on what command resulted in the stitch.
     * The first stitch is never a "to" stitch contained within an edge, and it is always going to be length 0 as this
     * will always be a cast on stitch, so this stitch is updated and played back first, before iterating over the edges
     * and their "to" stitches. Once all stitches have been "played", the KnitGraph is reset, and the whole process can
     * start from scratch.
     * @throws InterruptedException
     */
    synchronized void playGraph() throws InterruptedException{
        KNIT_PLAY.knittingCommands.setText("");
        KNIT_PLAY.stitchesMade.setText("Playing the knit graph");
        String command = "clapping";
        int loops = 1;
        EDGES.get(0).from.soundPlayer = new SoundPlayer(command, loops);
        SOUNDS.add(EDGES.get(0).from.soundPlayer);
        EDGES.get(0).from.thread = new Thread(EDGES.get(0).from.soundPlayer);
        EDGES.get(0).from.thread.start();
        Thread.sleep(250);
        EDGES.get(0).from.thread = null;
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
            else if (e.length > 4){     //these will be a result of dropped stitches
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
        KNIT_PLAY.knittingCommands.setText("Program resetting, please wait.");
        KNIT_PLAY.stitchesMade.setText("");
        resetGraph();
    }

    ////////////////////////////GENERAL HELPER METHODS/////////////////////////////
    private Stitch getLastStitch() {
        Stitch last  = STITCHES.get(STITCHES.size()-1);
        return last;
    }
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
    private static class Stitch {
        protected SoundPlayer soundPlayer;
        protected Thread thread;
        protected ArrayList<Stitch> stitchAbove;    //stores stitches that have been worked into this stitch
        protected int commandNo;                    //stores index of command that created stitch
        protected String command;
        protected int stitchNumber;                 //stores stitch number
        protected boolean turningStitch;            //is this a turning stitch?
        protected int turningStitchNo;              //if so, which one?
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
    
    
    ///////For JUnit Tests...///////////////////
    int getNumberOfStitches() {
    	return STITCHES.size();
    }
    int[] getDroppedStitches() {
    	int[] stitches = new int[DROPPED_STITCHES.size()];
    	for(int i=0;i<stitches.length;i++) {
    		stitches[i] = DROPPED_STITCHES.get(i).stitchNumber;
    	}
    	return stitches;
    }

    //this is a copy of the doKnitting method that has been modified to part-build the KnitGraph
    //so that it can be used for JUnit testing.
    synchronized void knitPartOfGraph(String[] testCommands, int fraction) throws Exception {
        KNIT_PLAY.stitchesMade.setText("let the knitting begin");
        Thread.sleep(250);       //pause to allow GUI update to be read.
         for (int i = 0; i < testCommands.length/fraction; i++) {
             currentCommandNo = i;
             KNIT_PLAY.knittingCommands.setText(testCommands[i]);
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
