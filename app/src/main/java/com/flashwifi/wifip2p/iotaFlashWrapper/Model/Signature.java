package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an Signature.
 *
 * @author Adrian
 **/
public class Signature {
    private int index;
    private String bundle;
    private String address;
    private List<String> signatureFragments;

    /**
     * Initializes a new instance of the Signature class.
     */
    public Signature() {
        this.signatureFragments = new ArrayList<>();
    }

    /**
     * Get the address.
     *
     * @return The address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set the address.
     *
     * @param address The address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get the signatureFragments.
     *
     * @return The signatureFragments.
     */
    public List<String> getSignatureFragments() {
        return signatureFragments;
    }

    /**
     * Set the signatureFragments.
     *
     * @param signatureFragments The signatureFragments.
     */
    public void setSignatureFragments(List<String> signatureFragments) {
        this.signatureFragments = signatureFragments;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    @Override
    public String toString() {
        String out = "{ \n" +
            " \tindex:" + index + ", " +
                " \n\tbundle: " + bundle + ", " +
            " \n\taddress:" + address + ",";

        out += "\n\t[ \n";
        for (String sf : signatureFragments) {
            out += "\n\t" + sf + ",";
        }
        out += " ]";
        out += "\n}";
        return out;
    }
}

