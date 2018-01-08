package org.hrana.hafez.di.component;

import org.hrana.hafez.di.module.TestNetModule;
import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.net.ReportAttachmentChainTest;
import org.hrana.hafez.service.ApiEndpointTest;

import dagger.Component;

/**
 * Test component
 */


@Component(modules = {TestNetModule.class}, dependencies = {IApplicationComponent.class})
@ReportScope
public interface ITestNetComponent {

    void inject(ApiEndpointTest test);
    void inject(ReportAttachmentChainTest test);
}
