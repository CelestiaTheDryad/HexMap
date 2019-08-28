package bthomas.hexmap.common;

/**
 * Indicates code that has not yet been implemented was thrown.
 * Instances of this error should not be able to be thrown in production builds
 */
public class NotImplementedError extends Error {
    private static final long serialVersionUID = 3848187637181302483L;

    public NotImplementedError() {
        super();
    }

    public NotImplementedError(String message) {
        super(message);
    }

    public NotImplementedError(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementedError(Throwable cause) {
        super(cause);
    }

    protected NotImplementedError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
