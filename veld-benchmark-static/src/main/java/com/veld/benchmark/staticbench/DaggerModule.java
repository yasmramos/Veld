package com.veld.benchmark.staticbench;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DaggerModule {
    
    @Provides
    @Singleton
    static StaticService provideStaticService() {
        return new StaticService();
    }
}

@Singleton
@Component(modules = DaggerModule.class)
interface BenchmarkComponent {
    StaticService staticService();
}
