import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class WebsocketSimInterface extends WebSocketClient {
    private Robot robot;
    public boolean is_connected;
    
	public WebsocketSimInterface(URI serverUri, Draft draft) {
		super(serverUri, draft);
    }

	public WebsocketSimInterface(URI serverURI, Robot robot) {
		super(serverURI);
        this.robot = robot;
	}

	public void onOpen(ServerHandshake handshakedata) {
        System.out.println("new connection opened");
        this.is_connected = true;
	}

	public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    public void onMessage(String message) {
        // System.out.println("recieved a text message");
        decode(message);
    }
    
    @Override
	public void onMessage(ByteBuffer message) {
        // System.out.println("received ByteBuffer");
        decode(new String(message.array()));
    }
    
    private void decode(String message) {
        try {
            // System.out.println(message);
            String chars = "[](){}$%^&*!@#;'/";
            for (int i = 0; i < chars.length(); i ++) {
                message = message.replace(Character.toString(chars.charAt(i)), "");
            }
            String[] parts = message.split(", ");
            // System.out.println("sensor values parsed from packet");
            float[] values = new float[] {Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])};
            robot.recieve_sensor_update(values); 
        }
        catch(NumberFormatException e) {
            System.out.println("received a corrupted packet: " + message);
            request_sensor_update();
        }
    }

	public void onError(Exception ex) {
        System.err.println("an error occurred: " + ex);
        ex.printStackTrace();
    }
    
    public void send_wheel_values(int wheel, float engine_force, float brake) {
        System.out.println("sending wheel value");
        send("wheel " + wheel + " " + engine_force + " " + brake);
    }

    public void request_sensor_update() {
        if (this.is_connected) {
            // System.out.println("requesting sensor update");
            send("sensors");
        }
        else {
            System.out.println("tried to request sensor update but is not connected, retrying");
            request_sensor_update();
        }
    }
}