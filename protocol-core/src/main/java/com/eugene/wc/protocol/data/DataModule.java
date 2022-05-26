package com.eugene.wc.protocol.data;

import com.eugene.wc.protocol.api.data.MetadataEncoder;
import com.eugene.wc.protocol.api.data.MetadataParser;

import dagger.Module;
import dagger.Provides;

@Module
public class DataModule {

    @Provides
    public MetadataParser provideMetadataParser(MetadataParserImpl parser) {
        return parser;
    }

    @Provides
    public MetadataEncoder provideMetadataEncoder(MetadataEncoderImpl encoder) {
        return encoder;
    }
}
