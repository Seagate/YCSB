package com.yahoo.ycsb.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import kinetic.client.Entry;
import kinetic.client.KineticException;
import kinetic.client.advanced.AdvancedKineticClient;
import kinetic.client.advanced.PersistOption;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.StringByteIterator;

public class KineticClusterDB extends DB {

    public static final int OK = 0;
    public static final int ERROR = -1;
    public static final int NOT_FOUND = -2;

    private static KineticConnectionPool kineticConnectionPool = new KineticConnectionPool();
    private static List<String> nodeList = new ArrayList<String>();
    private static int connectionPerNode = 0;
    private static int rangePerNode = 0;
    private static int totalNodes = 0;
    private static AtomicInteger instances = new AtomicInteger();

    private void init_connection_pool() {
        String hosts = getProperties().getProperty("hosts", "localhost:8123");

        connectionPerNode = Integer.parseInt(getProperties().getProperty(
                "connectionpernode", "1"));

        System.out.println("hosts=" + hosts);
        System.out.println("connection=" + connectionPerNode);

        String[] nodes = hosts.split(";");

        for (String node : nodes) {
            if (node.isEmpty()) {
                continue;
            }

            nodeList.add(node);
            totalNodes++;
        }

        rangePerNode = Integer.MAX_VALUE / totalNodes;

        kineticConnectionPool.init(nodeList, connectionPerNode);
    }

    @Override
    public void init() {
        instances.getAndIncrement();
        if (!kineticConnectionPool.inited()) {
            synchronized (kineticConnectionPool) {
                if (!kineticConnectionPool.inited()) {
                    System.out.println("Connection pool has been initiated.");
                    init_connection_pool();
                }
            }
        }
    }

    private int hashOfKey(byte[] key) {
        Checksum checksum = new CRC32();
        checksum.update(key, 0, key.length);

        int crc32 = (int) checksum.getValue();
        crc32 = crc32 >= 0 ? crc32 : (0 - crc32);
        return crc32;
    }

    private AdvancedKineticClient getKineticClientByKey(byte[] key) {
        int hashOfKey = hashOfKey(key);
        int nodeIndex = hashOfKey / rangePerNode;
        nodeIndex = nodeIndex > totalNodes ? totalNodes : nodeIndex;
        return kineticConnectionPool.getKineticClientByKey(nodeList
                .get(nodeIndex));
    }

    @Override
    public int read(String table, String key, Set<String> fields,
            HashMap<String, ByteIterator> result) {

        Entry entry = null;
        byte[] keyAsBytes = key.getBytes();
        try {
            entry = getKineticClientByKey(keyAsBytes).get(keyAsBytes);
        } catch (KineticException e) {
            return ERROR;
        }

        // does not support fields
        if (entry == null)
            return NOT_FOUND;
        else {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(key, new String(entry.getValue()));
            StringByteIterator.putAllAsByteIterators(result, map);
        }

        return OK;

    }

    @Override
    public int scan(String table, String startkey, int recordcount,
            Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        throw new RuntimeException("Not implemented");
        // return 0;
    }

    @Override
    public int update(String table, String key,
            HashMap<String, ByteIterator> values) {
        return this.insert(table, key, values);
    }

    @Override
    public int insert(String table, String key,
            HashMap<String, ByteIterator> values) {
        Entry entry = new Entry();
        entry.setKey(key.getBytes());
        if (!values.isEmpty())
            entry.setValue(values.values().iterator().next().toArray());

        try {
            getKineticClientByKey(key.getBytes()).put(entry, null,
                    PersistOption.ASYNC);
        } catch (KineticException e) {
            return ERROR;
        }
        return OK;
    }

    @Override
    public int delete(String table, String key) {
        Entry entry = new Entry();
        entry.setKey(key.getBytes());
        try {
            if (getKineticClientByKey(key.getBytes()).delete(entry))
                return OK;
            else
                return ERROR;
        } catch (KineticException e) {
            return ERROR;
        }
    }

    public void cleanup() {
        if (instances.decrementAndGet() == 0) {
            try {
                kineticConnectionPool.destroy();
                System.out.println("Connection pool has been closed.");
            } catch (KineticException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
