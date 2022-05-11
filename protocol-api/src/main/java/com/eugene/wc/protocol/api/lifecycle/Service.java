package com.eugene.wc.protocol.api.lifecycle;

import com.eugene.wc.protocol.api.lifecycle.exception.ServiceException;

public interface Service {

    void startService() throws ServiceException;

    void stopService() throws ServiceException;
}
