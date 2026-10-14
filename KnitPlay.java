import javax.swing.*;
import java.awt.*;
import java.util.Random;
public class KnitPlay extends JFrame {
    private static final long serialVersionUID = 8706410108159112253L;
    private final JPanel BACKGROUND;
    private SwingWorker<KnitGraph, Void> knitter;
    private SwingWorker<Void, Void> unPicker;
    private SwingWorker<Void, Void> stitchDropper;
    private SwingWorker<KnitGraph, Void> player;
    private KnitGraph knitGraph;
    private int randomInt;
    private String file;
    private String fileName;
    private boolean knitting;
    private JLabel knittingCommands;
    private JLabel stitchesMade;
    public KnitPlay() {
        knitting = true;
        setFileInfo();
        // Set up the main application window
        this.setTitle("KnitPlay");
        this.setSize(1000, 750);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
        //main image scaled to size
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
        //instantiate JButtons
        JButton knitIt = new JButton(knit);
        JButton unpickIt = new JButton(unpick);
        JButton dropIt = new JButton(dropAStitch);
        knitGraph = new KnitGraph(new KnitSpeakIn(file).getKnitFile(), this);
        addButtons(knitIt, unpickIt, dropIt);
        //basic stuff to stop program when frame is closed, make visible etc
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }
    //addButton method: takes a JButton, adds it to relevant JLabel and associates it with relevant KnitSpeak file,
    //adds action listeners to each button - each containing it's own SwingWorker for triggering KnitGraph processes.
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
    private void makeButton(JButton button) {
        button.setForeground(Color.MAGENTA);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
    }
    //method that selects a file to play
    private void setFileInfo() {
        String[] knitFile = new KnitFileSelector("knitSpeak/LaceFiles").getKnitFileSelection();
        Random random = new Random();
        this.randomInt = random.nextInt(knitFile.length);
        this.file = knitFile[randomInt];
        this.fileName = file.replace("knitSpeak/", " ").replace(".txt", " ");
    }
    public void updateKnittingCommands(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            knittingCommands.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> knittingCommands.setText(text));
        }
    }
    public void updateStitchesMade(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            stitchesMade.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> stitchesMade.setText(text));
        }
    }
    public static void main(String[] args) {
        new KnitPlay();
    }
}
