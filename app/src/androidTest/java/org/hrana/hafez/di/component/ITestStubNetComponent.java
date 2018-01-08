package org.hrana.hafez.di.component;

import org.hrana.hafez.di.module.StubNetModule;
import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.net.ReportAttachmentChainTest;

import dagger.Component;

/**
 * Test component
 */


@Component(modules = {StubNetModule.class}, dependencies = {IApplicationComponent.class})
@ReportScope
public interface ITestStubNetComponent {

    void inject(ReportAttachmentChainTest test);
}

