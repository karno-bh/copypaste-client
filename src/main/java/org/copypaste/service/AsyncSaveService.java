package org.copypaste.service;

import com.twmacinta.util.MD5;
import org.copypaste.consts.Global;
import org.copypaste.interthread_data.FileChunkImmutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The "Consumer" of gotten chunks. Implemented as a daemon thread. The thread is defined to be daemon because in the case
 * of failure of the main thread it will need to be signalized somehow that application is about to shutdown. However,
 * if it is blocked on the queue it will never get the signal and process will not be down.<br/>
 * Once the file name is set to this object it creates a temp file.
 * It reads on demand the incoming queue in case there is a work chunk it:
 * <ol>
 * <li>Decode the chunk data by Base64</li>
 * <li>Compare the checksum of the chunk</li>
 * <li>If the chunk check sum is "bad" it drops</li>
 * </ol>
 * Once it has got all the chunks
 * <ol>
 * <li>The file is checked against the overall checksum if it fails it drops and file deleted</li>
 * <li>On success check the file is renamed to real name</li>
 * </ol>
 */
@Service
public class AsyncSaveService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AsyncSaveService.class);

    // Assumption: network is slower than disk => no need big queue
    private final BlockingQueue<FileChunkImmutable> chunks = new ArrayBlockingQueue<>(16);

    private final AtomicBoolean canRun = new AtomicBoolean(true);

    private volatile String fileName;

    private volatile String tempFileName;

    private volatile Throwable throwable;

    private volatile RandomAccessFile tempRandomAccessFile;

    private volatile String fileCheckSum;

    @Override
    public void run() {
        boolean hasNextChunk = true;
        while(canRun.get() && hasNextChunk) {
            try {
                FileChunkImmutable fileChunk = chunks.take();
                requireCheckSum(fileChunk);
                appendChunk(fileChunk.getChunkEncodedContent());
                hasNextChunk = fileChunk.isHasNextChunk();
            } catch (Exception e) {
                log.error("Error while saving file", e);
                throwable = e;
                closeTempFile();
                canRun.set(false);
                break;
            }
        }

        if (throwable != null) {
            return;
        }

        try {
            postProcessingTempFile();
        } catch (Exception e) {
            log.error("Error while post processing temp file", e);
            this.throwable = e;
        }
    }

    private void postProcessingTempFile() {

        Exception t = closeTempFile();

        if (t != null) {
            throw new RuntimeException("Cannot close temp file", t);
        }

        checkFileSum();

        renameToReal();
    }

    public Thread startAndWaitForInput() {
        Thread t = new Thread(this,"Save Service");
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void put(FileChunkImmutable fileChunkImmutable) throws InterruptedException {
        try {
            chunks.put(fileChunkImmutable);
        } catch (InterruptedException e) {
            String message = "Main thread is interrupter on waiting putting to chunk queue";
            log.error(message);
            canRun.set(false);
            throw e;
        }
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5Hash = messageDigest.digest(fileName.getBytes("UTF-8"));
            this.tempFileName = MD5.asHex(md5Hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Check your JDK distribution. Cannot find MD5 digest", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Check your JDK distribution. Cannot find UTF-8 encoding", e);
        }

        try {
            tempRandomAccessFile = new RandomAccessFile(Paths.get(Global.INCOMING_DIRECTORY, tempFileName).toFile(), "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot create temp file");
        }
    }

    public void setFileCheckSum(String fileCheckSum) {
        this.fileCheckSum = fileCheckSum;
    }

    public void clearTemp() {
        closeTempFile();
        File tempFile = Paths.get(Global.INCOMING_DIRECTORY, tempFileName).toFile();
        if (tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                log.warn("Cannot delete temp file {}", tempFile);
            }
        }
    }

    public Exception closeTempFile() {
        try {
            if (tempRandomAccessFile != null) {
                tempRandomAccessFile.close();
                tempRandomAccessFile = null;
            }
        } catch (IOException ignore) {
            log.error("Cannot close temp file");
            return ignore;
        }
        return null;
    }

    private void requireCheckSum(FileChunkImmutable fileChunkImmutable) {
        byte[] chunkEncodedContent = fileChunkImmutable.getChunkEncodedContent();
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Check your JDK distribution. Cannot find MD5 digest", e);
        }
        byte[] md5Hash = messageDigest.digest(chunkEncodedContent);
        String nowMessageHash = MD5.asHex(md5Hash);
        if (!nowMessageHash.equals(fileChunkImmutable.getChunkHexMD5())) {
            throw new RuntimeException("Message digests are not equal for chunk. Expected: " + fileChunkImmutable.getChunkHexMD5() +
             " Got: " + nowMessageHash);
        }
    }

    private void appendChunk(byte[] data) {
        try {
            tempRandomAccessFile.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to temp file", e);
        }
    }

    private void checkFileSum() {
        File tempFile = Paths.get(Global.INCOMING_DIRECTORY, tempFileName).toFile();
        String gotFileMD5;
        try {
            gotFileMD5 = MD5.asHex(MD5.getHash(tempFile));
        } catch (IOException e) {
            throw new RuntimeException("Cannot calculate MD5 checksum", e);
        }

        if (!gotFileMD5.equals(fileCheckSum)) {
            throw new RuntimeException("Checksums are not equal");
        }
    }

    private void renameToReal() {
        File tempFile = Paths.get(Global.INCOMING_DIRECTORY, tempFileName).toFile();
        File newName = Paths.get(Global.INCOMING_DIRECTORY, fileName).toFile();
        boolean renamed = tempFile.renameTo(newName);
        if (!renamed) {
            throw new RuntimeException("Cannot rename temp file: " + tempFileName + " to real file: " + newName);
        }
    }
}
