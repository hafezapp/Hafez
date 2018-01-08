package org.hrana.hafez.di.module;

import dagger.Module;

/**
 * Test module
 */
@Module
public class TestNetModule extends NetModule {

    public TestNetModule(String baseUrl) {
        super(baseUrl);
    }

}
