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
    private Task task;
    private Thread thread;


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
                    }
                } catch (NumberFormatException e) {
                } catch (NullPointerException e) {
                }
            }
        });


        task = new Task() {
            @Override
            protected Object call() {
                selectedPort.openPort();
                try {
                    sleep();
                    write("@W36240;");      //Port B first 4 input and second 4 output

                } catch (InterruptedException e) {
                    return null;
                }
                while (!isCancelled()) {
                    try {
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

                        write("@W37" + String.format("%03d", out) + ";");
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                return null;
            }

            public void write(String string) throws InterruptedException {
                selectedPort.writeBytes(string.getBytes(), string.getBytes().length);
                sleep();
            }

            public void sleep() throws InterruptedException {

                Thread.sleep(60);

            }
        };

        thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void disconnect(MouseEvent mouseEvent) {
        selectedPort.closePort();
        task.cancel();
    }
}