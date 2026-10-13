import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.util.Random;

//takes a command and converts it into a file path for a given directory of sounds, and tells it how many times to loop
public class SoundPlayer implements Runnable {
    private final int LOOPS;
    private final File SOUND;
    private Clip clip;
    public SoundPlayer(String command, int loops) {
        Random random = new Random();
        int randomInt = random.nextInt(8);
        this.LOOPS = loops;
        String[] sounds = new String[8];
        for(int i = 0; i< sounds.length; i++) {
            sounds[i] = "sounds/" + command + "/" + i + ".wav";
        }
        SOUND = new File(sounds[randomInt]);
        clip = null;
    }
    private void play() throws Exception {
        clip = AudioSystem.getClip();
        String url = SOUND.toURI().toURL().toString();
        clip.open(AudioSystem.getAudioInputStream(new URL(url)));
        clip.loop(LOOPS);
        //clips are stored in an array and closed remotely from KnitGraph once a graph is completed. See resetGraph().
    }
    Clip getClip() {
        return clip;
    }
    @Override
    public void run() {
        try {
            play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}