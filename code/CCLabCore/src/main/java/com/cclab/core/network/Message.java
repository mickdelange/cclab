package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Data object representing a network message.
 * <p/>
 * On instantiation, a new id is associated automatically from the internal
 * static sequence. It also provides methods for serialization to/from a byte
 * array.
 * <p/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class Message implements Serializable {

    public static enum Type {
        PING((byte) 0),
        NEWTASK((byte) 1),
        FINISHED((byte) 2);

        private static final Map<Byte, Type> codeLookup = new HashMap<Byte, Type>();
        private static final Map<String, Type> nameLookup = new HashMap<String, Type>();

        static {
            for (Type s : EnumSet.allOf(Type.class)) {
                codeLookup.put(s.getCode(), s);
                nameLookup.put(s.toString(), s);
            }
        }

        private byte code;

        private Type(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static Type get(byte code) {
            return codeLookup.get(code);
        }

        public static Type get(String name) {
            return nameLookup.get(name);
        }
    }

    private static int nextId;

    private int id;
    private byte type;
    private String owner;
    private String details;
    private Object data;

    public Message() {
        this.id = nextId++;
    }

    public Message(Type type, String owner) {
        this.id = nextId++;
        this.type = type.getCode();
        this.owner = owner;
    }

    public byte getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public byte[] toBytes() {
        try {
            // write payload to buffer preceded by size
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            objOut.writeObject(this);
            objOut.flush();
            objOut.close();
            outStream.close();
            return outStream.toByteArray();
        } catch (Exception e) {
            NodeLogger.get().error("Could not pack message " + e.getMessage());
        }
        return null;
    }

    public static Message getFromBytes(byte[] data) {
        Message message = null;
        try {
            ByteArrayInputStream inStream = new ByteArrayInputStream(data);
            ObjectInputStream objIn = new ObjectInputStream(inStream);
            message = (Message) objIn.readObject();
            objIn.close();
            inStream.close();
        } catch (Exception e) {
            NodeLogger.get().error("Cannot read message: " + e.getMessage(), e);
        }
        return message;
    }

    public static Message getFromParts(Map<Integer, byte[]> data) {
        Message message = null;
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            for (int i = 0; i < data.size(); i++) {
                result.write(data.get(i));
            }
            result.flush();
            ByteArrayInputStream inStream = new ByteArrayInputStream(result.toByteArray());
            ObjectInputStream objIn = new ObjectInputStream(inStream);
            message = (Message) objIn.readObject();
            objIn.close();
            inStream.close();
            result.close();
        } catch (Exception e) {
            NodeLogger.get().error("Cannot read message: " + e.getMessage());
        }
        return message;
    }

    @Override
    public String toString() {
        return "Message[" + getId() + "][" + Type.get(type) + "] from " + getOwner() + " (" + getDetails() + ")";
    }
}
