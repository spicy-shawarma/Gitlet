package gitlet;



import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;


public class Repository {

    /** Path to CWD.*/
    static final File CWD = new File(".");
    /** Path to .gitlet.*/
    static final File REPO = Utils.join(CWD, ".gitlet");
    /** Path to commits.*/
    static final File COMMITS = Utils.join(REPO, "commits");
    /** Path to branches.*/
    static final File BRANCHES = Utils.join(REPO, "branches");
    /** Path to Staging Area.*/
    static final File STAGE = Utils.join(REPO, "stage");
    /** Path to blobs.*/
    static final File BLOBS = Utils.join(REPO, "blobs");
    /** Path to HEAD.*/
    static final File HEADPATH = Utils.join(BRANCHES, "head");
    /** Path to current branch name.*/
    static final File CURRENTACTIVE = Utils.join(BRANCHES, "current_active");
    /** Used to store path to current branch. */
    private static File activePath;
    /** Treemap of filenames to commits. */
    private transient TreeMap<String, Commit> commitTree;
    /** Staging Area of this repository. */
    private Stage stage;



    public Repository() {
        commitTree = new TreeMap<>();
        stage = new Stage();
    }


    /** COMMANDS. **/


    public void init() throws IOException {
        CWD.mkdirs();

        if (REPO.exists()) {
            System.out.println("Gitlet version-control system already"
                    + " exists in the current directory.");
            return;
        }
        REPO.mkdirs();
        BLOBS.mkdirs();
        BRANCHES.mkdirs();
        COMMITS.mkdirs();
        STAGE.createNewFile();

        Commit in = new Commit("initial commit");
        commitTree.put(in.getHash(), in);
        in.immortalize();

        stage = new Stage();
        stage.saveStage();

        HEADPATH.createNewFile();
        Utils.writeObject(CURRENTACTIVE, "master");
        setActivePath("master");
        activePath.createNewFile();
        updateHead(in);
    }

    public void add(String filename) throws IOException {
        stage = new Stage(STAGE);
        File file = Utils.join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        if (stage.getRms().contains(filename)) {
            stage.removeFromRms(filename);
            stage.saveStage();
        }
        if (sameAsHead(filename)) {
            stage.removeFromAdds(filename);
            stage.saveStage();
            return;
        }

        newBlob(file);
        stage.stageForAddition(filename);
        stage.saveStage();
    }

