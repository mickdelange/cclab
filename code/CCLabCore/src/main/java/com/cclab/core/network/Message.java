package com.cclab.core.network;

import com.cclab.core.NodeLogger;

import java.io.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ane on 10/19/14.
 */
public class Message implements Serializable {

    public static enum Type {
        LOADINPUT((byte) 1),
        LOADOUTPUT((byte) 2);

        private static final Map<Byte, Type> lookup = new HashMap<Byte, Type>();

        static {
            for (Type s : EnumSet.allOf(Type.class))
                lookup.put(s.getCode(), s);
        }

        private byte code;

        private Type(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static Type get(byte code) {
            return lookup.get(code);
        }
    }

    private byte type;
    private String owner;
    private String details;
    private Object data;

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

    public byte[] toBytes() {
        try {
            // write load to buffer preceded by size
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
            NodeLogger.get().error("Cannot read message: " + e.getMessage());
        }
        return message;
    }

    @Override
    public String toString(){
        return "Message["+ Type.get(type)+"] from "+getOwner()+" ("+getDetails()+")";
    }
}
