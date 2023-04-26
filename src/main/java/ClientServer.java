import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class ClientServer {
    private static final String SETTINGS_FILE = "settings.txt";
    private static String serverAddress;
    private static int serverPort;
    private static String name;

    public static void main(String[] args) {
        readSettings();

        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Успешно подключился к серверу.");
            System.out.print("Ваше имя: ");
            name = scanner.nextLine();
            writer.println(name);

            System.out.println("Добро пожаловать в чат, " + name + "!");

            Thread messageReceiver = new Thread(new MessageReceiver(reader));
            messageReceiver.start();

            while (true) {
                String message = readMessage(scanner);
                if (message == null) {
                    break;
                }
                sendMessage(writer, message);
            }

            scanner.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static String readMessage(Scanner scanner) {
        String message = scanner.nextLine();
        if ("/exit".equals(message) || "ВЫХОД".equals(message)) {
            return null;
        }
        return message;
    }

    private static void sendMessage(PrintWriter writer, String message) {
        Date date = new Date();
        String formattedDate = new SimpleDateFormat("HH:mm:ss").format(date);
        writer.println("[" + formattedDate + "] " + "(" + name + ") " + ": " + message);
    }

    private static void readSettings() {
        try (Scanner scanner = new Scanner(new File(SETTINGS_FILE))) {
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split("=");
                if (line[0].equals("port")) {
                    serverPort = Integer.parseInt(line[1]);
                } else if (line[0].equals("host")) {
                    serverAddress = line[1];
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Settings file not found.");
            System.exit(1);
        }
    }

    static class MessageReceiver implements Runnable {
        private BufferedReader reader;

        MessageReceiver(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = reader.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Error receiving message: " + e.getMessage());
            }
        }
    }
}
