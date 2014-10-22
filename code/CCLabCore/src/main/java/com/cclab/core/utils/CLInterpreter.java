package com.cclab.core.utils;

/**
 * Created by ane on 10/22/14.
 */
public interface CLInterpreter {
    //return false if should not continue reading
    public boolean interpretAndContinue(String[] command);
}
