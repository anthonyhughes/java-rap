package com.datalanguage.javarap;


import com.datalanguage.javarap.utils.Util;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * <p>Title: Daemon to delim the aquaint corpus.</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0
 */

public class Daemon {
    String homeDir = System.getProperty("workPath");
    String outputDir = System.getProperty("outputDir");
    String workDir = homeDir + "/status";//the folder where information for both the master and daemon is keeped.

    boolean keepWorking = true;
    String busyDir = null;
    String idleDir = null;
    String daemonID = null;

    public Daemon(String id, String dir) {
        workDir = dir;
        busyDir = workDir + File.separator + "busy";
        idleDir = workDir + File.separator + "idle";
        daemonID = id;
    }

    public Daemon(String id) {
        busyDir = workDir + File.separator + "busy";
        idleDir = workDir + File.separator + "idle";
        daemonID = id;
        System.out.println(daemonID + " comes into being!");
    }


    private boolean registerIdle() {
        try {
            new File(busyDir + File.separator + daemonID).delete();
            Util.errLog("Daemon " + daemonID + " registers idle.");
            String hh = idleDir + File.separator + daemonID;
            return new File(idleDir + File.separator + daemonID).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }

    private boolean registerBusy() {
        try {
            new File(idleDir + File.separator + daemonID).delete();
            Util.errLog("Daemon " + daemonID + " registers busy/unavailable.");
            return new File(busyDir + File.separator + daemonID).createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }

    private String checkTask() {
        return Util.checkFile(outputDir, daemonID);
    }

    //exit if master is idle
    private void checkMaster() {
        if (Util.checkFile(idleDir, "master") != null) {
            System.out.println("Master probably dead. Exists...");
            registerBusy();
            System.exit(0);
        }
    }

    public boolean process(String inputFileName) {
        boolean jobdone = false;
        if (inputFileName == null) {
            return jobdone;
        }
        //make alias
        String fileName = inputFileName.substring(0, inputFileName.lastIndexOf(daemonID) - 1);

        if (fileName.endsWith(".GZ")) {
            String outputFilePrefix = outputDir
                    + fileName.substring(fileName.lastIndexOf(File.separator),
                    fileName.lastIndexOf(".GZ"));

            if (!(new File(outputFilePrefix + ".txt").exists())) {
                Util.errLog(new java.util.Date() + ": " + fileName +
                        " decompressing file....");
                TRECFile trecF = new TRECFile(inputFileName, outputFilePrefix + ".tmp");

                new File(inputFileName).delete();

                Util.errLog(new java.util.Date() + ": " + " removing tag....");
                String s = trecF.removeTag();

                //Todo : elimit the need of this tmp file
                new File(outputFilePrefix + ".tmp").delete();


                PlainText plainText1 = new PlainText(new StringBuffer(s));
                Util.errLog(new java.util.Date() + ": " + " tagging sentences....");

                Util.write(outputFilePrefix + ".txt", plainText1.addQuote(false));
                jobdone = true;
            }
        }
        return jobdone;
    }

    public void finalize() {
        System.out.println(daemonID + " cleans up.");
        registerIdle();
    }


    public static void main(String[] args) {
        Env env = new Env();
        //get hostname
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }

        Daemon daemon1 = new Daemon(localhost.getHostName());
        String inputFileName = null;


        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                daemon1.keepWorking = false;
                System.exit(1);
            }
        }


        while (true) {
            //register
            if (!daemon1.keepWorking) {
                daemon1.registerBusy();
                System.out.println(daemon1.daemonID + " quits.");
                System.exit(1);
            }
            if (!daemon1.registerIdle()) {
                System.out.println("Can not register. Daemon " + daemon1.daemonID +
                        " exits.");
                System.exit(0);
            }

            // check task
            while ((inputFileName = daemon1.checkTask()) == null) {
                daemon1.checkMaster();
                try {
                    System.out.println("Deamon rests for a while...(" + daemon1.daemonID + ")");
                    Thread.sleep(10000); //sleeps for 1 min
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // unregister
            daemon1.registerBusy();

            //process
            if (!daemon1.process(inputFileName)) {
                new File(inputFileName).delete();
            }
        }
    }

}
