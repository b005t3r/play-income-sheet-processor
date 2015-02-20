package com.bytestorm.isp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class Utils {
    
    public static String unpack(File zipFile, File outFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            if (!entries.hasMoreElements()) {
                throw new IOException("Zip with one file expected (empty found)");
            }
            ZipEntry entry = entries.nextElement();
            if (entries.hasMoreElements()) {
                throw new IOException("Zip with one file expected (more entries found)");
            }
            Log.v("Unpacking report file " + entry.getName() + " from file " + zipFile.getName());
            try (InputStream in = zip.getInputStream(entry)) {
                try (OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[64 * 1024];
                    for (int read = 0; (read = in.read(buffer)) > 0; ) {
                        out.write(buffer, 0, read);
                    }
                }
            }
            return entry.getName();
        }
    }
    
    public static String unpack(InputStream zipStream, File outFile) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(zipStream)) {
            ZipEntry entry = zip.getNextEntry();
            String origName;
            if (null == entry) {
                throw new IOException("Zip with one file expected (empty found)");
            }            
            origName = entry.getName();
            Log.v("Unpacking report file " + entry.getName() + " from zip stream");
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[64 * 1024];
                for (int read = 0; (read = zip.read(buffer)) > 0; ) {
                    out.write(buffer, 0, read);
                }
            }
            if (null != zip.getNextEntry()) {
                throw new IOException("Zip with one file expected (more entries found)");
            }
            return origName;
        }
    }
    
    private Utils() {
    }
}
