import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Main application window and controller for KnitPlay.
 *
 * Displays the current knitting pattern and provides controls
 * for knitting, unpicking, dropping stitches and playing the
 * resulting graph as sound.
 *
 * Uses SwingWorkers to run graph operations in the background,
 * keeping the interface responsive during processing.
 */
public class KnitPlay extends JFrame {
    private static final long serialVersionUID = 8706410108159112253L;
    private final JPanel BACKGROUND;
    private SwingWorker<KnitGraph, Void> knitter;
    private SwingWorker<Void, Void> unPicker;
    private SwingWorker<Void, Void> stitchDropper;
    private SwingWorker<KnitGraph, Void> player;
    private KnitGraph knitGraph;
    private String file;
    private String fileName;
    private boolean knitting; // Tracks whether the application is in knitting mode rather than unpicking mode.
    private JLabel knittingCommands;
    private JLabel stitchesMade;

    /**
     * Initialises the application window and knitting controls.
     *
     * Selects a knitting pattern, creates its graph, configures
     * the background image and status labels, and registers
     * the button actions.
     */
    public KnitPlay() {
        knitting = true;
        setFileInfo();
        // Set up the main application window
        this.setTitle("KnitPlay");
        this.setSize(1000, 750);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
        // Scale the image to cover the panel while preserving its aspect ratio.
        // Centre the image, cropping any excess beyond the panel boundaries.
        Image backgroundImage = new ImageIcon("images/kidknits.jpg").getImage();
        this.BACKGROUND = new JPanel(new GridLayout(3, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imageWidth = backgroundImage.getWidth(this);
                int imageHeight = backgroundImage.getHeight(this);
                if (imageWidth <= 0 || imageHeight <= 0) {
                    return;
                }
                double scale = Math.max(
                    (double) panelWidth / imageWidth,
                    (double) panelHeight / imageHeight
                );
                int width = (int) Math.ceil(imageWidth * scale);
                int height = (int) Math.ceil(imageHeight * scale);
                int x = (panelWidth - width) / 2;
                int y = (panelHeight - height) / 2;
                g.drawImage(backgroundImage, x, y, width, height, this);
            }
        };
        this.add(BACKGROUND, BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new GridLayout(1,2));
        southPanel.setOpaque(false);
        this.add(southPanel, BorderLayout.SOUTH);
        knittingCommands = new JLabel("Today we are knitting: " );
        knittingCommands.setFont(new Font("Helvetica", Font.BOLD, 36));
        knittingCommands.setForeground(Color.DARK_GRAY);
        knittingCommands.setOpaque(false);
        southPanel.add(knittingCommands);
        stitchesMade = new JLabel(fileName);
        stitchesMade.setFont(new Font("Helvetica", Font.BOLD, 36));
        stitchesMade.setForeground(Color.DARK_GRAY);
        stitchesMade.setOpaque(false);
        southPanel.add(stitchesMade);
        String knit = "KNIT";
        String unpick = "UNPICK";
        String dropAStitch = "DROP A STITCH";
        // Create the controls for the knitting operations.
        JButton knitIt = new JButton(knit);
        JButton unpickIt = new JButton(unpick);
        JButton dropIt = new JButton(dropAStitch);
        knitGraph = new KnitGraph(new KnitSpeakIn(file).getKnitFile(), this);
        addButtons(knitIt, unpickIt, dropIt);
        // Close the application when the window closes and display the interface.
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }
    
