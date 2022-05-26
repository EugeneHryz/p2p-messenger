package com.eugene.wc.protocol.api.data;

import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.sync.Metadata;

public interface MetadataParser {

	WdfDictionary2 parse(Metadata m) throws FormatException;
}