    public void rm(String file) throws IOException {
        stage = new Stage(STAGE);
        if (!stage.getAdds().containsKey(file)
                && !activeBranch().getTracks().containsKey(file)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (stage.getAdds().containsKey(file)) {
            stage.removeFromAdds(file);
        }
        Commit active = activeBranch();
        if (active.getTracks().containsKey(file)) {
            stage.addToRms(file);
            stage.saveStage();
            File f = Utils.join(CWD, file);
            f.delete();
        }
        stage.saveStage();
    }

    public void newCommit(String message) throws IOException {
        Commit active = activeBranch();
        stage = new Stage(STAGE);
        if (stage.getAdds().isEmpty() && stage.getRms().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit next = Commit.cloneAndUpdate(active, message, stage);
        updateHead(next);
        next.immortalize();
        stage.clearStage();
        stage.saveStage();
    }


    public void find(String message) {
        List<String> commitfiles = Utils.plainFilenamesIn(COMMITS);
        boolean found = false;
        if (commitfiles == null) {
            System.out.println("Found no commit with that message");
            return;
        }
        for (String file: commitfiles) {
            File f = Utils.join(COMMITS, file);
            Commit temp = commitFromFile(file);
            if (temp.getMessage().equals(message)) {
                found = true;
                System.out.println(temp.getHash());
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message");
        }
    }

    public void log() {
        Commit commit = activeBranch();

        while (commit.getParent() != null) {
            logPrinter(commit);

            Commit parent = commitFromFile(commit.getParent());
            commit = parent;
        }
        logPrinter(commit);
    }

    public void globalLog() {
        List<String> commitfiles = Utils.plainFilenamesIn(COMMITS);
        Commit temp;
        assert commitfiles != null;
        for (String file : commitfiles) {
            File f = Utils.join(COMMITS, file);
            temp = Utils.readObject(f, Commit.class);
            logPrinter(temp);
        }
    }

    public void newBranch(String name) throws IOException {
        File branch = Utils.join(BRANCHES, name);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        } else {
            branch.createNewFile();
            Utils.writeObject(branch, activeBranch());
        }
    }


    public void deleteBranch(String name) throws IOException {
        File branch = Utils.join(BRANCHES, name);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (name.equals(getCurrentActive())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branch.delete();
    }

    public void reset(String commitID) throws IOException {
        File commit = Utils.join(COMMITS, commitID);
        if (!commit.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit c = commitFromFile(commitID);

        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);

        for (String file : cwdFiles) {
            if (!activeBranch().getTracks().containsKey(file)
                && c.getTracks().containsKey(file)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }




        for (String file : c.getTracks().keySet()) {
            checkoutCommit(c.getHash(), file);
        }
        for (String filename : activeBranch().getTracks().keySet()) {
            if (!(c.getTracks().containsKey(filename))) {
                Utils.restrictedDelete(Utils.join(CWD, filename));
            }
        }
        stage = new Stage();
        stage.saveStage();

        updateHead(c);

    }

    public void checkoutFile(String filename) {
        Commit active = activeBranch();
        overrwriteFile(filename, active);
    }

    public void checkoutCommit(String commitID, String filename) {


        List<String> commitFiles = Utils.plainFilenamesIn(COMMITS);
        for (String file : commitFiles) {
            if (file.contains(commitID)) {
                commitID = file;
            }
        }




        File thisCommit = Utils.join(COMMITS, commitID);
        if (!thisCommit.exists()) {
            System.out.println("No commit with that ID exists.");
            return;
        }
        Commit c = Utils.readObject(thisCommit, Commit.class);
        if (!c.getTracks().containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        overrwriteFile(filename, c);
    }

    public void checkoutBranch(String branchname) throws IOException {
        File branch = Utils.join(BRANCHES, branchname);
        if (!branch.exists()) {
            System.out.println("No such branch exists.");
            return;
        } else if (branchname.equals(getCurrentActive())) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        setActivePath(branchname);
        Commit branchCommit = Utils.readObject(Utils.join
                (BRANCHES, branchname), Commit.class);
        String hash = branchCommit.getHash();
        reset(hash);
    }

    public void status() {
        stage = new Stage(STAGE);
        List<String> branches = Utils.plainFilenamesIn(BRANCHES);
        activePath = Utils.join(BRANCHES, getCurrentActive());
        System.out.println("=== Branches ===");
        for (String branch: branches) {
            if (!branch.equals("head") && !branch.equals("current_active")) {
                if (getCurrentActive().equals(branch)) {
                    System.out.println("*" + branch);
                } else {
                    System.out.println(branch);
                }
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String add : stage.getAdds().keySet()) {
            System.out.println(add);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String rm : stage.getRms()) {
            System.out.println(rm);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String file : activeBranch().getTracks().keySet()) {
            File fileinCWD = Utils.join(CWD, file);
            if (fileinCWD.exists()) {
                String cwdBlob = Utils.sha1
                        (Utils.readContentsAsString(fileinCWD));
                if (!cwdBlob.equals(activeBranch().getTracks().get(file))) {
                    System.out.println(file);
                }
            }
        }

        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String file : Utils.plainFilenamesIn(CWD)) {
            if (!activeBranch().getTracks().containsKey(file)
                && !stage.getAdds().containsKey(file)) {
                System.out.println(file);
            }
        }
        System.out.println();
    }

    public boolean mergeSpecialCases(String branch, Commit mergedIn,
                                     Commit split, Commit split2,
                                     Commit active) throws IOException {

        if (branch.equals(getCurrentActive())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        if (split2.getHash().equals(active.getHash())) {
            reset(mergedIn.getHash());
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        if (split.getHash().equals(mergedIn.getHash())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return true;
        }
        stage = Utils.readObject(STAGE, Stage.class);
        if (!stage.getAdds().isEmpty() || !stage.getRms().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        for (String file : Utils.plainFilenamesIn(CWD)) {
            if (!activeBranch().getTracks().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return true;
            }
        }
        return false;
    }



    public void merge(String branch) throws IOException {
        File f = Utils.join(BRANCHES, branch);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        boolean mergeConflict = false;
        Commit mergedIn = Utils.readObject(Utils.join
                (BRANCHES, branch), Commit.class);
        Commit split = splitPoint(breadthFirstTraverse(mergedIn),
                activeBranch());
        Commit split2 = splitPoint(breadthFirstTraverse(activeBranch()),
                mergedIn);
        Commit active = activeBranch();
        if (mergeSpecialCases(branch, mergedIn, split, split2, active)) {
            return;
        }
        TreeMap<String, String> splitFiles = split.getTracks();
        TreeMap<String, String> currFiles = active.getTracks();
        TreeMap<String, String> branchFiles = mergedIn.getTracks();
        HashSet<String> files = new HashSet<>();
        stage = Utils.readObject(STAGE, Stage.class);
        files.addAll(splitFiles.keySet());
        files.addAll(currFiles.keySet());
        files.addAll(branchFiles.keySet());
        for (String file : files) {
            String currVer = currFiles.get(file);
            String mergeVer = branchFiles.get(file);
            String splitVer = splitFiles.get(file);
            if (matchGiven(currVer, mergeVer, splitVer)) {
                if (mergeVer == null) {
                    stage.addToRms(file);
                    Utils.join(CWD, file).delete();
                } else {
                    stage.stageSpecificVersion(file, mergeVer);
                }
            }  else if (conflict(currVer, mergeVer, splitVer)) {
                mergeConflict = true;
                conflictHandler(file, branchFiles, currFiles);
            }
        }
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        stage.saveStage();
        newMergeCommit(active, mergedIn, getCurrentActive(), branch);
    }

    public boolean matchGiven(String currVer,
                              String mergeVer, String splitVer) {
        if (((mergeVer == null) && currVer != null)
                && currVer.equals(splitVer)) {
            return true;
        } else if (currVer == null
                && splitVer == null) {
            return true;
        } else if (currVer == null && splitVer.equals(mergeVer)) {
            return false;
        } else if (currVer == null) {

            return false;
        } else if (currVer.equals(splitVer)) {
            return true;
        }
        return false;
    }

    public boolean conflict(String currVer, String mergeVer, String splitVer) {
        if (currVer == null && mergeVer == null) {
            return false;
        } else if (mergeVer == null && splitVer == null) {
            return false;
        } else if (mergeVer == null && !currVer.equals(splitVer)) {
            return true;
        } else if (currVer == null && !mergeVer.equals(splitVer)) {
            return true;
        } else if (currVer == null) {
            return false;
        } else if (!currVer.equals(splitVer)
                && !currVer.equals(mergeVer)
                && !mergeVer.equals(splitVer)) {
            return true;
        }
        return false;
    }

    public void newMergeCommit(Commit a, Commit b,
                               String activebranch,
                               String mergedbranch) throws IOException {
        String mergemessage =
                "Merged " + mergedbranch + " into " + activebranch + ".";
        Commit active = activeBranch();

        stage = new Stage(STAGE);


        if (stage.getAdds().isEmpty() && stage.getRms().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit next = Commit.cloneAndUpdate(active, mergemessage, stage);
        next.assign2ndParent(b.getHash());
        updateHead(next);
        next.immortalize();
        stage.clearStage();
        stage.saveStage();
    }

    /**PRINTERS.**/

    public void testMethod() {
        ArrayList<String> ancestors = breadthFirstTraverse(activeBranch());
        for (String ancestor : ancestors) {
            System.out.println(ancestor);
        }
        Commit sp = splitPoint(ancestors, activeBranch());
        System.out.println(sp.getHash());
    }

    public void logPrinter(Commit c) {
        System.out.println("===");
        System.out.println("commit " + c.getHash());
        System.out.println("Date: " + c.getTime());
        System.out.println(c.getMessage());
        System.out.println();
    }

    public void conflictHandler(String file,
                                TreeMap<String, String> branchFiles,
                                TreeMap<String, String> currFiles)
                                throws IOException {


        String mergedInFileHash = branchFiles.get(file);
        String currFileHash = currFiles.get(file);
        String currcontents = "";
        String mergecontents = "";

        if (mergedInFileHash != null) {
            mergecontents = getContentFromBlob(mergedInFileHash);
        }
        if (currFileHash != null) {
            currcontents = getContentFromBlob(currFileHash);
        }
        String content = "<<<<<<< HEAD\n" + currcontents
                + "=======\n"
                + mergecontents + ">>>>>>>\n";
        String contentBlobCode = Utils.sha1(content);
        File blobFile = Utils.join(BLOBS, contentBlobCode);
        blobFile.createNewFile();
        Utils.writeContents(blobFile, content);
        stage.stageSpecificVersion(file, contentBlobCode);
        stage.saveStage();
    }


    /** BRANCH UTILITES. **/




    public void setActivePath(String branch) {
        activePath = Utils.join(BRANCHES, branch);
        Utils.writeObject(CURRENTACTIVE, branch);
    }


    public String getCurrentActive() {
        return Utils.readObject(CURRENTACTIVE, String.class);
    }


    public void updateHead(Commit c) {
        Utils.writeObject(HEADPATH, c);
        activePath = Utils.join(BRANCHES, getCurrentActive());
        Utils.writeObject(activePath, c);
    }


    public Commit activeBranch() {
        return Utils.readObject(HEADPATH, Commit.class);
    }

    public ArrayList<String> breadthFirstTraverse(Commit c) {
        ArrayList<String> visited = new ArrayList<>();
        ArrayDeque<Commit> work = new ArrayDeque<>();
        work.push(c);

        while (!work.isEmpty()) {
            Commit node = work.remove();
            if (node != null) {
                visited.add(node.getHash());
                if (node.getParent2() != null) {
                    Commit p2 = commitFromFile(node.getParent2());
                    if (!visited.contains(p2.getHash())) {
                        work.push(p2);
                    }
                }
                if (node.getParent() != null) {
                    Commit p1 = commitFromFile(node.getParent());
                    if (!visited.contains(p1.getHash())) {
                        work.push(p1);
                    }
                }

            }
        }
        return visited;
    }

    public Commit splitPoint(ArrayList<String> ancestors, Commit c) {
        ArrayList<Commit> visited = new ArrayList<>();
        ArrayDeque<Commit> work = new ArrayDeque<>();
        work.push(c);

        while (!work.isEmpty()) {
            Commit node = work.remove();
            if (node != null) {
                visited.add(node);

                if (node.getParent() != null) {
                    Commit p1 = commitFromFile(node.getParent());
                    if (ancestors.contains(p1.getHash())) {
                        return p1;
                    }
                    if (!visited.contains(p1)) {
                        work.push(p1);
                    }
                }
                if (node.getParent2() != null) {
                    Commit p2 = commitFromFile(node.getParent2());
                    if (ancestors.contains(p2.getHash())) {
                        return p2;
                    }
                    if (!visited.contains(p2)) {
                        work.push(p2);
                    }
                }

            }
        }
        return c;
    }




    /* COMMIT UTILITIES. **/


    public Commit commitFromFile(String filename) {
        File f = Utils.join(Repository.COMMITS, filename);
        return Utils.readObject(f, Commit.class);
    }

    public void constructCommitTree() {
        List<String> commits = Utils.plainFilenamesIn(COMMITS);
        for (String filename : commits) {
            commitTree.put(filename, commitFromFile(filename));
        }
    }

    public boolean sameAsHead(String file) {
        File f = Utils.join(CWD, file);
        String blob = Utils.sha1(Utils.readContentsAsString(f));
        Commit active = activeBranch();
        if (active.getTracks().containsKey(file)) {
            if (active.getTracks().get(file).equals(blob)) {
                return true;
            }
        }
        return false;
    }


    public static void overrwriteFile(String filename, Commit c) {
        String blobRef = c.getTracks().get(filename);
        File blobpath = Utils.join(BLOBS, blobRef);
        if (!blobpath.exists()) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File fileInCWD = Utils.join(CWD, filename);
        Utils.writeContents(fileInCWD, Utils.readContentsAsString(blobpath));
    }

    private String getContentFromBlob(String blobID) {
        File f = Utils.join(BLOBS, blobID);


        if (f.exists()) {
            return Utils.readContentsAsString(f);
        } else {
            return "";
        }
    }

    public void newBlob(File f) throws IOException {
        String blob = Utils.readContentsAsString(f);
        String blobCode = Utils.sha1(blob);
        File blobFile = Utils.join(BLOBS, blobCode);
        blobFile.createNewFile();
        Utils.writeContents(blobFile, blob);
    }

}


