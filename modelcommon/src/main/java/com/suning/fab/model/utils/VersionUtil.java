package com.suning.fab.model.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VersionUtil {
	private static ConcurrentHashMap<String, String>  jarVersion = new ConcurrentHashMap<String, String>();
	private static final String versionFileName = "pom.properties";

	private VersionUtil() {
		throw new IllegalStateException("VersionUtil class");
	}

	public static String getJarVersion(Class<?> cls) {
		String dir = cls.getProtectionDomain().getCodeSource().getLocation().getPath();
		dir = dir + "META-INF/maven";
		if(null == jarVersion.get(dir)) {
			recursionSearch4GetVersion(dir, dir);
		}
		return jarVersion.get(dir);
	}

	private static void recursionSearch4GetVersion(final String dir, final String key) {
		File fileDir = new File(dir);
		if (fileDir.exists()) {
			File[] files = fileDir.listFiles();
			if (files.length != 0) {
				for (File file : files) {
					if (file.isDirectory()) {
						recursionSearch4GetVersion(file.getAbsolutePath(), key);
					} else {
						String fullPathName = file.getAbsolutePath();
						if(-1 != fullPathName.indexOf(versionFileName)) {
							Properties prop = new Properties();
							try {
								BufferedReader bufferedReader = new BufferedReader(new FileReader(fullPathName));
								prop.load(bufferedReader);
								String val = /*prop.getProperty("groupId") + "." + prop.getProperty("artifactId") + "-" +*/ prop.getProperty("version");
								jarVersion.put(key,  val);
							} catch (IOException e) {
							}
							return;
						}
					}
				}
			}
		}
	}	
}
