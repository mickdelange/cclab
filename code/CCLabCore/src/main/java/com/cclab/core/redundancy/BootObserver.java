package com.cclab.core.redundancy;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.cclab.core.AwsConnect;
import com.cclab.core.utils.NodeLogger;

/**
 * Thread that initialises a machine when it finished booting.
 * @author Mick de Lange
 */
public class BootObserver extends Thread {

    int interval;
    String jarPath;
    String pemPath;
    String instanceId;
    String settings;

    private boolean shouldExit = false;

    public BootObserver(String instId, String sett) {
        instanceId = instId;
        settings = sett;
        if (!loadProperties()) {
            // Something went wrong loading properties, set to default
            interval = 2000;
            jarPath = "";
            pemPath = "";
        }
    }

    /**
     * Run the checker
     */
    public void run() {
        String currState;
        try {
            while(true) {
                if(shouldExit) // Stop the loop.
                    break;

                Thread.sleep(interval);

                currState = AwsConnect.getInstanceState(instanceId);
                if (currState.equals("running")) {
                    //  Send the command, quit if successful
                    if (sendCommand())
                        quit(); // Work is done
                }
            }
        } catch (InterruptedException e) {
            NodeLogger.get().error(e.getMessage(), e);
        }
    }

    /**
     * Send SSH command to run jar on instance.
     */
    private boolean sendCommand() {
        JSch jsch = new JSch();
        String ip = AwsConnect.getInstancePrivIP(instanceId);

        try
        {
            jsch.addIdentity(pemPath);
            Session session = jsch.getSession("ubuntu", ip, 22);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            String command = "java -jar " + jarPath + " " + settings;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] tmp = new byte[1024];
            while (true)
            {
                while (in.available() > 0)
                {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0)
                        break;
                    NodeLogger.get().info(new String(tmp, 0, i));
                }
                if (channel.isClosed())
                {
                    NodeLogger.get().info("exit-status: " + channel.getExitStatus());
                    break;
                }
                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception ee)
                {
                    NodeLogger.get().error(ee.getMessage());
                }
            }

            channel.disconnect();
            session.disconnect();

            return true;
        }
        catch (Exception e)
        {
            NodeLogger.get().error(e.getMessage());
        }
        return false;
    }

    /**
     * Get the properties from the config file.
     * @return True if succeeded, False otherwise.
     */
    public boolean loadProperties() {
        Properties prop = new Properties();

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("observer.properties");

            if (inputStream != null) {
                prop.load(inputStream);
                interval = Integer.parseInt(prop.getProperty("bootupInterval"));
                jarPath = prop.getProperty("jarPath");
                pemPath = prop.getProperty("pemPath");
                return true;
            } else {
                NodeLogger.get().error("Observer properties file not found");
                return false;
            }
        } catch (IOException e) {
            NodeLogger.get().error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kill observer thread.
     */
    public void quit(){
        shouldExit = true;
    }

}
