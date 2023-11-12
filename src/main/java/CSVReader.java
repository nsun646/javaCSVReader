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

            Map<Integer, String> openPortsMap = scanOpenPorts(portMap.keySet());

            for (Map.Entry<Integer, String> entry : openPortsMap.entrySet()) {
                jedis.set(entry.getKey().toString(), entry.getValue());
            }

            for (Integer key : sortedKeys) {
                String value = jedis.get(key.toString());
                System.out.println("Key: " + key + ", Value: " + value);
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
    private static Map<Integer, String> scanOpenPorts(Set<Integer> ports) {
        Map<Integer, String> openPortsMap = new HashMap<>();

        String targetHost = "localhost";
        int minPort = 1;
        int maxPort = 65535;

        for (int port = minPort; port <= maxPort; port++) {
            try {
                Socket socket = new Socket(targetHost, port);
                openPortsMap.put(port, "Open Port");
                socket.close();
            } catch (IOException e) {
                // Port is likely closed or unreachable
            }
        }
        return openPortsMap;
    }
}