package com.minexcoin.atomic_swap.gui;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.bitcoinj.core.Coin;

final public class FormValidator {
    /**
     * Try to get valid inet socket address.
     * 
     * @param host
     * @param port
     * 
     * @throws IllegalArgumentException
     * 
     * @return InetSocketAddress
     */
    public static InetSocketAddress getSocketAddress(String host, String port) throws IllegalArgumentException {
        try {
            return new InetSocketAddress(InetAddress.getByName(host), Integer.valueOf(port));
        } catch (UnknownHostException | NumberFormatException e) {
            throw new IllegalArgumentException("Socket " + host + ":" + port + " is unknown.");
        }
    }
    
    /**
     * Try to get RPC credentials.
     * 
     * @param paramName
     * @param paramValue
     * 
     * @throws IllegalArgumentException
     * 
     * @return String
     */
    public static String getCredential(String paramName, String paramValue) throws IllegalArgumentException {
        if (paramValue.isEmpty()) {
            throw new IllegalArgumentException("Credential '" + paramName + "' is empty.");
        }
        
        return paramValue;
    }
            
    /**
     * Try to get port number.
     * 
     * @param paramName
     * @param paramValue
     * 
     * @throws IllegalArgumentException
     * 
     * @return Integer
     */
    public static Integer getPortNumber(String paramName, String paramValue) throws IllegalArgumentException {
        Integer port = Integer.valueOf(paramValue);
        
        if (port < 1) {
            throw new IllegalArgumentException(paramName + " is wrong");
        }
        
        return port;
    }
    
    /**
     * Try to get private key.
     * 
     * @param paramName
     * @param paramValue
     * 
     * @throws IllegalArgumentException
     * 
     * @return BigInteger
     */
    public static BigInteger getPrivateKey(String paramName, String paramValue) throws IllegalArgumentException {
        if (paramValue.isEmpty()) {
            throw new IllegalArgumentException(paramName + " is empty.");
        }
        
        return new BigInteger(paramValue, 16);
    }
    
    /**
     * Try to get private key.
     * 
     * @param paramName
     * @param paramValue
     * 
     * @throws IllegalArgumentException
     * 
     * @return byte[]
     */
    public static byte[] getPublicKey(String paramName, String paramValue) throws IllegalArgumentException {
        if (paramValue.isEmpty()) {
            throw new IllegalArgumentException(paramName + " is empty.");
        }
        
        return new BigInteger(paramValue, 16).toByteArray();
    }
    
    /**
     * Try to amount.
     * 
     * @param paramName
     * @param paramValue
     * 
     * @throws IllegalArgumentException
     * 
     * @return Coin
     */
    public static Coin getAmount(String paramName, String paramValue) throws IllegalArgumentException {
        if (paramValue.isEmpty()) {
            throw new IllegalArgumentException(paramName + " is empty.");
        }
        
        Coin coins = Coin.valueOf(Integer.valueOf(paramValue));
        
        if (coins.isZero() || coins.isNegative()) {
            throw new IllegalArgumentException(paramName + " should be greater than 0.");
        }
        
        return coins;
    }
    
    /**
     * Try to confirmations.
     * 
     * @param paramName
     * @param paramValue
     * 
     * @throws IllegalArgumentException
     * 
     * @return Integer
     */
    public static Integer getConfirmations(String paramName, String paramValue) throws IllegalArgumentException {
        if (paramValue.isEmpty()) {
            throw new IllegalArgumentException(paramName + " is empty.");
        }
        
        Integer confirmations = Integer.valueOf(paramValue);
        
        if (confirmations < 0) {
            throw new IllegalArgumentException(paramName + " is invalid.");
        }
        
        return confirmations;
    }
}
