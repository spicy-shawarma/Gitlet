# Gitlet Design Document
author: Manish Subramaniam

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

### 1. Commit

This class represents a commit object, or a snapshot of the repository.

####Fields
    String _message;
    String _parent;
    String _parent2;
    Date _time;
    TreeMap<String, String> _tracks: a mapping of file names  
    to their contents. contents represented as SHA1 codes in a 
    blob directory.


####




### Repository

This class represents a gitlet repository. It will consist of a staging area and 
collection of Commits stored in a TreeMap.

####Fields
    
    Stage stage: the staging area of this repository
    TreeMap<String, Commit> commitTree: A TreeMap of a commit's SHA1 to itself.
    ArrayList<Commit> branches: an array list branches, represented
    as pointers to commits in the tree.

###Stage

This is the staging area for adds and rms.

####Fields

      TreeMap<File, Blob> adds: The key is the file we are tracking, the Blob are it's contents
      at the time we want to track it.

      ArrayList<File> rms: The files we want to remove from the next commit.

## 2. Algorithms

###init()
Create a new Repository. Create .gitlet folder with appropriate subdirectories for 
persistence.

###add(File file)
Create a blob for the file and compare it to the blob of the file in the current head.
If they are not equal, add file to the staging area.

###commit()
Clone the current head and advance the head pointer to the new Commit. For each file
in the staging area, move the pointer of the file to the new blob of the file

###rm(File file)

Do what it says in the spec-- remove it from rms[] if it exists, or add it and remove it
from cwd if not.

###log()

Print the metadata of the commits as appropriate, with formatting. Follow the tree back until
we get a commit whose parent is null.

###global-log()

Iterate through the commits in .gitlet and print the required data.

###find [commit message]

We iterate through the commits in .gitlet, asking for the message from each one
until we get a match. If no match is found, return error message.

###status()

We print all the branches created in Repository. We print all the files currently 
in Stage (from the appropriate list). We also print the complement of those sets. This 
method will essentially request all the relevant data from our classes.

###checkout

Depending on the use case, our objective will be different. But in any case we iterate through
our commit files until we find a match
and overwrite files in CWD as necessary.

###branch [branch name]

Create a new pointer in Repository in branches[]. Point to the appropriate commit.

###rm-branch [branch name]

Remove this branch from branches[].

###reset [branch name]

Call checkout [branch name], but also move the head pointer to the appropriate commit.

###merge [branch name]

Access the branch and the current head. Determine the appropriate split point by
 following back the parents of both commits until a match is found (we calculate the length
of the paths here too. At most we have 4 paths, and we use the shortest one). Then, compare
the blobs of the files of the three commits (split point, head, branch). Create a new commit
along the current branch with the appropriate merges.

## 3. Persistence

In init(), create a .gitlet folder and its subdirectories, namely:
1. commits - the SHA1 code of every commit that is made.
2. blobs - the SHA1 code of every tracked file change.
3. commit tree - the serialized commit tree in repo to preserve the relationships between commits.

We write the commits and blobs to files in the appropriate directories whenever they are created. Since at 
any point we only need to keep track of commits, their relationship to each other, and the contents of the files,
this is sufficient for the operation of the program.

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to 
visualize the structure and workflow of your program.
![](/Users/Manish/Downloads/WhatsApp Image 2022-04-16 at 11.03.14 AM.jpeg)