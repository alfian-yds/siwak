package com.mosque.prayerclock.server

import com.mosque.prayerclock.data.service.compareAppVersions
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionCompareTest {
    @Test fun angkaDibandingkanLebihDulu() {
        assertEquals(1, compareAppVersions("2.0.0", "1.3"))
        assertEquals(-1, compareAppVersions("1.3", "2.0.0-rc.1"))
        assertEquals(0, compareAppVersions("1.3", "1.3.0"))
        assertEquals(1, compareAppVersions("v2.0.1", "2.0.0"))
    }

    @Test fun praRilisLebihLamaDariRilisFinal() {
        assertEquals(1, compareAppVersions("2.0.0", "2.0.0-rc.1"))
        assertEquals(-1, compareAppVersions("2.0.0-rc.1", "2.0.0"))
        assertEquals(1, compareAppVersions("2.0.0-rc.2", "2.0.0-rc.1"))
        assertEquals(1, compareAppVersions("2.0.0-rc.10", "2.0.0-rc.9"))
        assertEquals(1, compareAppVersions("2.0.0-rc.1", "2.0.0-alpha"))
        assertEquals(0, compareAppVersions("2.0.0-rc.1", "2.0.0-rc.1"))
    }
}
