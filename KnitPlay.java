import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class KnitPlay extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 8706410108159112253L;
	private final JLabel background;
    private SwingWorker<KnitGraph, Void> knitter;
    private SwingWorker<Void, Void> unPicker;
    private SwingWorker<Void, Void> stitchDropper;
    private SwingWorker<Void, Void> player;
    private KnitGraph knitGraph;
    int randomInt;
    String file;
    String fileName;
    private boolean knitting;
    public JTextField knittingCommands;
    public JTextField stitchesMade;
    public KnitPlay() {
        knitting = true;

        setFileInfo();
        //select which knitFile we are using at random
//        knitFile = new KnitFileSelector("knitSpeak/LaceFiles").getKnitFileSelection();
//        Random random = new Random();
//        randomInt = random.nextInt(3);
//        file = knitFile[randomInt];
//        fileName = file.replace("knitSpeak/", " ").replace(".txt", " ");

        //make frame fit size of screen and remove header buttons/tabs
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setUndecorated(true);
        this.setLayout(new BorderLayout());

        //main image scaled to size
        Icon icon = scaleIcon(new ImageIcon("images/kidknits.jpg"), 1500, 1400);

        //set background image
        this.background = new JLabel(icon, SwingConstants.CENTER);
        background.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));
        background.setLayout(new GridLayout(3,1));
        this.add(background);

        JPanel southPanel = new JPanel(new GridLayout(1,2));
        southPanel.setOpaque(false);
        this.add(southPanel, BorderLayout.SOUTH);

        knittingCommands = new JTextField("Today we are knitting: " );
        knittingCommands.setFont(new Font("Helvetica", Font.BOLD, 36));
        knittingCommands.setForeground(Color.DARK_GRAY);
        knittingCommands.setOpaque(false);
        southPanel.add(knittingCommands);

        stitchesMade = new JTextField(fileName);
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

       // addLabel(knitLabel);
        addButtons(knitIt, unpickIt, dropIt, knitGraph);

        //basic stuff to stop program when frame is closed, make visible etc
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }
//    private void addLabel(JLabel label) {
//        label.setOpaque(false);
//        label.setLayout(new GridLayout(3,1));
//        label.setBorder(BorderFactory.createEmptyBorder(30,10,10,10));
//        background.add(label, BorderLayout.CENTER);
//    }
    //addButton method: takes a JButton, adds it to relevant JLabel and associates it with relevant KnitSpeak file
    private void addButtons(JButton knit, JButton unpick, JButton drop, KnitGraph graph) {
        makeButton(knit);
        makeButton(unpick);
        makeButton(drop);
        Font knitFont = new Font("Helvetica", Font.BOLD, 94);
        Font unpickFont = new Font("Helvetica", Font.BOLD, 72);
        Font dropFont = new Font("Helvetica", Font.BOLD, 32);
        knit.setFont(knitFont);
        unpick.setFont(unpickFont);
        drop.setFont(dropFont);
        knit.setEnabled(true);
        unpick.setEnabled(false);
        drop.setEnabled(false);

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
                    graph.doKnitting();
                    knit.setText(" ");
                    unpick.setText("PLAY");
                    drop.setText(" ");
                    return graph;
                }
            };
            knitter.execute();
        });

        unpick.addActionListener(e -> {
            knit.setEnabled(true);
            unpick.setEnabled(false);
            drop.setEnabled(false);
            knitting = false;
            if (unpick.getText().equals("PLAY")) {
                player = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        graph.playGraph();
                        knit.setText("KNIT");
                        unpick.setText("UNPICK");
                        drop.setText("DROP A STITCH");
                        knitting = true;
                        setFileInfo();
                        knittingCommands.setText("Today we are knitting: " );
                        stitchesMade.setText(fileName);
                        return null;
                    }
                };
                player.execute();
            } else {
                unPicker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                            graph.doUnpicking();
                            graph.resetGraph();
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
                    graph.dropAStitch();
                    return null;
                }
            };
            knitter.cancel(true);
            stitchDropper.execute();
            knit.setText(" ");
            unpick.setText("PLAY");
            drop.setText(" ");
            unpick.setEnabled(true);
        });
        background.add(knit);
        background.add(unpick);
        background.add(drop);
    }
    private void makeButton(JButton button) {
        button.setForeground(Color.MAGENTA);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
    }
    //takes an icon, gets the image, scales it to size of JButton, method here for flexibility
    private static Icon scaleIcon(ImageIcon icon, int newWidth, int newHeight) {
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }
    private void setFileInfo() {
        String[] knitFile = new KnitFileSelector("knitSpeak/LaceFiles").getKnitFileSelection();
        Random random = new Random();
        this.randomInt = random.nextInt(3);
        this.file = knitFile[randomInt];
        this.fileName = file.replace("knitSpeak/", " ").replace(".txt", " ");
    }
    public static void main(String[] args) {
        new KnitPlay();
    }
}
