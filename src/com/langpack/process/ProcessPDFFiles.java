package com.langpack.process;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.PDFContentReader;

public class ProcessPDFFiles {
    public static final Logger logger = LogManager.getLogger("ProcessPDFFiles");
    
    public static void main(String[] args) {
        String encoding = System.getProperty("file.encoding");
        System.out.println("Default File Encoding: " + encoding);

        if (args.length == 0) {
            logger.error("No file path provided");
            return;
        }

        logger.info("Classpath={}", System.getProperty("java.class.path"));
        logger.info("*******************************");

        String sourceDirPath = args[0];
        sourceDirPath = "C:\\BooksRepo\\SORTED\\A\\";
        
        String targetDirPath = args[0];
        targetDirPath = "C:\\BooksRepo\\Text\\A\\";
        
        File dir = new File(sourceDirPath);

        String targetFileId = "67710"; // Extract fileId from targetFileName if needed

        int count = 0;
        if (dir.isDirectory()) {
            logger.info("Directory as expected ..");
            File[] dirFiles = dir.listFiles();

            int totalCount = dirFiles.length;
            
            if (dirFiles != null) {
                for (File item : dirFiles) {
                    String name = item.getName();
                    String fileId = name.substring(0, 5);
                    String baseFileName = "67710_Export";
                    
                    String exportFileName = targetDirPath + baseFileName + "." + "txt";
                    File exportFile = new File (exportFileName);

                    logger.info("[{}/{}] {} +++ {}", count, totalCount, fileId, name);

                    if (targetFileId.equals(fileId)) {
                    	item.getName();
                        logger.info("Found target file: {}", name);

                        PDFContentReader instance = new PDFContentReader(count, item, exportFile);
                        String content = instance.readContent();
                        instance.analyzeTextFollowingDashes(content);
                        logger.info("Content: {}", content);
                        System.exit(0);
                    }
                    count++;
                }
            }
        } else {
            logger.error("Provided path is not a directory");
        }
    }
}
