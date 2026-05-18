package com.eap09.reservas.acceptanceTest;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.intuit.karate.junit5.Karate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class AcceptanceTestRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run().relativeTo(getClass());
    }

    @Karate.Test
    Karate testIdentityAccess() {
        return Karate.run("identityaccess/Authentication.feature").relativeTo(getClass());
    }
}
