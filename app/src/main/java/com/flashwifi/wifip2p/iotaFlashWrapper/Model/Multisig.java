package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Multisig {
    private String address;
    private int securitySum;
    private int index;
    private int signingIndex;
    private int security = 2;
    private ArrayList<Multisig> children;
    private ArrayList<Bundle> bundles;

    public Multisig(String address, int securitySum) {
        this.address = address;
        this.securitySum = securitySum;
        this.children = new  ArrayList<Multisig>();
        this.bundles = new  ArrayList<Bundle>();
    }

    public Multisig(String address, int securitySum, ArrayList<Multisig> children) {
        this.address = address;
        this.securitySum = securitySum;
        this.children = children;
        this.bundles = new  ArrayList<Bundle>();
    }

    public Multisig find(String address) {
        if (getAddress().equals(address)) {
            return this;
        } else {
          for (Multisig mult: getChildren()) {
              Multisig result = mult.find(address);
              if (result != null) {
                  return result;
              }
          }
        }
        return null;
    }

    public Multisig clone() {
        Multisig output = new Multisig(this.getAddress(), this.getSecuritySum());

        output.setSecurity(this.getSecurity());
        output.setIndex(this.getIndex());
        output.setSigningIndex(this.getSigningIndex());
        // Copy all bundles
        ArrayList<Bundle> bundleCopy = new ArrayList<>();
        for (Bundle b : this.getBundles()) {
            bundleCopy.add(b.clone());
        }
        output.setBundles(bundleCopy);

        // Copy all children
        ArrayList<Multisig> childrenCopy = new ArrayList<>();
        for (Multisig child : this.getChildren()) {
            childrenCopy.add(child.clone());
        }
        output.setChildren(childrenCopy);
        return output;
    }

    public void push(Multisig addr) {
        children.add(addr);
    }

    public ArrayList<Multisig> getChildren() {
        return children;
    }

    public int getSecuritySum() {
        return securitySum;
    }

    public String getAddress() {
        return address;
    }
    public int getIndex() {
        return index;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setChildren(ArrayList<Multisig> children) {
        this.children = children;
    }

    public ArrayList<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(ArrayList<Bundle> bundles) {
        this.bundles = bundles;
    }

    public int getSigningIndex() {
        return signingIndex;
    }

    public int getSecurity() {
        return security;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setSecurity(int security) {
        this.security = security;
    }

    public void setSecuritySum(int securitySum) {
        this.securitySum = securitySum;
    }

    public void setSigningIndex(int signingIndex) {
        this.signingIndex = signingIndex;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("address", getAddress());
        map.put("securitySum", getSecuritySum());
        map.put("index", getIndex());
        map.put("signingIndex", getSigningIndex());
        map.put("security", security);
        List<Object> childrenList = new ArrayList<Object>();
        for (Multisig ma: children) {
            childrenList.add(ma.toMap());
        }
        map.put("children", childrenList);

        List<Object> bundleList = new ArrayList<Object>();
        for (Bundle b: bundles) {
            bundleList.add(b.toArrayList());
        }
        map.put("bundles", bundleList);

        return map;
    }

    public V8Object toV8Object(V8 engine) {
        return V8ObjectUtils.toV8Object(engine, this.toMap());
    }

    @Override
    public String toString() {
        String out =  "{ \n address':'" + address + "' \n, securitySum:" + securitySum + "\n, signingIndex: " + signingIndex + " \n";
        for (Multisig addr: children) {
            out += addr.toString();
        }
        return out;
    }
}