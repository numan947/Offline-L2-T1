package main;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.Date;
import java.util.Optional;

/**
 * Created by numan947 on 3/5/17.
 **/
public class MainNetworkThread implements Runnable {
    private Client_GUI_Controller controller;
    private String examCode;
    private String studentID;
    private String ipAddress;
    private int port;
    private byte[]buff=null;
    private File fileLocation;

    public MainNetworkThread(Client_GUI_Controller controller, String examCode, String studentID, String ipAddress, int port) {
        this.studentID = studentID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.examCode=examCode;
        this.controller=controller;
        buff=new byte[8192];



        Thread t=new Thread(this);
        t.setDaemon(true);
        t.start();

    }

    @Override
    public void run() {
        int cnt;
        String msg;
        NetworkUtil networkUtil=null;
        try {
            networkUtil = new NetworkUtil(ipAddress, port);

            //send requestType
            networkUtil.writeBuff("NEW_CONNECTION".getBytes());
            networkUtil.flushStream();

            cnt=networkUtil.readBuff(buff);
            msg=new String(buff,0,cnt);

            if(msg.equals("SEND_ADDITIONAL_INFO")){


                //send student id and exam code; $$$$ is our separator for multiple string information
                String data=examCode+"$$$$"+studentID;
                networkUtil.writeBuff(data.getBytes());
                networkUtil.flushStream();



                //reading server's response
                cnt=networkUtil.readBuff(buff);
                msg=new String(buff,0,cnt);

                //if response is "APPROVED"
                if(msg.equals("APPROVED")) {

                    //send msg for size of the details string
                    networkUtil.writeBuff("REQUESTING_SIZE_OF_DETAILS_STRING".getBytes());
                    networkUtil.flushStream();

                    //now we wait for the server to send the size of the details
                    cnt = networkUtil.readBuff(buff);
                    msg = new String(buff, 0, cnt);

                    int sizeOfDetails = Integer.parseInt(msg);

                    //send acknowledgement of receiving the size
                    networkUtil.writeBuff("SIZE_RECEIVED".getBytes());
                    networkUtil.flushStream();

                    //after this we'll start receiving a big msg, so, to read we make another method
                    cnt=networkUtil.readBuff(buff);
                    String fullmsg=new String(buff,0,cnt);

                    //now we decode the fullmsg:

                    //set the basic info appropriatelys

                    controller.setAll(fullmsg);

                    //todo may be we'll need clean up here
                    String[] info = fullmsg.split("\\$\\$\\$\\$");
                    System.out.println(fullmsg);

                    //parsed all the basic info
                    if(info.length>=6) {
                        String e_c = info[0];
                        String examName = info[1];
                        int duration = Integer.parseInt(info[2]);
                        int backupInterval = Integer.parseInt(info[3]);
                        int warningTime = Integer.parseInt(info[4]);
                        long startTimeInLong = Long.parseLong(info[5]);
                        Date startTime = new Date(startTimeInLong);
                    }
                    networkUtil.writeBuff("DATA_RECEIVED".getBytes());
                    networkUtil.flushStream();

                    cnt=networkUtil.readBuff(buff);

                    msg=new String(buff,0,cnt);

                    System.out.println(msg);

                    String[]tmp=msg.split("\\$\\$\\$\\$");
                    String fileName=tmp[0];
                    long fileSize= Long.parseLong(tmp[1]);

                    networkUtil.writeBuff("SEND_QUESTION".getBytes());
                    networkUtil.flushStream();

                    int totalRead=0;
                    boolean corrupted=false;


                    String path=System.getProperty("user.home")+File.separator+fileName;
                    System.out.println(path);
                    File fileToSave=new File(path);

                    if(!fileToSave.exists())fileToSave.createNewFile();


                    BufferedOutputStream fbuff=new BufferedOutputStream(new FileOutputStream(fileToSave));
                    while (true) {
                        if (cnt >= fileSize||totalRead==-1) break;
                        totalRead = networkUtil.readBuff(buff);
                        cnt += totalRead;
                        writeToFile(buff, totalRead,cnt,fbuff);
                        System.out.println(fileSize + "  " + totalRead);
                    }
                    if(cnt < fileSize||totalRead==-1){
                        System.out.println("Problem while transferring file..probably file's become corrupted");
                        corrupted=true;
                        fileToSave.delete();
                    }
                    if(!corrupted){
                        networkUtil.writeBuff("QUESTION_RECEIVED".getBytes());
                        networkUtil.flushStream();
                    }
                    fbuff.close();

                    //show alertDialog to save the file
                    fileLocation=null;
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Question Received");
                        alert.setHeaderText("Let the game of thrones begin :)");
                        alert.setContentText("You MUST SAVE the Question");

                        ButtonType buttonTypeOne = new ButtonType("SaveFile");
                        alert.getButtonTypes().setAll(buttonTypeOne);
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.get() == buttonTypeOne){
                            FileChooser fc=new FileChooser();
                            fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("doc only :)","doc"));
                            fileLocation=fc.showSaveDialog(controller.getInitiator().getMainStage());
                        }
                        //alert.showAndWait();
                        System.out.println(fileLocation.getName());
                        notifyAll();
                    });

                    wait();

                    //move the file
                    InputStream is = new FileInputStream(fileToSave);
                    OutputStream os = new FileOutputStream(fileLocation);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                    is.close();
                    os.close();



                    //now we wait for any correction or something :)

                    while(true){
                        networkUtil.readBuff(buff);



                    }













                }
                else{

                }

            }else{
                //todo what'll happen if the server sends something else?
            }

        } catch (IOException | InterruptedException e) {
            //todo show with alertDialog?
            e.printStackTrace();
        }


    }

    private String readNBytes(long sizeOfDetails, NetworkUtil networkUtil) throws IOException {

        System.out.println("HELLO WORLD");
        int cnt=0;
        int tmp=0;
        StringBuilder toReturn=new StringBuilder();
        while(cnt>=sizeOfDetails||tmp==-1){
            tmp=networkUtil.readBuff(buff);
            cnt+=tmp;
            toReturn.append(new String(buff,0,tmp));
            System.out.println(toReturn);
        }
        return toReturn.toString();
    }

    private void writeToFile(byte[] b, int totalRead, int cnt, BufferedOutputStream fbuff)
    {
        try {
            fbuff.write(b,0,totalRead);
            if(cnt>=8192)fbuff.flush();
        } catch (IOException e) {
            System.out.println("Exception In ServerPackage.FileProcessingThread.writeToFile "+e.getMessage());
        }
    }
}
