package com.cclab.core.utils;

/**
 * Interface for reacting to a command line instruction.
 * <p/>
 * The command line reader shuts down it the result of the interpretation is
 * false.
 * <p/>
 * Created on 10/22/14 for CCLabCore.
 *
 * @author an3m0na
 */
public interface CLInterpreter {
    public boolean interpretAndContinue(String[] command);
}
