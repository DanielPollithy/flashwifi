package com.flashwifi.wifip2p;

public class AddressBalanceTransfer {

    private String depositAddress;
    private String balance;
    private String message;

    public AddressBalanceTransfer(String inDepositAddress, String inBalance, String inMessage){
        depositAddress = inDepositAddress;
        balance = inBalance;
        message = inMessage;
    }

    public String getDepositAddress() {
        return depositAddress;
    }

    public void setDepositAddress(String depositAddress) {
        this.depositAddress = depositAddress;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}