package com.eap09.reservas.acceptanceTest;

import com.intuit.karate.junit5.Karate;

class AcceptanceTestRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run().relativeTo(getClass());
    }

    @Karate.Test
    Karate testIdentityAccess() {
        return Karate.run("identityaccess/Authentication.feature", "identityaccess/Registration.feature",
                "identityaccess/UpdateProfile.feature", "provider/GeneralSchedule.feature").relativeTo(getClass());
    }
}
