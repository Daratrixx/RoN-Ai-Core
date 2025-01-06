package com.daratrix.ronapi.utils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class FileLogger {

    private final String filePath;
    private final ArrayList<String> buffer = new ArrayList<>();
    private final File file;

    private void ensureDirectoryExists(File file) {
        if (file.exists()) {
            return;
        }
        this.ensureDirectoryExists(file.getParentFile());
        file.mkdirs();
    }

    public FileLogger(String filePath) {
        this.filePath = filePath;
        this.file = new File(this.filePath);
        try {
            this.ensureDirectoryExists(this.file.getParentFile());

            if (this.file.exists()) {
                this.file.delete();
            }

            this.file.createNewFile();
            System.out.println("FileLogger ready: " + this.file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public FileLogger log(String s) {
        synchronized (this.buffer) {
            this.buffer.add(s);
        }

        return this;
    }

    public void flush() {
        synchronized (this.buffer) {
            try {
                FileWriter fr = new FileWriter(this.file, true);
                for (String s : this.buffer) {
                    fr.write(s);
                    fr.write("\r\n");
                }

                fr.close();

                this.buffer.clear();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
