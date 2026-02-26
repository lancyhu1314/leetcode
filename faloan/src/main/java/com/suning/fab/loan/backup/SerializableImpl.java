package com.suning.fab.loan.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.suning.fab.tup4j.utils.LoggerUtil;


public class SerializableImpl implements Serializable {

	@Override
    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        try {
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } finally {
            oos.close();
        }
    }
	
	@Override
    @SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] data, Class<T> clazz) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bin);

        try {
            Object obj = ois.readObject();
            return (T) obj;
        } catch (ClassNotFoundException e) {
        	LoggerUtil.error("ObjectInputStream类型不存在{}", e);
        	return (T) new Object();
		} finally {
            ois.close();
        }
    }
}
