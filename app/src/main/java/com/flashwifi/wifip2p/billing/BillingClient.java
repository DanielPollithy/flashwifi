package com.flashwifi.wifip2p.billing;

/**
 * 1) This class keeps the socket connection alive.
 * 2) It tracks the state of the communication.
 * 3) It receives new bills and flash objects sent by the server
 * 4) It approves the changes
 * 5) It watches the connectivity (no internet -> no money)
 */

public class BillingClient {
    private State state = State.NOT_PAIRED;

    public BillingClient(){}

    enum State {
        NOT_PAIRED,
        INITIAL,
        ROAMING,
        CLOSED,
        FULLY_ATTACHED
    }
}
