package com.iah.nutrition.seed;

import com.iah.nutrition.data.RpgDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final RpgDataStore store;

    public DemoDataLoader(RpgDataStore store) {
        this.store = store;
    }

    @Override
    public void run(String... args) {
        store.seedIfEmpty();
        log.info("Local RPG data simulator seeded");
    }
}
