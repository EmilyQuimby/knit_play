import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.util.Random;

//takes a command and converts it into a file path for a given directory of sounds, and tells it how many times to loop
public class SoundPlayer implements Runnable {
    private final int loops;
    private final File sound;
    private Clip clip;
    public SoundPlayer(String command, int loops) {
        Random random = new Random();
        int randomInt = random.nextInt(8);
        this.loops = loops;
        String[] sounds = new String[8];
        for(int i = 0; i< sounds.length; i++) {
            sounds[i] = "sounds/" + command + "/" + i + ".wav";
        }
        sound = new File(sounds[randomInt]);
        clip = null;
    }
    private void play() throws Exception {
        clip = AudioSystem.getClip();
        String url = sound.toURI().toURL().toString();
        clip.open(AudioSystem.getAudioInputStream(new URL(url)));
        clip.loop(loops);
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