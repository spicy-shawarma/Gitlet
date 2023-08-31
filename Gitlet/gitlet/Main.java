package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, the amazing local version-control system!
 *  @author Manish Subramaniam
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */


    public static void main(String... args) throws IOException {
        Repository r = new Repository();
        if (baseCheck(args)) {
            switch (args[0]) {
            case "init" -> r.init();
            case "add" -> {
                assert (args.length == 2);
                r.add(args[1]);
            }
            case "commit" -> {
                if (args.length < 2 || args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                }
                r.newCommit(args[1]);
            }
            case "checkout" -> checkoutEntry(args, r);
            case "log" -> r.log();
            case "rm" -> r.rm(args[1]);
            case "global-log" -> r.globalLog();
            case "find" -> r.find(args[1]);
            case "branch" -> r.newBranch(args[1]);
            case "status" -> r.status();
            case "merge" -> r.merge(args[1]);
            case "rm-branch" -> r.deleteBranch(args[1]);
            case "reset" -> r.reset(args[1]);
            default -> System.out.println("No command with that name exists.");
            }
        }
    }

    public static boolean baseCheck(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return false;
        }
        File git = Utils.join(".", ".gitlet");
        if (!args[0].equals("init") && !git.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return false;
        }
        return true;
    }

    public static void checkoutEntry(String[] args, Repository r)
            throws IOException {
        if (args[1].equals("--")) {
            r.checkoutFile(args[2]);
            return;
        } else if (args.length > 2 && args[2].equals("--")) {
            r.checkoutCommit(args[1], args[3]);
            return;
        } else if (args.length > 2) {
            System.out.println("Incorrect operands.");
            return;
        } else {
            r.checkoutBranch(args[1]);
            return;
        }
    }
}
