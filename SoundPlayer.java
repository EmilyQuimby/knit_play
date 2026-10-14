
import javax.sound.sampled.*;
import java.io.File;
import java.util.Random;

/**
 * Plays a randomly selected sound associated with a knitting command.
 *
 * Each command corresponds to a directory containing eight sound files.
 * A sound is selected randomly and played using Java's sampled audio API.
 *
 * Implements Runnable so playback can be started on a separate thread.
 * Audio clips are retained for management by KnitGraph.
 */
public class SoundPlayer implements Runnable {

    private final int LOOPS;
    private final File SOUND;
    private Clip clip;

    /**
     * Selects a random sound file for the specified knitting command.
     *
     * Expects sound files numbered 0.wav to 7.wav within the
     * directory corresponding to the command.
     *
     * @param command the knitting command identifying the sound directory
     * @param loops the number of additional times to repeat the sound
     */
    public SoundPlayer(String command, int loops) {
        Random random = new Random();
        int randomInt = random.nextInt(8);

        this.LOOPS = loops;
        this.SOUND = new File("sounds/" + command + "/" + randomInt + ".wav");
    }

    /**
     * Opens and plays the selected audio file.
     *
     * Stores the resulting Clip so that KnitGraph can access
     * and close it when resetting the graph.
     *
     * @throws Exception if the audio file cannot be opened or played
     */
    private void play() throws Exception {
        clip = AudioSystem.getClip();

        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(SOUND)) {
            clip.open(audioStream);
        }

        clip.loop(LOOPS);

        // Clips are closed by KnitGraph during graph cleanup.
    }

    /**
     * Returns the audio clip associated with this sound player.
     *
     * @return the clip, or null if playback has not yet started
     */
    Clip getClip() {
        return clip;
    }

    /**
     * Starts audio playback on the calling thread.
     *
     * Intended to be executed by a separate thread.
     * Any playback exceptions are printed to standard error.
     */
    @Override
    public void run() {
        try {
            play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
