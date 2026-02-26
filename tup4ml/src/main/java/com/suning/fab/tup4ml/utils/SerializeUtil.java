package com.suning.fab.tup4ml.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SerializeUtil {
	private SerializeUtil() {
		throw new IllegalStateException("SerializeUtil class");
	}
	
	/**
	 * 将对象转化为字节流数组；
	 * @param object 要写成流的对象；
	 * @return 字节流数组；
	 * @throws IOException
	 */
	public static byte[] objectToBytes(Object object) throws IOException {
        if (object == null) {  
            throw new NullPointerException("byte[] objectToBytes(Object)");  
        }  
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		byte[] bytes = baos.toByteArray();
		baos.close();
		oos.close();
		return bytes;
	}

	/**
	 * 将字节流数组转化为对象；
	 * @param bytes 字节流数组；
	 * @return 生成的对象；
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException	{
        if (bytes == null) {  
            throw new NullPointerException("<T> T bytesToObject(byte[])");  
        }  
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object object = ois.readObject();
		bais.close();
		ois.close();
		return (T) object;
	}
	
	/**
	 *  将列表对象转化为字节流数组；
	 * @param value 要写成流的列表对象；
	 * @return 字节流数组；
	 * @throws IOException
	 */
    public static <T> byte[] objectListToBytes(List<T> value) throws IOException {  
        if (value == null) {  
            throw new NullPointerException("byte[] objectListToBytes(List<T>)");  
        }    
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
        for(T obj : value){  
        	oos.writeObject(obj);  
        }
        oos.writeObject(null);  
		byte[] bytes = baos.toByteArray();
		baos.close();
		oos.close();
		return bytes;
    }

    /**
     * 将字节流数组转化为列表对象；
     * @param bytes 字节流数组；
     * @return 生成的列表对象；
     */
	@SuppressWarnings("unchecked")
    public static <T> List<T> bytesToObjectList(byte[] bytes)  throws IOException, ClassNotFoundException {  
        if (bytes == null) {  
            throw new NullPointerException("<T> List<T> bytesToObjectList(byte[])");  
        }  
        List<T> list = new ArrayList<T>();  
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
        while (true) {  
            T obj = (T) ois.readObject();  
            if(obj == null){  
                break;  
            }else{  
                list.add(obj);  
            }  
        }  
		bais.close();
		ois.close();
		return list;
    }  
}
