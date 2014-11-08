package com.cclab.core.utils;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;

/**
 * Created on 11/8/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class DummyAwsInstance extends Instance {
    String name;

    public DummyAwsInstance(String name) {
        this.name = name;
    }

    @Override
    public String getInstanceId() {
        return name;
    }


    @Override
    public InstanceState getState() {
        InstanceState state = new InstanceState();
        state.setName("running");
        return state;
    }
}
