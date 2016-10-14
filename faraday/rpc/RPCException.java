package org.zaproxy.zap.extension.faraday.rpc;

/**
 * Exception representing an error during an RPC call.
 */
public class RPCException extends RuntimeException
{
    /**
     * Creates an instance of the exception with a message explaining the cause.
     *
     * @param message The message explaining the cause of the exception.
     */
    public RPCException(String message)
    {
        super(message);
    }

    /**
     * Creates an instance of the exception with a message explaining the cause and the exception that triggered it.
     *
     * @param message The message explaining the cause of the exception.
     * @param originalCause The exception that triggered this exception.
     */
    public RPCException(String message, Throwable originalCause)
    {
        super(message, originalCause);
    }
}
