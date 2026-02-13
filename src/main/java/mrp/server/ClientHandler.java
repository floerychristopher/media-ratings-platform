package mrp.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                System.out.println(line);
            }

            // Response senden
            String responseBody = "Hello from my REST server!";
            String response =
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + responseBody.length() + "\r\n" +
                            "\r\n" +
                            responseBody;

            out.write(response);
            out.flush();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
