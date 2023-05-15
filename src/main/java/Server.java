import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int DEFAULT_PORT = 1234;
    private static final String SETTINGS_FILE = "settings.txt";
    private static final String LOG_FILE = "file.log";
    private static int port;
    private List<ClientHandler> clients = new ArrayList<>();
    private PrintWriter logWriter;

    public Server() {
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(SETTINGS_FILE);
            props.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
        try {
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Сервер запустился за порту: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void broadcast(String message, ClientHandler clientHandler) {
        System.out.println(message);
        logWriter.println(message);
        for (ClientHandler handler : clients) {
            if (handler != clientHandler) {
                handler.sendMessage(message);
            }
        }
    }

    private synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String name;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                name = reader.readLine();
                broadcast(name + " зашел в чат.", this);

                new Thread(() -> {
                    Scanner scanner = new Scanner(System.in);
                    while (true) {
                        String messageAll = scanner.nextLine();
                        String fullMessage = "Сервер: " + messageAll;
                        sendMessage(fullMessage);
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = reader.readLine()) != null) {
                    broadcast(message, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            removeClient(this);
            broadcast(name + " вышел из чата.", this);
            if (clients.isEmpty()){
                logWriter.close();
            }
        }


        public void sendMessage(String message) {
            writer.println(message);
            writer.flush();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}