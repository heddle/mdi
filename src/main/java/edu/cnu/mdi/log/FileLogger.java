package edu.cnu.mdi.log;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An {@link ILogListener} that appends log messages to a file with automatic
 * size-based rotation.
 *
 * <h2>File format</h2>
 * <p>
 * Messages are written as UTF-8 text, one entry per line (or one entry per
 * block of lines for multi-line messages such as stack traces). Each entry
 * is prefixed with an ISO-8601 date-time stamp and a fixed-width level tag:
 * </p>
 * <pre>
 * 2025-04-07T09:14:02 [INFO   ] Application started.
 * 2025-04-07T09:14:02 [CONFIG ] Loaded 195 countries from GeoJSON resource.
 * 2025-04-07T09:14:05 [WARNING] Configuration file not found; using defaults.
 * </pre>
 * <p>
 * Unlike {@link ConsoleLogger}, the file logger uses a full date-time stamp
 * (not just wall-clock time) so that entries from different days can be
 * distinguished in a long-running application.
 * </p>
 *
 * <h2>Rotation</h2>
 * <p>
 * When the log file exceeds {@link #getMaxFileSizeBytes()} the current file
 * is renamed to {@code <name>.1} (overwriting any previous backup) and a
 * fresh file is opened. This keeps exactly two files on disk at all times —
 * the current log and one previous — which is sufficient for most desktop
 * applications. More elaborate rotation strategies can be achieved by
 * subclassing and overriding {@link #rotate()}.
 * </p>
 * <p>
 * The default maximum file size is 5 MB ({@value #DEFAULT_MAX_BYTES} bytes).
 * An alternative limit can be supplied to the
 * {@link #FileLogger(Path, long)} constructor.
 * </p>
 *
 * <h2>Flushing</h2>
 * <p>
 * The writer is flushed after every message. This is slightly less efficient
 * than batched flushing but ensures that no messages are lost if the JVM
 * terminates abnormally.
 * </p>
 *
 * <h2>Registration</h2>
 * <pre>
 * Log.getInstance().addLogListener(new FileLogger(Path.of("app.log")));
 * </pre>
 *
 * <h2>Shutdown</h2>
 * <p>
 * {@code FileLogger} implements {@link Closeable}. In applications that want
 * an orderly close of the underlying file, call {@link #close()} during
 * shutdown:
 * </p>
 * <pre>
 * FileLogger fileLogger = new FileLogger(Path.of("app.log"));
 * Log.getInstance().addLogListener(fileLogger);
 * // ... application runs ...
 * Log.getInstance().removeLogListener(fileLogger);
 * fileLogger.close();
 * </pre>
 * <p>
 * If {@link #close()} is not called the file is closed when the JVM exits via
 * a shutdown hook registered in the constructor, so no messages are silently
 * dropped.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * All write operations are {@code synchronized} on the {@code FileLogger}
 * instance, so it is safe to receive messages from multiple threads
 * simultaneously.
 * </p>
 */
public class FileLogger implements ILogListener, Closeable {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Default maximum log-file size before rotation: 5 MB. */
    public static final long DEFAULT_MAX_BYTES = 5L * 1024L * 1024L;

    /** Date-time formatter for log entry timestamps. */
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Indentation for continuation lines of multi-line messages.
     * Width matches {@code "yyyy-MM-ddTHH:mm:ss [XXXXXXX] "}.
     */
    private static final String CONTINUATION_INDENT =
            "                    " + "           ";
    //       timestamp (19 chars) + " [XXXXXXX] "

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------

    /** Path of the active (current) log file. */
    private final Path logPath;

    /** Path of the single backup file produced during rotation. */
    private final Path backupPath;

    /** Maximum file size in bytes before rotation is triggered. */
    private final long maxFileSizeBytes;

    /**
     * Active writer; {@code null} if the logger has been closed or the
     * file could not be opened.
     */
    private BufferedWriter writer;

    /** {@code true} once {@link #close()} has been called. */
    private boolean closed = false;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Construct a {@code FileLogger} that writes to {@code logPath} and
     * rotates when the file exceeds {@link #DEFAULT_MAX_BYTES}.
     *
     * @param logPath path of the log file to write; the file and any missing
     *                parent directories are created automatically
     * @throws IllegalArgumentException if {@code logPath} is {@code null}
     */
    public FileLogger(Path logPath) {
        this(logPath, DEFAULT_MAX_BYTES);
    }

    /**
     * Construct a {@code FileLogger} that writes to {@code logPath} and
     * rotates when the file exceeds {@code maxFileSizeBytes}.
     *
     * @param logPath         path of the log file to write
     * @param maxFileSizeBytes maximum file size in bytes before rotation;
     *                         must be positive
     * @throws IllegalArgumentException if {@code logPath} is {@code null} or
     *                                  {@code maxFileSizeBytes} is not positive
     */
    public FileLogger(Path logPath, long maxFileSizeBytes) {
        if (logPath == null) {
            throw new IllegalArgumentException("logPath must not be null.");
        }
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException(
                    "maxFileSizeBytes must be positive.");
        }

        this.logPath         = logPath;
        this.backupPath      = buildBackupPath(logPath);
        this.maxFileSizeBytes = maxFileSizeBytes;

        openWriter();
        writeSessionHeader();
        registerShutdownHook();
    }

    // -----------------------------------------------------------------------
    // ILogListener
    // -----------------------------------------------------------------------

    /**
     * Appends an {@link Log.Level#INFO} message to the log file.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void info(String message) {
        write(Log.Level.INFO, message);
    }

    /**
     * Appends a {@link Log.Level#CONFIG} message to the log file.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void config(String message) {
        write(Log.Level.CONFIG, message);
    }

    /**
     * Appends a {@link Log.Level#WARNING} message to the log file.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void warning(String message) {
        write(Log.Level.WARNING, message);
    }

    /**
     * Appends a {@link Log.Level#ERROR} message to the log file.
     *
     * @param message the message; never {@code null}
     */
    @Override
    public void error(String message) {
        write(Log.Level.ERROR, message);
    }

    /**
     * Appends a {@link Log.Level#EXCEPTION} message (stack trace) to the
     * log file.
     *
     * @param message the formatted stack trace; never {@code null}
     */
    @Override
    public void exception(String message) {
        write(Log.Level.EXCEPTION, message);
    }

    // -----------------------------------------------------------------------
    // Closeable
    // -----------------------------------------------------------------------

    /**
     * Flush and close the underlying file writer.
     * <p>
     * After this call the logger silently discards any further messages.
     * The logger should also be unregistered from {@link Log} so it stops
     * receiving messages:
     * </p>
     * <pre>
     * Log.getInstance().removeLogListener(fileLogger);
     * fileLogger.close();
     * </pre>
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeWriter();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the path of the active log file.
     *
     * @return the log file path; never {@code null}
     */
    public Path getLogPath() {
        return logPath;
    }

    /**
     * Returns the maximum file size in bytes before rotation is triggered.
     *
     * @return the rotation threshold in bytes
     */
    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    // -----------------------------------------------------------------------
    // Private — writing
    // -----------------------------------------------------------------------

    /**
     * Format and append a single log entry to the file, rotating first if the
     * file has grown beyond the threshold.
     *
     * @param level   the log level
     * @param message the message text
     */
    private synchronized void write(Log.Level level, String message) {
        if (closed || writer == null) {
            return;
        }

        try {
            if (shouldRotate()) {
                rotate();
            }
            writer.write(format(level, message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            // Last resort: stderr. Avoid recursive logging.
            System.err.println("FileLogger: write failed: " + e.getMessage());
        }
    }

    /**
     * Format a log entry as a single string.
     * <p>
     * The first line receives the full timestamp and level tag. Continuation
     * lines (present in stack traces) are indented to align with the message
     * text.
     * </p>
     *
     * @param level   the log level
     * @param message the raw message text
     * @return the formatted entry, without a trailing newline
     */
    private String format(Log.Level level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String tag       = levelTag(level);
        String prefix    = timestamp + " " + tag + " ";

        String text = message.endsWith("\n")
                ? message.substring(0, message.length() - 1)
                : message;

        String indented = text.replace("\n", "\n" + CONTINUATION_INDENT);
        return prefix + indented;
    }

    /**
     * Returns the fixed-width, bracketed level tag for {@code level}.
     *
     * @param level the log level
     * @return bracketed tag, padded to a uniform width
     */
    private static String levelTag(Log.Level level) {
        return switch (level) {
            case INFO      -> "[INFO   ]";
            case CONFIG    -> "[CONFIG ]";
            case WARNING   -> "[WARNING]";
            case ERROR     -> "[ERROR  ]";
            case EXCEPTION -> "[EXCEPT ]";
        };
    }

    // -----------------------------------------------------------------------
    // Private — rotation
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the log file has grown beyond
     * {@link #maxFileSizeBytes} and should be rotated before the next write.
     *
     * @return {@code true} if rotation is needed
     */
    private boolean shouldRotate() {
        try {
            return Files.exists(logPath)
                    && Files.size(logPath) >= maxFileSizeBytes;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Rotate the log file.
     * <p>
     * The current writer is closed, the active file is moved to the backup
     * path (overwriting any previous backup), and a fresh writer is opened on
     * the original path. Subclasses may override this method to implement more
     * elaborate rotation strategies, such as date-stamped archives or more
     * than one backup generation.
     * </p>
     */
    protected synchronized void rotate() {
        closeWriter();
        try {
            Files.move(logPath, backupPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println(
                    "FileLogger: rotation move failed: " + e.getMessage());
        }
        openWriter();
        writeSessionHeader();
    }

    /**
     * Derive the backup file path from the active log path.
     * <p>
     * For a path such as {@code /home/user/app.log} the backup is
     * {@code /home/user/app.log.1}.
     * </p>
     *
     * @param path the active log path
     * @return the backup path
     */
    private static Path buildBackupPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".1");
    }

    // -----------------------------------------------------------------------
    // Private — writer lifecycle
    // -----------------------------------------------------------------------

    /**
     * Open (or re-open after rotation) the {@link BufferedWriter} on
     * {@link #logPath}, creating the file and any missing parent directories.
     * <p>
     * If the file cannot be opened, {@link #writer} is set to {@code null}
     * and subsequent {@link #write} calls are silently discarded.
     * </p>
     */
    private void openWriter() {
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writer = Files.newBufferedWriter(
                    logPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            writer = null;
            System.err.println(
                    "FileLogger: could not open log file ["
                    + logPath + "]: " + e.getMessage());
        }
    }

    /**
     * Flush and close the current writer, suppressing any {@link IOException}.
     */
    private void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
            } finally {
                writer = null;
            }
        }
    }

    /**
     * Write a session-start separator line so that distinct application runs
     * are clearly visible inside a single growing log file.
     * <p>
     * Example output:
     * </p>
     * <pre>
     * ──────────────────────────────────────────────────────
     * Session started: 2025-04-07T09:14:01
     * ──────────────────────────────────────────────────────
     * </pre>
     */
    private void writeSessionHeader() {
        if (writer == null) {
            return;
        }
        String separator = "─".repeat(56);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        try {
            writer.write(separator);                      writer.newLine();
            writer.write("Session started: " + timestamp); writer.newLine();
            writer.write(separator);                      writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println(
                    "FileLogger: could not write session header: "
                    + e.getMessage());
        }
    }

    /**
     * Register a JVM shutdown hook that closes the writer if
     * {@link #close()} was not called explicitly.
     * <p>
     * This guarantees that buffered messages are flushed even if the
     * application exits without an orderly shutdown sequence.
     * </p>
     */
    private void registerShutdownHook() {
        Thread hook = new Thread(() -> {
            synchronized (FileLogger.this) {
                if (!closed) {
                    closeWriter();
                }
            }
        }, "FileLogger-shutdown-hook");
        hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(hook);
    }
}