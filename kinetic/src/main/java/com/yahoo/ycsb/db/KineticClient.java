package com.yahoo.ycsb.db;

import com.yahoo.ycsb.ByteArrayByteIterator;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import kinetic.admin.AdminClientConfiguration;
import kinetic.admin.KineticAdminClient;
import kinetic.admin.KineticAdminClientFactory;
import kinetic.client.ClientConfiguration;
import kinetic.client.Entry;
import kinetic.client.KineticClientFactory;
import kinetic.client.KineticException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class KineticClient extends DB {
    private static final int OK = 0;
    private static final int ERROR = 1;
    private static final AtomicInteger initCount = new AtomicInteger(0);
    private kinetic.client.KineticClient client;

    /**
     * Helper method for converting a String -> ByteIterator map into a byte array.
     * Kinetic doesn't natively support any concept of columns so this helper
     * makes it easy to correctly implement DB
     *
     * @param values
     * @return
     * @throws IOException
     */
    private static byte[] serializeMapToBytes(Map<String, ByteIterator> values) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        HashMap<String, byte[]> serializableValues = new HashMap<String, byte[]>();


        for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
            serializableValues.put(entry.getKey(), entry.getValue().toArray());
        }


        oos.writeObject(serializableValues);

        return baos.toByteArray();
    }

    /**
     * Reverse of serialzeMapToBytes
     *
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private static HashMap<String, ByteIterator> deserializeBytesToMap(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));

        HashMap<String, byte[]> map = (HashMap<String, byte[]>) ois.readObject();

        HashMap<String, ByteIterator> result = new HashMap<String, ByteIterator>();

        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            result.put(entry.getKey(), new ByteArrayByteIterator(entry.getValue()));
        }

        return result;
    }

    @Override
    public synchronized void init() throws DBException {
        initCount.incrementAndGet();
        if (client != null) {
            return;
        }


        AdminClientConfiguration clientConfiguration = new AdminClientConfiguration();
        clientConfiguration.setHost(getProperties().getProperty("host", "127.0.0.1"));
        clientConfiguration.setPort(Integer.parseInt(getProperties().getProperty("port", "8123")));
        clientConfiguration.setUseNio(Boolean.parseBoolean(getProperties().getProperty("nio", "true")));
        clientConfiguration.setUseSsl(Boolean.parseBoolean(getProperties().getProperty("ssl", "false")));

        try {
            if (Boolean.parseBoolean(getProperties().getProperty("instantSecureErase", "false"))) {
                KineticAdminClient kineticAdminClient = KineticAdminClientFactory.createInstance(clientConfiguration);
                kineticAdminClient.setup(null, null, 0, true);
                kineticAdminClient.close();
            }

            client = KineticClientFactory.createInstance(clientConfiguration);
        } catch (KineticException e) {
            throw new DBException(e);
        }
    }

    @Override
    public synchronized void cleanup() throws DBException {
        if (initCount.decrementAndGet() <= 0) {
            try {
                client.close();
            } catch (KineticException e) {
                throw new DBException(e);
            }
            client = null;
        }
    }

    @Override
    public int read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
        try {
            Entry entry = client.get(key.getBytes());

            if (entry == null) {
                return ERROR;
            }

            result.putAll(deserializeBytesToMap(entry.getValue()));

            return OK;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return ERROR;
        }
    }

    @Override
    public int scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        try {
            Entry firstEntry = client.get(startkey.getBytes());

            if (firstEntry == null) {
                return ERROR;
            }

            result.add(deserializeBytesToMap(firstEntry.getValue()));

            byte[] key = startkey.getBytes();
            while (recordcount > 0) {
                Entry next = client.getNext(key);
                if (next == null) {
                    return ERROR;
                }

                result.add(deserializeBytesToMap(next.getValue()));

                recordcount--;

                key = next.getKey();
            }

            return OK;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return ERROR;
        }

    }

    @Override
    public int update(String table, String key, HashMap<String, ByteIterator> values) {
        // Kinetic doesn't distinguish between update and insert so just call the insert logic
        return insert(table, key, values);
    }

    @Override
    public int insert(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            byte[] value = serializeMapToBytes(values);
            client.put(new Entry(key.getBytes(), value), null);
            return OK;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return ERROR;
        }
    }

    @Override
    public int delete(String table, String key) {
        try {
            return client.deleteForced(key.getBytes()) ? OK : ERROR;
        } catch (KineticException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return ERROR;
        }
    }
}
