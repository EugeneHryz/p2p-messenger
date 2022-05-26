package com.eugene.wc.protocol.api.data;

import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.sync.Metadata;

public interface MetadataEncoder {

	Metadata encode(WdfDictionary2 d) throws FormatException;
}
