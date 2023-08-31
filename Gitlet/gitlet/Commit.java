package gitlet;



import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;
import java.util.Formatter;

public class Commit implements Serializable {
    /** Message of this commit. */

    private String _message;
    /** Message of this commit.*/
    private String _parent;
    /** First parent of this commit.*/
    private String _parent2;
    /** Optional second of this commit.*/
    private Date _time;
    /** The names of the files this commit is tracking,
     * mapped to the hash of their contents stored in BLOBS. */
    private TreeMap<String, String> _tracks;

    public Commit(String message) {
        _message = message;
        _parent = null;
        _parent2 = null;
        _time = new Date(0);
        _tracks = new TreeMap<>();
    }

    public Commit(String message, TreeMap<String, String> tracks,
                  String parent, String parent2) {
        _message = message;
        _tracks = tracks;
        _parent = parent;
        _parent2 = parent2;
    }

    public static Commit cloneAndUpdate(Commit a, String message, Stage stage) {
        Commit c = new Commit(message, a._tracks, a.getHash(), null);
        TreeMap<String, String> tracks = stage.getAdds();
        for (String f : tracks.keySet()) {
            if (c._tracks != null) {
                if (c._tracks.containsKey(f)) {
                    c._tracks.remove(f, c._tracks.get(f));
                    c._tracks.put(f, tracks.get(f));
                } else {
                    c._tracks.put(f, tracks.get(f));
                }
            }

            for (String file : stage.getRms()) {
                c._tracks.remove(file);
            }
            for (String file : c._tracks.keySet()) {
                Utils.writeContents(Utils.join(Repository.CWD, file),
                        Utils.readContentsAsString(Utils.join
                                (Repository.BLOBS, c._tracks.get(file))));
            }
        }

        ArrayList<String> itemsToRemove = new ArrayList<>();

        for (String file : c._tracks.keySet()) {
            File f = Utils.join(Repository.CWD, file);
            if (!f.exists()) {
                itemsToRemove.add(file);
            }
        }

        for (String file : itemsToRemove) {
            c._tracks.remove(file);
        }



        c._time = new Date();
        return c;
    }

    public void assign2ndParent(String parenthash) {
        this._parent2 = parenthash;
    }




    public void immortalize() throws IOException {
        String hash = getHash();
        File commitFile = Utils.join(Repository.COMMITS, hash);
        commitFile.createNewFile();
        Utils.writeObject(commitFile, this);
    }

    public String getHash() {
        byte[] contents = Utils.serialize(this);
        return Utils.sha1(contents);
    }

    public TreeMap<String, String> getTracks() {
        return _tracks;
    }

    public String getMessage() {
        return _message;
    }

    public String getParent() {
        return _parent;
    }

    public String getParent2() {
        return _parent2;
    }





    public Formatter getTime() {
        Formatter f = new Formatter();
        return f.format("%ta %tb %td %tR:%tS %tY %tz",
                _time, _time, _time, _time, _time, _time, _time);
    }
}
