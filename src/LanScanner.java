import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LanScanner {
    static ArrayList<InetAddress> inetAddresses = new ArrayList<>(255);

    static Map<InetAddress, SortedSet<Integer>> portsIP = new HashMap<>();
    static final int TIMEOUT = 10;
    static final int THREAD_POOL = 100;

    /*
    * Д/з-курсовая 2: Реализовать приложение сканер сети, приложение должно на вход получать ip адрес и проводить
    * сканирование сети, находить активные устройства и получать список доступных портов. Информацию выводить
    * в консоль и запрашивать у пользователя сохранение в файл формата txt или csv.
    * */
    public static void main(String[] args) throws IOException {
        String mask = args[0].substring(0, args[0].lastIndexOf(".") + 1) ;
        for (int i = 0; i < 255; i++) {
            scanIP(mask + i, i);
        }

        for (InetAddress a : inetAddresses) {
            SortedSet<Integer> ports = scanPort(a);
            portsIP.put(a, ports);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("В каком формате сохранить результат сканирования\n1. txt\n2. csv");
        String format = (scanner.nextInt() == 1) ? "txt" : "csv";
        File file = new File("lan_scan." + format);
        FileWriter writer = new FileWriter(file);
        for (Map.Entry<InetAddress, SortedSet<Integer>> entry : portsIP.entrySet()){
            writer.append(entry.getKey().getHostAddress() + "\n");
            String ports = "";
            for (int port : entry.getValue()) {
                ports += port + " ";
            }
            writer.append(ports + "\n");
        };
        writer.flush();
    }

    static synchronized void addAddress(InetAddress addresses){
        inetAddresses.add(addresses);
    }
    static synchronized void addPort(int port, SortedSet<Integer> ports) {
        ports.add(port);
    }
    static void scanIP(String ip, int i) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ip);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(address.isReachable(TIMEOUT)) {
                        addAddress(address);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "T" + i);
        thread.start();
    }
    static SortedSet<Integer> scanPort(InetAddress a) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL);
        SortedSet<Integer> ports = new TreeSet<>();
        for (int i = 0; i < 65535; i++) {
            final int port = i;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(a, port), TIMEOUT);
                    addPort(port, ports);
                } catch (IOException e) {
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(100, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ports;
    }
}
