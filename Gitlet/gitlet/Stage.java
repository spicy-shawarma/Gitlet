package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

public class Stage implements Serializable {
    /**Mapping of file names to blobs.*/
    private TreeMap<String, String> _adds;
    /**List of files to remove.*/
    private ArrayList<String> _rms;

    public Stage() {
        _adds = new TreeMap<String, String>();
        _rms = new ArrayList<String>();
    }

    public Stage(File f) {
        _adds = Utils.readObject(f, Stage.class).getAdds();
        _rms = Utils.readObject(f, Stage.class).getRms();
    }

    public void stageForAddition(String filename) throws IOException {
        File f = Utils.join(Repository.CWD, filename);
        String blob = Utils.sha1(Utils.readContentsAsString(f));
        _adds.put(filename, blob);
        saveStage();
    }

    public void stageSpecificVersion(String filename, String versionID) {

        _adds.put(filename, versionID);
    }

    public void removeFromAdds(String file) throws IOException {
        if (_adds.containsKey(file)) {
            _adds.remove(file);
            saveStage();
        } else {
            return;
        }
    }

    public void removeFromRms(String file) throws IOException {
        if (_rms.contains(file)) {
            _rms.remove(file);

        } else {
            return;
        }
    }

    public void saveStage() throws IOException {
        File stageFile = Repository.STAGE;
        if (!stageFile.exists()) {
            stageFile.createNewFile();
        }
        Utils.writeObject(stageFile, this);

    }

    public void clearStage() throws IOException {
        _adds = new TreeMap<>();
        _rms = new ArrayList<>();
        this.saveStage();
    }

    public void addToRms(String filename) {
        if (!_rms.contains(filename)) {
            _rms.add(filename);
        }
    }

    public TreeMap<String, String> getAdds() {
        return _adds;
    }
    public ArrayList<String> getRms() {
        return _rms;
    }

}