    /**
     * Configures the knitting controls and their action listeners.
     *
     * Each control starts a SwingWorker to perform its graph operation
     * without blocking the Swing event dispatch thread.
     *
     * KNIT starts or resumes knitting, UNPICK interrupts knitting
     * and reverses stitches, and DROP A STITCH simulates laddering.
     * After knitting completes or a drop operation finishes, the
     * UNPICK control becomes PLAY, allowing the graph to be heard.
     *
     * @param knit button used to start or resume knitting
     * @param unpick button used to unpick stitches or play the graph
     * @param drop button used to drop a stitch
     */
    private void addButtons(JButton knit, JButton unpick, JButton drop) {
        makeButton(knit);
        makeButton(unpick);
        makeButton(drop);
        Font knitFont = new Font("Helvetica", Font.BOLD, 94);
        Font unpickFont = new Font("Helvetica", Font.BOLD, 72);
        Font dropFont = new Font("Helvetica", Font.BOLD, 32);
        knit.setFont(knitFont);
        knit.setEnabled(true);
        knit.setFocusPainted(false);
        unpick.setFont(unpickFont);
        unpick.setEnabled(false);
        unpick.setFocusPainted(false);
        drop.setFont(dropFont);
        drop.setEnabled(false);
        drop.setFocusPainted(false);

        // Start or resume knitting, cancelling an active unpicking worker.
        knit.addActionListener(e -> {
            knit.setEnabled(false);
            unpick.setEnabled(true);
            drop.setEnabled(true);
            if(!knitting) {
                unPicker.cancel(true);
                knitting = true;
            }
            knitter = new SwingWorker<KnitGraph, Void>() {
                @Override
                protected KnitGraph doInBackground() throws Exception {
                    knitGraph.doKnitting();
                    return knitGraph;
                }

                // SwingWorker.done() runs on the EDT, so button updates are safe here.
                @Override
                protected void done() {
                    if (isCancelled()) {
                        return;
                    }

                    try {
                        get();

                        knit.setText(" ");
                        unpick.setText("PLAY");
                        drop.setText(" ");

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            knitter.execute();
        });

        // Play the completed graph, or interrupt knitting to begin unpicking.
        unpick.addActionListener(e -> {
            if (unpick.getText().equals("PLAY")) {
                knit.setEnabled(false);
                unpick.setEnabled(false);
                drop.setEnabled(false);
                player = new SwingWorker<KnitGraph, Void>() {
                    private String nextFileName;
                    @Override
                    protected KnitGraph doInBackground() throws Exception {
                        knitGraph.playGraph();
                        setFileInfo();
                        nextFileName = fileName;
                        return new KnitGraph(
                            new KnitSpeakIn(file).getKnitFile(),
                            KnitPlay.this
                        );
                    }

                    // Install the next graph and restore the initial UI state on the EDT.
                    @Override
                    protected void done() {
                        try {
                            KnitGraph nextGraph = get();
                            knitGraph = nextGraph;
                            knit.setText("KNIT");
                            unpick.setText("UNPICK");
                            drop.setText("DROP A STITCH");
                            knittingCommands.setText("Today we are knitting: ");
                            stitchesMade.setText(nextFileName);
                            knitting = true;
                            knit.setEnabled(true);
                            unpick.setEnabled(false);
                            drop.setEnabled(false);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };
                player.execute();
            } else {
                knit.setEnabled(true);
                unpick.setEnabled(false);
                drop.setEnabled(false);
                knitting = false;
                unPicker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        knitGraph.doUnpicking();
                        knitGraph.resetGraph();
                        return null;
                    }
                };
                knitter.cancel(true);
                unPicker.execute();
            }
        });

        // Request cancellation of knitting, then start the stitch-dropping worker.
        drop.addActionListener(e -> {
            drop.setEnabled(false);
            knit.setEnabled(false);
            unpick.setEnabled(false);
            stitchDropper = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    knitGraph.dropAStitch();
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get(); // Check that dropping the stitch completed successfully
                        knit.setText(" ");
                        unpick.setText("PLAY");
                        drop.setText(" ");
                        unpick.setEnabled(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            knitter.cancel(true);
            stitchDropper.execute();
        });
        BACKGROUND.add(knit);
        BACKGROUND.add(unpick);
        BACKGROUND.add(drop);
    }

    /**
     * Applies the shared visual styling to a control button.
     *
     * Removes the default button background and border so that
     * the button text appears over the application's background image.
     *
     * @param button the button to style
     */
    private void makeButton(JButton button) {
        button.setForeground(Color.MAGENTA);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
    }
    
    /**
     * Randomly selects a knitting pattern from the available lace files.
     *
     * Stores the selected file path for loading the pattern and
     * derives a display name for the application interface.
     */
    private void setFileInfo() {
        String[] knitFile = new KnitFileSelector("knitSpeak/LaceFiles").getKnitFileSelection();
        Random random = new Random();
        this.file = knitFile[random.nextInt(knitFile.length)];
        this.fileName = file.replace("knitSpeak/", " ").replace(".txt", " ");
    }

    // Thread-safe UI updates used by background graph operations.

    /**
     * Updates the label displaying the current knitting command.
     *
     * Applies the update immediately when called on the Swing event
     * dispatch thread, or schedules it on that thread otherwise.
     *
     * @param text the command or status message to display
     */
    public void updateKnittingCommands(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            knittingCommands.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> knittingCommands.setText(text));
        }
    }

    /**
     * Updates the label displaying knitting progress.
     *
     * Ensures the label is updated on the Swing event dispatch
     * thread, including when called from a background worker.
     *
     * @param text the progress or status message to display
     */
    public void updateStitchesMade(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            stitchesMade.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> stitchesMade.setText(text));
        }
    }

    /**
     * Launches the KnitPlay application on the Swing event dispatch thread.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(KnitPlay::new);
    }
}
