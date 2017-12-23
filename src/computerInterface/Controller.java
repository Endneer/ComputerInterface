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
                String out = new String(newData);
                try {
                    System.out.print(Integer.parseInt(out));
                } catch (NumberFormatException e) {
                } catch (NullPointerException e) {
                }
            }
        });


        new Thread(new Task() {
            @Override
            protected Object call() throws Exception {
                selectedPort.openPort();
                sleep();
                write("@W39223;");
                write("@W40008;");
                write("@W36000");

                while (true) {
                    write("@R35000");
                }
            }

            public void write(String string) {
                selectedPort.writeBytes(string.getBytes(), string.getBytes().length);
                sleep();
            }

            public void sleep() {
                try {
                    Thread.sleep(50);
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