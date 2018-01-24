package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

public class CreateTransactionHelperObject {
    private int generate = 0;
    private Multisig address;

    public CreateTransactionHelperObject(int gen, Multisig addr) {
        this.generate = gen;
        this.address = addr;
    }

    public int getGenerate() {
        return generate;
    }

    public Multisig getAddress() {
        return address;
    }

    public void setAddress(Multisig address) {
        this.address = address;
    }
}