package computerInterface;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.MouseEvent;

public class Controller {

    @FXML
    private ChoiceBox<String> list_ports;

    private SerialPort[] serialPorts;
    private SerialPort selectedPort;

    private int readData = 000;


    @FXML
    public void initialize() {

        int numberOfPorts = SerialPort.getCommPorts().length;
        serialPorts = SerialPort.getCommPorts();
        for (int i = 0; i < numberOfPorts; i++) {
            list_ports.getItems().add(SerialPort.getCommPorts()[i].getSystemPortName());
        }
        list_ports.getSelectionModel().selectFirst();
    }

    public void connect(MouseEvent mouseEvent) {
        int selectedPortIndex = list_ports.getSelectionModel().getSelectedIndex();
        selectedPort = serialPorts[selectedPortIndex];
        selectedPort.setComPortParameters(2400, 8, 1, 0);

        selectedPort.addDataListener(new SerialPortDataListener() {

            int numberOfCharacters = 0;
            String buffer = "";

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                if (serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;
                byte[] newData = new byte[selectedPort.bytesAvailable()];
                selectedPort.readBytes(newData, newData.length);
                String in = new String(newData);
                try {
                    if (numberOfCharacters < 3) {
                        buffer = buffer + Integer.parseInt(in);
                        numberOfCharacters++;
                    } else {
                        numberOfCharacters = 0;
                        readData = Integer.parseInt(buffer);
                        buffer = "";
                        System.out.println(readData);
                    }
                } catch (NumberFormatException e) {
                } catch (NullPointerException e) {
                }
            }
        });


        new Thread(new Task() {
            @Override
            protected Object call() {
                selectedPort.openPort();
                sleep();
                write("@W36240;");      //Port B first 4 input and second 4 output


                while (true) {
                    write("@R35000;");
                    int out = 0;

                    if ((readData & 1) == 0)
                        out = out | 1 << 4;
                    else out = out & ~(1 << 4);

                    if ((readData & 2) == 0)
                        out = out | (1 << 5);
                    else out = out & ~(1 << 5);

                    if ((readData & 4) == 0)
                        out = out | (1 << 6);
                    else out = out & ~(1 << 6);

                    if ((readData & 8) == 0)
                        out = out | (1 << 7);
                    else out = out & ~(1 << 7);

                    System.out.println("@W37" + String.format("%03d", out) + ";");
                    write("@W37" + String.format("%03d", out) + ";");
                }
            }

            public void write(String string) {
                selectedPort.writeBytes(string.getBytes(), string.getBytes().length);
                sleep();
            }

            public void sleep() {
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void disconnect(MouseEvent mouseEvent) {
        selectedPort.closePort();
    }
}