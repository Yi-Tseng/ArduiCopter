/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quadcontroller;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;

/**
 *
 * @author takeshi
 */
public class FXMLDocumentController implements Initializable, SerialPortEventListener {

    SerialPort serialPort;

    private BufferedReader input;

    private OutputStream output;

    private static final String PORT_NAMES[] = {
        "/dev/tty.usbmodem1421",
        "/dev/tty.usbmodem1411",
        "/dev/tty.wch ch341 USB=>RS232 1420"
    };

    @FXML
    ProgressBar pgGyX;
    @FXML
    ProgressBar pgGyY;
    @FXML
    ProgressBar pgGyZ;
    @FXML
    ProgressBar pgAcX;
    @FXML
    ProgressBar pgAcY;
    @FXML
    ProgressBar pgAcZ;

    @FXML
    ProgressBar pgMt01;
    @FXML
    ProgressBar pgMt02;
    @FXML
    ProgressBar pgMt03;
    @FXML
    ProgressBar pgMt04;

    @FXML
    Slider slMt01;
    @FXML
    Slider slMt02;
    @FXML
    Slider slMt03;
    @FXML
    Slider slMt04;
    @FXML
    Slider slMtAvg;
    
    @FXML
    Text lbMt01;
    
    @FXML
    Text lbMt02;
    
    @FXML
    Text lbMt03;
    
    @FXML
    Text lbMt04;

    double[] initialAcce;

    int[] controlTh = new int[4];
    
    boolean avgTern;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
//        System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/tty.usbmodem1421");
//        System.setProperty("gnu.io.rxtx.SerialPorts", "tty.wch ch341 USB=>RS232 1420");

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        avgTern = false;
        
        
        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
//            System.out.println("port name : " + currPortId.getName());
            for (String portName : PORT_NAMES) {
                
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }
        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(),
                    2000);

            // set port parameters
            serialPort.setSerialPortParams(9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            for (int c = 0; c < 4; c++) {
                controlTh[c] = 20;
            }

            slMt01.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                    controlTh[0] = newValue.intValue();
//                    lbMt01.setText(controlTh[0]+"");
//                    double avg = (controlTh[0] + controlTh[1] + controlTh[2] + controlTh[3]) / 4;
//                    slMtAvg.valueProperty().set(avg);
                    updateControlTh();
                }
            });

            slMt02.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                    controlTh[1] = newValue.intValue();
//                    lbMt02.setText(controlTh[1]+"");
//                    double avg = (controlTh[0] + controlTh[1] + controlTh[2] + controlTh[3]) / 4;
//                    slMtAvg.valueProperty().set(avg);
                    updateControlTh();
                }
            });

            slMt03.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                    controlTh[2] = newValue.intValue();
//                    lbMt03.setText(controlTh[2]+"");
//                    double avg = (controlTh[0] + controlTh[1] + controlTh[2] + controlTh[3]) / 4;
//                    slMtAvg.valueProperty().set(avg);
                    updateControlTh();
                }
            });

            slMt04.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                    controlTh[3] = newValue.intValue();
//                    lbMt04.setText(controlTh[3]+"");
//                    double avg = (controlTh[0] + controlTh[1] + controlTh[2] + controlTh[3]) / 4;
//                    slMtAvg.valueProperty().set(avg);
                    updateControlTh();
                }
            });

            
            ControlThread controlThread = new ControlThread();
            controlThread.start();
                    
            
            slMtAvg.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                    
                    
                    slMt01.valueProperty().set(slMt01.getValue() + newValue.intValue() - oldValue.intValue());
                    slMt02.valueProperty().set(slMt02.getValue() + newValue.intValue() - oldValue.intValue());
                    slMt03.valueProperty().set(slMt03.getValue() + newValue.intValue() - oldValue.intValue());
                    slMt04.valueProperty().set(slMt04.getValue() + newValue.intValue() - oldValue.intValue());
                    
                    
                    
                }
            });
            
            
        } catch (PortInUseException | UnsupportedCommOperationException | IOException | TooManyListenersException e) {
            System.err.println(e.toString());
        }
    }

    public void updateControlTh() {
        byte[] th = new byte[4];

        for (int c = 0; c < 4; c++) {
            th[c] = (byte) controlTh[c];
        }
        sendMessage(th);

    }

    public void sendMessage(byte[] message) {
        synchronized (output) {
            try {
                output.write(message);
                output.flush();
            } catch (IOException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void serialEvent(SerialPortEvent spe) {
        
        
        
        if (spe.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                String inputLine = input.readLine();
                System.out.println("Input " + inputLine);
                if (inputLine.equals("initialize!")) {
                    return;
                } else if (inputLine.equals("ready to start")) {
                    inputLine = input.readLine();
                    String[] sps = inputLine.split(",");
                    
                    double[] acce = new double[3];
                    
                    for (int c = 0; c < 3; c++) {
                        acce[c] = Double.parseDouble(sps[c + 4]);
                    }
                    
                    initialAcce = new double[3];
                    System.arraycopy(acce, 0, initialAcce, 0, 3);

                    return;
                } else if (inputLine.length() < 15) {
                    return;
                }
                // throttle0,throttle1,throttle2,throttle3,accex,accey,accez,gyrox,gyroy,gyroz,magx,magy,magz,pressure,tempture
                String[] sps = inputLine.split(",");
                int[] th = new int[4];
                for (int c = 0; c < 4; c++) {
                    th[c] = Integer.parseInt(sps[c]);
                }

                pgMt01.progressProperty().set(th[0] / 180.0);
                pgMt02.progressProperty().set(th[1] / 180.0);
                pgMt03.progressProperty().set(th[2] / 180.0);
                pgMt04.progressProperty().set(th[3] / 180.0);
                
                lbMt01.textProperty().set("" + th[0] / 180.0);
                lbMt02.textProperty().set("" + th[1] / 180.0);
                lbMt03.textProperty().set("" + th[2] / 180.0);
                lbMt04.textProperty().set("" + th[3] / 180.0);

                final double[] acce = new double[3];
                final double[] gyro = new double[3];

                for (int c = 0; c < 3; c++) {
                    acce[c] = Double.parseDouble(sps[c + 4]) - initialAcce[c];
                }

                for (int c = 0; c < 3; c++) {
                    gyro[c] = Double.parseDouble(sps[c + 7]);
                }

                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {

                        pgAcX.setProgress(Math.abs(acce[0]));
                        pgAcY.setProgress(Math.abs(acce[1]));
                        pgAcZ.setProgress(Math.abs(acce[2]));

                        pgGyX.setProgress(Math.abs(gyro[0]) / 360);
                        pgGyY.setProgress(Math.abs(gyro[1]) / 360);
                        pgGyZ.setProgress(Math.abs(gyro[2]) / 360);
                    }
                });

            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
    }

    class ControlThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    updateControlTh();
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

    }

}
