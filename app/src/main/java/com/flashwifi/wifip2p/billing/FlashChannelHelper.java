package com.flashwifi.wifip2p.billing;

/**
 * This class takes care of flash channels and their states on the tangle.
 * It starts new threads which check the iota APIs and takes wrape the
 * flash channel
 */

public class FlashChannelHelper {
    // ToDo: replace this by real flash channel
    private String flashChannel;

    private boolean flashChannelRootAttached;
    private boolean flashChannelFundedByHotspot;
    private boolean flashChannelFundedByConsumer;
    private boolean flashChannelSettlementAttached;

    public FlashChannelHelper() {
        flashChannelRootAttached = false;
        flashChannelFundedByHotspot = false;
        flashChannelFundedByConsumer = false;
        flashChannelSettlementAttached = false;
    }

    public boolean checkRootAttached() {
        if (flashChannelRootAttached) {
            return true;
        } else {
            // ToDo: make an API call to check this
            return false;
        }
    }

    public boolean checkFundedByHotspot() {
        if (flashChannelFundedByHotspot) {
            return true;
        } else {
            // ToDo: make an API call to check this
            return false;
        }
    }

    public boolean checkFundedByConsumer() {
        if (flashChannelFundedByConsumer) {
            return true;
        } else {
            // ToDo: make an API call to check this
            return false;
        }
    }

    public boolean checkSettlementAttached() {
        if (flashChannelSettlementAttached) {
            return true;
        } else {
            // ToDo: make an API call to check this
            return false;
        }
    }
}
