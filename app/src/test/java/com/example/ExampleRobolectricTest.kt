package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Air Kopi", appName)
    }

    @Test
    fun `verify no duplicate water log on same UUID insertion`() = runBlocking {
        val waterDao = db.waterLogDao()
        val id = "test-water-uuid-123"

        val initialLog = WaterLog(id = id, amountMl = 250, dateString = "2026-08-01", isSynced = false)
        waterDao.insertWaterLog(initialLog)

        // Verify initial insertion
        var unsynced = waterDao.getUnsyncedWaterLogs()
        assertEquals(1, unsynced.size)
        assertEquals(250, unsynced[0].amountMl)

        // Re-insert with same primary key UUID (simulating duplicate/re-save)
        val duplicateLog = WaterLog(id = id, amountMl = 300, dateString = "2026-08-01", isSynced = false)
        waterDao.insertWaterLog(duplicateLog)

        // Verify OnConflictStrategy.REPLACE kept total count at 1 without creating duplicate
        unsynced = waterDao.getUnsyncedWaterLogs()
        assertEquals(1, unsynced.size)
        assertEquals(300, unsynced[0].amountMl)

        // Verify marking as synced prevents duplicate future syncs
        waterDao.markWaterLogsAsSynced(listOf(id))
        unsynced = waterDao.getUnsyncedWaterLogs()
        assertTrue(unsynced.isEmpty())
    }

    @Test
    fun `verify no duplicate coffee log on same UUID insertion`() = runBlocking {
        val coffeeDao = db.coffeeLogDao()
        val id = "test-coffee-uuid-456"

        val initialLog = CoffeeLog(id = id, coffeeType = "Espresso", caffeineMg = 63, dateString = "2026-08-01", isSynced = false)
        coffeeDao.insertCoffeeLog(initialLog)

        // Verify initial insertion
        var unsynced = coffeeDao.getUnsyncedCoffeeLogs()
        assertEquals(1, unsynced.size)

        // Re-insert with same primary key UUID
        val duplicateLog = CoffeeLog(id = id, coffeeType = "Espresso Double", caffeineMg = 126, dateString = "2026-08-01", isSynced = false)
        coffeeDao.insertCoffeeLog(duplicateLog)

        // Verify total count remains 1
        unsynced = coffeeDao.getUnsyncedCoffeeLogs()
        assertEquals(1, unsynced.size)
        assertEquals("Espresso Double", unsynced[0].coffeeType)

        // Verify sync mark
        coffeeDao.markCoffeeLogsAsSynced(listOf(id))
        unsynced = coffeeDao.getUnsyncedCoffeeLogs()
        assertTrue(unsynced.isEmpty())
    }
}

