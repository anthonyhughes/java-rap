package com.datalanguage.javarap;

import com.datalanguage.javarap.utils.Util;

import java.io.File;

/**
 * <p>Title: Master to delim the aquaint corpus.</p>
 * <p>Description: Master is responsible for fetching the files to be processed, adding nodeIdx as suffix and move the results back.
 * <p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0
 */

public class Master {
    String fileServer = null;
    String workDir = null;
    String sourceDir = null;
    String outputDir = null;
    String tmpDir = null;

    String[] jobList = null;
    String[] jobDone = null;

    public Master() {
    }

    private String[] getNodeScope(String scope) {
        String[] nodescope = scope.split("-");
        String node = "";
        int nodeNum = 0;
        if (nodescope.length == 1) {
            nodeNum = 1;
            node = String.valueOf(scope);
        } else if (nodescope.length == 2) {
            int l = Integer.parseInt(nodescope[0]);
            int u = Integer.parseInt(nodescope[1]);
            nodeNum = Math.abs(l - u);
            for (int i = Math.min(l, u); i <= Math.max(l, u); i++) {
                node += String.valueOf(i);
            }

        } else {
            System.out.println("Invalid node scope. Try again. (1-30)");
            System.exit(0);
        }
        return node.trim().split("|\\x16");

    }

    private boolean assignJob(String dataFile, String nodeId) {
        if (Util.checkFile(tmpDir, nodeId) != null) {
            return false;
        }

        try {
            new File(tmpDir + dataFile + "." + nodeId).createNewFile();
        } catch (Exception e) {
            System.out.println(tmpDir + dataFile + " can not be created.");
            return false;
        }
        return true;
    }

    private void cpFile(String fromFile, String toFile) {
        String command = "scp -B ";
        try {

            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    command + fromFile + " " + toFile};
            System.out.println("/bin/sh" +
                    " -c " +
                    command + fromFile + " " + toFile);
            Process proc = Runtime.getRuntime().exec(cmd);
            proc.waitFor();

        } catch (Exception e) {
            System.err.println("Wrong while moving " + fromFile + ".");
        }
    }

    public void moveFiles(String fromDir, String toDir, String suffix) {
        try {
            File[] inputFile = new File(fromDir).listFiles();
            if (inputFile != null) {
                for (int i = 0; i < inputFile.length; i++) {
                    if (inputFile[i].getName().endsWith("." + suffix)) {
                        cpFile(fromDir + File.separator + inputFile[i].getName(), toDir + File.separator + inputFile[i].getName());
                        new File(fromDir + File.separator + inputFile[i].getName()).delete();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    /**
     * @param scope
     * @return a list of the files having been processed
     */
    private String[] getJobScope(String scope) {
        File jobList = new File(scope);
        if (!jobList.exists()) {
            System.out.println("Couldn't find " + scope +
                    ". Double check the path name.");
            System.exit(0);
        }
        StringBuffer files = Util.read(scope);
        return files.toString().split(" |\\\n");
    }

    public void wakeup(String idleDir) {
        new File(idleDir + File.separator + "master.master").delete();
    }

    public void sleep(String idleDir) {
        try {
            File flag = new File(idleDir + File.separator + "master.master");
            if (flag.exists()) {
                return;
            } else {
                flag.createNewFile();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getRealFilePath(String fileName) {
        String org = fileName.substring(9, 12);
        String year = fileName.substring(0, 4);
        if (org.equals("XIN")) {
            org = "XIE";
        }
        return org + File.separator + year + File.separator + fileName;
    }

    //public String

    public static void main(String[] args) {


        Master master1 = new Master();

        if (args.length < 3) {
            System.out.println(
                    "Usage: java anaphoraresolution.Master workDir fileServer sourceDir targetDir");
            System.out.println(
                    "java anaphoraresolution.Master /home/userID userId@serverName /dir1/dir2 /dir3/dir4/targetDirOnFileServer");
            System.exit(0);
        }

        master1.workDir = args[0];
        master1.fileServer = args[1];
        master1.sourceDir = args[2];
        master1.outputDir = args[3];
        master1.tmpDir = System.getProperty("tmpDir");


        String busyDir = master1.workDir + File.separator + "busy";
        String idleDir = master1.workDir + File.separator + "idle";


        master1.jobList = master1.getJobScope(master1.workDir + File.separator + "jobList");
        master1.jobDone = master1.getJobScope(master1.workDir + File.separator + "jobDone");

        String updateDoneList = null;
        //I'm alive
        master1.wakeup(idleDir);


        //disperse daemons
        for (int i = 0; i < master1.jobList.length; i++) {
            String fileNameOnly = master1.jobList[i].substring(master1.jobList[i].lastIndexOf("/") + 1).trim();
            if (!(fileNameOnly.endsWith(".GZ"))) {
                continue;
            }
            if (Util.contains(master1.jobDone, fileNameOnly)) {
                System.out.println(fileNameOnly + " finished. Skip.");
                continue;
            }

            //check idle nodes and get first one
            File[] idleNodes = new File(idleDir).listFiles();
            boolean doneErrant = false;
            while (idleNodes.length == 0) {
                if (!doneErrant) {
                    //take the chance to move the output back
                    System.out.println("No deamons available. Do my own job and ~~~");
                    master1.moveFiles(master1.tmpDir, master1.fileServer + ":" + master1.outputDir, "txt");
                    doneErrant = true;
                } else {
                    System.out.print("*");
                }

                try {
                    Thread.sleep(10000); //sleeps for 30 sec
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                idleNodes = new File(idleDir).listFiles();
            }

            if (idleNodes[0].toString().endsWith(".master")) {
                master1.wakeup(idleDir);
                //backup
                i--;
                continue;
            }

            //fetch //assign tasks
            System.out.println("Assigning file " + fileNameOnly + " to " + idleNodes[0]);
            String fromFile = master1.fileServer + ":" + master1.sourceDir + File.separator + master1.getRealFilePath(fileNameOnly);
            String toFile = master1.tmpDir + File.separator + fileNameOnly;
            toFile += "." + idleNodes[0].getName();
            master1.cpFile(fromFile, toFile);
            try {
                System.out.println("Waiting for " + idleNodes[0] + " to begin the job..");
                Thread.sleep(20000 / idleNodes.length); //sleeps for twice longer as the deamons check for tasks
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            //update DoneList
            // Todo: make a list from output dir as update
            updateDoneList = fileNameOnly;
            for (int j = 0; j < master1.jobDone.length; j++) {
                updateDoneList += " " + master1.jobDone[j];
            }
            master1.jobDone = updateDoneList.split(" ");
            //and update jobDoneList
            if (updateDoneList != null) {
                Util.write(master1.workDir + File.separator + "jobDone", updateDoneList);
            }


        }

        System.out.println("Job done!");
        master1.sleep(idleDir);
        System.exit(0);

    }

}