General Instructions
=========================

### Building the project

First, ensure that you have installed Maven and git on your machine.

Next, checkout the project via

`$ git clone https://gitlab.tubit.tu-berlin.de/AIM3-SDS/ComputationalExercises.git`


Note that the `main` method expects path to the dataset ("full-game") as an argument. Extract `full-game.zip` before.

### Tools and IDE

We recommend to use IntelliJ Idea, and Java 8 or 11.


### Git workflow for an assignment

This is how you check on which branch you are:

    $ git branch
    * master

We suggest that you create a new branch for each assignment like this:

    $ git checkout -b assignment3
    Switched to a new branch 'assignment3'


Now do your changes in this branch. Let's assume that you implemented XYZ in class `MyClass`. You can see which files are changed like this:


    $ git status
    # On branch assignment3
    # Changes not staged for commit:
    #   (use "git add <file>..." to update what will be committed)
    #   (use "git checkout -- <file>..." to discard changes in working directory)
    #
    #  modified:   src/main/java/de/tuberlin/dima/aim3/assignment3/MyClass.java
    #
    no changes added to commit (use "git add" and/or "git commit -a")

Once you are done with your changes, you can add your file to the changes you want to have in your homework patch:

    $ git add src/main/java/de/tuberlin/dima/aim3/assignment3/MyClass.java

Once you are done with your changes, you can add your file to the changes you want to have in your homework patch:

    $ git commit -m "exercise3"
    [assignment2 b0fa2c5] exercise3
     1 file changed, 1 insertion(+)

After you are done with all changes and committed everything, you need to create a patch that contains all your changes like this:

    $ git format-patch master
    0001-exercise3.patch

The resulting file could be uploaded to ISIS.

Also submit the CSV files containing the outputs.
