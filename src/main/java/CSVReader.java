import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class CSVReader {
    public static void main(String[] args) {
        String pathToCsv = "service-names-port-numbers.csv";
        String line;

        Map<Integer, String> portMap = new HashMap<>();
        TreeSet<Integer> sortedKeys = new TreeSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(pathToCsv))) {
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",");

                if (columns.length >= 4) {
                    String keyString = columns[1];
                    String value = columns[3];

                    try {
                        int key = Integer.parseInt(keyString);

                        if (!portMap.containsKey(key)) {
                            portMap.put(key, value);
                            sortedKeys.add(key);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
    }

        Jedis jedis = null;
        try {
            jedis = new Jedis("localhost");

            Set<Integer> openPorts = scanOpenPorts(portMap.keySet());

            System.out.println("Open Ports Descriptions:");

            for (Integer key : sortedKeys) {
                if (openPorts.contains(key)) {
                    String description = portMap.get(key);
                    System.out.println("Port: " + key + ", Description: " + description);
                }
            }

        } catch (JedisConnectionException e) {
            System.out.println("Could not connect to Redis: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Issue: " + e.getMessage());
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
    private static Set<Integer> scanOpenPorts(Set<Integer> ports) {
        Set<Integer> openPorts = new TreeSet<>();

        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", port), 1000);
                openPorts.add(port);
            } catch (IOException e) {
            }
        }

        return openPorts;
    }
}