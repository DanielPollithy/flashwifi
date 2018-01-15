package com.flashwifi.wifip2p.billing;

/**
 * 1) This class keeps the socket connection alive.
 * 2) It tracks the state of the communication.
 * 3) It triggers the creation of bills
 * 4) It sends the bills and flash objects to the peer
 * 5) It watches the time and deadlines
 * 6)
 */

public class BillingServer {
    private State state = State.NOT_PAIRED;

    public BillingServer(int bookedMegabytes, int timeoutMinutes){
        Accountant.getInstance().start(bookedMegabytes,timeoutMinutes);
    }

    public void start() {
        // 0) create deadline guard
        createDeadlineGuard();
        // 1) create a socket

        while (state != State.ERROR || state != State.FULLY_ATTACHED) {
            // 2) accept a connection
            if (state == State.NOT_PAIRED) {
                // 3) pair client and hotspot and synchronize states
            }
            while (state == State.ROAMING) {
                // 4) sleep(60)
                // 5) createNewBill
                // 6) send the bill and the signed flash object
            }
            if (state == State.CLOSED) {
                // 7) close the channel and sign the final settlement
                // 8) receive the signed settlement from client
            }
            while (state == State.CLOSED) {
                // 9) reattach and rebroadcast the final transaction
            }
            if (state == State.FULLY_ATTACHED) {
                // DONE!
            }
        }
    }

    private void createDeadlineGuard() {
        // this method measures the time and stops the connection if
        // nothing happened after <timeoutMinutes>
    }

    enum State {
        NOT_PAIRED,
        INITIAL,
        ROAMING,
        CLOSED,
        FULLY_ATTACHED,
        ERROR
    }
}
