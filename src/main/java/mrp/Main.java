package mrp;

import mrp.server.HttpServer;

public class Main {

    public static void main(String[] args) {
        int port = 9090;
        HttpServer server = new HttpServer(port);
        server.start();
    }
}