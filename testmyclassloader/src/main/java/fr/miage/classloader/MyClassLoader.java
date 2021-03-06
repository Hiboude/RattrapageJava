package fr.miage.classloader;

import java.io.File;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.FileInputStream;
import java.util.regex.*;

public class MyClassLoader extends SecureClassLoader {
	private ArrayList<File> path = null;

	public MyClassLoader(ArrayList<File> p) {
		this.path = p;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException{
	    try {
            byte[] b = loadClassData(name);
            return super.defineClass(name, b, 0, b.length);
        } catch(IOException e) {
            e.printStackTrace();
        }
        return super.findClass(name);
	}

	private byte[] loadClassData(String name) throws ClassNotFoundException, IOException {
        File currentFile = path.get(0);
        // Regex pour match
        if(currentFile.getName().endsWith(".jar") || currentFile.getName().endsWith(".xxxjar")) return readJar(name.replace('.', '/'), currentFile);
        if(currentFile.getName().endsWith(".zip") || currentFile.getName().endsWith(".xxxzip")) return readZip(name.replace('.', '/'), currentFile);
        if(currentFile.isDirectory()) return readDirectory(name, currentFile);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                name.replace('.', File.separatorChar) + ".class");
        if(inputStream == null) throw new ClassNotFoundException();
        byte[] buffer ;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int nextValue = 0;
        try {
            while ( (nextValue = inputStream.read()) != -1 ) {
                byteStream.write(nextValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer = byteStream.toByteArray();
        return buffer;
	}

	private File getChildFile(File f) {
	    if(f.isDirectory()) {
	        return getChildFile(f.listFiles()[0]);
        }
        return f.getParentFile();
    }

    private byte[] readJar(String fileName, File currentFile) throws ClassNotFoundException {
        try {
            JarFile jar = new JarFile(currentFile);
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                String name = je.getName();
                if (name.endsWith(".class") && name.contains(fileName)) {
                    String clss = name.replace(".class", "").replaceAll("/", ".");
                    if (this.findLoadedClass(clss) != null) continue;

                    InputStream input = jar.getInputStream(je);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int bufferSize = 4096;
                    byte[] buffer = new byte[bufferSize];
                    int bytesNumRead = 0;
                    while ((bytesNumRead = input.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesNumRead);
                    }
                    byte[] cc = baos.toByteArray();
                    input.close();
                    return cc;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new ClassNotFoundException();
    }

    private byte[] readZip(String fileName, File currentFile) throws ClassNotFoundException {
        try {
            ZipFile zipFile = new ZipFile(currentFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && name.contains(fileName)) {
                    String clss = name.replace(".class", "").replaceAll("/", ".");
                    if (this.findLoadedClass(clss) != null) continue;

                    InputStream input = zipFile.getInputStream(entry);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int bufferSize = 4096;
                    byte[] buffer = new byte[bufferSize];
                    int bytesNumRead = 0;
                    while ((bytesNumRead = input.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesNumRead);
                    }
                    byte[] cc = baos.toByteArray();
                    input.close();
                    return cc;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new ClassNotFoundException();
    }
    private byte[] readDirectory(String fileName, File currentFile) throws ClassNotFoundException {
        String classPath = currentFile.getPath() + File.separatorChar + fileName.replace('.', File.separatorChar) + ".class";
        File file = new File(classPath);
        if(file.exists()) {
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesNumRead = 0;
                while ((bytesNumRead = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesNumRead);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new ClassNotFoundException();
    }
}