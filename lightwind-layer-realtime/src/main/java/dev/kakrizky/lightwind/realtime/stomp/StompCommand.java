package dev.kakrizky.lightwind.realtime.stomp;

public enum StompCommand {
    // Client commands
    CONNECT,
    STOMP,
    SUBSCRIBE,
    UNSUBSCRIBE,
    SEND,
    DISCONNECT,
    ACK,
    NACK,

    // Server commands
    CONNECTED,
    MESSAGE,
    RECEIPT,
    ERROR
}
