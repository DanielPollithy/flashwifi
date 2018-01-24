package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

public class FlashLibJSException extends Exception {

    public enum FlashLibJSExceptionType {
        INSUFFICIENT_FUNDS, UNKNOWN
    }


    private FlashLibJSExceptionType type = FlashLibJSExceptionType.UNKNOWN;

    public FlashLibJSException(String string) {
        if (string.contains(FlashLibJSExceptionType.INSUFFICIENT_FUNDS.name())) {
            this.type = FlashLibJSExceptionType.INSUFFICIENT_FUNDS;
        }
    }

    public FlashLibJSExceptionType getType() {
        return type;
    }
}
