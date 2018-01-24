package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an Signature.
 *
 * @author Adrian
 **/
public class Signature extends jota.model.Signature {
    private int index;
    private String bundle;

    /**
     * Initializes a new instance of the Signature class.
     */
    public Signature() {
        super();
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
            " \n\taddress:" + getAddress() + ",";

        out += "\n\t[ \n";
        for (String sf : getSignatureFragments()) {
            out += "\n\t" + sf + ",";
        }
        out += " ]";
        out += "\n}";
        return out;
    }
}

