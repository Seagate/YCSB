package com.yahoo.ycsb.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import kinetic.client.ClientConfiguration;
import kinetic.client.KineticException;
import kinetic.client.advanced.AdvancedKineticClient;
import kinetic.client.advanced.AdvancedKineticClientFactory;

public class KineticConnectionPool {
    private Map<String, List<AdvancedKineticClient>> connectionPool;
    private Map<String, AtomicLong> operationCounter;
    private int connectionPerNode;
    private boolean inited = false;

    public KineticConnectionPool() {
    }

    public void init(List<String> nodes, int connectionPerNode) {
        if (nodes == null || nodes.size() <= 0 || connectionPerNode <= 0) {
            return;
        }

        this.connectionPerNode = connectionPerNode;
        connectionPool = new HashMap<String, List<AdvancedKineticClient>>();
        operationCounter = new HashMap<String, AtomicLong>();

        ClientConfiguration clientConfig;
        String[] hostAndPort;
        String host;
        int port;

        for (String node : nodes) {
            hostAndPort = node.split(":");
            host = hostAndPort[0];
            port = Integer.parseInt(hostAndPort[1]);

            List<AdvancedKineticClient> connectionGroup = new ArrayList<AdvancedKineticClient>();
            connectionPool.put(node, connectionGroup);
            operationCounter.put(node, new AtomicLong());

            clientConfig = new ClientConfiguration();
            clientConfig.setHost(host);
            clientConfig.setPort(port);

            for (int i = 0; i < connectionPerNode; i++) {
                try {
                    connectionGroup.add(AdvancedKineticClientFactory
                            .createAdvancedClientInstance(clientConfig));
                } catch (KineticException e) {
                    e.printStackTrace();
                }
            }
        }

        inited = true;
    }

    public AdvancedKineticClient getKineticClientByKey(String node) {
        if (!inited) {
            throw new RuntimeException("Kinetic connection pool is initiated.");
        }

        int nextConnection = (int) (operationCounter.get(node)
                .getAndIncrement() % connectionPerNode);
        return connectionPool.get(node).get(nextConnection);

        // return
        // connectionPool.get(node).get(random.nextInt(connectionPerNode));
    }

    public void destroy() throws KineticException {
        if (!inited) {
            return;
        }

        for (String node : connectionPool.keySet()) {
            for (AdvancedKineticClient client : connectionPool.get(node)) {
                client.close();
            }
        }
    }

    public boolean inited() {
        return inited;
    }
}
