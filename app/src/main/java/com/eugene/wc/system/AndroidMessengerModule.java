package com.eugene.wc.system;

import dagger.Module;

@Module(includes = { AndroidSystemModule.class,
                    AndroidWakefulIoExecutorModule.class})
public class AndroidMessengerModule {
}
