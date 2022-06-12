package com.eugene.wc.work;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;

@Module
public abstract class WorkModule {

    @Binds
    @IntoMap
    @ClassKey(TransportKeyRotationWork.class)
    public abstract ChildWorkerFactory bindKeyRotationWorkerFactory(TransportKeyRotationWork.Factory factory);
}
