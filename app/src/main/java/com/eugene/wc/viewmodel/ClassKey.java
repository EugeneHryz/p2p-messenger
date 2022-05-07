package com.eugene.wc.viewmodel;

import dagger.MapKey;

@MapKey
public @interface ClassKey {

    Class<?> value();
}
