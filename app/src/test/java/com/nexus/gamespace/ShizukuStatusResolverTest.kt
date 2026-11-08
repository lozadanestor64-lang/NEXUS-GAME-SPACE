package com.nexus.gamespace

import com.nexus.gamespace.shizuku.PermissionCheck
import com.nexus.gamespace.shizuku.ShizukuDiagnostics
import com.nexus.gamespace.shizuku.ShizukuInputs
import com.nexus.gamespace.shizuku.ShizukuMode
import com.nexus.gamespace.shizuku.ShizukuState
import com.nexus.gamespace.shizuku.ShizukuStatus
import com.nexus.gamespace.shizuku.ShizukuStatusResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuStatusResolverTest {

    private fun inputs(
        installed: Boolean = true, binder: Boolean = true, lost: Boolean = false,
        perm: PermissionCheck? = PermissionCheck.GRANTED, version: Int? = 13,
    ) = ShizukuInputs(installed, binder, lost, perm, version)

    private fun resolve(i: ShizukuInputs) = ShizukuStatusResolver.resolve(i)

    @Test fun notInstalled() =
        assertEquals(ShizukuStatus.NOT_INSTALLED, resolve(inputs(installed = false, binder = false, perm = null, version = null)))

    @Test fun installedButNotRunning() =
        assertEquals(ShizukuStatus.NOT_RUNNING, resolve(inputs(binder = false, perm = null, version = null)))

    @Test fun installedAppAloneIsNeverConnected() {
        val status = resolve(inputs(installed = true, binder = false, perm = PermissionCheck.GRANTED))
        assertFalse(status == ShizukuStatus.CONNECTED)
    }

    @Test fun waitingAuthorization() =
        assertEquals(ShizukuStatus.WAITING_AUTHORIZATION, resolve(inputs(perm = PermissionCheck.NOT_GRANTED)))

    @Test fun denied() = assertEquals(ShizukuStatus.DENIED, resolve(inputs(perm = PermissionCheck.DENIED)))

    @Test fun connected() = assertEquals(ShizukuStatus.CONNECTED, resolve(inputs()))

    @Test fun oldServerVersionIsLimited() =
        assertEquals(ShizukuStatus.CONNECTED_LIMITED, resolve(inputs(version = 10)))

    @Test fun unsupportedIsLimited() =
        assertEquals(ShizukuStatus.CONNECTED_LIMITED, resolve(inputs(perm = PermissionCheck.UNSUPPORTED)))

    @Test fun permissionErrorIsError() =
        assertEquals(ShizukuStatus.ERROR, resolve(inputs(perm = PermissionCheck.ERROR)))

    @Test fun binderLostAfterConnection() =
        assertEquals(ShizukuStatus.CONNECTION_LOST, resolve(inputs(binder = false, lost = true, perm = null)))

    @Test fun recoveredBinderClearsLostState() =
        assertEquals(ShizukuStatus.CONNECTED, resolve(inputs(binder = true, lost = false)))

    @Test fun modeFromUid() {
        assertEquals(ShizukuMode.ROOT, ShizukuMode.fromUid(0))
        assertEquals(ShizukuMode.ADB, ShizukuMode.fromUid(2000))
        assertEquals(ShizukuMode.OTHER, ShizukuMode.fromUid(1234))
    }

    @Test fun canRequestOnlyWhenBinderAliveAndNotGranted() {
        val base = ShizukuState.initial()
        assertFalse(base.canRequestPermission)
        assertTrue(base.copy(binderAlive = true, permission = PermissionCheck.NOT_GRANTED).canRequestPermission)
        assertFalse(base.copy(binderAlive = true, permission = PermissionCheck.GRANTED).canRequestPermission)
        assertFalse(base.copy(binderAlive = false, permission = PermissionCheck.NOT_GRANTED).canRequestPermission)
    }

    @Test fun adbDiagnosticMentionsItIsNotRoot() {
        val state = ShizukuState.initial().copy(status = ShizukuStatus.CONNECTED, mode = ShizukuMode.ADB)
        assertTrue(ShizukuDiagnostics.messages(state).any { it.contains("root") })
    }
}
