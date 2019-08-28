package bthomas.hexmap.common.json;

public class JsonConversionException extends Exception {
    private static final long serialVersionUID = 154596899657000986L;

    public JsonConversionException() {
    }

    public JsonConversionException(String message) {
        super(message);
    }

    public JsonConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonConversionException(Throwable cause) {
        super(cause);
    }

    public JsonConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
