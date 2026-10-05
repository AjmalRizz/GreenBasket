package util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves and loads lists of objects to files using Java serialization.
 */
public final class FileHandler {

    private static final Logger LOG = Logger.getLogger(FileHandler.class.getName());

    /**
     * Only approved application domain classes and safe Java types may be read back
     * from a data file. Anything else is rejected, preventing deserialization attacks.
     */
    private static final ObjectInputFilter ALLOWED_CLASSES = info -> {
        Class<?> c = info.serialClass();
        if (c == null) return ObjectInputFilter.Status.ALLOWED;
        String name = c.getName();
        if (name.startsWith("model.") ||
            name.equals("java.util.ArrayList") ||
            name.equals("java.util.Date") ||
            name.equals("java.lang.String") ||
            name.equals("java.lang.Enum") ||
            name.equals("java.lang.Number") ||
            name.equals("java.lang.Integer") ||
            name.equals("java.lang.Long") ||
            name.equals("java.lang.Double") ||
            name.equals("java.math.BigDecimal") ||
            name.equals("java.math.BigInteger") ||
            name.startsWith("[")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    };

    private FileHandler() {
    }

    /**
     * Saves the list safely: writes to a temporary file first, then moves it
     * over the destination file in an atomic step. If the process is terminated
     * mid-save, the existing file remains uncorrupted.
     *
     * @throws UncheckedIOException if the file cannot be written
     */
    public static synchronized <T> void saveToFile(List<T> list, String filename) {
        File file = new File(filename).getAbsoluteFile();
        File temp = new File(file.getPath() + ".tmp");
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))) {
            out.writeObject(new ArrayList<>(list));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + filename, e);
        }

        try {
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not replace " + filename, e);
        }
    }

    /**
     * Loads a list from a file. Returns an empty list if the file does not exist.
     *
     * Reads all bytes into memory before deserializing so the file handle is
     * immediately closed. If the file is corrupted or incompatible, it is safely
     * renamed to "*.corrupt-TIMESTAMP" as a recovery backup.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> List<T> loadFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read bytes from " + filename, e);
            return new ArrayList<>();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
             ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bais))) {
            in.setObjectInputFilter(ALLOWED_CLASSES);
            Object data = in.readObject();
            if (data instanceof List<?>) {
                return (List<T>) data;
            }
            throw new InvalidObjectException("File does not contain a list");
        } catch (Exception e) {
            Path backup = file.toPath().resolveSibling(file.getName() + ".corrupt-" + System.currentTimeMillis());
            try {
                Files.move(file.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
                LOG.log(Level.WARNING, "Could not deserialize " + filename + ". Preserved as " + backup.getFileName(), e);
            } catch (IOException moveEx) {
                LOG.log(Level.WARNING, "Could not deserialize " + filename + " or preserve corrupt backup", moveEx);
            }
            return new ArrayList<>();
        }
    }
}
