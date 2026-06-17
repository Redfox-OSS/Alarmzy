package dev.redfox.alarmzy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.redfox.alarmzy.data.local.converter.Converters
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import dev.redfox.alarmzy.data.local.dao.GroupDao
import dev.redfox.alarmzy.data.local.entity.AlarmEntity
import dev.redfox.alarmzy.data.local.entity.GroupEntity

@Database(
    entities = [AlarmEntity::class, GroupEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AlarmzyDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun groupDao(): GroupDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `groups` ADD COLUMN `color` TEXT DEFAULT NULL")
            }
        }
    }
}
